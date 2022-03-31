package com.bitwig.extensions.controllers.mackie;

import com.bitwig.extension.controller.api.Device;

public enum VPotMode {
   // TRACK(NoteOnAssignment.V_TRACK, Assign.MIXER), //
   SEND(BasicNoteOnAssignment.V_SEND, Assign.BOTH), //
   PAN(BasicNoteOnAssignment.V_PAN, Assign.MIXER), //
   PLUGIN(BasicNoteOnAssignment.V_PLUGIN, Assign.CHANNEL, "audio-effect", "DEVICE"), // TODO PLUGIN mackie
   EQ(BasicNoteOnAssignment.V_EQ, Assign.CHANNEL, "EQ+ device", "EQ", "EQ+"), // possibly both
   INSTRUMENT(BasicNoteOnAssignment.V_INSTRUMENT, Assign.CHANNEL, "instrument", "INSTRUMENT"),
   MIDI_EFFECT(BasicNoteOnAssignment.GV_MIDI_LF1, Assign.CHANNEL, "note-effect", "NOTE FX"),
   ARPEGGIATOR(BasicNoteOnAssignment.V_INSTRUMENT, Assign.CHANNEL, "note-effect", "ALT+INSTRUMENT");

   public enum Assign {
      MIXER,
      CHANNEL,
      BOTH
   }

   private final BasicNoteOnAssignment buttonAssignment;
   private final Assign assign;
   private final String typeName;
   private final String deviceName;
   private final String typeDisplayName;
   private final String buttonDescription;

   VPotMode(final BasicNoteOnAssignment buttonAssignment, final Assign assign, final String typeName,
            final String buttonDescription, final String deviceName) {
      this.buttonAssignment = buttonAssignment;
      this.assign = assign;
      this.typeName = typeName;
      this.deviceName = deviceName;
      if (this.typeName != null && this.typeName.length() > 1) {
         typeDisplayName = this.typeName.substring(0, 1).toUpperCase() + this.typeName.substring(1);
      } else {
         typeDisplayName = null;
      }
      this.buttonDescription = buttonDescription;
   }

   VPotMode(final BasicNoteOnAssignment buttonAssignment, final Assign assign, final String typeName,
            final String buttonDescription) {
      this(buttonAssignment, assign, typeName, buttonDescription, null);
   }

   VPotMode(final BasicNoteOnAssignment buttonAssignment, final Assign assign) {
      this(buttonAssignment, assign, null, null);
   }

   public static VPotMode fittingMode(final Device device) {
      return fittingMode(device.deviceType().get(), device.name().get());
   }

   public static VPotMode fittingMode(final String typeName, final String deviceName) {
      final VPotMode[] values = VPotMode.values();
      for (final VPotMode value : values) {
         if (value.deviceName != null && value.deviceName.equals(deviceName)) {
            return value;
         }
      }
      for (final VPotMode value : values) {
         if (value.deviceName == null && value.typeName != null && value.typeName.equals(typeName)) {
            return value;
         }
      }
      return null;
   }

   public String getDeviceName() {
      return deviceName;
   }

   public Assign getAssign() {
      return assign;
   }

   public BasicNoteOnAssignment getButtonAssignment() {
      return buttonAssignment;
   }

   public String getName() {
      return buttonAssignment.toString();
   }

   public String getTypeName() {
      return typeName;
   }

   public boolean isDeviceMode() {
      return typeName != null;
   }

   public String getTypeDisplayName() {
      return typeDisplayName;
   }

   public String getButtonDescription() {
      return buttonDescription;
   }

}
