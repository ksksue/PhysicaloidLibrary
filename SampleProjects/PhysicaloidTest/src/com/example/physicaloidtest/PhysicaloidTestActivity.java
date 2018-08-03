package com.example.physicaloidtest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.physicaloid.lib.Boards;
import com.physicaloid.lib.Physicaloid;
import com.physicaloid.lib.Physicaloid.UploadCallBack;
import com.physicaloid.lib.fpga.PhysicaloidFpga;
import com.physicaloid.lib.programmer.avr.UploadErrors;
import com.physicaloid.lib.usb.driver.uart.ReadListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PhysicaloidTestActivity extends Activity {

        private static final String TAG = PhysicaloidTestActivity.class.getSimpleName();
        // The serialtest.*.hex is an echo-back program.
        //  http://www.physicaloid.com/hexfiles/serialtest.ino
        // You can download those hex files from
        //  http://www.physicaloid.com/hexfiles/serialtest.uno.hex
        //  http://www.physicaloid.com/hexfiles/serialtest.mega.hex
        @SuppressLint("SdCardPath")
        private static final String UPLOAD_FILE_UNO = "/sdcard/arduino/serialtest.uno.hex";
        @SuppressLint("SdCardPath")
        private static final String UPLOAD_FILE_MEGA = "/sdcard/arduino/serialtest.mega.hex";
        @SuppressLint("SdCardPath")
        private static final String UPLOAD_FILE_BALANDUINO = "/sdcard/arduino/serialtest.balanduino.hex";
        @SuppressLint("SdCardPath")
        private static final String UPLOAD_FILE_FPGA = "/sdcard/fpga/testtop.rbf";
        private static final String ASSET_FILE_NAME_UNO = "Blink.uno.hex";
        private static final String ASSET_FILE_NAME_MEGA = "Blink.mega.hex";
        private static final String ASSET_FILE_NAME_BALANDUINO = "Blink.balanduino.hex";
        private static final String ASSET_FILE_NAME_FPGA = "testtop.rbf";
        Physicaloid mPhysicaloid;
        PhysicaloidFpga mPhysicaloidFpga;
        Boards mSelectedBoard;
        Button btOpen;
        Button btClose;
        Button btWrite;
        Button btRead;
        Button btReadCallback;
        Button btUpload;
        EditText etWrite;
        TextView tvRead;
        TextView tvSelectedBoard;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_physicaloid_test);

                btOpen = (Button) findViewById(R.id.btOpen);
                btClose = (Button) findViewById(R.id.btClose);
                btWrite = (Button) findViewById(R.id.btWrite);
                btRead = (Button) findViewById(R.id.btRead);
                btReadCallback = (Button) findViewById(R.id.btReadCallback);
                btUpload = (Button) findViewById(R.id.btUpload);
                etWrite = (EditText) findViewById(R.id.etWrite);
                tvRead = (TextView) findViewById(R.id.tvRead);
                tvSelectedBoard = (TextView) findViewById(R.id.tvSelectedBoard);

                updateViews(false);

                mPhysicaloid = new Physicaloid(this);

                // Shows last selected board
                mBoardList = new ArrayList<Boards>();
                for(Boards board : Boards.values()) {
                        if(board.support > 0) {
                                mBoardList.add(board);
                        }
                }
                int lastBoard = getSelectedBoard();
                tvSelectedBoard.setText(mBoardList.get(lastBoard).text);
                mSelectedBoard = mBoardList.get(lastBoard);
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
                tvRead.append(Html.fromHtml("<font color=red>" + str + "</font>"));
        }
        private boolean readCallbackOn = false;

        public void onClickReadCallback(View v) {
                if(readCallbackOn) {
                        mPhysicaloid.clearReadListener();
                        btReadCallback.setText("ReadCallbackOff");
                        btRead.setEnabled(true);
                        readCallbackOn = false;
                } else {
                        mPhysicaloid.addReadListener(new ReadListener() {

                                @Override
                                public void onRead(int size) {
                                        byte[] buf = new byte[size];
                                        mPhysicaloid.read(buf, size);
                                        tvAppend(tvRead, Html.fromHtml("<font color=blue>" + new String(buf) + "</font>"));
                                }
                        });
                        btReadCallback.setText("ReadCallbackOn");
                        btRead.setEnabled(false);
                        readCallbackOn = true;
                }
        }

        public void onClickUpload(View v) {
                String fileName;
                if(mSelectedBoard == Boards.ARDUINO_MEGA_2560_ADK) {
                        fileName = UPLOAD_FILE_MEGA;
                } else if(mSelectedBoard == Boards.BALANDUINO) {
                        fileName = UPLOAD_FILE_BALANDUINO;
                } else {
                        fileName = UPLOAD_FILE_UNO;
                }
                mPhysicaloid.upload(mSelectedBoard, fileName,
                        mUploadCallback);
        }

        public void onClickUploadAsset(View v) {
                String assetFileName;
                if(mSelectedBoard == Boards.ARDUINO_MEGA_2560_ADK) {
                        assetFileName = ASSET_FILE_NAME_MEGA;
                } else if(mSelectedBoard == Boards.BALANDUINO) {
                        assetFileName = ASSET_FILE_NAME_BALANDUINO;
                } else if(mSelectedBoard == Boards.PERIDOT) {
                        assetFileName = ASSET_FILE_NAME_FPGA;
                        PhysicaloidFpga physicaloidFpga = new PhysicaloidFpga(this);
                        try {
                                physicaloidFpga.upload(mSelectedBoard, getResources().getAssets().open(assetFileName), mUploadCallback);
                        } catch(RuntimeException e) {
                                Log.e(TAG, e.toString());
                        } catch(IOException e) {
                                Log.e(TAG, e.toString());
                        }
                        return;
                } else {
                        assetFileName = ASSET_FILE_NAME_UNO;
                }
                try {
                        mPhysicaloid.upload(mSelectedBoard, getResources().getAssets().open(assetFileName), mUploadCallback);
                } catch(RuntimeException e) {
                        Log.e(TAG, e.toString());
                } catch(IOException e) {
                        Log.e(TAG, e.toString());
                }
        }

        public void onClickCancelUpload(View v) {
                mPhysicaloid.cancelUpload();
        }
        UploadCallBack mUploadCallback = new UploadCallBack() {

                @Override
                public void onUploading(int value) {
                        tvAppend(tvRead, "Upload : " + value + " %\n");
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
                public void onCancel() {
                        tvAppend(tvRead, "Cancel uploading\n");
                }

                @Override
                public void onError(UploadErrors err) {
                        tvAppend(tvRead, "Error  : " + err.toString() + "\n");
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

        @SuppressWarnings("unused")
        private String toHexStr(byte[] b, int length) {
                String str = "";
                for(int i = 0; i < length; i++) {
                        str += String.format("%02x ", b[i]);
                }
                return str;
        }
        /////////////////////////////////////////////////////////////////
        // Board select menu
        /////////////////////////////////////////////////////////////////
        private static final int MENU_ID_BOARD = 1;

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
                // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.activity_main, menu);
                menu.add(Menu.NONE, MENU_ID_BOARD, Menu.NONE, "Select board.");
                return super.onCreateOptionsMenu(menu);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
                switch(item.getItemId()) {
                        case MENU_ID_BOARD:
                                showSelectBoardDialog();
                                return true;
                        default:
                                return false;
                }
        }
        private ArrayList<Boards> mBoardList;
        private int mItemPos = 0;

        private void showSelectBoardDialog() {

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Select a board");

                // get from Boards list
                List<String> items = new ArrayList<String>();
                for(Boards board : Boards.values()) {
                        if(board.support > 0) {
                                items.add(board.text);
                        }
                }
                String[] itemStr = (String[]) items.toArray(new String[0]);
                builder.setSingleChoiceItems(itemStr, getSelectedBoard(), mItemListener);

                builder.setPositiveButton("OK", mButtonListener);
                builder.setNeutralButton("Cancel", mButtonListener);

                AlertDialog dialog = builder.create();
                dialog.show();
        }
        DialogInterface.OnClickListener mItemListener = new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                        mItemPos = which;
                }
        };
        DialogInterface.OnClickListener mButtonListener = new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                        switch(which) {
                                case AlertDialog.BUTTON_POSITIVE:   // OK pressed
                                        saveSelectedBoard(mItemPos);
                                        mSelectedBoard = mBoardList.get(mItemPos);
                                        tvSelectedBoard.setText(mBoardList.get(mItemPos).text);
                                        break;
                                case AlertDialog.BUTTON_NEUTRAL:    // Cancel pressed
                                        break;
                        }
                }
        };

        // Saves selected board position
        private void saveSelectedBoard(int pos) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
                sp.edit().putInt("SelectedBoardPosition", pos).commit();
        }

        // Gets selected board position
        private int getSelectedBoard() {
                int pos;
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
                pos = sp.getInt("SelectedBoardPosition", 0);
                return pos;
        }
        /////////////////////////////////////////////////////////////////
        // End of board select menu
        /////////////////////////////////////////////////////////////////
}
