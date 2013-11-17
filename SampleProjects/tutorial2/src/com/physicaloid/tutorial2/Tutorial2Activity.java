package com.physicaloid.tutorial2;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.physicaloid.lib.Boards;
import com.physicaloid.lib.Physicaloid;

public class Tutorial2Activity extends Activity {
    private static final String TAG = Tutorial2Activity.class.getSimpleName();
    /*
     * !!! You need to import PhysicaloidLibrary. !!!
     * If you have errors, check Project -> Properties -> Android -> Library.
     */

    /*
     * In this tutorial, You can learn
     *  - how to use upload
     *  
     *  You might check TODO tags.
     */

    Button btOpen, btClose, btRead, btWrite, btUpload;
    EditText etWrite;
    TextView tvRead;

    Physicaloid mPhysicaloid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial2);

        btOpen  = (Button) findViewById(R.id.btOpen);
        btClose = (Button) findViewById(R.id.btClose);
        btRead  = (Button) findViewById(R.id.btRead);
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
        }
    }

    public void onClickClose(View v) {
        if(mPhysicaloid.close()) {
            setEnabledUi(false);
        }
    }

    public void onClickRead(View v) {
        byte[] buf = new byte[256];
        int readSize=0;

        readSize = mPhysicaloid.read(buf);

        if(readSize>0) {
            String str;
            try {
                str = new String(buf, "UTF-8");
                tvRead.append(str);
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG,e.toString());
            }
        }
    }

    public void onClickWrite(View v) {
        String str = etWrite.getText().toString();
        if(str.length()>0) {
            byte[] buf = str.getBytes();
            mPhysicaloid.write(buf, buf.length);
        }
    }

    public void onClickUpload(View v) {
        try {
            //****************************************************************
            // TODO : set board type and assets file.
            // TODO : copy .hex file to porject_dir/assets directory.
            mPhysicaloid.upload(Boards.POCKETDUINO, getResources().getAssets().open("SerialEchoback.PocketDuino.hex"));
            //****************************************************************
        } catch (RuntimeException e) {
            Log.e(TAG, e.toString());
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }

    }

    private void setEnabledUi(boolean on) {
        if(on) {
            btOpen.setEnabled(false);
            btClose.setEnabled(true);
            btRead.setEnabled(true);
            btWrite.setEnabled(true);
            btUpload.setEnabled(true);
            etWrite.setEnabled(true);
            tvRead.setEnabled(true);
        } else {
            btOpen.setEnabled(true);
            btClose.setEnabled(false);
            btRead.setEnabled(false);
            btWrite.setEnabled(false);
            btUpload.setEnabled(true);
            etWrite.setEnabled(false);
            tvRead.setEnabled(false);
        }
    }
}
