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
package com.physicaloid.lib.framework;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import com.physicaloid.lib.UsbVidList;
import com.physicaloid.lib.bluetooth.driver.uart.UartBluetooth;
import com.physicaloid.lib.usb.UsbAccessor;
import com.physicaloid.lib.usb.driver.uart.UartCdcAcm;
import com.physicaloid.lib.usb.driver.uart.UartCp210x;
import com.physicaloid.lib.usb.driver.uart.UartFtdi;
import com.physicaloid.lib.usb.driver.uart.UartWinCH34x;
import com.physicaloid.lib.wifi.driver.uart.UartWifi;

public class AutoCommunicator {

        @SuppressWarnings("unused")
        private static final String TAG = AutoCommunicator.class.getSimpleName();
        private static boolean USE_USB = true;
        private static boolean USE_WIFI = false;
        private static boolean USE_BLUETOOTH = false;
        private String mNetdest = null;
        private String mBlueName = null;
        private int mDport = 9001;
        private int mCport = 9002;

        public AutoCommunicator(boolean u, boolean w, boolean b, int Dport, int Cport, String Netdest, String BlueName) {
                USE_USB = u;
                USE_WIFI = w;
                USE_BLUETOOTH = b;
                mDport = Dport;
                mCport = Cport;
                mNetdest = Netdest;
                mBlueName = BlueName;

        }

        public AutoCommunicator() {
        }

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

        /**
         * Scan and find a matching driver.
         *
         * @param context
         *
         * @return SerialCommunicator driver object
         */
        public SerialCommunicator getSerialCommunicator(Context context) {
                SerialCommunicator sc = null;
                // Does USB say if the particular interface is in-use?
                if(USE_USB) {
                        UsbAccessor usbAccess = UsbAccessor.INSTANCE;
                        usbAccess.init(context);
                        for(UsbDevice device : usbAccess.manager().getDeviceList().values()) {
                                int vid = device.getVendorId();
                                int pid = device.getProductId();
                                for(UsbVidList usbVid : UsbVidList.values()) {
                                        if(vid == usbVid.getVid()) {
                                                if(vid == UsbVidList.FTDI.getVid()) {
                                                        Log.d(TAG, "FTDI");
                                                        sc = new UartFtdi(context);
                                                } else if(vid == UsbVidList.CP210X.getVid()) {
                                                        Log.d(TAG, "CP210x");
                                                        sc = new UartCp210x(context);
                                                } else if((vid == UsbVidList.DCCDUINO.getVid()) || (vid == UsbVidList.WCH.getVid())) {
                                                         Log.d(TAG, "POSSIBLY WCH");
                                                       // check PID
                                                        if(pid == 0x5523 || pid == 0x7523) {
                                                              Log.d(TAG, "Yes WCH!");
                                                                sc = new UartWinCH34x(context);
                                                        }
                                                }
                                        }
                                }
                        }
                        if(sc == null) {
                                Log.d(TAG, "POSSIBLY CDC-ACM");
                                sc = new UartCdcAcm(context);
                        }
                        // check if it can actually open....
                        if(sc.open()) {
                                Log.d(TAG, "*************************whut?");
                                sc.close();
                                return sc;
                        } else {
                                Log.d(TAG, "Nothing on USB");
                                sc = null;
                        }
                        if(!USE_WIFI && !USE_BLUETOOTH) {
                                // early exit
                                return null;
                        }
                }


                // WiFi
                if(USE_WIFI) {
                        if(isNetworkConnected(context)) {
                                Log.d(TAG, "Network available");
                                sc = new UartWifi(context, mNetdest, mDport, mCport);
                                // check if it can actually open....
                                if(sc.open()) {
                                        Log.d(TAG, "*************************whut?");
                                        sc.close();
                                        return sc;
                                } else {
                                        sc = null;
                                }
                        } else {
                                Log.d(TAG, "No Network available");
                        }
                }

                // Bluetooth
                if(USE_BLUETOOTH) {
                        sc = new UartBluetooth(context, mBlueName);
                        // Last one so we don't need to check it.
                }
                if(sc.open()) {
                        Log.d(TAG, "*************************whut?");
                        sc.close();
                        return sc;
                } else {
                        sc = null;
                }
                return sc;
        }
}
