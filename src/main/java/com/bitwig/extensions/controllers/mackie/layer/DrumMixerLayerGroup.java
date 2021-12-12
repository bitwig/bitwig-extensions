package com.bitwig.extensions.controllers.mackie.layer;

import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extensions.controllers.mackie.ButtonViewState;
import com.bitwig.extensions.controllers.mackie.display.DisplayLayer;
import com.bitwig.extensions.controllers.mackie.section.DrumNoteHandler;
import com.bitwig.extensions.controllers.mackie.section.MixControl;
import com.bitwig.extensions.controllers.mackie.section.MixerSectionHardware;
import com.bitwig.extensions.controllers.mackie.section.ParamElement;
import com.bitwig.extensions.controllers.mackie.seqencer.DrumSequencerLayer;
import com.bitwig.extensions.controllers.mackie.value.BooleanValueObject;
import com.bitwig.extensions.framework.Layer;

public class DrumMixerLayerGroup extends MixerLayerGroup {

   private final DrumSequencerLayer drumSequencerLayer;

   public DrumMixerLayerGroup(final String name, final MixControl control) {
      super(name, control);
      drumSequencerLayer = new DrumSequencerLayer(control);
   }

   @Override
   public boolean hasCursorNavigation() {
      return true;
   }

   @Override
   public void navigateHorizontally(final int direction) {
      if (editMode == EditorMode.MIX) {
         super.navigateHorizontally(direction);
      } else {
         drumSequencerLayer.scrollStepsBy(direction);
      }
   }

   public void notifyBlink(final int ticks) {
      drumSequencerLayer.notifyBlink(ticks);
   }

   @Override
   public void navigateVertically(final int direction) {
      if (editMode == EditorMode.MIX) {
         super.navigateVertically(direction);
      } else {
         drumSequencerLayer.navigateVertically(direction);
      }
   }

   @Override
   public void advanceMode() {
      drumSequencerLayer.nextMenu();
   }

   @Override
   public Layer getMixerButtonLayer() {
      if (control.getDriver().getButtonView().get() == ButtonViewState.STEP_SEQUENCER) {
         return drumSequencerLayer;
      }
      return mixerButtonLayer;
   }

   @Override
   public EncoderLayer getEncoderLayer(final ParamElement type) {
      if (editMode == EditorMode.MIX) {
         return super.getEncoderLayer(type);
      }
      return drumSequencerLayer.getMenu().getEncoderLayer();
   }

   @Override
   public DisplayLayer getDisplayConfiguration(final ParamElement type, final DisplayLocation location) {
      if (location == DisplayLocation.BOTTOM || editMode == EditorMode.MIX) {
         return super.getDisplayConfiguration(type, DisplayLocation.BOTTOM);
      }
      return drumSequencerLayer.getMenu().getDisplayLayer(0);
   }

   public void init(final DrumPadBank drumPadBank, final DrumNoteHandler noteHandler) {
      final int sectionIndex = control.getHwControls().getSectionIndex();
      drumSequencerLayer.init(drumPadBank, noteHandler);
      for (int i = 0; i < 8; i++) {
         final int trackIndex = i + sectionIndex * 8;
         setUpDrumPadControl(i, drumPadBank.getItemAt(trackIndex), noteHandler);
      }
   }

   protected void setUpDrumPadControl(final int index, final DrumPad pad, final DrumNoteHandler noteHandler) {
      final MixerSectionHardware hwControls = control.getHwControls();
      mixerButtonLayer.setNoteHandler(noteHandler);
      setUpChannelControl(index, hwControls, pad);
      final BooleanValueObject selectedInMixer = new BooleanValueObject();
      pad.addIsSelectedInMixerObserver(selectedInMixer::set);

      hwControls.bindButton(mixerButtonLayer, index, MixerSectionHardware.SELECT_INDEX, selectedInMixer,
         () -> control.handlePadSelection(pad));
      hwControls.bindButton(mixerButtonLayer, index, MixerSectionHardware.REC_INDEX, noteHandler.isPlaying(index),
         () -> {
         });
   }


}
