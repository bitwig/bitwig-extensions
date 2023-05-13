package com.bitwig.extensions.controllers.mackie.layer;

import com.bitwig.extensions.controllers.mackie.definition.ManufacturerType;

import java.util.Optional;

/**
 * Manages information being displayed when pressing OPTION + NAME/VALUE.
 */

public enum HelperInfo {
   LAUNCHER_MCU("CLIP_LAUNCH", ManufacturerType.MACKIE, //
      "Launcher: Shift=Select Opt=Duplicate Alt=Stop", //
      "Shift+Opt=Delete Shift+Alt=Dbl.Content/EmptySl=new clip"), //
   LAUNCHER_ICON("CLIP_LAUNCH", ManufacturerType.ICON, //
      "Launcher: Shft=select Clear=Delete Option=Stop", //
      "Shft+Dup=Dbl.Content Duplicate=Duplicate/EmptySl=new clip"),
   SEQUENCER("NoteSeq", ManufacturerType.MACKIE, "Note Sequencer: Alt+Step=Duplication Mode",
      "Clear+Step=Delete Option+Step=Audition"),
   SEQUENCER_ICON("NoteSeq", ManufacturerType.ICON, "Note Sequencer: Duplicate+Step=Duplication Mode",
      "Clear+Step=Delete Option+Step=Audition"),
   DRUM_SEQUENCER("DrumSeq", ManufacturerType.MACKIE, "Drum: Alt+Step=Duplication Mode", ""),
   DRUM_SEQUENCER_ICON("DrumSeq", ManufacturerType.ICON, "Drum: Duplicate+Step=Duplication Mode", ""),
   TRACK("MN", ManufacturerType.MACKIE, "Tr.Select: Shift=GroupExp Dup=Dup.Track", //
      "Alt=Stop Control=Delete"), //
   TRACK_ICON("MN", ManufacturerType.ICON, "Tr.Select: Shift=GroupExp Dup=Dup.Track Clear=Delete", //
      "Shf+Opt=Stop Option=Nav into Group (exit Opt+Cancel) "),
   TRACK_GL("GL", ManufacturerType.MACKIE, TRACK.topInfo, TRACK.bottomInfo),
   TRACK_GL_ICON("GL", ManufacturerType.ICON, TRACK_ICON.topInfo, TRACK_ICON.bottomInfo);

   private final String prefixButtonLayer;
   private final String topInfo;
   private final String bottomInfo;
   private final ManufacturerType specType;

   HelperInfo(final String prefix, final String top, final String bottom) {
      prefixButtonLayer = prefix;
      topInfo = top;
      bottomInfo = bottom;
      specType = null;
   }

   HelperInfo(final String prefix, final ManufacturerType type, final String top, final String bottom) {
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

   private boolean matchesButtonLayer(final String buttonLayerName, final ManufacturerType type) {
      return buttonLayerName.startsWith(prefixButtonLayer) && (specType == null || type == specType);
   }

   public static Optional<HelperInfo> getInfo(final String nameButtonLayer, final String nameDisplayLayer,
                                              final ManufacturerType controllerType) {
      for (final HelperInfo info : values()) {
         if (info.matchesButtonLayer(nameButtonLayer, controllerType)) {
            return Optional.of(info);
         }
      }
      return Optional.empty();
   }

}
