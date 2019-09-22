package com.bitwig.extensions.controllers.presonus;

public interface ButtonTarget extends Target
{
   boolean isOn(boolean isPressed);

   void press();

   void release();
}
