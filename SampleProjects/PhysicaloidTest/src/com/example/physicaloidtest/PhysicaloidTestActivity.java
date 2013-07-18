package com.example.physicaloidtest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.physicaloid.lib.Boards;
import com.physicaloid.lib.Physicaloid;
import com.physicaloid.lib.Physicaloid.UploadCallBack;
import com.physicaloid.lib.programmer.avr.UploadErrors;
import com.physicaloid.lib.usb.driver.uart.ReadLisener;

public class PhysicaloidTestActivity extends Activity {

    @SuppressLint("SdCardPath")
    private static final String UPLOAD_FILE = "/sdcard/arduino/serialtest.uno.hex";
    Physicaloid mPhysicaloid;

    Button btOpen;
    Button btClose;
    Button btWrite;
    Button btRead;
    Button btReadCallback;
    Button btUpload;
    EditText etWrite;
    TextView tvRead;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_physicaloid_test);

        btOpen          = (Button) findViewById(R.id.btOpen);
        btClose         = (Button) findViewById(R.id.btClose);
        btWrite         = (Button) findViewById(R.id.btWrite);
        btRead          = (Button) findViewById(R.id.btRead);
        btReadCallback  = (Button) findViewById(R.id.btReadCallback);
        btUpload        = (Button) findViewById(R.id.btUpload);
        etWrite         = (EditText) findViewById(R.id.etWrite);
        tvRead          = (TextView) findViewById(R.id.tvRead);

        updateViews(false);

        mPhysicaloid = new Physicaloid(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        close();
    }

    public void onClickOpen(View v) {
        if(mPhysicaloid.open()) {
            updateViews(true);
        } else {
            Toast.makeText(this, "Cannot open", Toast.LENGTH_LONG).show();
        }
    }

    public void onClickClose(View v) {
        close();
    }

    private void close() {
        if(mPhysicaloid.close()) {
            updateViews(false);
        }
    }

    public void onClickWrite(View v) {
        String str = etWrite.getText().toString();
        byte[] buf = str.getBytes();
        mPhysicaloid.write(buf, buf.length);
    }

    public void onClickRead(View v) {
        byte[] buf = new byte[256];
        mPhysicaloid.read(buf, buf.length);
        String str = new String(buf);
        tvRead.append(Html.fromHtml("<font color=red>"+str+"</font>"));
    }

    private boolean readCallbackOn = false;
    public void onClickReadCallback(View v) {
        if(readCallbackOn) {
            mPhysicaloid.clearReadListener();
            btReadCallback.setText("ReadCallbackOff");
            btRead.setEnabled(true);
            readCallbackOn = false;
        } else {
            mPhysicaloid.addReadListener(new ReadLisener() {
                @Override
                public void onRead(int size) {
                    byte[] buf = new byte[size];
                    mPhysicaloid.read(buf, size);
                    tvAppend(tvRead, Html.fromHtml("<font color=blue>"+new String(buf)+"</font>"));
                }
            });
            btReadCallback.setText("ReadCallbackOn");
            btRead.setEnabled(false);
            readCallbackOn = true;
        }
    }

    public void onClickUpload(View v) {
        mPhysicaloid.upload(Boards.ARDUINO_UNO, UPLOAD_FILE,
                mUploadCallback);
    }

    UploadCallBack mUploadCallback = new UploadCallBack() {
        
        @Override
        public void onUploading(int value) {
            tvAppend(tvRead, "Upload : "+value+" %\n");
        }
        
        @Override
        public void onPreUpload() {
            tvAppend(tvRead, "Upload : Start\n");
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
        public void onError(UploadErrors err) {
            tvAppend(tvRead, "Error  : "+err.toString()+"\n");
        }
    };

    private void updateViews(boolean on) {
        if(on) {
            btOpen.setEnabled(false);
            btClose.setEnabled(true);
            btWrite.setEnabled(true);
            btRead.setEnabled(true);
            btReadCallback.setEnabled(true);
            etWrite.setEnabled(true);
        } else {
            btOpen.setEnabled(true);
            btClose.setEnabled(false);
            btWrite.setEnabled(false);
            btRead.setEnabled(false);
            btReadCallback.setEnabled(false);
            etWrite.setEnabled(false);
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


    private String toHexStr(byte[] b, int length) {
        String str="";
        for(int i=0; i<length; i++) {
            str += String.format("%02x ", b[i]);
        }
        return str;
    }
}
