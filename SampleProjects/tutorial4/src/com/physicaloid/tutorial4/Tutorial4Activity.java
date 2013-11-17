package com.physicaloid.tutorial4;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import com.physicaloid.lib.Boards;
import com.physicaloid.lib.Physicaloid;
import com.physicaloid.lib.Physicaloid.UploadCallBack;
import com.physicaloid.lib.programmer.avr.UploadErrors;
import com.physicaloid.lib.usb.driver.uart.ReadLisener;
import com.physicaloid.lib.usb.driver.uart.UartConfig;

public class Tutorial4Activity extends Activity {
    private static final String TAG = Tutorial4Activity.class.getSimpleName();
    /*
     * !!! You need to import PhysicaloidLibrary. !!!
     * If you have errors, check Project -> Properties -> Android -> Library.
     */

    /*
     * In this tutorial, You can learn
     *  - how to set UART configurations
     *  
     *  You might check TODO tags.
     */

    Button btOpen, btClose, btWrite, btUpload;
    EditText etWrite;
    TextView tvRead;
    RadioGroup rgBaudrate;
    RadioButton rb0, rb1;

    Physicaloid mPhysicaloid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial4);

        btOpen  = (Button) findViewById(R.id.btOpen);
        btClose = (Button) findViewById(R.id.btClose);
        btWrite = (Button) findViewById(R.id.btWrite);
        btUpload= (Button) findViewById(R.id.btUpload);
        etWrite = (EditText) findViewById(R.id.etWrite);
        tvRead  = (TextView) findViewById(R.id.tvRead);
        rgBaudrate = (RadioGroup) findViewById(R.id.rgBaudrate);
        rb0 = (RadioButton) findViewById(R.id.rb0);
        rb1 = (RadioButton) findViewById(R.id.rb1);

        setEnabledUi(false);

        mPhysicaloid = new Physicaloid(this);

        rgBaudrate.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                case R.id.rb0:
                    tvAppend(tvRead, "set 9600\n");
                    //****************************************************************
                    // TODO : set only baudrate
                    mPhysicaloid.setBaudrate(9600);
                    //****************************************************************
                    break;

                case R.id.rb1:
                    tvAppend(tvRead, "set 115200\n");
                    //****************************************************************
                    // TODO : set uart configurations
                    UartConfig uartConfig = new UartConfig(115200, UartConfig.DATA_BITS8, UartConfig.STOP_BITS1, UartConfig.PARITY_NONE, false, false);
                    mPhysicaloid.setConfig(uartConfig);
                    //****************************************************************
                    break;

                default:
                    tvAppend(tvRead, "no set");
                    break;
                }
            }
        });

    }

    public void onClickOpen(View v) {
        if(mPhysicaloid.open()) { // default 9600bps
            setEnabledUi(true);

            mPhysicaloid.addReadListener(new ReadLisener() {
                String readStr;

                // callback when reading one or more size buffer
                @Override
                public void onRead(int size) {
                    byte[] buf = new byte[size];

                    mPhysicaloid.read(buf, size);
                    try {
                        readStr = new String(buf, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        Log.e(TAG,e.toString());
                        return;
                    }

                    // UI thread
                    tvAppend(tvRead, readStr);
                }
            });

        }
    }

    public void onClickClose(View v) {
        if(mPhysicaloid.close()) {
            setEnabledUi(false);
            mPhysicaloid.clearReadListener();
        }
    }

    public void onClickWrite(View v) {
        String str = etWrite.getText().toString();
        if(str.length()>0) {
            byte[] buf = str.getBytes();
            mPhysicaloid.write(buf, buf.length);
        }
    }

    private UploadCallBack mUploadCallback = new UploadCallBack() {
        @Override
        public void onPreUpload() {
            tvAppend(tvRead, "Upload : Start\n");
        }

        @Override
        public void onUploading(int value) {
            tvAppend(tvRead, "Upload : "+value+" %\n");
        }

        @Override
        public void onPostUpload(boolean success) {
            if(success) {
                tvAppend(tvRead, "Upload : Successful\n");
            } else {
                tvAppend(tvRead, "Upload fail\n");
            }
        }

        @Override
        public void onCancel() {
            tvAppend(tvRead, "Cancel uploading\n");
        }

        @Override
        public void onError(UploadErrors err) {
            tvAppend(tvRead, "Error  : "+err.toString()+"\n");
        }
    };

    public void onClickUpload(View v) {
        try {
            mPhysicaloid.upload(Boards.POCKETDUINO, getResources().getAssets().open("SerialEchoback.PocketDuino.hex"), mUploadCallback);
        } catch (RuntimeException e) {
            Log.e(TAG, e.toString());
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }

    }

    Handler mHandler = new Handler();
    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                ftv.append(ftext);
            }
        });
    }

    private void setEnabledUi(boolean on) {
        if(on) {
            btOpen.setEnabled(false);
            btClose.setEnabled(true);
            btWrite.setEnabled(true);
            btUpload.setEnabled(true);
            etWrite.setEnabled(true);
            tvRead.setEnabled(true);
            rb0.setEnabled(true);
            rb1.setEnabled(true);
        } else {
            btOpen.setEnabled(true);
            btClose.setEnabled(false);
            btWrite.setEnabled(false);
            btUpload.setEnabled(true);
            etWrite.setEnabled(false);
            tvRead.setEnabled(false);
            rb0.setEnabled(false);
            rb1.setEnabled(false);
        }
    }
}
