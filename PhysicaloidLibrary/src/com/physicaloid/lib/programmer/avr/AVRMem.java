/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.physicaloid.lib.programmer.avr;

/**
 *
 * @author root
 */
public class AVRMem {
    public static final int AVR_OP_READ             = 0;
    public static final int AVR_OP_WRITE            = 1;
    public static final int AVR_OP_READ_LO          = 2;
    public static final int AVR_OP_READ_HI          = 3;
    public static final int AVR_OP_WRITE_LO         = 4;
    public static final int AVR_OP_WRITE_HI         = 5;
    public static final int AVR_OP_LOADPAGE_LO      = 6;
    public static final int AVR_OP_LOADPAGE_HI      = 7;
    public static final int AVR_OP_LOAD_EXT_ADDR    = 8;
    public static final int AVR_OP_WRITEPAGE        = 9;
    public static final int AVR_OP_CHIP_ERASE       = 10;
    public static final int AVR_OP_PGM_ENABLE       = 11;
    public static final int AVR_OP_MAX              = 12;

    public static final int AVR_CMDBIT_IGNORE   = 0;    /* bit is ignored on input and output */
    public static final int AVR_CMDBIT_VALUE    = 1;    /* bit is set to 0 or 1 for input or output */
    public static final int AVR_CMDBIT_ADDRESS  = 2;    /* this bit represents an input address bit */
    public static final int AVR_CMDBIT_INPUT    = 3;    /* this bit is an input bit */
    public static final int AVR_CMDBIT_OUTPUT   = 4;    /* this bit is an output bit */

    String desc;                // memory description ("flash", "eeprom", etc)
    boolean paged;                  // page addressed (e.g. ATmega flash)
    int size;                   // total memory size in bytes
    int page_size;              // size of memory page (if page addressed)
    int num_pages;              // number of pages (if page addressed)
    long offset;                // offset in IO memory (ATxmega)
    int min_write_delay;        // microseconds
    int max_write_delay;        // microseconds
    int pwroff_after_write;     // after this memory type is written to,
                                // the device must be powered off and
                                // back on, see errata
                                // http://www.atmel.com/atmel/acrobat/doc1280.pdf
    byte[] readback;            // polled read-back values

    int mode;                   // stk500 v2 xml file parameter
    int delay;                  // stk500 v2 xml file parameter
    int blocksize;              // stk500 v2 xml file parameter
    int readsize;               // stk500 v2 xml file parameter
    int pollindex;              // stk500 v2 xml file parameter

    byte[]      buf;            // pointer to memory buffer
    OPCODE[]    op;             // opcodes

    AVRMem(AvrConf avrConf){
        // Only memory type is "flash"
        desc                = avrConf.flash.desc;
        paged               = avrConf.flash.paged;
        size                = avrConf.flash.size;
        page_size           = avrConf.flash.page_size;
        num_pages           = avrConf.flash.num_pages;
        offset              = 0;
        min_write_delay     = avrConf.flash.min_write_delay;
        max_write_delay     = avrConf.flash.max_write_delay;
        pwroff_after_write  = 0;
        readback            = new byte[2];
        readback[0]         = (byte)avrConf.flash.readback_p1;
        readback[1]         = (byte)avrConf.flash.readback_p2;
        mode                = avrConf.flash.mode;
        delay               = avrConf.flash.delay;
        blocksize           = avrConf.flash.blocksize;
        readsize            = avrConf.flash.readsize;
        pollindex           = 0;
        buf                 = null;
        op                  = new OPCODE[AVR_OP_MAX];
        for(int i=0; i<AVR_OP_MAX; i++) {
            op[i] = new OPCODE();
        }
        parseOpcode(op[AVR_OP_READ_LO], avrConf.flash.read_lo);
        parseOpcode(op[AVR_OP_READ_HI], avrConf.flash.read_hi);
        parseOpcode(op[AVR_OP_LOADPAGE_LO], avrConf.flash.loadpage_lo);
        parseOpcode(op[AVR_OP_LOADPAGE_HI], avrConf.flash.loadpage_hi);
        parseOpcode(op[AVR_OP_LOAD_EXT_ADDR], avrConf.flash.load_ext_addr);
        parseOpcode(op[AVR_OP_WRITEPAGE], avrConf.flash.writepage);
    }

