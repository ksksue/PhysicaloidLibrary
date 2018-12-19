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

public class UartFtdi extends SerialCommunicator {

        private static final String TAG = UartFtdi.class.getSimpleName();
        private boolean DEBUG_SHOW = false;
        private static final int DEFAULT_BAUDRATE = 9600;
        private UsbCdcConnection mUsbConnetionManager;
        private UartConfig mUartConfig;
        private static final int RING_BUFFER_SIZE = 1024;
        private static final int USB_WRITE_BUFFER_SIZE = 2;
        private RingBuffer mBuffer;
        private boolean mReadThreadStop = true;
        private UsbDeviceConnection mConnection;
        private UsbEndpoint mEndpointIn;
        private UsbEndpoint mEndpointOut;
        private boolean isOpened;
        private byte[] wbuf = new byte[USB_WRITE_BUFFER_SIZE];
        //private final Object DevLock = new Object();
        // USB_TYPE_VENDOR | USB_RECIP_DEVICE | USB_DIR_OUT
        private static final byte REQTYPE_HOST_TO_INTERFACE = (byte) 0x41;
        // USB_TYPE_VENDOR | USB_RECIP_DEVICE | USB_DIR_IN
        private static final byte REQTYPE_INTERFACE_TO_HOST = (byte) 0xc1;
        @SuppressWarnings("unused")
        private static final byte REQTYPE_HOST_TO_DEVICE = (byte) 0x40;
        @SuppressWarnings("unused")
        private static final byte REQTYPE_DEVICE_TO_HOST = (byte) 0xc0;
        // Official PIDs
        private static final int FT232AM = (0x0200);
        @SuppressWarnings("unused")
        private static final int FT232BM = (0x0400);
        @SuppressWarnings("unused")
        private static final int FT2232C = (0x0500);
        @SuppressWarnings("unused")
        private static final int FT232R = (0x0600);
        @SuppressWarnings("unused")
        private static final int FT2232H = (0x0700);
        @SuppressWarnings("unused")
        private static final int FT4232H = (0x0800);
        @SuppressWarnings("unused")
        private static final int FT232H = (0x0900);
        @SuppressWarnings("unused")
        private static final int FT230X = (0x1000);
        // Commands
        private static final int FTDI_SIO_RESET = (0x00); // Reset the port
        private static final int FTDI_SIO_MODEM_CTRL = (0x01); // Set the modem control register
        private static final int FTDI_SIO_SET_FLOW_CTRL = (0x02); // Set flow control register
        private static final int FTDI_SIO_SET_BAUD_RATE = (0x03); // Set baud rate
        private static final int FTDI_SIO_SET_DATA = (0x04); // Set the data characteristics of the port
        private static final int FTDI_SIO_GET_MODEM_STATUS = (0x05); // Get the current value of modem status register
        @SuppressWarnings("unused")
        private static final int FTDI_SIO_SET_EVENT_CHAR = (0x06); // Set the event character
        @SuppressWarnings("unused")
        private static final int FTDI_SIO_SET_ERROR_CHAR = (0x07); // Set the error character
        private static final int FTDI_SIO_SET_LATENCY_TIMER = (0x09); // Set the latency timer
        @SuppressWarnings("unused")
        private static final int FTDI_SIO_GET_LATENCY_TIMER = (0x0A); // Get the latency timer
        @SuppressWarnings("unused")
        private static final int FTDI_SIO_RESET_SIO = (0x00);
        @SuppressWarnings("unused")
        private static final int FTDI_SIO_RESET_PURGE_RX = (0x01);
        @SuppressWarnings("unused")
        private static final int FTDI_SIO_RESET_PURGE_TX = (0x02);
        @SuppressWarnings("unused")
        private static final int FTDI_SIO_DISABLE_FLOW_CTRL = (0x00);
        @SuppressWarnings("unused")
        private static final int FTDI_SIO_RTS_CTS_HS = (0x01);
        @SuppressWarnings("unused")
        private static final int FTDI_SIO_DTR_DSR_HS = (0x02);
        @SuppressWarnings("unused")
        private static final int FTDI_SIO_XON_XOFF_HS = (0x04);
        // status 0
        @SuppressWarnings("unused")
        private static final int FTDI_SIO_CTS_MASK = (0x10);
        @SuppressWarnings("unused")
        private static final int FTDI_SIO_DSR_MASK = (0x20);
        @SuppressWarnings("unused")
        private static final int FTDI_SIO_RI_MASK = (0x40);
        @SuppressWarnings("unused")
        private static final int FTDI_SIO_RLSD_MASK = (0x80);
        // status 1
        @SuppressWarnings("unused")
        private static final int FTDI_SIO_DR = (0x01); // Data Ready
        @SuppressWarnings("unused")
        private static final int FTDI_SIO_OE = (0x02); // Overrun Error
        @SuppressWarnings("unused")
        private static final int FTDI_SIO_PE = (0x04); // Parity Error
        @SuppressWarnings("unused")
        private static final int FTDI_SIO_FE = (0x08); // Framing Error
        @SuppressWarnings("unused")
        private static final int FTDI_SIO_BI = (0x10); // Break Interrupt
        @SuppressWarnings("unused")
        private static final int FTDI_SIO_THRE = (0x20); // Transmitter Holding Register Empty
        @SuppressWarnings("unused")
        private static final int FTDI_SIO_TEMT = (0x40); // Transmitter Empty
        @SuppressWarnings("unused")
        private static final int FTDI_SIO_FIFO = (0x80); // Error in RX FIFO
        private static final int FTDI_SIO_SET_RTS_HIGH = 0x0101; //((FTDI_SIO_SET_DTR_MASK << 8) | 1);
        private static final int FTDI_SIO_SET_RTS_LOW = 0x0100; //((FTDI_SIO_SET_DTR_MASK << 8) | 0);
        private static final int FTDI_SIO_SET_DTR_HIGH = 0x0202; //((FTDI_SIO_SET_RTS_MASK << 8) | 2);
        private static final int FTDI_SIO_SET_DTR_LOW = 0x0200; //((FTDI_SIO_SET_RTS_MASK << 8) | 0);
        private static final int FTDI_RS_TEMT = (1 << 6);

