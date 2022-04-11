package com.bitwig.extensions.controllers.mackie.devices;

import com.bitwig.extension.callback.DoubleValueChangedCallback;
import com.bitwig.extension.callback.IntegerValueChangedCallback;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.mackie.bindings.FaderParameterBankBinding;
import com.bitwig.extensions.controllers.mackie.bindings.ResetableAbsoluteValueBinding;
import com.bitwig.extensions.controllers.mackie.bindings.ResetableRelativeValueBinding;
import com.bitwig.extensions.controllers.mackie.bindings.ring.RingParameterBankDisplayBinding;
import com.bitwig.extensions.controllers.mackie.display.FaderResponse;
import com.bitwig.extensions.controllers.mackie.display.RingDisplay;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.controllers.mackie.value.ModifierValueObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

public class ParameterPage implements SettableRangedValue {

   private static final int RING_RANGE = 10;
   private DeviceParameter currentParameter;

   private ResetableRelativeValueBinding relativeEncoderBinding;
   private ResetableAbsoluteValueBinding absoluteEncoderBinding;
   private RingParameterBankDisplayBinding ringBinding;
   private FaderParameterBankBinding faderBinding;

   // listeners for the parameter Name
   private final List<Consumer<String>> nameChangeCallbacks = new ArrayList<>();
   // listeners for the display values
   private final List<Consumer<String>> valueChangeCallbacks = new ArrayList<>();
   // listeners for the ring display values
   private final List<IntConsumer> intValueCallbacks = new ArrayList<>();
   // listeners for existence of value
   private final List<DoubleConsumer> doulbeValueCallbacks = new ArrayList<>();

   private final List<DeviceParameter> pages = new ArrayList<>();

   public ParameterPage(final int index, final SpecificDevice device) {

      for (int page = 0; page < device.getPages(); page++) {
         final int pIndex = pages.size();
         final DeviceParameter deviceParameter = device.createDeviceParameter(page, index);
         final Parameter param = deviceParameter.parameter;

         param.value().markInterested();
         param.value().addValueObserver(v -> {
            if (pIndex == device.getCurrentPage()) {
               final int intv = (int) (v * RING_RANGE);
               notifyIntValueChanged(intv);
               notifyValueChanged(v);
            }
         });
         if (deviceParameter.getCustomValueConverter() != null) {
            final CustomValueConverter converter = deviceParameter.getCustomValueConverter();
            param.value().addValueObserver(converter.getIntRange(), v -> {
               if (pIndex == device.getCurrentPage()) {
                  notifyValueChanged(converter.convert(v));
               }
            });
         } else {
            param.value().displayedValue().addValueObserver(v -> {
               if (pIndex == device.getCurrentPage()) {
                  notifyValueChanged(v);
               }
            });
         }

         pages.add(deviceParameter);
      }
      currentParameter = pages.get(device.getCurrentPage());
   }

   public Parameter getParameter(final int pageIndex) {
      assert pageIndex < pages.size();
      return pages.get(pageIndex).parameter;
   }

   public void triggerUpdate() {
      if (ringBinding != null) {
         ringBinding.update();
      }
      if (faderBinding != null) {
         faderBinding.update();
      }
   }

   public RingParameterBankDisplayBinding createRingBinding(final RingDisplay display) {
      ringBinding = new RingParameterBankDisplayBinding(this, display);
      return ringBinding;
   }

   public FaderParameterBankBinding createFaderBinding(final FaderResponse fader) {
      faderBinding = new FaderParameterBankBinding(this, fader);
      return faderBinding;
   }

   public ResetableRelativeValueBinding getRelativeEncoderBinding(final RelativeHardwareKnob encoder) {
      relativeEncoderBinding = new ResetableRelativeValueBinding(encoder, this);
      return relativeEncoderBinding;
   }

   public ResetableAbsoluteValueBinding getFaderBinding(final HardwareSlider fader) {
      absoluteEncoderBinding = new ResetableAbsoluteValueBinding(fader, this);
      return absoluteEncoderBinding;
   }

   public void updatePage(final int currentPage) {
      currentParameter = pages.get(currentPage);
      resetBindings();
   }

   public void resetBindings() {
      if (relativeEncoderBinding != null) {
         relativeEncoderBinding.reset();
      }
      if (absoluteEncoderBinding != null) {
         absoluteEncoderBinding.reset();
      }
      notifyValueChanged(getCurrentValue());
      notifyNameChanged(getCurrentName());
   }

