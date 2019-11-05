package com.bitwig.extension.controllers.studiologic;

/** A binding defines a connection between a source and a target. */

public abstract class Binding<SourceType, TargetType>
{
   protected Binding(final SourceType source, final TargetType target)
   {
      super();
      mSource = source;
      mTarget = target;
   }

   public SourceType getSource()
   {
      return mSource;
   }

   public TargetType getTarget()
   {
      return mTarget;
   }

   public boolean isActive()
   {
      return mIsActive;
   }

   void setIsActive(final boolean value)
   {
      if (value != mIsActive)
      {
         if (mIsActive)
            deactivate();

         mIsActive = value;

         if (mIsActive)
            activate();
      }
   }

   public Layer getLayer()
   {
      return mLayer;
   }

   void setLayer(final Layer layer)
   {
      assert mLayer == null;
      assert layer != null;

      mLayer = layer;
   }

   protected abstract void deactivate();

   protected abstract void activate();

   private final SourceType mSource;

   private final TargetType mTarget;

   private boolean mIsActive;

   private Layer mLayer;
}
