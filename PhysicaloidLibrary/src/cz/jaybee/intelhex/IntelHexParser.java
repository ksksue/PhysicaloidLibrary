/**
 * @license
 * Copyright (c) 2012, Jan Breuer
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this 
 * list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */
package cz.jaybee.intelhex;

import java.io.*;

/**
 *
 * @author Jan Breuer
 * @license BSD 2-Clause
 */
public class IntelHexParser {

    private BufferedReader reader = null;
    private IntelHexDataListener dataListener = null;
    private static final int HEX = 16;
    private boolean eof = false;
    private int recordIdx = 0;
    private long upperAddress = 0;

    private class Record {

        int length;
        int address;
        IntelHexRecordType type;
        byte[] data;
    }

    public IntelHexParser(Reader reader) {
        if (reader instanceof BufferedReader) {
            this.reader = (BufferedReader) reader;
        } else {
            this.reader = new BufferedReader(reader);
        }
    }

    public IntelHexParser(InputStream stream) {
        this.reader = new BufferedReader(new InputStreamReader(stream));
    }

    public void setDataListener(IntelHexDataListener listener) {
        this.dataListener = listener;
    }

    private Record parseRecord(String record) throws Exception {
        Record result = new Record();
        // check, if there wasn an accidential EOF record
        if (eof) {
            throw new Exception("Data after eof (" + recordIdx + ")");
        }

        // every IntelHEX record must start with ":"
        if (!record.startsWith(":")) {
            throw new Exception("Invalid Intel HEX record (" + recordIdx + ")");
        }

        int lineLength = record.length();
        byte[] hexRecord = new byte[lineLength / 2];

        // sum of all bytes modulo 256 (including checksum) shuld be 0
        int sum = 0;
        for (int i = 0; i < hexRecord.length; i++) {
            String num = record.substring(2 * i + 1, 2 * i + 3);
            hexRecord[i] = (byte) Integer.parseInt(num, HEX);
            sum += hexRecord[i] & 0xff;
        }
        sum &= 0xff;

        if (sum != 0) {
            throw new Exception("Invalid checksum (" + recordIdx + ")");
        }

        // if the length field does not correspond with line length
        result.length = hexRecord[0];
        if ((result.length + 5) != hexRecord.length) {
            throw new Exception("Invalid record length (" + recordIdx + ")");
        }
        // length is OK, copy data
        result.data = new byte[result.length];
        System.arraycopy(hexRecord, 4, result.data, 0, result.length);

        // build lower part of data address
        result.address = ((hexRecord[1] & 0xFF) << 8) + (hexRecord[2] & 0xFF);

        // determine record type
        result.type = IntelHexRecordType.fromInt(hexRecord[3] & 0xFF);
        if (result.type == IntelHexRecordType.UNKNOWN) {
            throw new Exception("Unsupported record type " + (hexRecord[3] & 0xFF) + " (" + recordIdx + ")");
        }

        return result;
    }

    private void processRecord(Record record) throws Exception {
        // build full address
        long addr = record.address | upperAddress;
        switch (record.type) {
            case DATA:
                if (dataListener != null) {
                    dataListener.data(addr, record.data);
                }
                break;
            case EOF:
                if (dataListener != null) {
                    dataListener.eof();
                }
                eof = true;
                break;
            case EXT_LIN:
                if (record.length == 2) {
                    upperAddress = ((record.data[0] & 0xFF) << 8) + (record.data[1] & 0xFF);
                    upperAddress <<= 16; // ELA is bits 16-31 of the segment base address (SBA), so shift left 16 bits
                } else {
                    throw new Exception("Invalid EXT_LIN record (" + recordIdx + ")");
                }

                break;
            case EXT_SEG:
                if (record.length == 2) {
                    upperAddress = ((record.data[0] & 0xFF) << 8) + (record.data[1] & 0xFF);
                    upperAddress <<= 4; // ESA is bits 4-19 of the segment base address (SBA), so shift left 4 bits
                } else {
                    throw new Exception("Invalid EXT_SEG record (" + recordIdx + ")");
                }
                break;
            case START_SEG:
            case START_LIN:
                throw new Exception(record.type + " record not implemented (" + recordIdx + ")");
            case UNKNOWN:
                break;
        }

    }

    public void parse() throws IOException, Exception {
        recordIdx = 1;
        upperAddress = 0;
        String recordStr;

        while ((recordStr = reader.readLine()) != null) {
            Record record = parseRecord(recordStr);
            processRecord(record);
            recordIdx++;
        }
        
        if (!eof) {
            throw new Exception("No eof at the end of file");
        }
    }
}
