package com.bitwig.extensions.controllers.presonus;

public abstract class ControlElement<T extends Target> implements MidiReceiver
{
   protected T getEffectiveTarget(Mode mode)
   {
      if (mode != null)
      {
         return mode.getTarget(this);
      }

      return null;
   }
}
