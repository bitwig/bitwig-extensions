package com.bitwig.extensions.framework2;

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

   public Layer addLayer(final String name)
   {
      final Layer layer = new Layer(this, name);

      mLayers.add(layer);

      return layer;
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
               final Object source = binding.getExclusivityObject();

               for (final Iterator<Binding> i = mActiveBindings.iterator(); i.hasNext();)
               {
                  final Binding activeBinding = i.next();

                  if (Objects.equals(activeBinding.getExclusivityObject(), source) && activeBinding.getLayer() != layer)
                  {
                     i.remove();
                     activeBinding.setIsActive(false);
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

   void invalidateBindings()
   {
      updateActiveBindings();
   }

   private final List<Layer> mLayers = new ArrayList<>(4);

   @SuppressWarnings("rawtypes")
   private final List<Binding> mActiveBindings = new ArrayList<>();

   private final ControllerExtension mControllerExtension;
}
