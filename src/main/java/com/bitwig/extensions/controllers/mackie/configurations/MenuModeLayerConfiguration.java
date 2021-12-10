package com.bitwig.extensions.controllers.mackie.configurations;

import com.bitwig.extension.callback.BooleanValueChangedCallback;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.mackie.bindings.*;
import com.bitwig.extensions.controllers.mackie.display.DisplayLayer;
import com.bitwig.extensions.controllers.mackie.display.RingDisplay;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.controllers.mackie.layer.DisplayLocation;
import com.bitwig.extensions.controllers.mackie.layer.EncoderLayer;
import com.bitwig.extensions.controllers.mackie.layer.EncoderMode;
import com.bitwig.extensions.controllers.mackie.section.MixControl;
import com.bitwig.extensions.controllers.mackie.section.MixerSectionHardware;
import com.bitwig.extensions.controllers.mackie.section.ParamElement;
import com.bitwig.extensions.controllers.mackie.value.*;
import com.bitwig.extensions.framework.Layer;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class MenuModeLayerConfiguration extends LayerConfiguration {
   private final EncoderLayer encoderLayer;
   private final DisplayLayer displayLayer;
   private Consumer<String> displayEvaluation;
   private final ModifierValueObject modifier;

   public MenuModeLayerConfiguration(final String name, final MixControl mixControl) {
      super(name, mixControl);
      final int sectionIndex = mixControl.getHwControls().getSectionIndex();
      encoderLayer = new EncoderLayer(mixControl, name + "_ENCODER_LAYER_" + sectionIndex);
      encoderLayer.setEncoderMode(EncoderMode.NONACCELERATED);
      final int section = mixControl.getHwControls().getSectionIndex();
      displayLayer = new DisplayLayer(name, section, mixControl.getDriver().getLayers(), mixControl.getHwControls());
      modifier = mixControl.getModifier();
   }

   public void setTextEvaluation(final Consumer<String> action) {
      displayEvaluation = action;
   }

   public void evaluateTextDisplay(final String text) {
      if (displayEvaluation != null) {
         displayEvaluation.accept(text);
      }
   }

   @Override
   public boolean isActive() {
      return encoderLayer.isActive();
   }

   @Override
   public Layer getFaderLayer() {
      return mixControl.getActiveMixGroup().getFaderLayer(ParamElement.VOLUME);
   }

   @Override
   public EncoderLayer getEncoderLayer() {
      return encoderLayer;
   }

   @Override
   public DisplayLayer getDisplayLayer(final int which) {
      return displayLayer;
   }

   @Override
   public DisplayLayer getBottomDisplayLayer(final int which) {
      return getMixControl().getActiveMixGroup().getDisplayConfiguration(ParamElement.VOLUME, DisplayLocation.BOTTOM);
   }

   public void addNameBinding(final int index, final int span, final StringValue name) {
      displayLayer.bindTitle(0, index, span, name, "[]");
   }

   public void addNameBinding(final int index, final StringValue name) {
      displayLayer.bindTitle(index, name);
   }

   public void addNameBinding(final int index, final StringValue nameSource, final ObjectProxy source,
                              final String emptyValue) {
      displayLayer.bindTitle(index, nameSource, source, emptyValue);
   }

   public void addPressEncoderBinding(final int index, final IntConsumer pressAction, final boolean feedback) {
      final MixerSectionHardware hwControls = mixControl.getHwControls();

      final HardwareButton encoderPress = hwControls.getEncoderPress(index);
      encoderLayer.addBinding(
         new ButtonBinding(encoderPress, hwControls.createAction(() -> pressAction.accept(index))));
      if (feedback) {
         final RingDisplayBoolBinding ringBinding = hwControls.createRingDisplayBinding(index, encoderPress.isPressed(),
            RingDisplayType.FILL_LR_0);
         encoderLayer.addBinding(ringBinding);
      }
   }

   public void addPressEncoderBinding(final int index, final BooleanValueChangedCallback callback) {
      final MixerSectionHardware hwControls = mixControl.getHwControls();
      final HardwareButton encoderPress = hwControls.getEncoderPress(index);
      encoderLayer.addBinding(
         new ButtonBinding(encoderPress, hwControls.createAction(() -> callback.valueChanged(true))));
   }


   public void addRingBoolBinding(final int index, final BooleanValue value) {
      final MixerSectionHardware hwControls = mixControl.getHwControls();
      encoderLayer.addBinding(hwControls.createRingDisplayBinding(index, value, RingDisplayType.FILL_LR_0));
   }

   public void addRingBoolBinding(final int index, final Parameter value) {
      final MixerSectionHardware hwControls = mixControl.getHwControls();
      encoderLayer.addBinding(hwControls.createRingDisplayBinding(index, value, RingDisplayType.FILL_LR_0));
   }

   public void addRingFixedBinding(final int index) {
      final MixerSectionHardware hwControls = mixControl.getHwControls();
      encoderLayer.addBinding(hwControls.createRingDisplayBinding(index, 0, RingDisplayType.FILL_LR_0));
   }

   public void addRingFixedBindingActive(final int index) {
      final MixerSectionHardware hwControls = mixControl.getHwControls();
      encoderLayer.addBinding(hwControls.createRingDisplayBinding(index, 11, RingDisplayType.FILL_LR_0));
   }

   public void addRingExistsBinding(final int index, final ObjectProxy existSource) {
      final MixerSectionHardware hwControls = mixControl.getHwControls();
      encoderLayer.addBinding(hwControls.createRingDisplayBinding(index, existSource, RingDisplayType.FILL_LR_0));
   }

   public void addValueBinding(final int i, final DoubleValue value, final ObjectProxy existSource,
                               final String nonExistText, final ValueConverter converter) {
      displayLayer.bindParameterValue(i, value, existSource, nonExistText, converter);
   }

   public void addDisplayValueBinding(final int i, final StringValue value) {
      displayLayer.bindTitle(1, i, value);
   }

   public void addDisplayValueBinding(final int i, final SettableEnumValue value, final EnumValueSetting values) {
      displayLayer.bindParameterValue(i, value, values);
   }

   public void addDisplayValueBinding(final int i, final IntValueObject value) {
      displayLayer.bindParameterValue(i, value);
   }

   public <T> void addDisplayValueBinding(final int i, final ValueObject<T> value) {
      displayLayer.bindParameterValue(i, value);
   }

   public void addValueBinding(final int i, final DoubleValue value, final ValueConverter converter) {
      displayLayer.bindParameterValue(i, value, converter);
   }

   public void addValueBinding(final int i, final SettableRangedValue value, final ValueConverter converter) {
      displayLayer.bindValue(i, value, converter);
   }

   public void addValueBinding(final int i, final BooleanValue value, final String trueString,
                               final String falseString) {
      displayLayer.bindBool(i, value, trueString, falseString);
   }

   public void addEncoderIncBinding(final int i, final IntConsumer intHandler) {
      final MixerSectionHardware hwControls = mixControl.getHwControls();
      final RelativeHardwareKnob encoder = hwControls.getEncoder(i);
      final RelativeHardwarControlBindable incBinder = getDriver().createIncrementBinder(intHandler);
      encoderLayer.bind(encoder, incBinder);
   }

   public void addEncoderIncBinding(final int i, final SettableBeatTimeValue position, final double increment,
                                    final double shiftIncrement) {
      final MixerSectionHardware hwControls = mixControl.getHwControls();
      final RelativeHardwareKnob encoder = hwControls.getEncoder(i);
      final RelativeHardwarControlBindable incBinder = getDriver().createIncrementBinder(
         inc -> position.set(position.get() + inc * (modifier.isShiftSet() ? shiftIncrement : increment)));
      encoderLayer.bind(encoder, incBinder);
   }

   public void addEncoderIncBinding(final int i, final SettableEnumValue value, final EnumValueSetting values) {
      final MixerSectionHardware hwControls = mixControl.getHwControls();
      final RelativeHardwareKnob encoder = hwControls.getEncoder(i);
      final RelativeHardwarControlBindable incBinder = getDriver().createIncrementBinder(
         inc -> values.increment(value, inc));
      encoderLayer.bind(encoder, incBinder);
   }

   public <T> void addEncoderIncBinding(final int i, final IncrementalValue value) {
      final MixerSectionHardware hwControls = mixControl.getHwControls();
      final RelativeHardwareKnob encoder = hwControls.getEncoder(i);
      final RelativeHardwarControlBindable incBinder = getDriver().createIncrementBinder(inc -> value.increment(inc));
      encoderLayer.bind(encoder, incBinder);
   }

   public void addEncoderBinding(final int i, final SettableRangedValue value, final double sensitivity) {
      final MixerSectionHardware hwControls = mixControl.getHwControls();
      final RelativeHardwareKnob encoder = hwControls.getEncoder(i);
      final ResetableRelativeValueBinding absoluteEncoderBinding = new ResetableRelativeValueBinding(encoder, value,
         sensitivity);
      encoderLayer.setEncoderMode(EncoderMode.ACCELERATED);
      encoderLayer.addBinding(absoluteEncoderBinding);
      final RingDisplay ringDisplay = hwControls.getRingDisplay(i);
      final RingDisplayRangedValueBinding ringBinding = new RingDisplayRangedValueBinding(value, ringDisplay,
         RingDisplayType.FILL_LR);
      encoderLayer.addBinding(ringBinding);
   }

   public void addRingBinding(final int i, final SettableEnumValue value, final EnumValueSetting values) {
      final MixerSectionHardware hwControls = mixControl.getHwControls();
      final RingDisplay ringDisplay = hwControls.getRingDisplay(i);
      final RingDisplayEnumValueBinding ringBinding = new RingDisplayEnumValueBinding(value, ringDisplay,
         RingDisplayType.FILL_LR_0, values);
      encoderLayer.addBinding(ringBinding);
   }

   public void addRingBinding(final int i, final SettableRangedValue value) {
      final MixerSectionHardware hwControls = mixControl.getHwControls();
      final RingDisplay ringDisplay = hwControls.getRingDisplay(i);
      final RingDisplayRangedValueBinding binding = new RingDisplayRangedValueBinding(value, ringDisplay,
         RingDisplayType.FILL_LR_0);
      encoderLayer.addBinding(binding);
   }
}
