package com.bitwig.extensions.framework;

public class LayerGroup
{
   public LayerGroup(final Layer...layers)
   {
      super();
      mLayers = layers;

      for (final Layer layer : layers)
      {
         layer.setLayerGroup(this);
      }
   }

   public Layer[] getLayers()
   {
      return mLayers;
   }

   private final Layer[] mLayers;
}
