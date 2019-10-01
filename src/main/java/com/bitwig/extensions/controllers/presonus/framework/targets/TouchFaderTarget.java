package com.bitwig.extensions.controllers.presonus.framework.targets;

public interface TouchFaderTarget extends Target
{
   float get();

   void set(int value, int steps);

   void touch(boolean b);
}
