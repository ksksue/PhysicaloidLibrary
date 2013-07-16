package com.physicaloid.lib.programmer.avr;

import cz.jaybee.intelhex.IntelHexParser;
import cz.jaybee.intelhex.IntelHexParserRun;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class IntelHexFileToBuf {

    @SuppressWarnings("unused")
    private static final String TAG = IntelHexFileToBuf.class.getSimpleName();

    IntelHexParser ihp;
    IntelHexParserRun ihpd;

    public IntelHexFileToBuf() {
    }

    public long getByteLength() {
        if(ihpd != null) {
            return ihpd.getTotalBufLength();
        } else {
            return 0;
        }
    }

    public void getHexData(byte[] buf) {
        if(ihpd != null) {
            ihpd.getBufData(buf);
        }
    }

    public void parse(String filePath) throws FileNotFoundException, IOException, Exception {
        InputStream is = null;
        is = new FileInputStream(filePath);
        parse(is);
    }

    public void parse(InputStream is) throws FileNotFoundException, IOException, Exception {

        ihp = new IntelHexParser(is);
        ihpd = new IntelHexParserRun(0, 0xFFFF);
        ihp.setDataListener(ihpd);

        ihp.parse();

        is.close();

    }

}
