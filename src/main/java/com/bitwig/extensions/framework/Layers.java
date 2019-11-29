package com.bitwig.extensions.framework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import com.bitwig.extension.controller.ControllerExtension;

public class Layers
{
   public Layers(final ControllerExtension controllerExtension)
   {
      super();
      mControllerExtension = controllerExtension;
   }

   public ControllerExtension getControllerExtension()
   {
      return mControllerExtension;
   }

   void addLayer(final Layer layer)
   {
      mLayers.add(layer);
   }

   public Layer addLayer(final String name)
   {
      return new Layer(this, name);
   }

   public List<Layer> getLayers()
   {
      return Collections.unmodifiableList(mLayers);
   }

   @SuppressWarnings("rawtypes")
   private void updateActiveBindings()
   {
      mActiveBindings.clear();

      for (final Layer layer : mLayers)
      {
         if (layer.isActive())
         {
            for (final Binding binding : layer.mBindings)
            {
               if (layer.shouldReplaceBindingsInLayersBelow())
               {
                  final Object exclusivityObject = binding.getExclusivityObject();

                  for (final Iterator<Binding> i = mActiveBindings.iterator(); i.hasNext();)
                  {
                     final Binding activeBinding = i.next();

                     if (Objects.equals(activeBinding.getExclusivityObject(), exclusivityObject)
                        && activeBinding.getLayer() != layer)
                     {
                        i.remove();
                        activeBinding.setIsActive(false);
                     }
                  }
               }

               mActiveBindings.add(binding);
            }
         }
         else
         {
            for (final Binding binding : layer.mBindings)
            {
               binding.setIsActive(false);
            }
         }
      }

      for (final Binding binding : mActiveBindings)
      {
         binding.setIsActive(true);
      }
   }

   protected void activeLayersChanged()
   {
      updateActiveBindings();
   }

   public List<Binding> getActiveBindings()
   {
      return Collections.unmodifiableList(mActiveBindings);
   }

   public double getGlobalSensitivity()
   {
      return mGlobalSensitivity;
   }

   public void setGlobalSensitivity(final double value)
   {
      if (value != mGlobalSensitivity)
      {
         mGlobalSensitivity = value;

         for (final Layer layer : mLayers)
         {
            for (final Binding binding : layer.getBindings())
            {
               if (binding instanceof BindingWithSensitivity)
                  ((BindingWithSensitivity)binding).setGlobalSensitivity(value);
            }
         }
      }
   }

   private final List<Layer> mLayers = new ArrayList<>(4);

   @SuppressWarnings("rawtypes")
   private final List<Binding> mActiveBindings = new ArrayList<>();

   private final ControllerExtension mControllerExtension;

   private double mGlobalSensitivity = 1;
}
