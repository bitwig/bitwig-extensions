package com.bitwig.extensions.controllers.nativeinstruments.komplete.midi;

/**
 * @author Eric
 */
public abstract class NhiaSysexCommand {
   public static final byte SYSEX_START = (byte) 0xF0;
   public static final byte SYSEX_END = (byte) 0xF7;

   public final static byte OFF = 0;
   public final static byte ON = 1;

   static final byte[] BASE_FORMAT = {SYSEX_START, //
      0, 0x21, 0x09, // Manufacturer ID
      0x0, 0x0, // Device ID
      0x44, 0x43, // Protocol ID
      0x01, 0x00, // Protocol Version
      0x40, // Track Available
      0x00, // Track value
      0x00, // Track number
      SYSEX_END}; // END

}
