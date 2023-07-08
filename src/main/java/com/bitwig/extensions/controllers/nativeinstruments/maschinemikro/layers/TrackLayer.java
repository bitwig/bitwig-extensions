package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.nativeinstruments.commons.ColorBrightness;
import com.bitwig.extensions.controllers.nativeinstruments.commons.Colors;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.HwElements;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.MidiProcessor;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.RgbColor;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.ViewControl;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.buttons.RgbButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;

import java.util.Arrays;
import java.util.List;

@Component
public class TrackLayer extends Layer {
   private static final RgbColor MUTE_COLOR = RgbColor.of(Colors.LIGHT_ORANGE);
   private static final RgbColor SOLO_COLOR = RgbColor.of(Colors.YELLOW);

   private final Layer muteLayer;
   private final Layer soloLayer;
   private final CursorTrack cursorTrack;
   private RgbColor[] trackColors = new RgbColor[16];
   private final boolean[] selectionField = new boolean[16];
   private RgbColor[] sendColors = new RgbColor[8];
   private MuteSoloMode muteSoloMode = MuteSoloMode.NONE;
   private EncoderDestination currentEncoderDestination = EncoderDestination.VOLUME;
   @Inject
   private ModifierLayer modifierLayer;
   @Inject
   private MidiProcessor midiProcessor;

   private enum EncoderDestination {
      VOLUME,
      PAN,
      SEND1,
      SEND2;
   }

