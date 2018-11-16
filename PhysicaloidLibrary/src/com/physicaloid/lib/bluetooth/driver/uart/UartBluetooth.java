/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.physicaloid.lib.bluetooth.driver.uart;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;
import com.physicaloid.BuildConfig;
import com.physicaloid.lib.Physicaloid;
import com.physicaloid.lib.framework.SerialCommunicator;
import com.physicaloid.lib.usb.driver.uart.ReadLisener;
import com.physicaloid.lib.usb.driver.uart.ReadListener;
import com.physicaloid.lib.usb.driver.uart.UartConfig;
import com.physicaloid.misc.RingBuffer;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author root
 */
public class UartBluetooth extends SerialCommunicator {

        private static final String TAG = UartBluetooth.class.getSimpleName();
        private boolean DEBUG_SHOW = false;
        private static final int DEFAULT_BAUDRATE = 9600;
        private UartConfig mUartConfig;
        private static final int RING_BUFFER_SIZE = 1024;
        private static final int READ_BUFFER_SIZE = 256;
        private static final int WRITE_BUFFER_SIZE = 256;
        private RingBuffer mBuffer;
        private boolean mReadThreadStop = true;
        private boolean isOpened;
        private String mBlueName;
        private static final UUID uu = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        private BluetoothSocket DATA_socket;
        volatile boolean DATA_keep_going = true;
        volatile boolean DATA_still_going = true;
        private DataOutputStream DATA_OUT;
        private DataInputStream DATA_IN;
        private BluetoothAdapter mBluetoothAdapter;
        private BluetoothServerSocket serverSocket;

        public UartBluetooth(Context context, String BlueName) {
                super(context);
                mUartConfig = new UartConfig();
                mBuffer = new RingBuffer(RING_BUFFER_SIZE);
                mBlueName = BlueName;
                isOpened = false;
                DATA_socket = null;
                DATA_OUT = null;
                DATA_IN = null;
        }

