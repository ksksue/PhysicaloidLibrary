/*
 * Copyright (C) 2013 Keisuke SUZUKI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * Distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This code has built in knowledge of avrdude.
 * Thanks to avrdude coders
 *  Brian S. Dean, Joerg Wunsch, Eric Weddington, Jan-Hinnerk Reichert,
 *  Alex Shepherd, Martin Thomas, Theodore A. Roth, Michael Holzt
 *  Colin O'Flynn, Thomas Fischl, David Hoerl, Michal Ludvig,
 *  Darell Tan, Wolfgang Moser, Ville Voipio, Hannes Weisbach,
 *  Doug Springer, Brett Hagman
 *  and all contributers.
 */

package com.physicaloid.lib.programmer.avr;

public enum UploadErrors {
    // reference
    // http://stackoverflow.com/questions/446663/best-way-to-define-error-codes-strings-in-java
    AVR_CHIPTYPE    (1,     "Unknown chip type."),
    SIGNATURE       (2,     "Incorrect MCU signature."),
    FILE_OPEN       (3,     "Can't open firmware file."),
    HEX_FILE_OPEN   (4,     "Wrong .hex file."),
    OPEN_DEVICE     (5,     "Can't open connection to MCU."),
    // These two can be retried by an additional reset attempt and start from the beginning.
    CHIP_INIT       (6,     "Can't initialize MCU."),
    PAGE_WRITE      (7,     "An unexpected error occurred while writing flash."),
    NO_ERROR        (0, "");

    private final int code;
    private final String description;

    private UploadErrors(int code, String description) {
      this.code = code;
      this.description = description;
    }

    public String getDescription() {
       return description;
    }

    public int getCode() {
       return code;
    }

    @Override
    public String toString() {
      return description;
    }
}
