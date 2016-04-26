/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.physicaloid.lib.programmer.avr;

/**
 *
 * @author root
 */
public class AVRConfMemFlash {

    String desc;
    boolean paged;
    int size;
    int page_size;
    int num_pages;
    int min_write_delay;
    int max_write_delay;
    int readback_p1;
    int readback_p2;
    String[] read_lo;
    String[] read_hi;
    String[] loadpage_lo;
    String[] loadpage_hi;
    String[] load_ext_addr;
    String[] writepage;

    int mode;
    int delay;
    int blocksize;
    int readsize;

    public AVRConfMemFlash(
            boolean memoryPaged,
            int memorySize,
            int memoryPage_size,
            int memoryNum_pages,
            int memoryMin_write_delay,
            int memoryMax_write_delay,
            int memoryReadback_p1,
            int memoryReadback_p2,
            String[] memoryRead_lo,
            String[] memoryRead_hi,
            String[] memoryLoadpage_lo,
            String[] memoryLoadpage_hi,
            String[] memoryWritepage,
            String[] memoryLoad_ext_addr,

            int memoryMode,
            int memoryDelay,
            int memoryBlocksize,
            int memoryReadsize) {

        desc            = "flash";
        paged           = memoryPaged;
        size            = memorySize;
        page_size       = memoryPage_size;
        num_pages       = memoryNum_pages;
        min_write_delay = memoryMin_write_delay;
        max_write_delay = memoryMax_write_delay;
        readback_p1     = memoryReadback_p1;
        readback_p2     = memoryReadback_p2;
        read_lo         = memoryRead_lo;
        read_hi         = memoryRead_hi;
        loadpage_lo     = memoryLoadpage_lo;
        loadpage_hi     = memoryLoadpage_hi;
        writepage       = memoryWritepage;

        mode            = memoryMode;
        delay           = memoryDelay;
        blocksize       = memoryBlocksize;
        readsize        = memoryReadsize;
    }
}
/*
memory "flash"
paged           = yes;
size            = 32768;
page_size       = 128;
num_pages       = 256;
min_write_delay = 4500;
max_write_delay = 4500;
readback_p1     = 0xff;
readback_p2     = 0xff;
read_lo = " 0 0 1 0 0 0 0 0",
          " 0 0 a13 a12 a11 a10 a9 a8",
          " a7 a6 a5 a4 a3 a2 a1 a0",
          " o o o o o o o o";

read_hi = " 0 0 1 0 1 0 0 0",
          " 0 0 a13 a12 a11 a10 a9 a8",
          " a7 a6 a5 a4 a3 a2 a1 a0",
          " o o o o o o o o";

loadpage_lo = " 0 1 0 0 0 0 0 0",
              " 0 0 0 x x x x x",
              " x x a5 a4 a3 a2 a1 a0",
              " i i i i i i i i";

loadpage_hi = " 0 1 0 0 1 0 0 0",
              " 0 0 0 x x x x x",
              " x x a5 a4 a3 a2 a1 a0",
              " i i i i i i i i";

writepage = " 0 1 0 0 1 1 0 0",
            " 0 0 a13 a12 a11 a10 a9 a8",
            " a7 a6 x x x x x x",
            " x x x x x x x x";

mode            = 0x41;
delay           = 6;
blocksize       = 128;
readsize        = 256;

;
*/

