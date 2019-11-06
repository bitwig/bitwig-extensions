package com.bitwig.extensions.framework2;

/** A binding defines a connection between a source and a target. */

public abstract class Binding<SourceType, TargetType>
{
   protected Binding(final Object exclusiveSource, final SourceType source, final TargetType target)
   {
      super();
      mExclusiveSource = exclusiveSource;
      mSource = source;
      mTarget = target;
   }

   protected Binding(final SourceType source, final TargetType target)
   {
      this(source, source, target);
   }

   /**
    * Object that represents an exclusive source for binding purposes. If 2 layers use the same exclusivity
    * object for a binding then all bindings in the lower layer with that object will be inactive.
    */
   public Object getExclusivityObject()
   {
      return mExclusiveSource;
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

   private final Object mExclusiveSource;

   private final SourceType mSource;

   private final TargetType mTarget;

   private boolean mIsActive;

   private Layer mLayer;
}
