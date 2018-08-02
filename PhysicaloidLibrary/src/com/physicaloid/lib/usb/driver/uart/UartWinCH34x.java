package com.physicaloid.lib.usb.driver.uart;


/*
 * Reference source Linux Kernel
 */
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

public class UartWinCH34x extends SerialCommunicator {
        /* supported VID,PID
         * 0x4348, 0x5523
         * 0x1a86, 0x7523
         * 0x1a86, 0x5523
         */
        /* TO-DO:
         * data bits [5, 8]
         * parity {NONE, EVEN, ODD}
         * stop bits [1, 2]
         */

        private static final String TAG = UartWinCH34x.class.getSimpleName();
        private static final boolean DEBUG_SHOW = false && BuildConfig.DEBUG;
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
        private boolean isOpened;
        private static final int CH341_BIT_RTS = (1 << 6);
        private static final int CH341_BIT_DTR = (1 << 5);
        private static final int CH341_MULT_STAT = 0x04;
        private static final int CH341_BIT_CTS = 0x01;
        private static final int CH341_BIT_DSR = 0x02;
        private static final int CH341_BIT_RI = 0x04;
        private static final int CH341_BIT_DCD = 0x08;
        private static final int CH341_BITS_MODEM_STAT = 0x0f;
        private static final int CH341_REQ_WRITE_REG = 0x9A;
        private static final int CH341_REQ_READ_REG = 0x95;
        private static final int CH341_REG_BREAK1 = 0x05;
        private static final int CH341_REG_BREAK2 = 0x18;
        private static final int CH341_NBREAK_BITS_REG1 = 0x01;
        private static final int CH341_NBREAK_BITS_REG2 = 0x40;
        private static final int CH341_BAUDBASE_FACTOR = 1532620800;
        private static final int CH341_BAUDBASE_DIVMAX = 3;
        private int line_status;
        private int multi_status_change;

        /*
         * Config request types
         */
        // USB_TYPE_VENDOR | USB_RECIP_DEVICE | USB_DIR_OUT
        private static final byte REQTYPE_HOST_TO_INTERFACE = (byte) 0x41;
        // USB_TYPE_VENDOR | USB_RECIP_DEVICE | USB_DIR_IN
        private static final byte REQTYPE_INTERFACE_TO_HOST = (byte) 0xc1;
        @SuppressWarnings("unused")
        private static final byte REQTYPE_HOST_TO_DEVICE = (byte) 0x40;
        @SuppressWarnings("unused")
        private static final byte REQTYPE_DEVICE_TO_HOST = (byte) 0xc0;

