package com.bitwig.extensions.controllers.mackie.layer;

import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extensions.controllers.mackie.ButtonViewState;
import com.bitwig.extensions.controllers.mackie.configurations.MenuModeLayerConfiguration;
import com.bitwig.extensions.controllers.mackie.display.DisplayLayer;
import com.bitwig.extensions.controllers.mackie.section.DrumNoteHandler;
import com.bitwig.extensions.controllers.mackie.section.MixControl;
import com.bitwig.extensions.controllers.mackie.section.MixerSectionHardware;
import com.bitwig.extensions.controllers.mackie.section.ParamElement;
import com.bitwig.extensions.controllers.mackie.seqencer.DrumSeqencerLayer;
import com.bitwig.extensions.controllers.mackie.value.BooleanValueObject;
import com.bitwig.extensions.controllers.mackie.value.ValueObject;
import com.bitwig.extensions.framework.Layer;

public class DrumMixerLayerGroup extends MixerLayerGroup {

   private final DrumSeqencerLayer drumSeqencerLayer;
   private final ValueObject<ButtonViewState> buttonView;
   private EditorMode editMode = EditorMode.MIX;

   MenuModeLayerConfiguration clipMenu;

   public enum EditorMode {
      MIX, CLIP
   }

   public DrumMixerLayerGroup(final String name, final MixControl control) {
      super(name, control);
      drumSeqencerLayer = new DrumSeqencerLayer(control);
      buttonView = control.getDriver().getButtonView();
      buttonView.addValueObserver((oldValue, newValue) -> {
         if (newValue == ButtonViewState.STEP_SEQUENCER) {
            editMode = EditorMode.CLIP;
         } else {
            editMode = EditorMode.MIX;
         }
      });
   }

   @Override
   public Layer getMixerButtonLayer() {
      if (buttonView.get() == ButtonViewState.STEP_SEQUENCER) {
         return drumSeqencerLayer;
      }
      return mixerButtonLayer;
   }

   @Override
   public EncoderLayer getEncoderLayer(final ParamElement type) {
      if (editMode == EditorMode.MIX) {
         return super.getEncoderLayer(type);
      }
      return drumSeqencerLayer.getMenu(editMode).getEncoderLayer();
   }

   @Override
   public DisplayLayer getDisplayConfiguration(final ParamElement type, final DisplayLocation location) {
      if (location == DisplayLocation.BOTTOM || editMode == EditorMode.MIX) {
         return super.getDisplayConfiguration(type, DisplayLocation.BOTTOM);
      }
      return drumSeqencerLayer.getMenu(editMode).getDisplayLayer(0);
   }

   public void init(final DrumPadBank drumPadBank, final DrumNoteHandler noteHandler) {
      final int sectionIndex = control.getHwControls().getSectionIndex();
      drumSeqencerLayer.init(drumPadBank, noteHandler);
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
