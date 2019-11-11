package com.bitwig.extensions.framework;

import java.util.function.BooleanSupplier;

public class BooleanObject implements BooleanSupplier
{
   public BooleanObject(final boolean value)
   {
      super();

      mValue = value;
   }

   public BooleanObject()
   {
      this(false);
   }

   @Override
   public boolean getAsBoolean()
   {
      return mValue;
   }

   public void setValue(final boolean value)
   {
      mValue = value;
   }

   public void toggle()
   {
      mValue = !mValue;
   }

   private boolean mValue;
}
