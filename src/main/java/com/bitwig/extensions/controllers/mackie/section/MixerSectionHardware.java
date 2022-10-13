package com.bitwig.extensions.controllers.mackie.section;

import com.bitwig.extension.callback.BooleanValueChangedCallback;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.mackie.BasicNoteOnAssignment;
import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;
import com.bitwig.extensions.controllers.mackie.Midi;
import com.bitwig.extensions.controllers.mackie.NoteAssignment;
import com.bitwig.extensions.controllers.mackie.bindings.ButtonBinding;
import com.bitwig.extensions.controllers.mackie.bindings.FaderBinding;
import com.bitwig.extensions.controllers.mackie.bindings.TouchFaderBinding;
import com.bitwig.extensions.controllers.mackie.bindings.ring.RingDisplayBoolBinding;
import com.bitwig.extensions.controllers.mackie.bindings.ring.RingDisplayExistsBinding;
import com.bitwig.extensions.controllers.mackie.bindings.ring.RingDisplayFixedBinding;
import com.bitwig.extensions.controllers.mackie.bindings.ring.RingDisplayParameterBinding;
import com.bitwig.extensions.controllers.mackie.definition.ControllerConfig;
import com.bitwig.extensions.controllers.mackie.definition.SimulationLayout;
import com.bitwig.extensions.controllers.mackie.display.*;
import com.bitwig.extensions.controllers.mackie.layer.EncoderMode;
import com.bitwig.extensions.framework.AbsoluteHardwareControlBinding;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.RelativeHardwareControlToRangedValueBinding;

