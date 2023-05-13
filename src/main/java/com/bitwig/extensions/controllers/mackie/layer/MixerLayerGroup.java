package com.bitwig.extensions.controllers.mackie.layer;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.mackie.BasicNoteOnAssignment;
import com.bitwig.extensions.controllers.mackie.ButtonViewState;
import com.bitwig.extensions.controllers.mackie.StringUtil;
import com.bitwig.extensions.controllers.mackie.display.DisplayLayer;
import com.bitwig.extensions.controllers.mackie.display.MotorSlider;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.controllers.mackie.section.MixControl;
import com.bitwig.extensions.controllers.mackie.section.MixerSectionHardware;
import com.bitwig.extensions.controllers.mackie.section.ParamElement;
import com.bitwig.extensions.controllers.mackie.section.TrackSelectionHandler;
import com.bitwig.extensions.controllers.mackie.seqencer.NoteSequenceLayer;
import com.bitwig.extensions.controllers.mackie.seqencer.SequencerLayer;
import com.bitwig.extensions.controllers.mackie.value.*;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MixerLayerGroup {
   protected final MixControl control;

   private final List<Bank<? extends Parameter>> sendBankList = new ArrayList<>();
   private final Layer volumeFaderLayer;
   protected final EncoderLayer volumeEncoderLayer;
   private final Layer panFaderLayer;
   protected final EncoderLayer panEncoderLayer;
   protected final ButtonLayer mixerButtonLayer;

   private final Layer sendFaderLayer;
   protected final EncoderLayer sendEncoderLayer;
   private final DisplayLayer volumeDisplayConfiguration;
   private final DisplayLayer panDisplayConfiguration;
   private final DisplayLayer sendDisplayConfiguration;

   private final DisplayLayer activeSendDisplayConfig;
   private final DisplayLayer activePanDisplayConfig;
   private boolean flipped;
   protected EditorMode editMode = EditorMode.MIX;
   private NoteSequenceLayer sequenceLayer;
   private final TrackSelectionHandler selectionHandler;

   private final String name;
   private boolean active;
   private final TrackColor trackColor = new TrackColor();
   protected final Layer colorLayer;

   public enum EditorMode {
      MIX,
      SEQUENCE
   }

   public MixerLayerGroup(final String name, final MixControl control, final TrackSelectionHandler selectionHandler) {
      final int sectionIndex = control.getHwControls().getSectionIndex();
      this.control = control;
      this.name = name;
      final Layers layers = this.control.getDriver().getLayers();

      this.selectionHandler = selectionHandler;
      colorLayer = new Layer(layers, name + "_COLORS_" + sectionIndex);
      colorLayer.bindLightState(trackColor::getState, control.getBackgroundColoring());

      mixerButtonLayer = new ButtonLayer(name, control, BasicNoteOnAssignment.REC_BASE);

      volumeFaderLayer = new Layer(layers, name + "_VOLUME_FADER_LAYER_" + sectionIndex);
      volumeEncoderLayer = new EncoderLayer(control, name + "_VOLUME_ENCODER_LAYER_" + sectionIndex);

      panFaderLayer = new Layer(layers, name + "_PAN_FADER_LAYER_" + sectionIndex);
      panEncoderLayer = new EncoderLayer(control, name + "_PAN_ENCODER_LAYER_" + sectionIndex);

      sendFaderLayer = new Layer(layers, name + "_SEND_FADER_LAYER_" + sectionIndex);
      sendEncoderLayer = new EncoderLayer(control, name + "_SEN_ENCODER_LAYER_" + sectionIndex);

      final int section = control.getHwControls().getSectionIndex();
      final MixerSectionHardware hwControls = control.getHwControls();

      volumeDisplayConfiguration = new DisplayLayer("MixVolume", section, layers, hwControls);

      panDisplayConfiguration = new DisplayLayer("MixPan", section, layers, hwControls);
      sendDisplayConfiguration = new DisplayLayer("MixSend", section, layers, hwControls);
      activeSendDisplayConfig = sendDisplayConfiguration;
      activePanDisplayConfig = panDisplayConfiguration;
      final ValueObject<ButtonViewState> buttonView = control.getDriver().getButtonView();
      buttonView.addValueObserver((oldValue, newValue) -> {
         if (getSequenceLayer() == null) {
            return;
         }
         if (newValue == ButtonViewState.STEP_SEQUENCER) {
            editMode = EditorMode.SEQUENCE;
         } else {
            editMode = EditorMode.MIX;
         }
      });
      control.getDriver().getFlipped().addValueObserver(flipped -> this.flipped = flipped);
   }

   public void setActive(final boolean active) {
      this.active = active;
      colorLayer.setIsActive(active);
   }

   public SequencerLayer getSequenceLayer() {
      return sequenceLayer;
   }

   public void setSequenceLayer(final NoteSequenceLayer sequenceLayer) {
      this.sequenceLayer = sequenceLayer;
   }

   public boolean hasCursorNavigation() {
      return editMode == EditorMode.SEQUENCE && sequenceLayer != null;
   }

   public Layer getFaderLayer(final ParamElement type) {
      switch (type) {
         case PAN:
            return panFaderLayer;
         case SENDMIXER:
            return sendFaderLayer;
         case VOLUME:
         default:
            return volumeFaderLayer;
      }
   }

   public DisplayLayer getDisplayConfigurationMix(final ParamElement type, final DisplayLocation location) {
      switch (type) {
         case PAN:
            return activePanDisplayConfig;
         case SENDMIXER:
            return activeSendDisplayConfig;
         case VOLUME:
         default:
            return volumeDisplayConfiguration;
      }
   }

   public EncoderLayer getEncoderLayerMix(final ParamElement type) {
      switch (type) {
         case PAN:
            return panEncoderLayer;
         case SENDMIXER:
            return sendEncoderLayer;
         case VOLUME:
         default:
            return volumeEncoderLayer;
      }
   }

   public Layer getMixerButtonLayer() {
      if (editMode == EditorMode.MIX || sequenceLayer == null) {
         return mixerButtonLayer;
      }
      return sequenceLayer;
   }

   public boolean isFlipped() {
      if (editMode == EditorMode.SEQUENCE) {
         return false;
      }
      return flipped;
   }

   public Optional<DisplayLayer> getModeDisplayLayer() {
      if (editMode == EditorMode.SEQUENCE && sequenceLayer != null) {
         return Optional.of(sequenceLayer.getMenu().getDisplayLayer(0));
      }
      return Optional.empty();
   }

   public Optional<EncoderLayer> getModeEncoderLayer() {
      if (editMode == EditorMode.SEQUENCE && sequenceLayer != null) {
         return Optional.of(sequenceLayer.getMenu().getEncoderLayer());
      }
      return Optional.empty();
   }


   public DisplayLayer getDisplayConfiguration(final ParamElement type, final DisplayLocation location) {
      if (location == DisplayLocation.BOTTOM || editMode == EditorMode.MIX || sequenceLayer == null) {
         return getDisplayConfigurationMix(type, location);
      }
      return sequenceLayer.getMenu().getDisplayLayer(0);
   }

   public EncoderLayer getEncoderLayer(final ParamElement type) {
      if (editMode == EditorMode.MIX || sequenceLayer == null) {
         return getEncoderLayerMix(type);
      }
      return sequenceLayer.getMenu().getEncoderLayer();
   }

   public void navigateHorizontally(final int direction) {
      if (editMode == EditorMode.SEQUENCE && sequenceLayer != null) {
         sequenceLayer.scrollStepsBy(direction);
      } else {
         for (final Bank<?> bank : sendBankList) {
            bank.scrollBy(direction);
         }
      }
   }

   public void navigateVertically(final int direction) {
      if (editMode == EditorMode.SEQUENCE && sequenceLayer != null) {
         sequenceLayer.navigateVertically(direction);
      }
   }


   public void initMainSlider(final Track mainTrack, final MotorSlider slider) {

      slider.bindParameter(volumeFaderLayer, mainTrack.volume());
      slider.bindParameter(panFaderLayer, mainTrack.volume());
      slider.bindParameter(sendFaderLayer, mainTrack.volume());

      final boolean hasLower = control.getDriver().getControllerConfig().hasLowerDisplay();

      if (hasLower) {
         volumeDisplayConfiguration.bindTitle(8, mainTrack.name());
         volumeDisplayConfiguration.bindDisplayParameterValue(8, mainTrack.volume(),
            s -> StringUtil.condenseVolumeValue(s, 7));
         panDisplayConfiguration.bindTitle(8, mainTrack.name());
         panDisplayConfiguration.bindDisplayParameterValue(8, mainTrack.volume(),
            s -> StringUtil.condenseVolumeValue(s, 7));
         sendDisplayConfiguration.bindTitle(8, mainTrack.name());
         sendDisplayConfiguration.bindDisplayParameterValue(8, mainTrack.volume(),
            s -> StringUtil.condenseVolumeValue(s, 7));
      }

      final MixerSectionHardware hwControls = control.getHwControls();

      if (control.getDriver().getControllerConfig().hasMasterVu()) {
         mainTrack.addVuMeterObserver(14, 0, true, value -> {
            if (volumeEncoderLayer.isActive() || volumeFaderLayer.isActive()) {
               hwControls.sendMasterVuUpdateL(value);
            }
         });
         mainTrack.addVuMeterObserver(14, 1, true, value -> {
            if (volumeEncoderLayer.isActive() || volumeFaderLayer.isActive()) {
               hwControls.sendMasterVuUpdateR(value);
            }
         });
      }
   }

   public void init(final TrackBank trackBank) {
      final int sectionIndex = control.getHwControls().getSectionIndex();
      for (int i = 0; i < 8; i++) {
         final int trackIndex = i + sectionIndex * 8;
         setUpTrackControl(i, trackBank.getItemAt(trackIndex));
      }
   }

   protected void setUpTrackControl(final int index, final Track track) {
      final MixerSectionHardware hwControls = control.getHwControls();
      setUpChannelControl(index, hwControls, track);
      hwControls.bindButton(mixerButtonLayer, index, MixerSectionHardware.REC_INDEX, track.arm(), track.arm()::toggle);
      final BooleanValueObject selectedInMixer = new BooleanValueObject();
      track.addIsSelectedInEditorObserver(selectedInMixer::set);
      hwControls.bindButton(mixerButtonLayer, index, MixerSectionHardware.SELECT_INDEX, selectedInMixer,
         () -> selectionHandler.handleTrackSelection(track));
   }

   protected void setUpChannelControl(final int index, final MixerSectionHardware hwControls, final Channel channel) {
      channel.exists().markInterested();
      channel.addVuMeterObserver(14, -1, true, value -> {
         if (volumeEncoderLayer.isActive() || volumeFaderLayer.isActive()) {
            hwControls.sendVuUpdate(index, value);
         }
      });

      channel.color().addValueObserver((r, g, b) -> trackColor.set(index, r, g, b));

      setControlLayer(index, channel.volume(), volumeFaderLayer, volumeEncoderLayer, RingDisplayType.FILL_LR_0);
      setControlLayer(index, channel.pan(), panFaderLayer, panEncoderLayer, RingDisplayType.PAN_FILL);
      final SendBank sendBank = channel.sendBank();
      final Send focusSendItem = sendBank.getItemAt(0);

      setControlLayer(index, focusSendItem, sendFaderLayer, sendEncoderLayer, RingDisplayType.FILL_LR);
      sendBankList.add(sendBank);

      volumeDisplayConfiguration.bindDisplayParameterValue(index, channel.volume(),
         s -> StringUtil.condenseVolumeValue(s, 7));
      panDisplayConfiguration.bindParameterValue(index, channel.pan(), StringUtil::panToString);
      sendDisplayConfiguration.bindDisplayParameterValue(index, focusSendItem,
         s -> StringUtil.condenseVolumeValue(s, 7));

      final ChannelStateValueHandler trackNameHandler = ChannelStateValueHandler.create(channel);
      final TrackNameValueHandler sendNameHandler = new TrackNameValueHandler(focusSendItem.name());

      volumeDisplayConfiguration.bindName(index, trackNameHandler);
      if (control.getDriver().getControllerConfig().hasLowerDisplay()) {
         panDisplayConfiguration.bindTitle(index, trackNameHandler, new BasicStringValue("Pan"));
         sendDisplayConfiguration.bindTitle(index, trackNameHandler, focusSendItem.name());
      } else {
         panDisplayConfiguration.bindName(index, trackNameHandler, new BasicStringValue("Pan"));
         sendDisplayConfiguration.bindName(index, trackNameHandler, focusSendItem.name());
      }

      hwControls.bindButton(mixerButtonLayer, index, MixerSectionHardware.SOLO_INDEX, channel.solo(),
         () -> control.handleSoloAction(channel));
      hwControls.bindButton(mixerButtonLayer, index, MixerSectionHardware.MUTE_INDEX, channel.mute(),
         () -> channel.mute().toggle());
   }

   private void setControlLayer(final int index, final Parameter parameter, final Layer faderLayer,
                                final Layer encoderLayer, final RingDisplayType type) {
      final MixerSectionHardware hwControls = control.getHwControls();
      faderLayer.addBinding(hwControls.createMotorFaderBinding(index, parameter));
      faderLayer.addBinding(hwControls.createFaderParamBinding(index, parameter));
      faderLayer.addBinding(hwControls.createFaderTouchBinding(index, () -> {
         if (control.getModifier().isShift()) {
            parameter.reset();
         }
      }));
      encoderLayer.addBinding(hwControls.createEncoderPressBinding(index, parameter));
      encoderLayer.addBinding(hwControls.createEncoderToParamBinding(index, parameter));
      encoderLayer.addBinding(hwControls.createRingDisplayBinding(index, parameter, type));
   }

   public void advanceMode() {
   }
}
