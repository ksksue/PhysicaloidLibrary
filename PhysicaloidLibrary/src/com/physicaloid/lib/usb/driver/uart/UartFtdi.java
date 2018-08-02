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
 *
 *
 * IMPORTANT NOTE!
 * Even FTDI screwed up on the d2xx driver for the FT232RL and forgot to swap DTR and RTS,
 * so we flip them in the driver for all of them.
 *
 * TO-DO: Nuke the need for the bullshit d2xx buggy driver.
 */
package com.physicaloid.lib.usb.driver.uart;

import android.content.Context;
import android.util.Log;
import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;
import com.physicaloid.lib.Physicaloid;
import com.physicaloid.lib.framework.SerialCommunicator;
import com.physicaloid.misc.RingBuffer;
import java.util.ArrayList;
import java.util.List;

public class UartFtdi extends SerialCommunicator {

        private static final boolean DEBUG_SHOW = true; //false && BuildConfig.DEBUG;
        private static final String TAG = UartFtdi.class.getSimpleName();
        private Context mContext;
        private D2xxManager ftD2xx = null;
        private volatile FT_Device ftDev = null;
        private UartConfig mUartConfig;
        private static final int RING_BUFFER_SIZE = 1024;
        private static final int USB_READ_BUFFER_SIZE = 256;
        private static final int USB_WRITE_BUFFER_SIZE = 256;
        private RingBuffer mBuffer;
        private static final int USB_OPEN_INDEX = 0;
        private static final int MAX_READBUF_SIZE = 256;
        private static final int READ_WAIT_MS = 1;
        private boolean mReadThreadStop;
        private byte[] wbuf = new byte[USB_WRITE_BUFFER_SIZE];
        private final Object ftDevLock = new Object();

        public UartFtdi(Context context) {
                super(context);
                mContext = context;
                mReadThreadStop = true;
                mUartConfig = new UartConfig();
                mBuffer = new RingBuffer(RING_BUFFER_SIZE);
                try {
                        ftD2xx = D2xxManager.getInstance(mContext);
                } catch(D2xxManager.D2xxException ex) {
                        Log.e(TAG, ex.toString());
                }
                Log.d(TAG, "D2xxManager version: " + Integer.toHexString(D2xxManager.getLibraryVersion()) + " " + D2xxManager.getLibraryVersion());
        }

        @Override
        public boolean open() {

                try {
                        if(ftD2xx == null) {
                                try {
                                        ftD2xx = D2xxManager.getInstance(mContext);
                                } catch(D2xxManager.D2xxException ex) {
                                        Log.e(TAG, ex.toString());
                                        return false;
                                }
                        }

                        if(ftDev == null) {
                                int devCount = ftD2xx.createDeviceInfoList(mContext);

                                if(DEBUG_SHOW) {
                                        Log.d(TAG, "Device number : " + Integer.toString(devCount));
                                }

                                D2xxManager.FtDeviceInfoListNode[] deviceList = new D2xxManager.FtDeviceInfoListNode[devCount];
                                ftD2xx.getDeviceInfoList(devCount, deviceList);

                                if(devCount <= 0) {
                                        return false;
                                }

                                ftDev = ftD2xx.openByIndex(mContext, USB_OPEN_INDEX);
                        } else {
                                if(ftD2xx.createDeviceInfoList(mContext) > 0) {
                                        synchronized(ftDevLock) {
                                                ftDev = ftD2xx.openByIndex(mContext, USB_OPEN_INDEX);
                                        }
                                }
                        }
                        if(ftDev.isOpen()) {
                                synchronized(ftDevLock) {
                                        ftDev.resetDevice(); // flush any data from the device buffers
                                        ftDev.setFlowControl(D2xxManager.FT_FLOW_NONE, (byte) 0, (byte) 0);
                                }
                                setBaudrate(mUartConfig.baudrate);
                                if(DEBUG_SHOW) {
                                        Log.d(TAG, "An FTDI device is opened.");
                                }
                                startRead();
                                return true;
                        } else {
                                if(DEBUG_SHOW) {
                                        Log.e(TAG, "Cannot open an FTDI device.");
                                }
                        }
                } catch(NullPointerException ex) {
                        // What has happened here is that the device is in some odd limbo state.
                        // This apears to ONLY happen with the reboot fast option on some tablets.
                        // We need to do a retry, but also exit gracefully.
                        // Hopefully this will only occur under this particular
                        // circumstance, and hopefully this fixes the issue.
                        System.exit(-1);
                }
                return false;
        }

