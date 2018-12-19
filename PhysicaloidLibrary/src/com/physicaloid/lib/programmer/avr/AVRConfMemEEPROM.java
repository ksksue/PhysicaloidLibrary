/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.physicaloid.lib.programmer.avr;

/**
 *
 * @author root
 */
public class AVRConfMemEEPROM {
    String desc;
    boolean paged;
    int page_size;
    int size;
    int min_write_delay;
    int max_write_delay;
    int readback_p1;
    int readback_p2;
    String[] read;
    String[] write;
    String[] loadpage_lo;
    String[] writepage;
    int mode;
    int delay;
    int blocksize;
    int readsize;

    public AVRConfMemEEPROM(
            boolean memoryPaged,
            int memoryPage_size,
            int memorySize,
            int memoryMin_write_delay,
            int memoryMax_write_delay,
            int memoryReadback_p1,
            int memoryReadback_p2,
            String[] memoryRead,
            String[] memoryWrite,
            String[] memoryLoadpage_lo,
            String[] memoryWritepage,
            int memoryMode,
            int memoryDelay,
            int memoryBlocksize,
            int memoryReadsize
            ) {
        desc            = "eeprom";
        paged           = memoryPaged;
        page_size       = memoryPage_size;
        size            = memorySize;
        min_write_delay = memoryMin_write_delay;
        max_write_delay = memoryMax_write_delay;
        readback_p1     = memoryReadback_p1;
        readback_p2     = memoryReadback_p2;
        read            = memoryRead;
        write           = memoryWrite;
        loadpage_lo     = memoryLoadpage_lo;
        writepage       = memoryWritepage;
        mode            = memoryMode;
        delay           = memoryDelay;
        blocksize       = memoryBlocksize;
        readsize        = memoryReadsize;
    }
}

/*
memory "eeprom"
paged           = no;
page_size       = 4;
size            = 1024;
min_write_delay = 3600;
max_write_delay = 3600;
readback_p1     = 0xff;
readback_p2     = 0xff;
read = " 1 0 1 0 0 0 0 0",
       " 0 0 0 x x x a9 a8",
       " a7 a6 a5 a4 a3 a2 a1 a0",
       " o o o o o o o o";

write = " 1 1 0 0 0 0 0 0",
        " 0 0 0 x x x a9 a8",
        " a7 a6 a5 a4 a3 a2 a1 a0",
        " i i i i i i i i";

loadpage_lo = " 1 1 0 0 0 0 0 1",
              " 0 0 0 0 0 0 0 0",
              " 0 0 0 0 0 0 a1 a0",
              " i i i i i i i i";

writepage = " 1 1 0 0 0 0 1 0",
            " 0 0 x x x x a9 a8",
            " a7 a6 a5 a4 a3 a2 0 0",
            " x x x x x x x x";

mode            = 0x41;
delay           = 20;
blocksize       = 4;
readsize        = 256;
;
*/

