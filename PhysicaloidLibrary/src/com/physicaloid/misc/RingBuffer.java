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

package com.physicaloid.misc;

import android.util.Log;

public class RingBuffer{
    private static final String TAG = RingBuffer.class.getSimpleName();

    private static final boolean DEBUG_SHOW_ADD = true;
    private static final boolean DEBUG_SHOW_GET = true;

    private int mRingBufSize;
    private byte[] mRingBuf;
    private int mAddOffset; // データの先頭
    private int mGetOffset; // データの後尾

    // バッファの位置が追い越されないよう、十分なサイズをとること
    public RingBuffer(int bufferSize) {
        mRingBufSize = bufferSize;
        mRingBuf = new byte[mRingBufSize];
        mAddOffset = 0;
        mGetOffset = 0;
        
    }

    public int getRingBufferSize() {
        return mRingBufSize;
    }

    public int getBufLength() {
        if(mAddOffset >= mGetOffset) {
            return mAddOffset - mGetOffset;
        } else {
            return mAddOffset + (mRingBufSize - mGetOffset);
        }
    }

    public synchronized int add(byte[] buf, int length) {
        int addLen = length;
        if((mAddOffset < mGetOffset) // storeがloadを追い抜く場合
                && (mAddOffset + length) >= mGetOffset) {
            addLen = mGetOffset - mAddOffset;
        }

        if(buf.length < addLen) {
            addLen = buf.length;
        }

        if((mAddOffset+addLen) > (mRingBufSize-1)) { // storeがバッファ終端をまたぐ場合
            int remain = mAddOffset + addLen - mRingBufSize;
            int copyLen = addLen-remain;
            if(copyLen != 0) {
                System.arraycopy(buf, 0, mRingBuf, mAddOffset, copyLen);
            }

            if(DEBUG_SHOW_ADD){ Log.d(TAG,"add("+length+") : copy buf[0:"+(copyLen-1)+"] to mRingBuf["+mAddOffset+":"+(mAddOffset+copyLen-1)+"]"); }

            mAddOffset = 0;

            if(DEBUG_SHOW_ADD){ Log.d(TAG,"add("+length+") : addOffset = "+mAddOffset+", getOffset = "+mGetOffset); }

            System.arraycopy(buf, copyLen, mRingBuf, mAddOffset, remain);
            mAddOffset = remain;

            if(DEBUG_SHOW_ADD){
                Log.d(TAG,"add("+length+") : copy buf["+(copyLen)+":"+(addLen-1)+"] to mRingBuf[0:"+(remain-1)+"]");
                Log.d(TAG,"add("+length+") : addOffset = "+mAddOffset+", getOffset = "+mGetOffset);
            }
            return addLen;
        } else {
            System.arraycopy(buf, 0, mRingBuf, mAddOffset, addLen);

            if(DEBUG_SHOW_ADD){ Log.d(TAG,"add("+length+") : copy buf[0:"+(addLen-1)+"] to mRingBuf["+mAddOffset+":"+(mAddOffset+addLen-1)+"]"); }

            mAddOffset += addLen;

            if(DEBUG_SHOW_ADD){ Log.d(TAG,"add("+length+") : addOffset = "+mAddOffset+", getOffset = "+mGetOffset);}

            return addLen;
        }
    }

    public synchronized int get(byte[] buf, int length) {
        int getLen = length;
        if(mAddOffset == mGetOffset) {
            return 0;
        }

        if((mGetOffset < mAddOffset) // storeがloadを追い抜く場合
                && (mGetOffset + length) > mAddOffset) {
            getLen = mAddOffset - mGetOffset;
        }

        if(buf.length < getLen) {
            getLen = buf.length;
        }

        if((mGetOffset+getLen) > (mRingBufSize-1)) {
            int remain = mGetOffset + getLen - mRingBufSize;
            int copyLen = getLen - remain;
            if( copyLen != 0) {
                System.arraycopy(mRingBuf, mGetOffset, buf, 0, copyLen);
            }

            if(DEBUG_SHOW_GET){ Log.d(TAG,"get("+length+") : copy mRingBuf["+mGetOffset+":"+(mGetOffset+copyLen-1)+"] to buf[0:"+(copyLen-1)+"]"); }

            mGetOffset = 0;

            if(DEBUG_SHOW_GET){  Log.d(TAG,"get("+length+") : addOffset = "+mAddOffset+", getOffset = "+mGetOffset); }

            System.arraycopy(mRingBuf, mAddOffset, buf, copyLen, remain);
            mGetOffset = remain;

            if(DEBUG_SHOW_GET){
                Log.d(TAG,"get("+length+") : copy mRingBuf[0:"+(remain-1)+"] to buf["+copyLen+":"+(remain-1)+"]");
                Log.d(TAG,"get("+length+") : addOffset = "+mAddOffset+", getOffset = "+mGetOffset);
            }
            return getLen;
        } else {
            System.arraycopy(mRingBuf, mGetOffset, buf, 0, getLen);

            if(DEBUG_SHOW_GET){ Log.d(TAG,"get("+length+") : copy mRingBuf["+mGetOffset+":"+(mGetOffset+getLen-1)+"] to buf[0:"+(getLen-1)+"]");}

            mGetOffset += getLen;

            if(DEBUG_SHOW_GET){ Log.d(TAG,"get("+length+") : addOffset = "+mAddOffset+", getOffset = "+mGetOffset); }

            return getLen;
        }
    }


    public synchronized void clear() {
        mAddOffset = 0;
        mGetOffset = 0;
    }

}