        @Override
        public boolean close() {
                if(ftDev != null) {
                        stopRead();
                        synchronized(ftDevLock) {
                                ftDev.close();
                        }
                        if(DEBUG_SHOW) {
                                Log.d(TAG, "An FTDI device is closed.");
                        }
                }
                return true;
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
                                synchronized(ftDevLock) {
                                        written_size = ftDev.write(buf, write_size);
                                }
                        } else {
                                System.arraycopy(buf, offset, wbuf, 0, write_size);
                                synchronized(ftDevLock) {
                                        written_size = ftDev.write(wbuf, write_size);
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
        private Runnable mLoop = new Runnable() {

                @Override
                @SuppressWarnings("CallToThreadYield")
                public void run() {
                        try {
                                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_FOREGROUND);
                        } catch(Exception e) {
                        }
                        int len;
                        byte[] rbuf = new byte[USB_READ_BUFFER_SIZE];
                        // byte[] rbuf = new byte[mEndpointIn.getMaxPacketSize()];
                        //android.os.Process.setThreadPriority(-20);
                        for(;;) {// this is the main loop for transferring
                                // warning, BUG!!! NPE
                                synchronized(ftDevLock) {
                                        len = ftDev.getQueueStatus();
                                }

                                if(len > 0) {
                                        if(len > MAX_READBUF_SIZE) {
                                                len = MAX_READBUF_SIZE;
                                        }
                                        synchronized(ftDevLock) {
                                                len = ftDev.read(rbuf, len, READ_WAIT_MS); // You might want to set wait_ms.
                                        }

                                        //if(DEBUG_SHOW) {
                                        //        Log.e(TAG, "read(" + len + "): " + toHexStr(rbuf, len));
                                        //}
                                        mBuffer.add(rbuf, len);
                                        onRead(len);

                                } else if(mBuffer.getBufferdLength() > 0) {
                                        onRead(mBuffer.getBufferdLength());
                                }

                                if(mReadThreadStop) {
                                        return;
                                }

                                // needed?????
                                //try {
                                //        Thread.sleep(1);
                                //} catch(InterruptedException e) {
                                //}
                                //Thread.yield();

                        }
                } // end of run()
        }; // end of runnable

        @Override
        public boolean setUartConfig(UartConfig config) {
                boolean res;
                boolean ret = true;
                //if(mUartConfig.baudrate != config.baudrate) {
                res = setBaudrate(config.baudrate);
                ret = ret && res;
                //}

                //if(mUartConfig.dataBits != config.dataBits) {
                res = setDataBits(config.dataBits);
                ret = ret && res;
                //}

                //if(mUartConfig.parity != config.parity) {
                res = setParity(config.parity);
                ret = ret && res;
                //}

                //if(mUartConfig.stopBits != config.stopBits) {
                res = setStopBits(config.stopBits);
                ret = ret && res;
                //}

                //if(mUartConfig.dtrOn != config.dtrOn
                //        || mUartConfig.rtsOn != config.rtsOn) {
                res = setDtrRts(config.dtrOn, config.rtsOn);
                ret = ret && res;
                //}

                return ret;
        }

        @Override
        public boolean isOpened() {
                if(ftDev == null) {
                        return false;
                }
                synchronized(ftDevLock) {
                        return ftDev.isOpen();
                }
        }

        @Override
        public boolean setBaudrate(int baudrate) {
                if(ftDev == null) {
                        return false;
                }
                boolean ret;
                synchronized(ftDevLock) {
                        ret = ftDev.setBaudRate(baudrate);
                }
                if(ret) {
                        mUartConfig.baudrate = baudrate;
                }
                return ret;
        }

        @Override
        public boolean setDataBits(int dataBits) {
                if(ftDev == null) {
                        return false;
                }
                boolean ret;
                byte ftdiDataBits = convertFtdiDataBits(dataBits);
                byte ftdiStopBits = convertFtdiStopBits(mUartConfig.stopBits);
                byte ftdiParity = convertFtdiParity(mUartConfig.parity);
                synchronized(ftDevLock) {
                        ret = ftDev.setDataCharacteristics(ftdiDataBits, ftdiStopBits, ftdiParity);
                }
                if(ret) {
                        mUartConfig.dataBits = dataBits;
                }
                return ret;
        }

        @Override
        public boolean setParity(int parity) {
                if(ftDev == null) {
                        return false;
                }
                boolean ret;
                byte ftdiDataBits = convertFtdiDataBits(mUartConfig.dataBits);
                byte ftdiStopBits = convertFtdiStopBits(mUartConfig.stopBits);
                byte ftdiParity = convertFtdiParity(parity);
                synchronized(ftDevLock) {
                        ret = ftDev.setDataCharacteristics(ftdiDataBits, ftdiStopBits, ftdiParity);
                }
                if(ret) {
                        mUartConfig.parity = parity;
                }
                return ret;
        }

        @Override
        public boolean setStopBits(int stopBits) {
                if(ftDev == null) {
                        return false;
                }
                boolean ret;
                byte ftdiDataBits = convertFtdiDataBits(mUartConfig.dataBits);
                byte ftdiStopBits = convertFtdiStopBits(stopBits);
                byte ftdiParity = convertFtdiParity(mUartConfig.parity);
                synchronized(ftDevLock) {
                        ret = ftDev.setDataCharacteristics(ftdiDataBits, ftdiStopBits, ftdiParity);
                }
                if(ret) {
                        mUartConfig.stopBits = stopBits;
                }
                return ret;
        }

        @Override
        public boolean setDtrRts(boolean dtrOn, boolean rtsOn) {
                if(ftDev == null) {
                        return false;
                }
                boolean retDtr;// = false;
                boolean retRts;// = false;
                if(DEBUG_SHOW) {
                        Log.d(TAG, "setDtrRts " + Boolean.toString(dtrOn) + ", " + Boolean.toString(rtsOn));
                }
                if(dtrOn) {
                        synchronized(ftDevLock) {
                                retDtr = ftDev.setRts(); // note RTS <-> DTR
                        }
                } else {
                        synchronized(ftDevLock) {
                                retDtr = ftDev.clrRts(); // note RTS <-> DTR
                        }
                }
                if(retDtr) {
                        mUartConfig.dtrOn = dtrOn;
                }

                if(rtsOn) {
                        synchronized(ftDevLock) {
                                retRts = ftDev.setDtr(); // note RTS <-> DTR
                        }
                } else {
                        synchronized(ftDevLock) {
                                retRts = ftDev.clrDtr(); // note RTS <-> DTR
                        }
                }
                if(retRts) {
                        mUartConfig.rtsOn = rtsOn;
                }
                return retDtr && retRts;
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
                // clear ftdi chip buffer
                synchronized(ftDevLock) {
                        ftDev.purge((byte) (D2xxManager.FT_PURGE_TX | D2xxManager.FT_PURGE_RX));
                }
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

        private byte convertFtdiDataBits(int dataBits) {
                switch(dataBits) {
                        case UartConfig.DATA_BITS7:
                                return D2xxManager.FT_DATA_BITS_7;
                        case UartConfig.DATA_BITS8:
                                return D2xxManager.FT_DATA_BITS_8;
                        default:
                                return D2xxManager.FT_DATA_BITS_8;
                }
        }

        private byte convertFtdiStopBits(int stopBits) {
                switch(stopBits) {
                        case UartConfig.STOP_BITS1:
                                return D2xxManager.FT_STOP_BITS_1;
                        case UartConfig.STOP_BITS2:
                                return D2xxManager.FT_STOP_BITS_2;
                        default:
                                return D2xxManager.FT_STOP_BITS_1;
                }
        }

        private byte convertFtdiParity(int parity) {
                switch(parity) {
                        case UartConfig.PARITY_NONE:
                                return D2xxManager.FT_PARITY_NONE;
                        case UartConfig.PARITY_ODD:
                                return D2xxManager.FT_PARITY_ODD;
                        case UartConfig.PARITY_EVEN:
                                return D2xxManager.FT_PARITY_EVEN;
                        case UartConfig.PARITY_MARK:
                                return D2xxManager.FT_PARITY_MARK;
                        case UartConfig.PARITY_SPACE:
                                return D2xxManager.FT_PARITY_SPACE;
                        default:
                                return D2xxManager.FT_PARITY_NONE;
                }
        }

        private String toHexStr(byte[] b, int length) {
                String str = "";
                for(int i = 0; i < length; i++) {
                        str += String.format("%02x ", b[i]);
                }
                return str;
        }

        @Override
        public String getPhysicalConnectionName() {
                return Physicaloid.USB_STRING;
        }

        @Override
        public int getPhysicalConnectionType() {
                return Physicaloid.USB;
        }
}
