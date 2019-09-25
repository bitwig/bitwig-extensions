package com.bitwig.extensions.controllers.presonus.framework;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.controllers.presonus.framework.targets.Target;

public abstract class LayeredControllerExtension extends ControllerExtension
{
   protected LayeredControllerExtension(
      final ControllerExtensionDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void flush()
   {
      final MidiOut midiOut = getMidiOut();

      for (ControlElement element : mElements)
      {
         final Target target = getTarget(element);

         if (target != null)
         {
            element.flush(target, midiOut);
         }
      }
   }

   private Target getTarget(final ControlElement element)
   {
      for (Layer layer : mActiveLayers)
      {
         Target target = layer.getTarget(element);

         if (target != null) return target;
      }

      return null;
   }

   protected void onMidi(final int status, final int data1, final int data2)
   {
      for (ControlElement element : mElements)
      {
         final Target target = getTarget(element);

         if (target != null)
         {
            element.onMidi(target, status, data1, data2);
         }
      }
   }

   protected void toggleLayer(Layer layer)
   {
      if (isLayerActive(layer))
      {
         deactivateLayer(layer);
      }
      else
      {
         activateLayer(layer);
      }
   }

   protected void activateLayer(Layer layer)
   {
      if (!mActiveLayers.contains(layer))
      {
         mActiveLayers.add(0, layer);
         layer.setActivate(true);
      }
   }

   protected void activateLayerInGroup(Layer layer, Layer... layerGroup)
   {
      for (Layer l : layerGroup)
      {
         if (l != layer && isLayerActive(l))
         {
            deactivateLayer(l);
         }
      }

      activateLayer(layer);
   }

   protected void deactivateLayer(Layer layer)
   {
      if (mActiveLayers.contains(layer))
      {
         mActiveLayers.remove(layer);
         layer.setActivate(false);
      }
   }

   protected boolean isLayerActive(Layer layer)
   {
      return mActiveLayers.contains(layer);
   }

   public <T extends ControlElement> T addElement(T element)
   {
      mElements.add(element);

      return (T)element;
   }

   @Override
   public void init()
   {

   }

   @Override
   public void exit()
   {

   }

   protected abstract MidiOut getMidiOut();

   private List<ControlElement> mElements = new ArrayList<>();

   private List<Layer> mActiveLayers = new ArrayList<>();
}
