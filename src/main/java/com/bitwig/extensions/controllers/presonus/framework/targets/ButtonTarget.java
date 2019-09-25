package com.bitwig.extensions.controllers.presonus.framework.targets;

public interface ButtonTarget extends Target
{
   boolean get();

   void set(boolean pressed);
}