   public Parameter getCurrentParameter() {
      return currentParameter.parameter;
   }

   @Override
   public double get() {
      return currentParameter.parameter.get();
   }

   @Override
   public void markInterested() {
      currentParameter.parameter.markInterested();
   }

   public void addStringValueObserver(final Consumer<String> callback) {
      valueChangeCallbacks.add(callback);
   }

   @Override
   public void addValueObserver(final DoubleValueChangedCallback callback) {
   }

   @Override
   public boolean isSubscribed() {
      return currentParameter.parameter.isSubscribed();
   }

   @Override
   public void setIsSubscribed(final boolean value) {
   }

   @Override
   public void subscribe() {
      currentParameter.parameter.subscribe();
   }

   @Override
   public void unsubscribe() {
      currentParameter.parameter.unsubscribe();
   }

   @Override
   public void set(final double value) {
      currentParameter.parameter.set(value);
   }

   @Override
   public void inc(final double amount) {
      currentParameter.parameter.inc(amount);
   }

   @Override
   public double getRaw() {
      return currentParameter.parameter.getRaw();
   }

   @Override
   public StringValue displayedValue() {
      return currentParameter.parameter.displayedValue();
   }

   @Override
   public void addValueObserver(final int range, final IntegerValueChangedCallback callback) {
      // Not needed
   }

   @Override
   public void addRawValueObserver(final DoubleValueChangedCallback callback) {
      // Not needed
   }

   @Override
   public void setImmediately(final double value) {
      currentParameter.parameter.setImmediately(value);
   }

   @Override
   public void set(final Number value, final Number resolution) {
      currentParameter.parameter.set(value, resolution);
   }

   @Override
   public void inc(final Number increment, final Number resolution) {
      currentParameter.parameter.inc(increment, resolution);
   }

   @Override
   public void setRaw(final double value) {
      currentParameter.parameter.setRaw(value);
   }

   @Override
   public void incRaw(final double delta) {
      currentParameter.parameter.incRaw(delta);
   }

   @Override
   public AbsoluteHardwareControlBinding addBindingWithRange(final AbsoluteHardwareControl hardwareControl,
                                                             final double minNormalizedValue,
                                                             final double maxNormalizedValue) {
      return currentParameter.parameter.addBindingWithRange(hardwareControl, minNormalizedValue, maxNormalizedValue);
   }

   @Override
   public RelativeHardwareControlToRangedValueBinding addBindingWithRangeAndSensitivity(
      final RelativeHardwareControl hardwareControl, final double minNormalizedValue, final double maxNormalizedValue,
      final double sensitivity) {
      return currentParameter.parameter.addBindingWithRangeAndSensitivity(hardwareControl, minNormalizedValue,
         maxNormalizedValue, currentParameter.getSensitivity());
   }

   private void notifyValueChanged(final String value) {
      valueChangeCallbacks.forEach(callback -> callback.accept(value));
   }

   public String getCurrentValue() {
      return currentParameter.getStringValue();
   }

   public void addNameObserver(final Consumer<String> callback) {
      nameChangeCallbacks.add(callback);
   }

   private void notifyNameChanged(final String name) {
      nameChangeCallbacks.forEach(callback -> callback.accept(currentParameter.getName()));
   }

   public String getCurrentName() {
      return currentParameter.getName();
   }

   public void addIntValueObserver(final IntConsumer listener) {
      intValueCallbacks.add(listener);
   }

   private void notifyIntValueChanged(final int value) {
      intValueCallbacks.forEach(callback -> callback.accept(value));
   }

   private void notifyValueChanged(final double value) {
      doulbeValueCallbacks.forEach(callback -> callback.accept(value));
   }

   public int getIntValue() {
      return (int) (currentParameter.parameter.value().get() * RING_RANGE);
   }

   public RingDisplayType getRingDisplayType() {
      return currentParameter.getRingDisplayType();
   }

   public double getParamValue() {
      return currentParameter.parameter.value().get();
   }

   public void addDoubleValueObserver(final DoubleConsumer listener) {
      doulbeValueCallbacks.add(listener);
   }

   public void doReset() {
      currentParameter.parameter.reset();
   }

   public void resetAll() {
      for (final DeviceParameter parameter : pages) {
         parameter.doReset();
      }
   }

   public void notifyEnablement(final int value) {
      ringBinding.handleEnabled(value);
   }

   public void doReset(final ModifierValueObject modifier) {
      currentParameter.parameter.reset();
   }

}