        @Override
        public boolean open() {
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
        private Runnable connect_DATA = new Runnable() {

                InetAddress serverAddr;

                @Override
                @SuppressWarnings("CallToThreadDumpStack")
                public void run() {
                        while(DATA_keep_going) {
                                try {
                                        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                                        if(mBluetoothAdapter != null) {
                                                if(mBluetoothAdapter.isEnabled()) {
                                                        serverSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(mBlueName, uu);
                                                        DATA_socket = serverSocket.accept();
                                                        DATA_OUT = new DataOutputStream(DATA_socket.getOutputStream());
                                                        DATA_IN = new DataInputStream(DATA_socket.getInputStream());
                                                        DATA_still_going = false;
                                                        return;
                                                }
                                        }
                                } catch(Exception ex) {
                                        Log.d(TAG, ex.toString());
                                        ex.printStackTrace();
                                }
                        }
                        DATA_still_going = false;
                }
        };

        @SuppressWarnings({"SleepWhileInLoop", "CallToThreadDumpStack"})
        private boolean init() {
                DATA_keep_going = true;
                DATA_still_going = true;

                Log.d(TAG, "********************* Bluetooth Connecting DATA **************");
                new Thread(connect_DATA).start();
                Log.d(TAG, "********************* Bluetooth WAITING **************");
                // timeout after how many seconds? How about 30?
                int timeout = (30 * 1000) / 50;
                while(DATA_still_going && (timeout > 0)) {
                        try {
                                // STALL APP :-)
                                Thread.sleep(50);
                                timeout--;
                        } catch(InterruptedException ex) {
                        }
                }
                if(timeout == 0) {
                        // did not connect, kill threads.
                        DATA_keep_going = false;
                        while(DATA_still_going) {
                                try {
                                        Thread.sleep(50);
                                } catch(InterruptedException ex) {
                                }
                        }
                        Log.d(TAG, "********************* Bluetooth Timed out! **************");
                        return false;
                }
                try {
                        DATA_OUT = new DataOutputStream(DATA_socket.getOutputStream());
                        DATA_IN = new DataInputStream(DATA_socket.getInputStream());
                } catch(Exception ex) {
                        Log.d(TAG, ex.toString());
                        ex.printStackTrace();
                }
                Log.d(TAG, "********************* Bluetooth Connected! **************");
                return true;
        }

        @Override
        @SuppressWarnings({"SleepWhileInLoop", "CallToThreadDumpStack"})
        public boolean close() {
                stopRead();
                isOpened = false;
                if(DATA_OUT != null) {
                        try {
                                DATA_OUT.close();
                        } catch(Exception ex) {
                                Log.d(TAG, ex.toString());
                                ex.printStackTrace();
                        }
                }
                if(DATA_IN != null) {
                        try {
                                DATA_IN.close();
                        } catch(Exception ex) {
                                Log.d(TAG, ex.toString());
                                ex.printStackTrace();
                        }
                }
                if(DATA_socket != null) {
                        try {
                                DATA_socket.close();
                        } catch(Exception ex) {
                                Log.d(TAG, ex.toString());
                                ex.printStackTrace();
                        }
                }
                DATA_keep_going = false;
                while(DATA_still_going) {
                        try {
                                Thread.sleep(50);
                        } catch(InterruptedException ex) {
                        }
                }

                return true;
        }

        @Override
        public int read(byte[] buf, int size) {
                return mBuffer.get(buf, size);
        }

        @Override
        @SuppressWarnings("CallToThreadDumpStack")
        public int write(byte[] buf, int size) {
                try {
                        DATA_OUT.write(buf, 0, size);
                        DATA_OUT.flush();
                } catch(Exception ex) {
                        Log.d(TAG, ex.toString());
                        ex.printStackTrace();
                }
                return size;
        }

        @Override
        public boolean setBaudrate(int baudrate) {
                // We don't do this...
                mUartConfig.baudrate = baudrate;
                return true;
        }

        @Override
        public boolean setDataBits(int dataBits) {
                // We don't do this...
                mUartConfig.dataBits = dataBits;
                return true;
        }

        @Override
        public boolean setParity(int parity) {
                // We don't do this...
                mUartConfig.parity = parity;
                return true;
        }

        @Override
        public boolean setStopBits(int stopBits) {
                // We don't do this...
                mUartConfig.stopBits = stopBits;
                return true;
        }

        @Override
        public boolean setDtrRts(boolean dtrOn, boolean rtsOn) {
                // We don't do this...
                mUartConfig.dtrOn = dtrOn;
                mUartConfig.rtsOn = rtsOn;
                return true;
        }

        //////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////
        /**
         * Sets Uart configurations
         *
         * @param config configurations
         * @return true : successful, false : fail
         */
        public boolean setUartConfig(UartConfig config) {
                boolean res;
                boolean ret = true;
                if(mUartConfig.baudrate != config.baudrate) {
                        res = setBaudrate(config.baudrate);
                        ret = ret && res;
                }

                if(mUartConfig.dataBits != config.dataBits) {
                        res = setDataBits(config.dataBits);
                        ret = ret && res;
                }

                if(mUartConfig.parity != config.parity) {
                        res = setParity(config.parity);
                        ret = ret && res;
                }

                if(mUartConfig.stopBits != config.stopBits) {
                        res = setStopBits(config.stopBits);
                        ret = ret && res;
                }

                if(mUartConfig.dtrOn != config.dtrOn
                        || mUartConfig.rtsOn != config.rtsOn) {
                        res = setDtrRts(config.dtrOn, config.rtsOn);
                        ret = ret && res;
                }

                return ret;
        }

        @Override
        public boolean isOpened() {
                return isOpened;
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
                        byte[] rbuf = new byte[READ_BUFFER_SIZE];
                        android.os.Process.setThreadPriority(-20);
                        //ByteBuffer buf = ByteBuffer.wrap(rbuf);
                        for(;;) {
                                try {
                                        // this is the main loop for transferring
                                        len = 0;
                                        //if (request.queue(buf, rbuf.length)) {
                                        //    response = mConnection.requestWait();
                                        //    if (response != null) {
                                        //        len = buf.position();
                                        //    }

                                        while(DATA_IN.available() > 0 && len < READ_BUFFER_SIZE) {
                                                DATA_IN.read(rbuf, len, 1);
                                                len++;
                                        }
                                        if(len > 0) {
                                                mBuffer.add(rbuf, len);
                                                onRead(len);
                                        }
                                        if(mReadThreadStop) {
                                                return;
                                        }
                                } catch(IOException ex) {
                                        // TO-DO: Needs to broadcast that it has been disconnected....
                                }
                        }
                } // end of run()
        }; // end of runnable

        @Override
        public String getPhysicalConnectionName() {
                return Physicaloid.BLUETOOTH_STRING;
        }

        @Override
        public int getPhysicalConnectionType() {
                return Physicaloid.BLUETOOTH;
        }

        @Override
        public void setDebug(boolean flag) {
                DEBUG_SHOW = flag;
        }
}