        public UartFtdi(Context context) {
                super(context);
                mUsbConnetionManager = new UsbCdcConnection(context);
                mReadThreadStop = true;
                mUartConfig = new UartConfig();
                mBuffer = new RingBuffer(RING_BUFFER_SIZE);
                isOpened = false;
        }

        public boolean open(UsbVidPid ids) {
                if(mUsbConnetionManager.open(ids)) {
                        mConnection = mUsbConnetionManager.getConnection();
                        mEndpointIn = mUsbConnetionManager.getEndpointIn();
                        mEndpointOut = mUsbConnetionManager.getEndpointOut();
                        //pid = mUsbConnetionManager.getPID();
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
        public boolean open() {
                for(UsbVidList id : UsbVidList.values()) {
                        if(id.getVid() == 0x0403) {
                                if(open(new UsbVidPid(id.getVid(), 0))) {
                                        return true;
                                }
                        }
                }
                return false;
        }

        private boolean init() {

                if(mConnection == null) {
                        return false;
                }
                int rv;
                rv = control_out(FTDI_SIO_RESET, 0, 0);
                if(rv < 0) {
                        return false;
                }
                rv = control_out(FTDI_SIO_SET_FLOW_CTRL, 0, 1);
                if(rv < 0) {
                        return false;
                }
                // set the latency timer to a very low number to improve performance.
                rv = control_out(FTDI_SIO_SET_LATENCY_TIMER, 0, 1);
                if(rv < 0) {
                        return false;
                }
                return true;
        }

        @Override
        public boolean close() {
                if(mUsbConnetionManager != null) {
                        stopRead();
                        isOpened = false;
                        return mUsbConnetionManager.close();
                }
                return true;
        }

        @Override
        public int read(byte[] buf, int size) {
                return mBuffer.get(buf, size);
        }

        private int control_out(int request, int value, int index) {
                if(mConnection == null) {
                        return -1;
                }
                if(DEBUG_SHOW) {
                        Log.d(TAG, "XXXXXXXXXXXXXXXXXXXXXXXXX CTRL r=" + String.format("0x%02X", request) + " v=" + String.format("0x%04X", value) + " i=" + String.format("0x%04X", index));
                }
                int ret = mConnection.controlTransfer(REQTYPE_HOST_TO_INTERFACE, request, value, index, null, 0, 100);
                return ret;
        }

        private int control_in(int request, int value, int index, byte buf[], int bufsize) {
                if(mConnection == null) {
                        return -1;
                }
                int ret = mConnection.controlTransfer(REQTYPE_INTERFACE_TO_HOST, request, value, index, buf, bufsize, 100);
                return ret;

        }

        @Override
        public int write(byte[] buf, int size) {
                if(buf == null) {
                        return 0;
                }
                int offset = 0;
                int write_size;
                int written_size;
                int len;

                if(DEBUG_SHOW) {
                        Log.e(TAG, "write(" + size + "): " + toHexStr(buf, size));
                }

                // FTDI is crap, makes us work hard.
                // We have to treat the chip as if it is an 8250 on the outbound
                // otherwise it seems that characters don't always seem to make it.
                while(offset < size) {
                        // check empty
                        while(true) {
                                len = 2;
                                written_size = control_in(FTDI_SIO_GET_MODEM_STATUS, 0, 0, wbuf, len);
                                if(written_size < 1) {
                                        return -1;
                                }
                                if(written_size == 1) {
                                        wbuf[1] = 0;
                                }
                                if((wbuf[1] & FTDI_RS_TEMT) == FTDI_RS_TEMT) {
                                        break;
                                }
                        }
                        write_size = 1;
                        if(offset + write_size > size) {
                                write_size = size - offset;
                        }
                        // optimization!
                        if(offset == 0) {
                                //synchronized(DevLock) {
                                written_size = mConnection.bulkTransfer(mEndpointOut, buf, write_size, 100);

                                //}
                        } else {
                                System.arraycopy(buf, offset, wbuf, 0, write_size);
                                //synchronized(DevLock) {
                                written_size = mConnection.bulkTransfer(mEndpointOut, wbuf, write_size, 100);
                                //}
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
                        //byte[] rbuf = new byte[USB_READ_BUFFER_SIZE];
                        byte[] rbuf = new byte[mEndpointIn.getMaxPacketSize()];
                        byte[] cbuf = new byte[mEndpointIn.getMaxPacketSize()];
                        //android.os.Process.setThreadPriority(-20);
                        UsbRequest response;
                        UsbRequest request = new UsbRequest();
                        request.initialize(mConnection, mEndpointIn);
                        ByteBuffer buf = ByteBuffer.wrap(rbuf);
                        for(;;) {// this is the main loop for transferring
                                len = 0;
                                //synchronized(DevLock) {
                                if(request.queue(buf, rbuf.length)) {
                                        response = mConnection.requestWait();
                                        if(response != null) {
                                                len = buf.position();
                                        }
                                }
                                //}
                                if(len > 1) {
                                        if(len > 2) {
                                                //if(DEBUG_SHOW) {
                                                //        Log.e(TAG, "read(" + len + "): " + toHexStr(rbuf, len));
                                                //}
                                                // FTDI stuffs status in the first 2 bytes.
                                                len -= 2;
                                                System.arraycopy(rbuf, 2, cbuf, 0, len);
                                                if(DEBUG_SHOW) {
                                                        Log.e(TAG, "read(" + len + "): " + toHexStr(cbuf, len));
                                                }
                                                mBuffer.add(cbuf, len);
                                                onRead(mBuffer.getBufferdLength());
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

        @Override
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

        @Override
        public boolean isOpened() {
                return isOpened;
        }

        @Override
        public boolean setBaudrate(int baudrate) {
                if(mUsbConnetionManager == null) {
                        return false;
                }
                int divfrac[] = {0, 3, 2, 4, 1, 5, 6, 7};
                int baud_value = 0; // uint16_t
                int baud_index = 0; // uint16_t
                int divisor3;
                divisor3 = 48000000 / 2 / baudrate; // divisor shifted 3 bits to the left

                if(mUsbConnetionManager.getPID() == FT232AM) {
                        if((divisor3 & 0x7) == 7) {
                                divisor3++; // round x.7/8 up to x+1
                        }
                        baud_value = divisor3 >> 3;
                        divisor3 &= 0x7;

                        if(divisor3 == 1) {
                                baud_value |= 0xc000;
                        } else // 0.125
                        if(divisor3 >= 4) {
                                baud_value |= 0x4000;
                        } else // 0.5
                        if(divisor3 != 0) {
                                baud_value |= 0x8000; // 0.25
                        }
                        if(baud_value == 1) {
                                baud_value = 0; /* special case for maximum baud rate */
                        }
                } else {

                        baud_value = divisor3 >> 3;
                        baud_value |= divfrac[divisor3 & 0x7] << 14;
                        //baud_index = divindex[divisor3 & 0x7];

                        /* Deal with special cases for highest baud rates. */
                        if(baud_value == 1) {
                                baud_value = 0;
                        } else // 1.0
                        if(baud_value == 0x4001) {
                                baud_value = 1; // 1.5
                        }
                }
                baud_index = (baud_value >> 16) & 0xFFFF;
                baud_value &= 0xFFFF;

                int rv = control_out(FTDI_SIO_SET_BAUD_RATE, baud_value, baud_index);
                if(rv < 0) {
                        if(DEBUG_SHOW) {
                                Log.d(TAG, "setBaudrate failed " + rv);
                        }
                        return false;
                }
                mUartConfig.baudrate = baudrate;
                return true;
        }

        @Override
        public boolean setDataBits(int dataBits) {
                if(mUsbConnetionManager == null) {
                        return false;
                }
                int s = ((mUartConfig.stopBits) << 11) | ((mUartConfig.parity) << 8) | dataBits;
                int rv = control_out(FTDI_SIO_SET_DATA, s, 1);
                if(rv < 0) {
                        if(DEBUG_SHOW) {
                                Log.d(TAG, "setDataBits failed " + rv);
                        }
                        return false;
                }
                mUartConfig.dataBits = dataBits;
                return true;
        }

        @Override
        public boolean setParity(int parity) {
                if(mUsbConnetionManager == null) {
                        return false;
                }
                int s = ((mUartConfig.stopBits) << 11) | ((parity) << 8) | mUartConfig.dataBits;
                int rv = control_out(FTDI_SIO_SET_DATA, s, 1);
                if(rv < 0) {
                        if(DEBUG_SHOW) {
                                Log.d(TAG, "setParity failed " + rv);
                        }
                        return false;
                }
                mUartConfig.parity = parity;
                return true;
        }

        @Override
        public boolean setStopBits(int stopBits) {
                if(mUsbConnetionManager == null) {
                        return false;
                }
                int s = ((stopBits) << 11) | ((mUartConfig.parity) << 8) | mUartConfig.dataBits;
                int rv = control_out(FTDI_SIO_SET_DATA, s, 1);
                if(rv < 0) {
                        if(DEBUG_SHOW) {
                                Log.d(TAG, "setStopBits failed " + rv);
                        }
                        return false;
                }
                mUartConfig.stopBits = stopBits;
                return true;
        }

        @Override
        public boolean setDtrRts(boolean dtrOn, boolean rtsOn) {
                if(mUsbConnetionManager == null) {
                        return false;
                }
                if(DEBUG_SHOW) {
                        Log.d(TAG, "setDtrRts " + Boolean.toString(dtrOn) + ", " + Boolean.toString(rtsOn));
                }
                int s = FTDI_SIO_SET_DTR_HIGH;
                if(!dtrOn) {
                        s = FTDI_SIO_SET_DTR_LOW;
                }
                int rv = control_out(FTDI_SIO_MODEM_CTRL, s, 1);
                if(rv < 0) {
                        if(DEBUG_SHOW) {
                                Log.d(TAG, "setDtr failed " + rv);
                        }
                        return false;
                }
                s = FTDI_SIO_SET_RTS_HIGH;
                if(!rtsOn) {
                        s = FTDI_SIO_SET_RTS_LOW;
                }
                rv = control_out(FTDI_SIO_MODEM_CTRL, s, 1);
                if(rv < 0) {
                        if(DEBUG_SHOW) {
                                Log.d(TAG, "setRts failed " + rv);
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
                // clear ftdi chip buffer
                //synchronized(ftDevLock) {
                //        ftDev.purge((byte) (D2xxManager.FT_PURGE_TX | D2xxManager.FT_PURGE_RX));
                //}

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
                addReadListener((ReadListener) listener);
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

        @Override
        public void setDebug(boolean flag) {
                DEBUG_SHOW = flag;
        }
}
