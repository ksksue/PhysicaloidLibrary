package com.physicaloid.tutorial3;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.physicaloid.lib.Boards;
import com.physicaloid.lib.Physicaloid;
import com.physicaloid.lib.Physicaloid.UploadCallBack;
import com.physicaloid.lib.programmer.avr.UploadErrors;
import com.physicaloid.lib.usb.driver.uart.ReadLisener;

public class Tutorial3Activity extends Activity {
    private static final String TAG = Tutorial3Activity.class.getSimpleName();
    /*
     * !!! You need to import PhysicaloidLibrary. !!!
     * If you have errors, check Project -> Properties -> Android -> Library.
     */

    /*
     * In this tutorial, You can learn
     *  - how to use read/upload callbacks
     *  
     *  You might check TODO tags.
     */

    Button btOpen, btClose, btWrite, btUpload;
    EditText etWrite;
    TextView tvRead;

    Physicaloid mPhysicaloid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial3);

        btOpen  = (Button) findViewById(R.id.btOpen);
        btClose = (Button) findViewById(R.id.btClose);
        btWrite = (Button) findViewById(R.id.btWrite);
        btUpload= (Button) findViewById(R.id.btUpload);
        etWrite = (EditText) findViewById(R.id.etWrite);
        tvRead  = (TextView) findViewById(R.id.tvRead);

        setEnabledUi(false);

        mPhysicaloid = new Physicaloid(this);

    }

    public void onClickOpen(View v) {
        if(mPhysicaloid.open()) { // default 9600bps
            setEnabledUi(true);

            //****************************************************************
            // TODO : add read callback
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
            //****************************************************************

        }
    }

    public void onClickClose(View v) {
        if(mPhysicaloid.close()) {
            setEnabledUi(false);
            //****************************************************************
            // TODO : clear read callback
            mPhysicaloid.clearReadListener();
            //****************************************************************
        }
    }

    public void onClickWrite(View v) {
        String str = etWrite.getText().toString();
        if(str.length()>0) {
            byte[] buf = str.getBytes();
            mPhysicaloid.write(buf, buf.length);
        }
    }

    //****************************************************************
    // TODO : create upload callback
    // normal process:
    // onPreUpload -> onUploading -> onPostUpload
    //
    // cancel:
    // onPreUpload -> onUploading -> onCancel -> onPostUpload
    //
    // error:
    // onPreUpload  |
    // onUploading  | -> onError
    // onPostUpload |

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
    //****************************************************************

    public void onClickUpload(View v) {
        try {
            //****************************************************************
            // TODO : add upload callback
            mPhysicaloid.upload(Boards.POCKETDUINO, getResources().getAssets().open("SerialEchoback.PocketDuino.hex"), mUploadCallback);
            //****************************************************************
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
        } else {
            btOpen.setEnabled(true);
            btClose.setEnabled(false);
            btWrite.setEnabled(false);
            btUpload.setEnabled(true);
            etWrite.setEnabled(false);
            tvRead.setEnabled(false);
        }
    }
}
