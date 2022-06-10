/*
 * Copyright (C) 2013 Keisuke SUZUKI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * Distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.physicaloid.lib.usb.driver.uart;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbRequest;
import android.util.Log;
import com.physicaloid.BuildConfig;
import com.physicaloid.lib.Physicaloid;
import com.physicaloid.lib.UsbVidList;
import com.physicaloid.lib.framework.SerialCommunicator;
import com.physicaloid.lib.usb.UsbCdcConnection;
import com.physicaloid.lib.usb.UsbVidPid;
import com.physicaloid.misc.RingBuffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class UartCdcAcm extends SerialCommunicator {

        private static final String TAG = UartCdcAcm.class.getSimpleName();
        private boolean DEBUG_SHOW = false;
        private static final int DEFAULT_BAUDRATE = 9600;
        private UsbCdcConnection mUsbConnetionManager;
        private UartConfig mUartConfig;
        private static final int RING_BUFFER_SIZE = 1024;
        private static final int USB_READ_BUFFER_SIZE = 256;
        private static final int USB_WRITE_BUFFER_SIZE = 256;
        private RingBuffer mBuffer;
        private boolean mReadThreadStop = true;
        private UsbDeviceConnection mConnection;
        private UsbEndpoint mEndpointIn;
        private UsbEndpoint mEndpointOut;
        private int mInterfaceNum;
        private boolean isOpened;
        private byte[] wbuf = new byte[USB_WRITE_BUFFER_SIZE];
        private final Object DevLock = new Object();

        public UartCdcAcm(Context context) {
                super(context);
                mUsbConnetionManager = new UsbCdcConnection(context);
                mUartConfig = new UartConfig();
                mBuffer = new RingBuffer(RING_BUFFER_SIZE);
                isOpened = false;
        }

        @Override
        public boolean open() {
                for(UsbVidList id : UsbVidList.values()) {
                        if(open(new UsbVidPid(id.getVid(), 0))) {
                                return true;
                        }
                }
                return false;
        }

        public boolean open(UsbVidPid ids) {

                if(mUsbConnetionManager.open(ids, true)) {
                        mConnection = mUsbConnetionManager.getConnection();
                        mEndpointIn = mUsbConnetionManager.getEndpointIn();
                        mEndpointOut = mUsbConnetionManager.getEndpointOut();
                        mInterfaceNum = mUsbConnetionManager.getCdcAcmInterfaceNum();
                        if(!init()) {
                                return false;
                        }
                        if(!setBaudrate(DEFAULT_BAUDRATE)) {
                                return false;
                        }
                        mBuffer.clear();
                        startRead();
                        isOpened = true;
                        return true;
                }
                return false;
        }

        @Override
        public boolean close() {
                stopRead();
                isOpened = false;
                return mUsbConnetionManager.close();
        }

        @Override
        public int read(byte[] buf, int size) {
                return mBuffer.get(buf, size);
        }

        @Override
        public int write(byte[] buf, int size) {
                if(buf == null) {
                        return 0;
                }
                int offset = 0;
                int write_size;
                int written_size;


                while(offset < size) {
                        write_size = USB_WRITE_BUFFER_SIZE;

                        if(offset + write_size > size) {
                                write_size = size - offset;
                        }
                        // optimization!
                        if(offset == 0) {
                                synchronized(DevLock) {
                                        written_size = mConnection.bulkTransfer(mEndpointOut, buf, write_size, 100);

                                }
                        } else {
                                System.arraycopy(buf, offset, wbuf, 0, write_size);
                                synchronized(DevLock) {
                                        written_size = mConnection.bulkTransfer(mEndpointOut, wbuf, write_size, 100);
                                }
                        }
                        if(written_size < 0) {
                                return -1;
                        }
                        offset += written_size;
                }

                return offset;
        }

        private void stopRead() {
                mReadThreadStop = true;
        }

        private void startRead() {
                if(mReadThreadStop) {
                        mReadThreadStop = false;
                        new Thread(mLoop).start();
                }
        }

        private String toHexStr(byte[] b, int length) {
                String str = "";
                for(int i = 0; i < length; i++) {
                        str += String.format("%02x ", b[i]);
                }
                return str;
        }
        private Runnable mLoop = new Runnable() {

                @Override
                @SuppressWarnings("CallToThreadYield")
                public void run() {
                        try {
                                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_FOREGROUND);
                        } catch(Exception e) {
                        }
                        int len;
                        byte[] rbuf = new byte[mEndpointIn.getMaxPacketSize()];
                        UsbRequest response;
                        UsbRequest request = new UsbRequest();
                        request.initialize(mConnection, mEndpointIn);
                        ByteBuffer buf = ByteBuffer.wrap(rbuf);
                        for(;;) {// this is the main loop for transferring
                                len = 0;
                                if(request.queue(buf, rbuf.length)) {
                                        response = mConnection.requestWait();
                                        if(response != null) {
                                                len = buf.position();
                                        }
                                        if(len > 0) {
                                                if(DEBUG_SHOW) {
                                                        Log.e(TAG, "read(" + len + "): " + toHexStr(rbuf, len));
                                                }

                                                mBuffer.add(rbuf, len);
                                                onRead(len);
                                        } else if(mBuffer.getBufferdLength() > 0) {
                                                onRead(mBuffer.getBufferdLength());
                                        }

                                } else if(mBuffer.getBufferdLength() > 0) {
                                        onRead(mBuffer.getBufferdLength());
                                }

                                if(mReadThreadStop) {
                                        return;
                                }
                        }
                } // end of run()
        }; // end of runnable

        /**
         * Sets Uart configurations
         *
         * @param config configurations
         *
         * @return true : successful, false : fail
         */
        public boolean setUartConfig(UartConfig config) {
                boolean res;
                boolean ret = true;
                res = setBaudrate(config.baudrate);
                ret = ret && res;

                res = setDataBits(config.dataBits);
                ret = ret && res;

                res = setParity(config.parity);
                ret = ret && res;

                res = setStopBits(config.stopBits);
                ret = ret && res;

                res = setDtrRts(config.dtrOn, config.rtsOn);
                ret = ret && res;

                return ret;
        }

        /**
         * Initializes CDC communication
         *
         * @return true : successful, false : fail
         */
        private boolean init() {
                if(mConnection == null) {
                        return false;
                }
                int ret = mConnection.controlTransfer(0x21, 0x22, 0x00, mInterfaceNum, null, 0, 0); // init CDC
                if(ret < 0) {
                        return false;
                }
                return true;
        }

        @Override
        public boolean isOpened() {
                return isOpened;
        }

        /**
         * Sets baudrate
         *
         * @param baudrate baudrate e.g. 9600
         *
         * @return true : successful, false : fail
         */
        public boolean setBaudrate(int baudrate) {
                byte[] baudByte = new byte[4];

                baudByte[0] = (byte) (baudrate & 0x000000FF);
                baudByte[1] = (byte) ((baudrate & 0x0000FF00) >> 8);
                baudByte[2] = (byte) ((baudrate & 0x00FF0000) >> 16);
                baudByte[3] = (byte) ((baudrate & 0xFF000000) >> 24);
                int ret = mConnection.controlTransfer(0x21, 0x20, 0, mInterfaceNum, new byte[] {
                                baudByte[0], baudByte[1], baudByte[2], baudByte[3], 0x00, 0x00,
                                0x08}, 7, 100);
                if(ret < 0) {
                        if(DEBUG_SHOW) {
                                Log.d(TAG, "Fail to setBaudrate");
                        }
                        return false;
                }
                mUartConfig.baudrate = baudrate;
                return true;
        }

        /**
         * Sets Data bits
         *
         * @param dataBits data bits e.g. UartConfig.DATA_BITS8
         *
         * @return true : successful, false : fail
         */
        public boolean setDataBits(int dataBits) {
                // TODO : implement
                if(DEBUG_SHOW) {
                        Log.d(TAG, "Fail to setDataBits");
                }
                mUartConfig.dataBits = dataBits;
                return false;
        }

        /**
         * Sets Parity bit
         *
         * @param parity parity bits e.g. UartConfig.PARITY_NONE
         *
         * @return true : successful, false : fail
         */
        public boolean setParity(int parity) {
                // TODO : implement
                if(DEBUG_SHOW) {
                        Log.d(TAG, "Fail to setParity");
                }
                mUartConfig.parity = parity;
                return false;
        }

        /**
         * Sets Stop bits
         *
         * @param stopBits stop bits e.g. UartConfig.STOP_BITS1
         *
         * @return true : successful, false : fail
         */
        public boolean setStopBits(int stopBits) {
                // TODO : implement
                if(DEBUG_SHOW) {
                        Log.d(TAG, "Fail to setStopBits");
                }
                mUartConfig.stopBits = stopBits;
                return false;
        }

        @Override
        public boolean setDtrRts(boolean dtrOn, boolean rtsOn) {
                int ctrlValue = 0x0000;
                if(dtrOn) {
                        ctrlValue |= 0x0001;
                }
                if(rtsOn) {
                        ctrlValue |= 0x0002;
                }
                int ret = mConnection.controlTransfer(0x21, 0x22, ctrlValue, mInterfaceNum, null, 0, 100);
                if(ret < 0) {
                        if(DEBUG_SHOW) {
                                Log.d(TAG, "Fail to setDtrRts");
                        }
                        return false;
                }
                mUartConfig.dtrOn = dtrOn;
                mUartConfig.rtsOn = rtsOn;
                return true;
        }

        @Override
        public UartConfig getUartConfig() {
                return mUartConfig;
        }

        @Override
        public int getBaudrate() {
                return mUartConfig.baudrate;
        }

        @Override
        public int getDataBits() {
                return mUartConfig.dataBits;
        }

        @Override
        public int getParity() {
                return mUartConfig.parity;
        }

        @Override
        public int getStopBits() {
                return mUartConfig.stopBits;
        }

        @Override
        public boolean getDtr() {
                return mUartConfig.dtrOn;
        }

        @Override
        public boolean getRts() {
                return mUartConfig.rtsOn;
        }

        @Override
        public void clearBuffer() {
                mBuffer.clear();
        }
        //////////////////////////////////////////////////////////
        // Listener for reading uart
        //////////////////////////////////////////////////////////
        private List<ReadListener> uartReadListenerList = new ArrayList<ReadListener>();
        private boolean mStopReadListener = false;

        @Override
        public void addReadListener(ReadListener listener) {
                uartReadListenerList.add(listener);
        }

        @Override
        @Deprecated
        public void addReadListener(ReadLisener listener) {
                addReadListener((ReadListener)listener);
        }

        @Override
        public void clearReadListener() {
                uartReadListenerList.clear();
        }

        @Override
        public void startReadListener() {
                mStopReadListener = false;
        }

        @Override
        public void stopReadListener() {
                mStopReadListener = true;
        }

        private void onRead(int size) {
                if(mStopReadListener) {
                        return;
                }
                for(ReadListener listener : uartReadListenerList) {
                        listener.onRead(size);
                }
        }
        //////////////////////////////////////////////////////////

        @Override
        public String getPhysicalConnectionName() {
                return Physicaloid.USB_STRING;
        }

        @Override
        public int getPhysicalConnectionType() {
                return Physicaloid.USB;
        }

        @Override
        public void setDebug(boolean flag) {
                DEBUG_SHOW = flag;
        }
}
