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
import android.util.Log;
import com.physicaloid.lib.UsbVidList;
import com.physicaloid.lib.bluetooth.driver.uart.UartBluetooth;
import com.physicaloid.lib.usb.UsbAccessor;
import com.physicaloid.lib.usb.driver.uart.UartCdcAcm;
import com.physicaloid.lib.usb.driver.uart.UartCp210x;
import com.physicaloid.lib.usb.driver.uart.UartFtdi;
import com.physicloid.lib.wifi.driver.uart.UartWifi;

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
                Log.d(TAG, "Network available?");
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                return cm.getActiveNetworkInfo() != null;
        }

        /**
         * Scan and find a matching driver.
         *
         * @param context
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
                                for(UsbVidList usbVid : UsbVidList.values()) {
                                        if(vid == usbVid.getVid()) {
                                                if(vid == UsbVidList.FTDI.getVid()) {
                                                        return new UartFtdi(context);
                                                } else if(vid == UsbVidList.CP210X.getVid()) {
                                                        return new UartCp210x(context);
                                                }
                                        }
                                }
                        }
                        sc = new UartCdcAcm(context);
                        if(!USE_WIFI && !USE_BLUETOOTH) {
                                // early exit
                                return sc;
                        }
                        // check if it can actually open....
                        if(sc.open()) {
                                Log.d(TAG, "*************************whut?");
                                sc.close();
                                return sc;
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


                // This should cause a safe failure.
                if(sc == null) {
                        sc = new UartCdcAcm(context);
                }
                return sc;
        }
}
