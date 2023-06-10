package com.bitwig.extensions.controllers.akai.apcmk2;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Preferences;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extensions.framework.values.FocusMode;

public class ApcPreferences {
   private final SettableBooleanValue recordButtonAsAlt;
   private FocusMode recordFocusMode = FocusMode.ARRANGER;

   public ApcPreferences(final ControllerHost host, boolean isKeys) {
      final Preferences preferences = host.getPreferences(); // THIS
      if (isKeys) {
         final SettableEnumValue recordButtonAssignment = preferences.getEnumSetting("Record Button assignment", //
            "Transport", new String[]{FocusMode.LAUNCHER.getDescriptor(), FocusMode.ARRANGER.getDescriptor()},
            recordFocusMode.getDescriptor());
         recordButtonAssignment.addValueObserver(value -> recordFocusMode = FocusMode.toMode(value));
      }
      recordButtonAsAlt = preferences.getBooleanSetting("Use as ALT trigger modifier", "Shift Button", false);
      recordButtonAsAlt.markInterested();
   }

   public FocusMode getRecordFocusMode() {
      return recordFocusMode;
   }

   public SettableBooleanValue getRecordButtonAsAlt() {
      return recordButtonAsAlt;
   }
}