        public UartWinCH34x(Context context) {
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
                if(mUsbConnetionManager.open(ids)) {
                        mConnection = mUsbConnetionManager.getConnection();
                        mEndpointIn = mUsbConnetionManager.getEndpointIn();
                        mEndpointOut = mUsbConnetionManager.getEndpointOut();
                        if(!init()) {
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
                byte[] wbuf = new byte[USB_WRITE_BUFFER_SIZE];

                while(offset < size) {
                        write_size = USB_WRITE_BUFFER_SIZE;

                        if(offset + write_size > size) {
                                write_size = size - offset;
                        }
                        System.arraycopy(buf, offset, wbuf, 0, write_size);

                        written_size = mConnection.bulkTransfer(mEndpointOut, wbuf, write_size, 100);

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
                public void run() {
                        int len;
                        byte[] rbuf = new byte[mEndpointIn.getMaxPacketSize()];
                        android.os.Process.setThreadPriority(-20);
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
                                                mBuffer.add(rbuf, len);
                                                onRead(len);
                                        } else if(mBuffer.getBufferdLength() > 0) {
                                                onRead(mBuffer.getBufferdLength());
                                        } else if(mBuffer.getBufferdLength() > 0) {
                                                onRead(mBuffer.getBufferdLength());
                                        }


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

        /**
         * Initializes UART communication
         *
         * @return true : successful, false : fail
         */
        private boolean init() {
                int size = 8;
                byte[] buffer = new byte[size];

                if(mConnection == null) {
                        return false;
                }

                /* expect two bytes 0x27 0x00 */
                int r = ch341_control_in(0x5f, 0, 0, buffer, size);
                if(r < 0) {
                        return false;
                }
                r = ch341_control_out(0xa1, 0, 0);
                if(r < 0) {
                        return false;
                }

                if(!setBaudrate(DEFAULT_BAUDRATE)) {
                        return false;
                }

                /* expect two bytes 0x56 0x00 */
                r = ch341_control_in(0x95, 0x2518, 0, buffer, size);
                if(r < 0) {
                        return false;
                }

                r = ch341_control_out(0x9a, 0x2518, 0x0050);
                if(r < 0) {
                        return false;
                }

                /* expect 0xff 0xee */
                r = ch341_get_status();
                if(r < 0) {
                        return false;
                }

                r = ch341_control_out(0xa1, 0x501f, 0xd90a);
                if(r < 0) {
                        return false;
                }

                if(!setBaudrate(mUartConfig.baudrate)) {
                        return false;
                }

                if(!setDtrRts(mUartConfig.dtrOn, mUartConfig.rtsOn)) {
                        return false;
                }

                /* expect 0x9f 0xee */
                r = ch341_get_status();
                if(r < 0) {
                        return false;
                }

                return true;
        }

        @Override
        public boolean isOpened() {
                return isOpened;
        }

        private int ch341_control_out(int request, int value, int index) {
                if(mConnection == null) {
                        return -1;
                }
                int ret = mConnection.controlTransfer(REQTYPE_HOST_TO_INTERFACE, request, value, index, null, 0, 100);
                return ret;
        }
        /*
         * int requestType,
         * int request,
         * int value,
         * int index,
         * byte[] buffer,
         * int length,
         * int timeout
         */

        private int ch341_control_in(int request, int value, int index, byte buf[], int bufsize) {
                if(mConnection == null) {
                        return -1;
                }
                int ret = mConnection.controlTransfer(REQTYPE_INTERFACE_TO_HOST, request, value, index, buf, bufsize, 100);
                return ret;

        }

        private int ch341_get_status() {
                int size = 8;
                long flags;
                byte[] buffer = new byte[size];
                int r = ch341_control_in(0x95, 0x0706, 0, buffer, size);
                if(r < 0) {
                        return r;
                }
                if(r == 2) {
                        r = 0;
                        line_status = (~buffer[0]) & CH341_BITS_MODEM_STAT;
                        multi_status_change = 0;
                } else {
                        r = -1;
                }
                return r;
        }

        @Override
        public boolean setBaudrate(int baudrate) {

                if(mConnection == null) {
                        return false;
                }
                long factor = CH341_BAUDBASE_FACTOR / baudrate;
                short divisor = CH341_BAUDBASE_DIVMAX;

                while((factor > 0xfff0) && divisor != 0) {
                        factor >>= 3;
                        divisor--;
                }

                if(factor > 0xfff0) {
                        return false;
                }
                factor = 0x10000 - factor;
                int a = (int) (factor & 0xff00) | divisor;
                int b = (int) (factor & 0xff);

                int r = ch341_control_out(CH341_REQ_WRITE_REG, 0x1312, a);
                if(r < 0) {
                        if(DEBUG_SHOW) {
                                Log.d(TAG, "Fail to setBaudrate");
                        }
                        return false;
                }
                r = ch341_control_out(CH341_REQ_WRITE_REG, 0x0f2c, b);

                if(r < 0) {
                        if(DEBUG_SHOW) {
                                Log.d(TAG, "Fail to setBaudrate");
                        }
                        return false;
                }
                mUartConfig.baudrate = baudrate;
                return true;
        }

        @Override
        public boolean setDataBits(int dataBits) {
                // NOT IMPLEMENTED YET
                mUartConfig.dataBits = dataBits;
                return true;
        }

        @Override
        public boolean setParity(int parity) {
                // NOT IMPLEMENTED YET
                mUartConfig.parity = parity;
                return true;
        }

        @Override
        public boolean setStopBits(int stopBits) {
                // NOT IMPLEMENTED YET
                mUartConfig.stopBits = stopBits;
                return true;
        }

        @Override
        public boolean setDtrRts(boolean dtrOn, boolean rtsOn) {
                int ctrlValue = 0x0000;

                if(dtrOn) {
                        ctrlValue |= CH341_BIT_DTR;
                }

                if(rtsOn) {
                        ctrlValue |= CH341_BIT_RTS;
                }

                int r = ch341_control_out(0xa4, ~ctrlValue, 0);

                if(r < 0) {
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

        @Override
        public String getPhysicalConnectionName() {
                return Physicaloid.USB_STRING;
        }

        @Override
        public int getPhysicalConnectionType() {
                return Physicaloid.USB;
        }
}
