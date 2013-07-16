package com.physicaloid.lib.usb;

public class UsbVidPid {
    @SuppressWarnings("unused")
    private static final String TAG = UsbVidPid.class.getSimpleName();

    private int vid;
    private int pid;

    public UsbVidPid(int vid, int pid) {
        this.vid = vid;
        this.pid = pid;
    }

    public void setVid(int vid) {this.vid = vid;}
    public void setPid(int pid) {this.pid = pid;}
    public int getVid() {return this.vid;}
    public int getPid() {return this.pid;}
}
