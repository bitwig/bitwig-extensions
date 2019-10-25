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
import com.bitwig.extensions.framework.targets.RGBButtonTarget;
import com.bitwig.extensions.framework.targets.Target;
import com.bitwig.extensions.framework.targets.TouchFaderTarget;

public class Layer
{
   public void bind(final ControlElement element, final Target target)
   {
      mMap.put(element, target);
   }

   public void setActivate(final boolean active)
   {
      if (active)
      {
         for (final Map.Entry<ControlElement, Target> entry : mMap.entrySet())
         {
            final ControlElement controlElement = entry.getKey();
            final Target target = entry.getValue();

            controlElement.setTarget(target);
         }
      }
   }

   public void bindToggle(final ControlElement<ButtonTarget> element, final SettableBooleanValue target)
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

   public void bindPressedRunnable(final ControlElement<ButtonTarget> element, final BooleanValue ledValue, final Runnable runnable)
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

   public void bindPressedRunnable(final ControlElement<RGBButtonTarget> element, final float[] RGB, final Runnable runnable)
   {
      bind(element, new RGBButtonTarget()
      {
         @Override
         public float[] getRGB()
         {
            return RGB;
         }

         @Override
         public boolean get()
         {
            return true;
         }

         @Override
         public void set(final boolean pressed)
         {
            if (pressed) runnable.run();
         }
      });
   }

   public void bindButton(final ControlElement<ButtonTarget> element, final BooleanValue ledValue, final Consumer<Boolean> consumer)
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

   public void bindLayerToggle(final LayeredControllerExtension host, final ControlElement<ButtonTarget> element, final Layer layer)
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

   public void bindLayerGate(final LayeredControllerExtension host, final ControlElement<ButtonTarget> element, final Layer layer)
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
            if (pressed) host.activateLayer(layer);
            else host.deactivateLayer(layer);
         }
      });
   }

   public void bindLayerInGroup(final LayeredControllerExtension host, final ControlElement<ButtonTarget> element, final Layer layer, final Layer... layerGroup)
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

   public void bind(final ControlElement<TouchFaderTarget> element, final Parameter parameter)
   {
      if (parameter != null)
      {
         parameter.markInterested();
      }

      bind(element, new FaderParameterTarget(parameter));
   }

   public void bindEncoder(final ControlElement<EncoderTarget> element, final Parameter parameter, final int resolution)
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
