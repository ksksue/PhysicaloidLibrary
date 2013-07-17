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

import com.physicaloid.lib.framework.SerialCommunicator;

public class UartFtdi extends SerialCommunicator {
    @SuppressWarnings("unused")
    private static final String TAG = UartFtdi.class.getSimpleName();

    public UartFtdi(Context context) {
        super(context);
    }


    @Override
    public boolean open() {
        return false;
    }


    @Override
    public boolean close() {
        return false;
    }


    @Override
    public int read(byte[] rbuf, int size) {
        return 0;
    }


    @Override
    public int write(byte[] wbuf, int size) {
        return 0;
    }


    @Override
    public boolean setUartConfig(UartConfig config) {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean init() {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean setBaudrate(int baudrate) {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean setDataBits(int dataBits) {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean setParity(int parity) {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean setStopBits(int stopBits) {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean setDtrRts(boolean dtr, boolean rts) {
        return false;
    }


    @Override
    public UartConfig getUartConfig() {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public int getBaudrate() {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    public int getDataBits() {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    public int getParity() {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    public int getStopBits() {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    public boolean getDtr() {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean getRts() {
        // TODO Auto-generated method stub
        return false;
    }

}
