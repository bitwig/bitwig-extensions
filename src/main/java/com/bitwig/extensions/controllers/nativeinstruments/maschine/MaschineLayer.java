package com.bitwig.extensions.controllers.nativeinstruments.maschine;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.buttons.ModeButton;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.buttons.PadButton;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.display.DisplayLayer;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.display.OnOffLightStateBinding;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.modes.PadMode;
import com.bitwig.extensions.framework.Binding;
import com.bitwig.extensions.framework.Layer;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

public class MaschineLayer extends Layer {

   private final MaschineExtension driver;

   public MaschineLayer(final MaschineExtension driver, final String name) {
      super(driver.getLayers(), name);
      this.driver = driver;
   }

   public MaschineExtension getDriver() {
      return driver;
   }

   protected void bindPressed(final ModeButton bt, final Runnable runnable) {
      bindPressed(bt.getHwButton(), runnable);
   }

   public void bindPressed(final ModeButton bt, final SettableBooleanValue value) {
      bindPressed(bt.getHwButton(), value);
   }

   void bindIsPressed(final ModeButton bt, final SettableBooleanValue value) {
      bindIsPressed(bt.getHwButton(), value);
   }

   public void bindPressed(final ModeButton bt, final BooleanValue state, final Runnable action) {
      state.markInterested();
      bindPressed(bt.getHwButton(), action);
      bindLightState(state, bt);
   }

   public void bindPressed(final RgbButton bt, final Runnable runnable) {
      bindPressed(bt.getHwButton(), runnable);
   }

   protected void bindPressed(final RgbButton bt, final DoubleConsumer pressedPressureConsumer) {
      bindPressed(bt.getHwButton(), pressedPressureConsumer);
   }

   protected void bindReleased(final RgbButton bt, final Runnable runnable) {
      bindReleased(bt.getHwButton(), runnable);
   }

   protected void bindPressed(final RgbButton bt, final HardwareActionBindable bindable) {
      bindPressed(bt.getHwButton(), bindable);
   }

   private void bindReleased(final RgbButton bt, final HardwareActionBindable bindable) {
      bindReleased(bt.getHwButton(), bindable);
   }

   //	void bindOverlay(final PadButton bt, final Overlay overlay, final LedState ledStateOff) {
//		bindPressed(bt, () -> driver.setBottomOverlay(overlay, true, bt));
//		bindReleased(bt, () -> driver.setBottomOverlay(overlay, false, bt));
//		bindLightState(ledStateOff, bt);
//	}
//
   public void bindMode(final ModeButton bt, final DisplayLayer mode, final boolean decoupleLigth) {
      assert mode != null;
      bindPressed(bt.getHwButton(), () -> driver.setDisplayMode(mode));
      if (!decoupleLigth) {
         mode.getActive().addValueObserver(v -> bt.getLed().isOn().setValue(v));
      }
   }

   public void bindModeMomentary(final ModeButton bt, final DisplayLayer mode, final boolean decoupleLigth) {
      assert mode != null;
      bindPressed(bt.getHwButton(), () -> driver.setDisplayMode(mode));
      bindReleased(bt.getHwButton(), () -> driver.backToPreviousDisplayMode());
      if (!decoupleLigth) {
         mode.getActive().addValueObserver(v -> bt.getLed().isOn().setValue(v));
      }
   }

   public void bindMode(final ModeButton bt, final PadMode mode) {
      assert mode != null;
      bindPressed(bt.getHwButton(), () -> driver.setMode(mode));
      mode.getActive().addValueObserver(v -> bt.getLed().isOn().setValue(v));
   }

   protected void bindLightState(final ModeButton but, final BooleanValue value) {
      value.addValueObserver(v -> but.getLed().isOn().setValue(v));
   }

   public void bindLightState(final HardwareButton button, final BooleanValue value) {
      final HardwareLight backgroundLight = button.backgroundLight();

      if (backgroundLight instanceof OnOffHardwareLight) {
         bind(value, (OnOffHardwareLight) button.backgroundLight());
      }
   }

   protected void bindLayer(final PadButton button, final Layer layer) {
      bindPressed(button, layer.getActivateAction());
      bindReleased(button, layer.getDeactivateAction());
   }

   @SuppressWarnings("rawtypes")
   public Binding bindLightState(final Supplier<InternalHardwareLightState> supplier, final RgbButton button) {
      return bindLightState(supplier, button.getLight());
   }

   @SuppressWarnings("rawtypes")
   public Binding bindLightState(final ModeButton button, final BooleanSupplier supplier) {
      final OnOffHardwareLight led = button.getLed();
      final OnOffLightStateBinding binding = new OnOffLightStateBinding(supplier, led);
      addBinding(binding);
      return binding;
   }

   @SuppressWarnings("rawtypes")
   public Binding bindLightState(final BooleanSupplier supplier, final ModeButton button) {
      final OnOffHardwareLight led = button.getLed();
      final OnOffLightStateBinding binding = new OnOffLightStateBinding(supplier, led);
      addBinding(binding);
      return binding;
   }

   @SuppressWarnings("rawtypes")
   public Binding bindLightState(final RgbLed state, final RgbButton button) {
      return bindLightState(() -> state, button);
   }

   public void bindToggle(final PadButton button, final SettableBooleanValue target) {
      bindToggle(button.getHwButton(), target);
   }

}