    void setBuf(byte[] inBuf, int length) {
        buf = new byte[length];
        System.arraycopy(inBuf, 0, buf, 0, length);
    }

    static void parseOpcode(OPCODE op, String[] mem) {
        String tmpstr="";
        String[] str;
        int no = 31;

        if(mem == null) {
            return;
        }

        for(int i=0; i<mem.length; i++) {
            tmpstr += mem[i] + " ";
        }

        // 最初の空白を取り除く
        while(tmpstr.charAt(0) == ' ') {
            tmpstr = tmpstr.substring(1);
        }
        str = tmpstr.split("[\\s]+");//空白がいくつあっても1区切りとする

        for(int i=0; i<32; i++) {
            if(str[i].charAt(0) == '0') {
                op.bit[no].type  = AVR_CMDBIT_VALUE;
                op.bit[no].bitno = no%8;
                op.bit[no].value = 0;
            } else if(str[i].charAt(0) == '1') {
                op.bit[no].type  = AVR_CMDBIT_VALUE;
                op.bit[no].bitno = no%8;
                op.bit[no].value = 1;
            } else if(str[i].charAt(0) == 'i') {
                op.bit[no].type  = AVR_CMDBIT_INPUT;
                op.bit[no].bitno = no%8;
                op.bit[no].value = 0;
            } else if(str[i].charAt(0) == 'o') {
                op.bit[no].type  = AVR_CMDBIT_OUTPUT;
                op.bit[no].bitno = no%8;
                op.bit[no].value = 0;
            } else if(str[i].charAt(0) == 'a') {
                op.bit[no].type  = AVR_CMDBIT_ADDRESS;
                op.bit[no].bitno = Integer.valueOf(str[i].substring(1));
                op.bit[no].value = 0;
            } else if(str[i].charAt(0) == 'x') {
                op.bit[no].type  = AVR_CMDBIT_IGNORE;
                op.bit[no].bitno = no%8;
                op.bit[no].value = 0;
            }
            no--;
        }
    }

    class OPCODE {
        CMDBIT[] bit = new CMDBIT[32];
        OPCODE() {
            for(int i=0; i<32; i++) {
                bit[i] = new CMDBIT();
            }
        }
        class CMDBIT {
            public int type;
            public int bitno;
            public int value;
        }
    }
}

/*
typedef struct avrmem {
  char desc[AVR_MEMDESCLEN];  // memory description ("flash", "eeprom", etc)
  int paged;                  // page addressed (e.g. ATmega flash)
  int size;                   // total memory size in bytes
  int page_size;              // size of memory page (if page addressed)
  int num_pages;              // number of pages (if page addressed)
  unsigned int offset;        // offset in IO memory (ATxmega)
  int min_write_delay;        // microseconds
  int max_write_delay;        // microseconds
  int pwroff_after_write;     // after this memory type is written to,
                              // the device must be powered off and
                              // back on, see errata
                              // http://www.atmel.com/atmel/acrobat/doc1280.pdf
  unsigned char readback[2];  // polled read-back values

  int mode;                   // stk500 v2 xml file parameter
  int delay;                  // stk500 v2 xml file parameter
  int blocksize;              // stk500 v2 xml file parameter
  int readsize;               // stk500 v2 xml file parameter
  int pollindex;              // stk500 v2 xml file parameter

  unsigned char * buf;        // pointer to memory buffer
  OPCODE * op[AVR_OP_MAX];    // opcodes
} AVRMEM;

*/

