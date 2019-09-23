package com.bitwig.extensions.controllers.presonus.framework;

public interface MotorFaderTarget extends Target
{
   float get();

   void set(int value, int steps);

   void touch(boolean b);
}
