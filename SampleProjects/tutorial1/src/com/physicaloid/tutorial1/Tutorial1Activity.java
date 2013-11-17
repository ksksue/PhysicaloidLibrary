package com.physicaloid.tutorial1;

import java.io.UnsupportedEncodingException;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.physicaloid.lib.Physicaloid;

public class Tutorial1Activity extends Activity {
    private static final String TAG = Tutorial1Activity.class.getSimpleName();
    /*
     * !!! You need to import PhysicaloidLibrary. !!!
     * If you have errors, check Project -> Properties -> Android -> Library.
     */

    /*
     * In this tutorial, You can learn
     *  - how to use open/close/read/write
     *  
     *  You might check TODO tags.
     */

    //****************************************************************
    // TODO : Add <uses-feature android:name="android.hardware.usb.host" /> to AndroidManifest.xml
    //****************************************************************
    Button btOpen, btClose, btRead, btWrite;
    EditText etWrite;
    TextView tvRead;

    Physicaloid mPhysicaloid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial1);

        btOpen  = (Button) findViewById(R.id.btOpen);
        btClose = (Button) findViewById(R.id.btClose);
        btRead  = (Button) findViewById(R.id.btRead);
        btWrite = (Button) findViewById(R.id.btWrite);
        etWrite = (EditText) findViewById(R.id.etWrite);
        tvRead  = (TextView) findViewById(R.id.tvRead);

        setEnabledUi(false);

        //****************************************************************
        // TODO : create a new instance
        mPhysicaloid = new Physicaloid(this);
        //****************************************************************
    }

    public void onClickOpen(View v) {
        //****************************************************************
        // TODO : open a device
        if(mPhysicaloid.open()) { // default 9600bps
            setEnabledUi(true);
        }
        //****************************************************************
    }

    public void onClickClose(View v) {
        //****************************************************************
        // TODO : close the device
        if(mPhysicaloid.close()) {
            setEnabledUi(false);
        }
        //****************************************************************
    }

    public void onClickRead(View v) {
        byte[] buf = new byte[256];
        int readSize=0;

        //****************************************************************
        // TODO : read from the device to a buffer and get read size
        readSize = mPhysicaloid.read(buf);
        //****************************************************************

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
            //****************************************************************
            // TODO : write a buffer to the device
            mPhysicaloid.write(buf, buf.length);
            //****************************************************************
        }
    }

    private void setEnabledUi(boolean on) {
        if(on) {
            btOpen.setEnabled(false);
            btClose.setEnabled(true);
            btRead.setEnabled(true);
            btWrite.setEnabled(true);
            etWrite.setEnabled(true);
            tvRead.setEnabled(true);
        } else {
            btOpen.setEnabled(true);
            btClose.setEnabled(false);
            btRead.setEnabled(false);
            btWrite.setEnabled(false);
            etWrite.setEnabled(false);
            tvRead.setEnabled(false);
        }
    }
}
