/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.physicaloid.lib.wifi.driver.uart;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author xxxajk@gmail.com
 */
public class UartWifi extends SerialCommunicator {

        private static final String TAG = UartWifi.class.getSimpleName();
        private boolean DEBUG_SHOW = false;
        private static final int DEFAULT_BAUDRATE = 9600;
        private UartConfig mUartConfig;
        private static final int RING_BUFFER_SIZE = 1024;
        private static final int READ_BUFFER_SIZE = 256;
        private static final int WRITE_BUFFER_SIZE = 256;
        private RingBuffer mBuffer;
        private boolean mReadThreadStop = true;
        private boolean isOpened;
        private String SERVER_IP = null;
        private int DATA_PORT = 0;
        private int CTRL_PORT = 0;
        private Socket DATA_socket;
        private Socket CTRL_socket;
        private DataOutputStream CTRL_OUT;
        private DataOutputStream DATA_OUT;
        private DataInputStream DATA_IN;
        volatile boolean CTRL_keep_going = false;
        volatile boolean CTRL_still_going = false;
        volatile boolean DATA_keep_going = false;
        volatile boolean DATA_still_going = false;
        volatile Context me;

        private boolean isNetworkConnected(Context context) {
                //Log.d(TAG, "Network available?");
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo netInfo = cm.getActiveNetworkInfo();
                if(netInfo == null) {
                        return false;
                }
                return (netInfo.isConnected() && netInfo.getType() == ConnectivityManager.TYPE_WIFI);
                //return cm.getActiveNetworkInfo() != null;
        }

        public UartWifi(Context context, String host, int Dport, int Cport) {
                super(context);
                SERVER_IP = host;
                DATA_PORT = Dport;
                CTRL_PORT = Cport;
                mUartConfig = new UartConfig();
                mBuffer = new RingBuffer(RING_BUFFER_SIZE);
                isOpened = false;
                CTRL_socket = null;
                DATA_socket = null;
                CTRL_OUT = null;
                DATA_OUT = null;
                DATA_IN = null;
                me = context;
        }

        @Override
        public boolean open() {
                if(!isOpened) {
                        if(!init()) {
                                close();
                                return false;
                        }
                        if(!setBaudrate(DEFAULT_BAUDRATE)) {
                                close();
                                return false;
                        }
                        mBuffer.clear();
                        startRead();
                        isOpened = true;
                }
                return true;
        }
        private Runnable connect_CTRL = new Runnable() {

                InetAddress serverAddr;

                @Override
                @SuppressWarnings("CallToThreadDumpStack")
                public void run() {
                        Log.d(TAG, "Network? " + isNetworkConnected(me));
                        while(CTRL_keep_going && isNetworkConnected(me)) {
                                try {
                                        serverAddr = InetAddress.getByName(SERVER_IP);
                                        CTRL_socket = new Socket(serverAddr, CTRL_PORT);
                                        CTRL_socket.setKeepAlive(true);
                                        CTRL_socket.setTcpNoDelay(true);
                                        CTRL_still_going = false;
                                        CTRL_keep_going = false;
                                        return;
                                } catch(Exception ex) {
                                        //Log.d(TAG, ex.toString());
                                        //ex.printStackTrace();
                                }
                        }
                        CTRL_still_going = false;
                }
        };
        private Runnable connect_DATA = new Runnable() {

                InetAddress serverAddr;

                @Override
                @SuppressWarnings("CallToThreadDumpStack")
                public void run() {
                        while(DATA_keep_going && isNetworkConnected(me)) {
                                try {
                                        serverAddr = InetAddress.getByName(SERVER_IP);
                                        DATA_socket = new Socket(serverAddr, DATA_PORT);
                                        DATA_socket.setKeepAlive(true);
                                        DATA_socket.setTcpNoDelay(true);
                                        DATA_keep_going = false;
                                        DATA_still_going = false;
                                        return;
                                } catch(Exception ex) {
                                        //Log.d(TAG, ex.toString());
                                        //ex.printStackTrace();
                                }
                        }
                        DATA_still_going = false;
                }
        };

        @SuppressWarnings({"SleepWhileInLoop", "CallToThreadDumpStack"})
        private boolean init() {

                CTRL_keep_going = true;
                CTRL_still_going = true;
                DATA_keep_going = true;
                DATA_still_going = true;
                Log.d(TAG, "********************* WiFi Connecting CTRL **************");
                new Thread(connect_CTRL).start();
                Log.d(TAG, "********************* WiFi Connecting DATA **************");
                new Thread(connect_DATA).start();
                Log.d(TAG, "********************* WiFi WAITING **************");

                // timeout after how many seconds? How about 30?
                int timeout = (30 * 1000) / 50;
                while(CTRL_still_going && DATA_still_going && (timeout > 0) && isNetworkConnected(me)) {
                        try {
                                // STALL APP :-)
                                Thread.sleep(50);
                                timeout--;
                        } catch(InterruptedException ex) {
                        }
                }
                if(timeout <= 0) {
                        // did not connect, kill threads.
                        DATA_keep_going = false;
                        CTRL_keep_going = false;
                        while(DATA_still_going && CTRL_still_going) {
                                try {
                                        Thread.sleep(50);
                                } catch(InterruptedException ex) {
                                }
                        }
                        Log.d(TAG, "********************* WiFi Timed out! **************");
                        return false;
                }
                try {
                        CTRL_OUT = new DataOutputStream(CTRL_socket.getOutputStream());
                        DATA_OUT = new DataOutputStream(DATA_socket.getOutputStream());
                        DATA_IN = new DataInputStream(DATA_socket.getInputStream());
                } catch(Exception ex) {
                        Log.d(TAG, "********************* WiFi DIED! **************");
                        Log.d(TAG, ex.toString());
                        ex.printStackTrace();
                        return false;
                }
                Log.d(TAG, "********************* WiFi Connected! **************");
                return true;
        }

