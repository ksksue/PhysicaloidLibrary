/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.physicaloid.lib.programmer.avr;

/**
 *
 * @author root
 */
public class AVRConfMemFuse {
    String name;
    int size;
    int min_write_delay;
    int max_write_delay;
    String[] read;
    String[] write;

    public AVRConfMemFuse(
            String memoryName,
            int memorySize,
            int memoryMinWriteDelay,
            int memoryMaxWriteDelay,
            String[] memoryRead,
            String[] memoryWrite) {
        name            = memoryName;
        size            = memorySize;
        min_write_delay = memoryMinWriteDelay;
        max_write_delay = memoryMaxWriteDelay;
        read            = memoryRead;
        write           = memoryWrite;
    }
}
/*
memory "lfuse"
    size = 1;
    min_write_delay = 4500;
    max_write_delay = 4500;
    read = "0 1 0 1 0 0 0 0 0 0 0 0 0 0 0 0",
           "x x x x x x x x o o o o o o o o";

    write = "1 0 1 0 1 1 0 0 1 0 1 0 0 0 0 0",
            "x x x x x x x x i i i i i i i i";
;
*/

