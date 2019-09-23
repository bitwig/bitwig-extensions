package com.bitwig.extensions.controllers.presonus;

public interface ButtonTarget extends Target
{
   boolean get();

   void set(boolean pressed);
}
