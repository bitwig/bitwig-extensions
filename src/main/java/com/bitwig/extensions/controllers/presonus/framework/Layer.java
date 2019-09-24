package com.bitwig.extensions.controllers.presonus.framework;

import java.util.HashMap;
import java.util.Map;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.SettableBooleanValue;

public class Layer
{
   public void bind(ControlElement element, Target target)
   {
      mMap.put(element, target);
   }

   public void setActivate(final boolean active)
   {
   }

   public void bindToggle(ControlElement<ButtonTarget> element, SettableBooleanValue target)
   {
      target.markInterested();

      bind(element, new ButtonTarget()
      {
         @Override
         public boolean get()
         {
            return target.get();
         }

         @Override
         public void set(final boolean pressed)
         {
            if (pressed) target.toggle();
         }
      });
   }

   public void bindPressedRunnable(ControlElement<ButtonTarget> element, BooleanValue ledValue, final Runnable runnable)
   {
      if (ledValue != null)
      {
         ledValue.markInterested();
      }

      bind(element, new ButtonTarget()
      {
         @Override
         public boolean get()
         {
            return ledValue != null ? ledValue.get() : false;
         }

         @Override
         public void set(final boolean pressed)
         {
            if (pressed) runnable.run();
         }
      });
   }

   public void bindLayerInGroup(ControllerExtensionWithLayers host, ControlElement<ButtonTarget> element, Layer layer, Layer... layerGroup)
   {
      bind(element, new ButtonTarget()
      {
         @Override
         public boolean get()
         {
            return host.isLayerActive(layer);
         }

         @Override
         public void set(final boolean pressed)
         {
            if (pressed) host.activateLayerInGroup(layer, layerGroup);
         }
      });
   }

   public void bind(ControlElement<TouchFaderTarget> element, Parameter parameter)
   {
      if (parameter != null)
      {
         parameter.markInterested();
      }

      bind(element, new FaderParameterTarget(parameter));
   }

   public <T extends Target> T getTarget(final ControlElement element)
   {
      return (T) mMap.get(element);
   }

   Map<ControlElement, Target> mMap = new HashMap<>();
}
