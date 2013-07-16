package com.physicaloid.lib.usb;

public enum UsbIds {
    FTDI        (Vid.FTDI,  0,  DriverType.FTDI),
    ARDUINO     (Vid.AVR,   0,  DriverType.CDCACM),
    MBED        (Vid.NXP,   0,  DriverType.CDCACM);

    int vid;
    int pid;
    int driverType;

    UsbIds(int vid, int pid, int driverType){
        this.vid        = vid;
        this.pid        = pid;
        this.driverType = driverType;
    }

    public class Vid {
        public static final int FTDI        = 0x0403;
        public static final int AVR         = 0x0403;
        public static final int NXP         = 0x0403;
    }

    public class DriverType {
        public static final int FTDI    = 1;
        public static final int CDCACM  = 2;
    }

}
