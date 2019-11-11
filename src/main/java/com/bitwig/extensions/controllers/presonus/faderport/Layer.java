package com.bitwig.extensions.controllers.presonus.faderport;

import com.bitwig.extensions.framework.Layers;

class Layer extends com.bitwig.extensions.framework.Layer
{
   public Layer(final Layers layers, final String name)
   {
      super(layers, name);
   }

   public void bind(final Display display, final DisplayTarget displayTarget)
   {
      addBinding(new DisplayBinding(display, displayTarget));
   }
}
