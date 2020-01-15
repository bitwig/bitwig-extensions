package com.bitwig.extensions.controllers.novation.launchpad_pro;

import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extensions.framework.Binding;
import com.bitwig.extensions.framework.Layer;

class LaunchpadLayer extends Layer
{
   public LaunchpadLayer(final LaunchpadProControllerExtension driver, final String name)
   {
      super(driver.getLayers(), name);
      mDriver = driver;
   }

   void bindPressed(final Button bt, final Runnable runnable)
   {
      bindPressed(bt.mButton, runnable);
   }

   void bindPressed(final Button bt, final DoubleConsumer pressedPressureConsumer)
   {
      bindPressed(bt.mButton, pressedPressureConsumer);
   }

   void bindReleased(final Button bt, final Runnable runnable)
   {
      bindReleased(bt.mButton, runnable);
   }

   void bindPressed(final Button bt, final HardwareActionBindable bindable)
   {
      bindPressed(bt.mButton, bindable);
   }

   private void bindReleased(final Button bt, final HardwareActionBindable bindable)
   {
      bindReleased(bt.mButton, bindable);
   }

   void bindOverlay(final Button bt, final Overlay overlay, final LedState ledStateOff)
   {
      bindPressed(bt, () -> mDriver.setBottomOverlay(overlay, true, bt));
      bindReleased(bt, () -> mDriver.setBottomOverlay(overlay, false, bt));
      bindLightState(ledStateOff, bt);
   }

   void bindMode(final Button bt, final Mode mode, final LedState ledState)
   {
      assert mode != null;
      bindPressed(bt, () -> mDriver.setMode(mode));
      bindLightState(ledState, bt);
   }

   void bindLayer(final Button button, final Layer layer)
   {
      bindPressed(button, layer.getActivateAction());
      bindReleased(button, layer.getDeactivateAction());
   }

   public Binding bindLightState(final Supplier<InternalHardwareLightState> supplier, final Button button)
   {
      return bindLightState(supplier, button.mLight);
   }

   public Binding bindLightState(final LedState state, final Button button)
   {
      return bindLightState(() -> state, button);
   }

   public void bindToggle(final Button button, final SettableBooleanValue target)
   {
      bindToggle(button.mButton, target);
   }

   final LaunchpadProControllerExtension mDriver;
}
