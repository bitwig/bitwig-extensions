package com.bitwig.extensions.controllers.presonus.framework;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiOut;

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
         final Target target = getTarget(element);

         if (target != null)
         {
            element.flush(target, midiOut);
         }
      }
   }

   private Target getTarget(final ControlElement element)
   {
      if (mMode != null)
      {
         Target target = mMode.getTarget(element);

         if (target != null) return target;
      }

      return mDefaultMode.getTarget(element);
   }

   protected void onMidi(final int status, final int data1, final int data2)
   {
      final Mode mode = getMode();

      for (ControlElement element : mElements)
      {
         final Target target = getTarget(element);

         if (target != null)
         {
            element.onMidi(target, status, data1, data2);
         }
      }
   }

   protected void setMode(Mode mode)
   {
      if (mode != mMode)
      {
         mMode = mode;

         if (mode != null)
         {
            mode.selected();
         }

      }
   }

   protected void setOrResetMode(Mode mode)
   {
      if (mode != mMode)
      {
         setMode(mode);
      }
      else
      {
         setMode(null);
      }
   }

   protected Mode getMode()
   {
      if (mMode != null)
      {
         return mMode;
      }

      return mDefaultMode;
   }

   public Mode getDefaultMode()
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

   protected Mode createDefaultMode()
   {
      return new Mode();
   }

   protected abstract MidiOut getMidiOut();

   private List<ControlElement> mElements = new ArrayList<>();
   private Mode mDefaultMode = createDefaultMode();

   private Mode mMode;
}