import java.util.Arrays;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class MixerSectionHardware {
   public static final int REC_INDEX = 0;
   public static final int SOLO_INDEX = 1;
   public static final int MUTE_INDEX = 2;
   public static final int SELECT_INDEX = 3;

   private final int[] lightStatusMap = new int[127];
   private final HardwareSlider[] volumeKnobs = new HardwareSlider[8];
   private final RelativeHardwareKnob[] encoders = new RelativeHardwareKnob[8];
   private final HardwareButton[] encoderPress = new HardwareButton[8];
   private final HardwareButton[] faderTouch = new HardwareButton[8];
   private final FaderResponse[] motorFaderDest = new FaderResponse[8];
   private final RingDisplay[] ringDisplays = new RingDisplay[8];
   private final HardwareButton[][] buttonMatrix = new HardwareButton[4][8];
   private final RelativeHardwareValueMatcher[] nonAcceleratedMatchers = new RelativeHardwareValueMatcher[8];
   private final RelativeHardwareValueMatcher[] acceleratedMatchers = new RelativeHardwareValueMatcher[8];

   private final MidiIn midiIn;
   private final MidiOut midiOut;
   private final MackieMcuProExtension driver;
   private final int sectionIndex;
   private final LcdDisplay mainDisplay;
   private final LcdDisplay bottomDisplay;

   public MixerSectionHardware(final MackieMcuProExtension driver, final MidiIn midiIn, final MidiOut midiOut,
                               final int sectionIndex, final SectionType type) {
      this.midiIn = midiIn;
      this.midiOut = midiOut;
      this.driver = driver;
      this.sectionIndex = sectionIndex;
      final ControllerConfig controllerConfig = driver.getControllerConfig();
      mainDisplay = new LcdDisplay(driver, sectionIndex, midiOut, type, DisplayPart.UPPER,
         controllerConfig.isHasDedicateVu());
      bottomDisplay = controllerConfig.hasLowerDisplay() ? new LcdDisplay(driver, sectionIndex, midiOut, type,
         DisplayPart.LOWER, false) : null;
      Arrays.fill(lightStatusMap, -1);
      initControlHardware(driver.getSurface());
      initButtonSection();
   }

   private void initButtonSection() {
      for (int i = 0; i < 8; i++) {
         final HardwareButton armButton = createLightButton("ARM", i, BasicNoteOnAssignment.REC_BASE.getNoteNo());
         final HardwareButton soloButton = createLightButton("SOLO", i, BasicNoteOnAssignment.SOLO_BASE.getNoteNo());
         final HardwareButton muteButton = createLightButton("MUTE", i, BasicNoteOnAssignment.MUTE_BASE.getNoteNo());
         final HardwareButton selectButton = createLightButton("SELECT", i,
            BasicNoteOnAssignment.SELECT_BASE.getNoteNo());
         buttonMatrix[MixerSectionHardware.REC_INDEX][i] = armButton;
         buttonMatrix[MixerSectionHardware.SOLO_INDEX][i] = soloButton;
         buttonMatrix[MixerSectionHardware.MUTE_INDEX][i] = muteButton;
         buttonMatrix[MixerSectionHardware.SELECT_INDEX][i] = selectButton;

         final SimulationLayout simulationLayout = driver.getControllerConfig().getSimulationLayout();
         simulationLayout.layoutMatrixButton(sectionIndex, i, 0, armButton, "Arm", "#f00");
         simulationLayout.layoutMatrixButton(sectionIndex, i, 1, soloButton, "Solo", "#ff8c00");
         simulationLayout.layoutMatrixButton(sectionIndex, i, 2, muteButton, "Mute", "#ff0");
         simulationLayout.layoutMatrixButton(sectionIndex, i, 3, selectButton, "Sel", "#00f");
      }
   }

   private void initControlHardware(final HardwareSurface surface) {
      for (int i = 0; i < 8; i++) {
         final HardwareSlider slider = surface.createHardwareSlider("VOLUME_FADER_" + sectionIndex + "_" + i);

         volumeKnobs[i] = slider;
         faderTouch[i] = createTouchButton("FADER_TOUCH", i);
         slider.setHardwareButton(faderTouch[i]);
         slider.setAdjustValueMatcher(midiIn.createAbsolutePitchBendValueMatcher(i));

         driver.getControllerConfig().getSimulationLayout().layoutSlider(sectionIndex, i, slider);

         motorFaderDest[i] = new FaderResponse(midiOut, i);
         ringDisplays[i] = new RingDisplay(midiOut, i);

         final RelativeHardwareKnob encoder = surface.createRelativeHardwareKnob("PAN_KNOB" + sectionIndex + "_" + i);
         encoders[i] = encoder;
         encoderPress[i] = createEncoderButon(i);
         acceleratedMatchers[i] = midiIn.createRelativeSignedBitCCValueMatcher(0x0, 0x10 + i, 200);

         encoder.setHardwareButton(encoderPress[i]);
         driver.getControllerConfig().getSimulationLayout().layoutEncoder(sectionIndex, i, encoder);

         encoder.isUpdatingTargetValue().addValueObserver(v -> {
            if (v) {
               driver.doActionImmediate("TOUCH");
            }
         });

         final ControllerHost host = driver.getHost();
         final RelativeHardwareValueMatcher stepDownMatcher = midiIn.createRelativeValueMatcher(
            "(status == 176 && data1 == " + (0x10 + i) + " && data2 > 64)", -1);
         final RelativeHardwareValueMatcher stepUpMatcher = midiIn.createRelativeValueMatcher(
            "(status == 176 && data1 == " + (0x10 + i) + " && data2 < 65)", 1);
         nonAcceleratedMatchers[i] = host.createOrRelativeHardwareValueMatcher(stepDownMatcher, stepUpMatcher);
         setEncoderBehavior(i, EncoderMode.ACCELERATED, 128);
      }
   }

   public void setEncoderBehavior(final EncoderMode[] modes, final int stepSizeDivisor) {
      for (int i = 0; i < 8; i++) {
         setEncoderBehavior(i, modes[i], stepSizeDivisor);
      }
   }

   public void setEncoderBehavior(final int index, final EncoderMode mode, final int stepSizeDivisor) {
      if (mode == EncoderMode.ACCELERATED) {
         encoders[index].setAdjustValueMatcher(acceleratedMatchers[index]);
         encoders[index].setStepSize(1.0 / stepSizeDivisor);
      } else if (mode == EncoderMode.NONACCELERATED) {
         encoders[index].setAdjustValueMatcher(nonAcceleratedMatchers[index]);
         encoders[index].setStepSize(1);
      }
   }

   public void sendVuUpdate(final int index, final int value) {
      midiOut.sendMidi(Midi.CHANNEL_AT, index << 4 | value, 0);
   }

   public void sendMasterVuUpdateL(final int value) {
      midiOut.sendMidi(Midi.CHANNEL_AT | 0x1, value, 0);
   }

   public void sendMasterVuUpdateR(final int value) {
      midiOut.sendMidi(Midi.CHANNEL_AT | 0x1, 0x10 | value, 0);
   }

   public MidiIn getMidiIn() {
      return midiIn;
   }

   public void bindButton(final Layer layer, final int index, final int buttonIndex, final BooleanSupplier param,
                          final Runnable action) {
      final HardwareButton button = buttonMatrix[buttonIndex][index];
      final OnOffHardwareLight light = (OnOffHardwareLight) button.backgroundLight();
      layer.bindPressed(button, action);
      layer.bind(param, light);
   }

   public void bindButton(final Layer layer, final int index, final int buttonIndex, final BooleanSupplier param,
                          final Consumer<Boolean> action) {
      final HardwareButton button = buttonMatrix[buttonIndex][index];
      final OnOffHardwareLight light = (OnOffHardwareLight) button.backgroundLight();
      layer.bindIsPressed(button, action);
      layer.bind(param, light);
   }

   private HardwareButton createEncoderButon(final int index) {
      final HardwareButton button = driver.getSurface()
         .createHardwareButton("ENCODER_PRESS_" + sectionIndex + "_" + index);
      button.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(0, 32 + index));
      button.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(0, 32 + index));
      return button;
   }

   private HardwareButton createTouchButton(final String name, final int index) {
      final HardwareSurface surface = driver.getSurface();
      final int notNr = BasicNoteOnAssignment.TOUCH_VOLUME.getNoteNo() + index;
      final HardwareButton button = surface.createHardwareButton(name + "_" + sectionIndex + "_" + index);
      button.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(0, notNr));
      button.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(0, notNr));
      return button;
   }

   private HardwareButton createLightButton(final String name, final int index, final int notNr) {
      final HardwareSurface surface = driver.getSurface();
      final HardwareButton button = surface.createHardwareButton(name + "_" + sectionIndex + "_" + index);
      button.pressedAction().setActionMatcher(midiIn.createNoteOnActionMatcher(0, notNr + index));
      button.releasedAction().setActionMatcher(midiIn.createNoteOffActionMatcher(0, notNr + index));
      final OnOffHardwareLight led = surface.createOnOffHardwareLight(
         name + "BUTTON_LED" + "_" + sectionIndex + "_" + index);
      button.setBackgroundLight(led);
      led.onUpdateHardware(() -> sendLedLightStatus(notNr + index, led.isOn().currentValue() ? 127 : 0));
      return button;
   }

   public void resetLeds() {
      final NoteAssignment[] nv = BasicNoteOnAssignment.values();
      for (final NoteAssignment noteOnAssignment : nv) {
         sendLedLightStatus(noteOnAssignment.getNoteNo(), 0);
      }
      for (int i = 0; i < 127; i++) {
         if (lightStatusMap[i] > 0) {
            midiOut.sendMidi(Midi.NOTE_ON, i, 0);
         }
      }
      for (final RingDisplay ringDisplay : ringDisplays) {
         ringDisplay.clear();
      }
   }

   private void sendLedLightStatus(final int noteNr, final int value) {
      lightStatusMap[noteNr] = value;
      midiOut.sendMidi(Midi.NOTE_ON, noteNr, value);
   }

   public void resetFaders() {
      for (final FaderResponse fader : motorFaderDest) {
         fader.sendValue(0);
      }
   }

   public HardwareButton getButton(final int index) {
      return buttonMatrix[index / 8][index % 8];
   }

   public HardwareButton getButton(final int row, final int column) {
      return buttonMatrix[row][column];
   }

   public void fullHardwareUpdate() {
      mainDisplay.refreshDisplay();
      for (final FaderResponse fader : motorFaderDest) {
         fader.refresh();
      }

      for (final RingDisplay ringDisplay : ringDisplays) {
         ringDisplay.refresh();
      }

      for (int i = 0; i < lightStatusMap.length; i++) {
         if (lightStatusMap[i] >= 0) {
            midiOut.sendMidi(Midi.NOTE_ON, i, lightStatusMap[i]);
         }
      }
   }

   public LcdDisplay getMainDisplay() {
      return mainDisplay;
   }

   public LcdDisplay getBottomDisplay() {
      return bottomDisplay;
   }

   public boolean hasBottomDisplay() {
      return bottomDisplay != null;
   }

   public FaderResponse getMotorFader(final int index) {
      return motorFaderDest[index];
   }

   public RelativeHardwareKnob getEncoder(final int index) {
      return encoders[index];
   }

   public HardwareButton getEncoderPress(final int index) {
      return encoderPress[index];
   }

   public HardwareSlider getVolumeFader(final int index) {
      return volumeKnobs[index];
   }

   public RingDisplay getRingDisplay(final int index) {
      return ringDisplays[index];
   }

   public int getSectionIndex() {
      return sectionIndex;
   }

   public AbsoluteHardwareControlBinding createFaderParamBinding(final int index, final Parameter parameter) {
      return new AbsoluteHardwareControlBinding(volumeKnobs[index], parameter);
   }

   public TouchFaderBinding createFaderTouchBinding(final int index, final Runnable execAction) {
      return new TouchFaderBinding(faderTouch[index], createAction(execAction));
   }

   public FaderBinding createMotorFaderBinding(final int index, final Parameter param) {
      return new FaderBinding(param, motorFaderDest[index]);
   }

   public ButtonBinding createEncoderPressBinding(final int index, final Parameter param) {
      return new ButtonBinding(encoderPress[index], createAction(param::reset));
   }

   public RelativeHardwareControlToRangedValueBinding createEncoderToParamBinding(final int index,
                                                                                  final Parameter param) {
      final RelativeHardwareControlToRangedValueBinding binding = new RelativeHardwareControlToRangedValueBinding(
         encoders[index], param);
      if (driver.getControllerConfig().isHas2ClickResolution()) {
         binding.setSensitivity(2.0);
      }
      return binding;
   }

   public RingDisplayParameterBinding createRingDisplayBinding(final int index, final Parameter param,
                                                               final RingDisplayType type) {
      return new RingDisplayParameterBinding(param, ringDisplays[index], type);
   }

   public RingDisplayBoolBinding createRingDisplayBinding(final int index, final BooleanValue value,
                                                          final RingDisplayType type) {
      return new RingDisplayBoolBinding(value, ringDisplays[index], type);
   }

   public RingDisplayFixedBinding createRingDisplayBinding(final int index, final Integer value,
                                                           final RingDisplayType type) {
      return new RingDisplayFixedBinding(value, ringDisplays[index], type);
   }

   public RingDisplayExistsBinding createRingDisplayBinding(final int index, final ObjectProxy object,
                                                            final RingDisplayType type) {
      return new RingDisplayExistsBinding(object, ringDisplays[index], type);
   }

   public HardwareActionBindable createAction(final Runnable action) {
      return driver.getHost().createAction(action, null);
   }

   public void assignFaderTouchAction(final int index, final BooleanValueChangedCallback action) {
      faderTouch[index].isPressed().addValueObserver(action);
   }

}
