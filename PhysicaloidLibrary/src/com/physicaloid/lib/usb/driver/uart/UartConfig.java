package com.physicaloid.lib.usb.driver.uart;

public class UartConfig {
    public static final int DATA_BITS7          = 7;
    public static final int DATA_BITS8          = 8;

    public static final int STOP_BITS1          = 1;
    public static final int STOP_BITS2          = 2;
    public static final int STOP_BITS1_5        = 3;

    public static final int PARITY_NONE         = 0;
    public static final int PARITY_ODD          = 1;
    public static final int PARITY_EVEN         = 2;
    public static final int PARITY_MARK         = 3;
    public static final int PARITY_SPACE        = 4;

    public int baudrate;
    public int dataBits;
    public int stopBits;
    public int parity;
    public boolean dtrOn;
    public boolean rtsOn;

    public UartConfig() {
        this.baudrate       = 9600;
        this.dataBits       = DATA_BITS8;
        this.stopBits       = STOP_BITS1;
        this.parity         = PARITY_NONE;
        this.dtrOn          = false;
        this.rtsOn          = false;
    }

    public UartConfig(int baudrate, int dataBits, int stopBits, int parity, boolean dtrOn, boolean rtsOn) {
        this.baudrate       = baudrate;
        this.dataBits       = dataBits;
        this.stopBits       = stopBits;
        this.parity         = parity;
        this.dtrOn          = dtrOn;
        this.rtsOn          = rtsOn;
    }
}
