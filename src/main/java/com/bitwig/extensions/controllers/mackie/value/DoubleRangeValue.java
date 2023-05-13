package com.bitwig.extensions.controllers.mackie.value;

import java.util.function.DoubleConsumer;

public interface DoubleRangeValue {
   double getMin();

   double getMax();

   void addDoubleValueObserver(DoubleConsumer callback);

   double getRawValue();

   String displayedValue();

   int scale(double v, int range);
}
