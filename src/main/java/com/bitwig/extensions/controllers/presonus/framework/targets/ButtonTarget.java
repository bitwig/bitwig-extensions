package com.bitwig.extensions.controllers.presonus.framework.target;

public interface ButtonTarget extends Target
{
   boolean get();

   void set(boolean pressed);
}
