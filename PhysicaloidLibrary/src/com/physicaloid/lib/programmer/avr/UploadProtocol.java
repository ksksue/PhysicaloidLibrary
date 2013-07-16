package com.physicaloid.lib.programmer.avr;

import com.physicaloid.lib.Physicaloid.UploadCallBack;
import com.physicaloid.lib.framework.SerialCommunicator;

public abstract class UploadProtocol {
    @SuppressWarnings("unused")
    private static final String TAG = UploadProtocol.class.getSimpleName();

    UploadCallBack callback;
    public UploadProtocol(){};

    public abstract void setSerial(SerialCommunicator comm);
    public abstract void setConfig(AvrConf conf, AVRMem mem);
    public abstract int  open();
    public abstract void enable();
    public abstract int  initialize();
    public abstract int  check_sig_bytes();
    public abstract int  paged_write();
    public abstract void disable();

    public void setCallback(UploadCallBack callback) {
        this.callback = callback;
    }

    protected void report_progress(int prog) {
        if(callback == null) return;
        callback.onUploading(prog);
    }
}
