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

package com.physicaloid.lib;

import android.content.Context;

import com.physicaloid.lib.framework.AutoCommunicator;
import com.physicaloid.lib.framework.SerialCommunicator;
import com.physicaloid.lib.framework.Uploader;
import com.physicaloid.lib.programmer.avr.UploadErrors;
import com.physicaloid.lib.usb.driver.uart.UartConfig;
import com.physicaloid.lib.usb.driver.uart.ReadLisener;

public class Physicaloid {

    Context mContext;
    Boards mBoard;

    SerialCommunicator mSerial;
    Uploader mUploader;

    UploadCallBack mCallBack;   // callback on program() method
    String mFilePath;           // file path on program() method

    public Physicaloid(Context context) {
        this.mContext = context;
    }

    /**
     * Opens a device and communicate USB UART by default settings
     * @return true : successful , false : fail
     * @throws RuntimeException
     */
    public boolean open() throws RuntimeException {
        return open(new UartConfig());
    }

    /**
     * Opens a device and communicate USB UART
     * @param uart UART configuration
     * @return true : successful , false : fail
     * @throws RuntimeException
     */
    public boolean open(UartConfig uart) throws RuntimeException {
        mSerial = new AutoCommunicator().getSerialCommunicator(mContext);
        if(mSerial == null) return false;
        synchronized (mSerial) {
            return mSerial.open();
        }
    }

    /**
     * Closes a device.
     * @return true : successful , false : fail
     * @throws RuntimeException
     */
    public boolean close() throws RuntimeException {
        if(mSerial == null) return false;
        synchronized (mSerial) {
            return mSerial.close();
        }
    }

    /**
     * Reads from a device
     * @param buf
     * @param size
     * @return read byte size
     * @throws RuntimeException
     */
    public int read(byte[] buf, int size) throws RuntimeException {
        if(mSerial == null) return 0;
        synchronized (mSerial) {
            return mSerial.read(buf, size);
        }
    }

    /**
     * Adds read listener
     * @param listener ReadListener
     * @return true : successful , false : fail
     * @throws RuntimeException
     */
    public boolean addReadListener(ReadLisener listener) throws RuntimeException {
        if(mSerial == null) return false;
        if(listener == null) return false;
        synchronized (mSerial) {
            mSerial.addReadListener(listener);
        }
        return true;
    }

    /**
     * Clears read listener
     * @throws RuntimeException
     */
    public void clearReadListener() throws RuntimeException {
        if(mSerial == null) return;
        synchronized (mSerial) {
            mSerial.clearReadListener();
        }
    }

    /**
     * Writes to a device.
     * @param buf
     * @param size
     * @return
     * @throws RuntimeException
     */
    public int write(byte[] buf, int size) throws RuntimeException {
        if(mSerial == null) return 0;
        synchronized (mSerial) {
            return mSerial.write(buf, size);
        }
    }

    /**
     * Uploads a binary file to a device on background process.
     * @param board board profile e.g. Boards.ARDUINO_UNO
     * @param filePath a binary file path e.g. /sdcard/arduino/Blink.hex
     * @throws RuntimeException
     */
    public void upload(Boards board, String filePath) throws RuntimeException {
        upload(board, filePath, null);
    }

    /**
     * Uploads a binary file to a device on background process.
     * @param board board profile e.g. Boards.ARDUINO_UNO
     * @param filePath a binary file path e.g. /sdcard/arduino/Blink.hex
     * @param callback
     * @return true: success, false: fail
     * @throws RuntimeException
     */
    public void upload(Boards board, String filePath, UploadCallBack callback) throws RuntimeException {

        mUploader   = new Uploader();
        mCallBack   = callback;
        mFilePath   = filePath;
        mBoard      = board;

        new Thread(new Runnable(){
            @Override
            public void run() {
                boolean cleanAfter = false;
                UartConfig tmpUartConfig = new UartConfig();

                if(mSerial == null) {   // if not open
                    mSerial = new AutoCommunicator().getSerialCommunicator(mContext);
                    if(!mSerial.open()) {   // fail
                        if(mCallBack != null) { mCallBack.onError(UploadErrors.OPEN_DEVICE); }
                        mBoard      = null;
                        mFilePath   = null;
                        mCallBack   = null;
                        mUploader   = null;
                        mSerial     = null;
                        return;
                    } else {    // successful
                        cleanAfter = true;
                    }
                } else {                // if already open
                    synchronized (mSerial) {
                        tmpUartConfig = mSerial.getUartConfig();
                    }
                }

                synchronized (mSerial) {
                    mUploader.upload(mFilePath, mBoard, mSerial, mCallBack);
                    mSerial.setUartConfig(tmpUartConfig); // recover if already open
                    if(cleanAfter) {
                        mSerial.close();
                        mSerial = null;
                    }
                }

                mBoard      = null;
                mFilePath   = null;
                mCallBack   = null;
                mUploader   = null;

            }
            
        }).start();
    }

    /**
     * Callbacks of program process
     * @author keisuke
     *
     */
    public interface UploadCallBack{
        /*
         * Callback methods
         */
        void onPreUpload();
        void onUploading(int value);    // TODO: implement this method
        void onPostUpload(boolean success);
        void onError(UploadErrors err);
    }

    /**
     * Sets 
     * @param settings
     */
    void setConfig(UartConfig settings) throws RuntimeException{
        if(mSerial == null) return;
        synchronized (mSerial) {
            mSerial.setUartConfig(settings);
        }
    }
}
