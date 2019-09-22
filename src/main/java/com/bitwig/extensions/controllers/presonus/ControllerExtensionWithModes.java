package com.bitwig.extensions.controllers.presonus;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.SettableBooleanValue;

public abstract class ControllerExtensionWithModes extends ControllerExtension
{
   protected ControllerExtensionWithModes(
      final ControllerExtensionDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void flush()
   {
      final Mode mode = getMode();

      for (ControlElement element : mElements)
      {
         if (element instanceof Flushable)
         {
            ((Flushable) element).flush(mode, getMidiOut());
         }
      }
   }

   protected void onMidi(final int status, final int data1, final int data2)
   {
      final Mode mode = getMode();

      for (MidiReceiver midiReceiver : mElements)
      {
         midiReceiver.onMidi(mode, status, data1, data2);
      }
   }

   protected Mode getMode()
   {
      return mDefaultMode;
   }

   public void addElement(ControlElement element)
   {
      mElements.add(element);
   }

   @Override
   public void init()
   {

   }

   @Override
   public void exit()
   {

   }

   protected void bind(ControlElement element, Target target)
   {
      mDefaultMode.bind(element, target);
   }

   protected void bindToggle(ControlElement element, SettableBooleanValue target)
   {
      target.markInterested();

      mDefaultMode.bind(element, new ButtonTarget()
      {
         @Override
         public boolean isOn(final boolean isPressed)
         {
            return target.get();
         }

         @Override
         public void press()
         {
            target.toggle();
         }

         @Override
         public void release()
         {

         }
      });
   }

   protected abstract MidiOut getMidiOut();

   private List<Flushable> mFlushables = new ArrayList<>();
   private List<ControlElement> mElements = new ArrayList<>();
   private Mode mDefaultMode = new Mode();

}
