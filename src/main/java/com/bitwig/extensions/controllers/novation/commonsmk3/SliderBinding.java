package com.bitwig.extensions.controllers.novation.commonsmk3;

import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.framework.Binding;

/**
 * A two way binding for Launchpad virtual sliders.
 */
public class SliderBinding extends Binding<Parameter, HardwareSlider> {
   private final int ccNr;
   private final MidiProcessor midiProcessor;
   private int currentValue;
   private boolean process = true;
   int currSliderValue;
   private final int index;

   public SliderBinding(final int ccNr, final Parameter source, final HardwareSlider target, final int index,
                        final MidiProcessor midiProcessor) {

      super(source, source, target);
      target.value().addValueObserver(sliderValue -> this.handleSliderChange(source, sliderValue));

      source.value().addValueObserver(128, this::valueChanged);
      this.ccNr = ccNr + index;
      this.midiProcessor = midiProcessor;
      this.index = index;
   }

   private void handleSliderChange(final Parameter parameter, final double sliderValue) {
      currSliderValue = (int) Math.round(sliderValue * 127);
      if (isActive()) {
         process = false;
         parameter.set(sliderValue);
      }
   }

   private void valueChanged(final int value) {
      if (value != currentValue) {
         currentValue = value;
         if (isActive() && process) {
            midiProcessor.sendToSlider(ccNr, value);
         }
         process = true;
      }
   }

   public void update() {
      if (isActive()) {
         midiProcessor.sendToSlider(ccNr, currentValue);
      }
   }

   @Override
   protected void deactivate() {
   }

   @Override
   protected void activate() {
      midiProcessor.sendToSlider(ccNr, currentValue);
   }

}