   public TrackLayer(Layers layers, HwElements hwElements, ViewControl viewControl) {
      super(layers, "GROUP_LAYER");
      muteLayer = new Layer(layers, "MUTE_LAYER");
      soloLayer = new Layer(layers, "SOLO_LAYER");
      Arrays.fill(trackColors, RgbColor.OFF);
      Arrays.fill(sendColors, RgbColor.OFF);
      cursorTrack = viewControl.getCursorTrack();
      SendBank sendBank = cursorTrack.sendBank();
      for (int i = 0; i < sendBank.getSizeOfBank(); i++) {
         int index = i;
         sendBank.getItemAt(i)
            .sendChannelColor()
            .addValueObserver((r, g, b) -> sendColors[index] = RgbColor.toColor(r, g, b));
      }
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

            button.bindPressed(muteLayer, () -> track.mute().toggle());
            button.bindLight(muteLayer, () -> this.getMuteLight(track, index));

            button.bindPressed(soloLayer, () -> track.solo().toggle());
            button.bindLight(soloLayer, () -> this.getSoloLight(track, index));
         } else if (i == 12) {
            button.bindPressed(this, () -> currentEncoderDestination = EncoderDestination.VOLUME);
            button.bindLight(this, () -> getModeState(EncoderDestination.VOLUME, RgbColor.WHITE));
         } else if (i == 13) {
            button.bindPressed(this, () -> currentEncoderDestination = EncoderDestination.PAN);
            button.bindLight(this, () -> getModeState(EncoderDestination.PAN, RgbColor.ORANGE));
         } else if (i == 14) {
            button.bindPressed(this, () -> currentEncoderDestination = EncoderDestination.SEND1);
            button.bindLight(this, () -> getModeState(EncoderDestination.SEND1, 0, sendBank.getItemAt(0)));
         } else if (i == 15) {
            button.bindPressed(this, () -> currentEncoderDestination = EncoderDestination.SEND2);
            button.bindLight(this, () -> getModeState(EncoderDestination.SEND2, 1, sendBank.getItemAt(1)));
         } else {
            button.bindEmptyAction(this);
            button.bindLight(this, () -> RgbColor.OFF);
         }
      }
      hwElements.bindEncoder(this, hwElements.getMainEncoder(), dir -> handleEncoder(dir));
   }

   private RgbColor getModeState(EncoderDestination mode, RgbColor color) {
      return currentEncoderDestination == mode ? color.brightness(ColorBrightness.BRIGHT) : color.brightness(
         ColorBrightness.DARKENED);
   }

   private RgbColor getModeState(EncoderDestination mode, int index, Send send) {
      if (send.exists().get()) {
         RgbColor color = sendColors[index];
         if (color == RgbColor.OFF) {
            color = RgbColor.BLUE;
         }
         return currentEncoderDestination == mode ? color.brightness(ColorBrightness.BRIGHT) : color.brightness(
            ColorBrightness.DARKENED);
      }
      return RgbColor.OFF;
   }

   private void handleEncoder(int diff) {
      switch (currentEncoderDestination) {
         case VOLUME -> {
            if (modifierLayer.getShiftHeld().get()) {
               cursorTrack.volume().value().inc(diff, 128);
            } else {
               cursorTrack.volume().value().inc(diff * 4, 128);
            }
         }
         case PAN -> {
            if (modifierLayer.getShiftHeld().get()) {
               cursorTrack.pan().value().inc(diff * 0.01);
            } else {
               cursorTrack.pan().value().inc(diff * 0.05);
            }
         }
         case SEND1 -> {
            if (modifierLayer.getShiftHeld().get()) {
               cursorTrack.sendBank().getItemAt(0).value().inc(diff * 0.01);
            } else {
               cursorTrack.sendBank().getItemAt(0).value().inc(diff * 0.05);
            }
         }
         case SEND2 -> {
            if (modifierLayer.getShiftHeld().get()) {
               cursorTrack.sendBank().getItemAt(1).value().inc(diff * 0.01);
            } else {
               cursorTrack.sendBank().getItemAt(1).value().inc(diff * 0.05);
            }
         }
      }
   }

   public void setMutSoloMode(MuteSoloMode muteSoloMode) {
      this.muteSoloMode = muteSoloMode;
      if (isActive()) {
         soloLayer.setIsActive(muteSoloMode == MuteSoloMode.SOLO);
         muteLayer.setIsActive(muteSoloMode == MuteSoloMode.MUTE);
      }
   }

   private InternalHardwareLightState getSoloLight(Track track, int index) {
      if (!track.exists().get()) {
         return RgbColor.OFF;
      }
      if (track.solo().get()) {
         return SOLO_COLOR.brightness(ColorBrightness.BRIGHT);
      }
      return SOLO_COLOR;
   }

   private InternalHardwareLightState getMuteLight(Track track, int index) {
      if (!track.exists().get()) {
         return RgbColor.OFF;
      }
      if (track.mute().get()) {
         return MUTE_COLOR.brightness(ColorBrightness.BRIGHT);
      }
      return MUTE_COLOR;
   }

   private InternalHardwareLightState getLight(Track track, int index) {
      if (!track.exists().get()) {
         return RgbColor.OFF;
      }
      if (selectionField[index]) {
         return trackColors[index].brightness(ColorBrightness.SUPERBRIGHT);
      }
      if (track.isQueuedForStop().get()) {
         return midiProcessor.blinkMid(trackColors[index]);
      }
      if (track.isStopped().get()) {
         return trackColors[index].brightness(ColorBrightness.DARKENED);
      }
      return trackColors[index].brightness(ColorBrightness.BRIGHT);
   }

   private void selectTrack(Track track, int index) {
      if (modifierLayer.getDuplicateHeld().get()) {
         track.duplicate();
      } else if (modifierLayer.getEraseHeld().get()) {
         track.deleteObject();
      } else {
         track.selectInEditor();
      }
   }

   private void prepareTrack(Track track, int index) {
      track.isQueuedForStop().markInterested();
      track.arm().markInterested();
      track.solo().markInterested();
      track.mute().markInterested();
      track.isQueuedForStop().markInterested();
      track.isStopped().markInterested();
      track.exists().markInterested();
      track.color().addValueObserver((r, g, b) -> trackColors[index] = RgbColor.toColor(r, g, b));
      track.addIsSelectedInMixerObserver(selectedInMixer -> selectionField[index] = selectedInMixer);
   }

   @Override
   protected void onActivate() {
      super.onActivate();
      soloLayer.setIsActive(muteSoloMode == MuteSoloMode.SOLO);
      muteLayer.setIsActive(muteSoloMode == MuteSoloMode.MUTE);
   }

   @Override
   protected void onDeactivate() {
      super.onDeactivate();
      soloLayer.setIsActive(false);
      muteLayer.setIsActive(false);
   }
}
