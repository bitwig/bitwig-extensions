package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers;

import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.nativeinstruments.commons.ColorBrightness;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.HwElements;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.RgbColor;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.ViewControl;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.buttons.RgbButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class TrackLayer extends Layer {

   private final Layer muteLayer;
   private final Layer soloLayer;
   private RgbColor[] trackColors = new RgbColor[16];
   private final boolean[] selectionField = new boolean[16];

   public TrackLayer(Layers layers, HwElements hwElements, ViewControl viewControl, ModifierLayer modifierLayer) {
      super(layers, "GROUP_LAYER");
      muteLayer = new Layer(layers, "MUTE_LAYER");
      soloLayer = new Layer(layers, "SOLO_LAYER");
      Arrays.fill(trackColors, RgbColor.OFF);
      TrackBank trackBank = viewControl.getGroupTrackBank();
      List<RgbButton> gridButtons = hwElements.getPadButtons();
      for (int i = 0; i < 16; i++) {
         final int index = i;
         RgbButton button = gridButtons.get(i);
         if (i < trackBank.getSizeOfBank()) {
            Track track = trackBank.getItemAt(i);
            prepareTrack(track, index);
            button.bindPressed(this, () -> selectTrack(track, index));
            button.bindLight(this, () -> this.getLight(track, index));
         } else {
            button.bindEmptyAction(this);
            button.bindLight(this, () -> RgbColor.OFF);
         }
      }
   }

   private InternalHardwareLightState getLight(Track track, int index) {
      if (selectionField[index]) {
         return trackColors[index].brightness(ColorBrightness.SUPERBRIGHT);
      }
      return trackColors[index].brightness(ColorBrightness.DIMMED);
   }

   private void selectTrack(Track track, int index) {
      track.selectInEditor();
   }

   private void prepareTrack(Track track, int index) {
      track.isQueuedForStop().markInterested();
      track.arm().markInterested();
      track.color().addValueObserver((r, g, b) -> trackColors[index] = RgbColor.toColor(r, g, b));
      track.addIsSelectedInMixerObserver(selectedInMixer -> selectionField[index] = selectedInMixer);
   }

}
