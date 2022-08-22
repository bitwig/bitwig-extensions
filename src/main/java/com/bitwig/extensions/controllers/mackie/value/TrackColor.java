package com.bitwig.extensions.controllers.mackie.value;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.controllers.mackie.DebugUtil;

public class TrackColor extends InternalHardwareLightState {

   private final int[] colors = new int[8];

   private TrackColor lastFetch = null;

   public TrackColor() {
   }

   private TrackColor(final int[] color) {
      System.arraycopy(color, 0, colors, 0, colors.length);
   }

   @Override
   public HardwareLightVisualState getVisualState() {
      return null;
   }

   @Override
   public boolean equals(final Object obj) {
      if (obj instanceof TrackColor) {
         return compares((TrackColor) obj);
      }
      return false;
   }

   private boolean compares(final TrackColor other) {
      for (int i = 0; i < colors.length; i++) {
         if (other.colors[i] != colors[i]) {
            return false;
         }
      }
      return true;
   }

   public void send(final MidiOut midiOut) {
      final StringBuilder sysEx = new StringBuilder("F0 00 00 68 16 14 ");
      for (int i = 0; i < colors.length; i++) {
         final int red = colors[i] >> 16;
         final int green = colors[i] >> 8 & 0x7F;
         final int blue = colors[i] & 0x7F;
         sysEx.append(String.format("%02x %02x %02x ", red, green, blue));
      }
      sysEx.append("F7");
      DebugUtil.println(" SEND Coloring %s", sysEx);
      midiOut.sendSysex(sysEx.toString());
   }

   public void set(final int index, final double r, final double g, final double b) {
      final int red = (int) (Math.floor(r * 127));
      final int green = (int) (Math.floor(g * 127));
      final int blue = (int) (Math.floor(b * 127));
      colors[index] = red << 16 | green << 8 | blue;
   }

   public TrackColor getState() {
      if (lastFetch != null && lastFetch.compares(this)) {
         return this;
      }
      lastFetch = new TrackColor(colors);
      return lastFetch;
   }
}
