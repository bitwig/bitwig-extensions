package com.bitwig.extensions.controllers.arturia.keylab.mk2;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareTextDisplay;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extensions.framework.Layers;

class Layer extends com.bitwig.extensions.framework.Layer
{
   public Layer(final Layers layers, final String name)
   {
      super(layers, name);

      mExtension = (ArturiaKeylabMkII)layers.getControllerExtension();
   }

   public void bindPressed(final ButtonId buttonId, final HardwareActionBindable action)
   {
      bindPressed(button(buttonId), action);
   }

   public void bindPressed(final ButtonId buttonId, final Runnable action)
   {
      bindPressed(button(buttonId), action);
   }

   public void bindReleased(final ButtonId buttonId, final Runnable action)
   {
      bindReleased(button(buttonId), action);
   }

   public void bindToggle(final ButtonId buttonId, final SettableBooleanValue value)
   {
      bindToggle(button(buttonId), value);
   }

   public void bindToggle(final ButtonId buttonId, final Layer value)
   {
      bindToggle(button(buttonId), value);
   }

   public void bindToggle(
      final ButtonId button,
      final HardwareActionBindable pressedAction,
      final BooleanSupplier isLightOnOffSupplier)
   {
      bindToggle(button(button), pressedAction, isLightOnOffSupplier);
   }

   public void bindToggle(
      final ButtonId button,
      final Runnable pressedAction,
      final BooleanSupplier isLightOnOffSupplier)
   {
      bindToggle(button(button), pressedAction, isLightOnOffSupplier);
   }

   public void bind(final Supplier<Color> color, final ButtonId buttonId)
   {
      bind(color, button(buttonId));
   }

   public void bind(final Color color, final ButtonId buttonId)
   {
      bind(() -> color, button(buttonId));
   }

   public void bind(final BooleanSupplier isOn, final ButtonId buttonId)
   {
      bind(isOn, button(buttonId));
   }

   public void showText(final Supplier<String> topLine, final Supplier<String> bottomLine)
   {
      final HardwareTextDisplay display = mExtension.mDisplay;

      bind(topLine, display, 0);
      bind(bottomLine, display, 1);
   }

   private HardwareButton button(final ButtonId buttonId)
   {
      return mExtension.getButton(buttonId);
   }

   private final ArturiaKeylabMkII mExtension;
}
