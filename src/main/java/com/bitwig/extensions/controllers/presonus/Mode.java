package com.bitwig.extensions.controllers.presonus;

import java.util.HashMap;
import java.util.Map;

public class Mode
{
   void bind(ControlElement element, Target target)
   {
      mMap.put(element, target);
   }

   public <T extends Target> T getTarget(final ControlElement element)
   {
      return (T) mMap.get(element);
   }

   Map<ControlElement, Target> mMap = new HashMap<>();
}