        @Override
        @SuppressWarnings({"CallToThreadDumpStack", "SleepWhileInLoop"})
        public boolean close() {
                stopRead();
                isOpened = false;
                CTRL_keep_going = false;
                DATA_keep_going = false;
                while(DATA_still_going || CTRL_still_going) {
                        try {
                                Thread.sleep(50);
                        } catch(InterruptedException ex) {
                        }
                }
                if(CTRL_OUT != null) {
                        try {
                                CTRL_OUT.close();
                        } catch(Exception ex) {
                                Log.d(TAG, ex.toString());
                                ex.printStackTrace();
                        }
                        CTRL_OUT = null;

                }
                if(DATA_OUT != null) {
                        try {
                                DATA_OUT.close();
                        } catch(Exception ex) {
                                Log.d(TAG, ex.toString());
                                ex.printStackTrace();
                        }
                        DATA_OUT = null;
                }
                if(DATA_IN != null) {
                        try {
                                DATA_IN.close();
                        } catch(Exception ex) {
                                Log.d(TAG, ex.toString());
                                ex.printStackTrace();
                        }
                        DATA_IN = null;
                }
                if(CTRL_socket != null) {
                        try {
                                CTRL_socket.close();
                        } catch(Exception ex) {
                                Log.d(TAG, ex.toString());
                                ex.printStackTrace();
                        }
                        CTRL_socket = null;
                }
                if(DATA_socket != null) {
                        try {
                                DATA_socket.close();
                        } catch(Exception ex) {
                                Log.d(TAG, ex.toString());
                                ex.printStackTrace();
                        }
                        DATA_socket = null;
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
                if(buf == null) {
                        return 0;
                }
                try {
                        DATA_OUT.write(buf, 0, size);
                        DATA_OUT.flush();
                } catch(Exception ex) {
                        close();
                        Log.d(TAG, ex.toString());
                        ex.printStackTrace();
                        return -1;
                }
                return size;
        }

        @Override
        @SuppressWarnings("CallToThreadDumpStack")
        public boolean setBaudrate(int baudrate) {
                byte b[] = {0x40, (byte) (baudrate & 0xff), (byte) ((baudrate >> 8) & 0xff), (byte) ((baudrate >> 16) & 0xff), (byte) ((baudrate >> 24) & 0xff)};
                try {
                        CTRL_OUT.write(b, 0, 5);
                        CTRL_OUT.flush();
                } catch(Exception ex) {
                        close();
                        Log.d(TAG, ex.toString());
                        ex.printStackTrace();
                        return false;
                }
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
        @SuppressWarnings("CallToThreadDumpStack")
        public boolean setDtrRts(boolean dtrOn, boolean rtsOn) {
                try {
                        // TO-DO: rts... This is good enough for now, though.
                        if(dtrOn) {
                                byte b[] = {1};
                                CTRL_OUT.write(b, 0, 1);
                        } else {
                                byte b[] = {0};
                                CTRL_OUT.write(b, 0, 1);
                        }
                        CTRL_OUT.flush();
                } catch(Exception ex) {
                        close();
                        Log.d(TAG, ex.toString());
                        ex.printStackTrace();
                        return false;
                }
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
                @SuppressWarnings("SleepWhileInLoop")
                public void run() {
                        int len;
                        byte[] rbuf = new byte[READ_BUFFER_SIZE];
                        android.os.Process.setThreadPriority(-20);
                        for(;;) {
                                if(mReadThreadStop) {
                                        return;
                                }
                                if(CTRL_socket.isClosed() || DATA_socket.isClosed()) {
                                        close();
                                        try {
                                                Thread.sleep(50);
                                        } catch(InterruptedException ex) {
                                        }
                                } else {
                                        try {
                                                // this is the main loop for transferring
                                                len = 0;

                                                while(DATA_IN.available() > 0 && len < READ_BUFFER_SIZE) {
                                                        DATA_IN.read(rbuf, len, 1);
                                                        len++;
                                                }
                                                if(len > 0) {
                                                        mBuffer.add(rbuf, len);
                                                        onRead(len);
                                                }

                                        } catch(IOException ex) {
                                                close();
                                                // TO-DO: Needs to broadcast that it has died
                                                try {
                                                        Thread.sleep(50);
                                                } catch(InterruptedException exx) {
                                                }
                                                return;
                                        }
                                }
                        }
                } // end of run()
        }; // end of runnable

        @Override
        public String getPhysicalConnectionName() {
                return Physicaloid.WIFI_STRING;
        }

        @Override
        public int getPhysicalConnectionType() {
                return Physicaloid.WIFI;
        }

        @Override
        public void setDebug(boolean flag) {
                DEBUG_SHOW = flag;
        }
}
