package com.bitwig.extensions.framework;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiDataReceivedCallback;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.framework.targets.Target;

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
      final MidiOut midiOut = getMidiOutToUseForLayers();

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

   protected ShortMidiDataReceivedCallback getMidiCallbackToUseForLayers()
   {
      return (ShortMidiMessageReceivedCallback)this::onMidi;
   }

   protected abstract MidiOut getMidiOutToUseForLayers();

   protected void onMidi(final ShortMidiMessage data)
   {
      for (ControlElement element : mElements)
      {
         final Target target = getTarget(element);

         if (target != null)
         {
            element.onMidi(target, data);
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

   private List<ControlElement> mElements = new ArrayList<>();

   private List<Layer> mActiveLayers = new ArrayList<>();
}
