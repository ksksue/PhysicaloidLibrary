package com.physicaloid.lib;

public enum UsbVidList {
    ARDUINO                         (0x2341),
    FTDI                            (0x0403),
    MBED_LPC1768                    (0x0d28),
    MBED_LPC11U24                   (0x0d28),
    MBED_FRDM_KL25Z_OPENSDA_PORT    (0x1357),
    MBED_FRDM_KL25Z_KL25Z_PORT      (0x15a2);

    int vid;
    private UsbVidList(int vid) {
        this.vid = vid;
    }

    public int getVid() {
        return vid;
    }
}
