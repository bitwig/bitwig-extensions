package com.bitwig.extensions.controllers.presonus;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.BooleanValue;
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
      final MidiOut midiOut = getMidiOut();

      for (ControlElement element : mElements)
      {
         final Target target = mode.getTarget(element);

         if (target != null)
         {
            element.flush(target, midiOut);
         }
      }
   }

   protected void onMidi(final int status, final int data1, final int data2)
   {
      final Mode mode = getMode();

      for (ControlElement element : mElements)
      {
         final Target target = mode.getTarget(element);

         if (target != null)
         {
            element.onMidi(target, status, data1, data2);
         }
      }
   }

   protected Mode getMode()
   {
      return mDefaultMode;
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

   protected void bind(ButtonControlElement element, Target target)
   {
      mDefaultMode.bind(element, target);
   }

   protected void bindToggle(ButtonControlElement element, SettableBooleanValue target)
   {
      target.markInterested();

      mDefaultMode.bind(element, new ButtonTarget()
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

   protected void bindPressedRunnable(ButtonControlElement element, BooleanValue ledValue, final Runnable runnable)
   {
      ledValue.markInterested();
      mDefaultMode.bind(element, new ButtonTarget()
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

   protected abstract MidiOut getMidiOut();

   private List<ControlElement> mElements = new ArrayList<>();
   private Mode mDefaultMode = new Mode();

}
