package com.bitwig.extensions.oldframework.targets;

public interface TouchFaderTarget extends Target
{
   float get();

   void set(int value, int steps);

   void touch(boolean b);
}
