package com.bitwig.extensions.controllers.mackie.layer;

import com.bitwig.extensions.controllers.mackie.definition.SubType;

import java.util.Optional;

/**
 * Manages information being displayed when pressing OPTION + NAME/VALUE.
 */

public enum HelperInfo {
   LAUNCHER_MCU("CLIP_LAUNCH", SubType.MACKIE, //
      "Launcher: Shift=select Opt=Duplicate +lt=Stop", //
      "Shift+Opt=Delete Shift+Alt=Dupl.Content"), //
   LAUNCHER_ICON("CLIP_LAUNCH", SubType.ICON, //
      "Launcher: Shift=select Clear=Delete Duplicate=Duplicate", //
      "Shft+Dup=Dup.Content Option=Stop"),
   SEQUENCER("NoteSeq", "Note Sequencer: Step+Duplicate=Duplication Mode", ""),
   DRUM_SEQUENCER("DrumSeq", "Drum: Step+Duplicate=Duplication Mode", ""),
   TRACK("MN", SubType.MACKIE, "Tr.Select: +Shift=GroupExp +Dup=Dup.Track", //
      "+Alt=Stop +Control=Delete"), //
   TRACK_ICON("MN", SubType.ICON, "Tr.Select: +Shift=GroupExp +Dup=Dup.Track +Clear=Delete", //
      "+Shf+Opt=Stop +Option=Nav into Group (exit OPT+CANCEL) "),
   TRACK_GL("GL", SubType.MACKIE, TRACK.topInfo, TRACK.bottomInfo),
   TRACK_GL_ICON("GL", SubType.ICON, TRACK_ICON.topInfo, TRACK_ICON.bottomInfo);

   private final String prefixButtonLayer;
   private final String topInfo;
   private final String bottomInfo;
   private final SubType specType;

   HelperInfo(final String prefix, final String top, final String bottom) {
      prefixButtonLayer = prefix;
      topInfo = top;
      bottomInfo = bottom;
      specType = null;
   }

   HelperInfo(final String prefix, final SubType type, final String top, final String bottom) {
      prefixButtonLayer = prefix;
      topInfo = top;
      bottomInfo = bottom;
      specType = type;
   }

   public String getBottomInfo() {
      return bottomInfo;
   }

   public String getTopInfo() {
      return topInfo;
   }

   private boolean matchesButtonLayer(final String buttonLayerName, final SubType type) {
      return buttonLayerName.startsWith(prefixButtonLayer) && (specType == null || type == specType);
   }

   public static Optional<HelperInfo> getInfo(final String nameButtonLayer, final String nameDisplayLayer,
                                              final SubType controllerType) {
      for (final HelperInfo info : values()) {
         if (info.matchesButtonLayer(nameButtonLayer, controllerType)) {
            return Optional.of(info);
         }
      }
      return Optional.empty();
   }

}
