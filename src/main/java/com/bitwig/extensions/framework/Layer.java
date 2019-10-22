package com.bitwig.extensions.framework;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extensions.framework.targets.ButtonTarget;
import com.bitwig.extensions.framework.targets.EncoderTarget;
import com.bitwig.extensions.framework.targets.FaderParameterTarget;
import com.bitwig.extensions.framework.targets.Target;
import com.bitwig.extensions.framework.targets.TouchFaderTarget;

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

   public void bindButton(ControlElement<ButtonTarget> element, BooleanValue ledValue, final Consumer<Boolean> consumer)
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
            consumer.accept(pressed);
         }
      });
   }

   public void bindLayerToggle(LayeredControllerExtension host, ControlElement<ButtonTarget> element, Layer layer)
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
            if (pressed) host.toggleLayer(layer);
         }
      });
   }

   public void bindLayerInGroup(LayeredControllerExtension host, ControlElement<ButtonTarget> element, Layer layer, Layer... layerGroup)
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

   public void bindEncoder(ControlElement<EncoderTarget> element, Parameter parameter, final int resolution)
   {
      if (parameter != null)
      {
         parameter.markInterested();
      }

      bind(element, new EncoderTarget()
      {
         @Override
         public void inc(final int steps)
         {
            parameter.inc(steps, resolution);
         }
      });
   }


   public <T extends Target> T getTarget(final ControlElement element)
   {
      return (T) mMap.get(element);
   }

   Map<ControlElement, Target> mMap = new HashMap<>();
}
