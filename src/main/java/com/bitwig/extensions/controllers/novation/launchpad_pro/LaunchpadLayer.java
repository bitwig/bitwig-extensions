package com.bitwig.extensions.controllers.novation.launchpad_pro;

import java.util.function.Supplier;

import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extensions.framework.Binding;
import com.bitwig.extensions.framework.Layer;

public class LaunchpadLayer extends Layer
{
   public LaunchpadLayer(final LaunchpadProControllerExtension launchpad, final String name)
   {
      super(launchpad.getLayers(), name);
      mLaunchpad = launchpad;
   }

   void bindPressed(final Button bt, final Runnable runnable)
   {
      bindPressed(bt.getButton(), runnable);
   }

   void bindReleased(final Button bt, final Runnable runnable)
   {
      bindReleased(bt.getButton(), runnable);
   }

   void bindPressed(final Button bt, final HardwareActionBindable bindable)
   {
      bindPressed(bt.getButton(), bindable);
   }

   void bindReleased(final Button bt, final HardwareActionBindable bindable)
   {
      bindReleased(bt.getButton(), bindable);
   }

   void bindOverlay(final Button bt, final Overlay overlay, final LedState ledStateOff)
   {
      bindPressed(bt, () -> mLaunchpad.setBottomOverlay(overlay, true, bt));
      bindReleased(bt, () -> mLaunchpad.setBottomOverlay(overlay, false, bt));
      bindLightState(ledStateOff, bt);
   }

   void bindMode(final Button bt, final Mode mode, final LedState ledState)
   {
      assert mode != null;
      bindPressed(bt, () -> mLaunchpad.setMode(mode));
      bindLightState(ledState, bt);
   }

   void bindLayer(final Button button, final Layer layer)
   {
      bindPressed(button, layer.getActivateAction());
      bindReleased(button, layer.getDeactivateAction());
   }

   public Binding bindLightState(final Supplier<InternalHardwareLightState> supplier, final Button button)
   {
      return bindLightState(supplier, button.getLight());
   }

   public Binding bindLightState(final LedState state, final Button button)
   {
      return bindLightState(() -> state, button);
   }

   private final LaunchpadProControllerExtension mLaunchpad;
}
