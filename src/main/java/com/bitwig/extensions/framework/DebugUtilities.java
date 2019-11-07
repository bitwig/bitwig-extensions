package com.bitwig.extensions.framework;

import com.bitwig.extension.controller.api.AbsoluteHardwareControl;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareControl;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.RelativeHardwareControl;

public class DebugUtilities
{
   public static Layer createDebugLayer(final Layers layers, final HardwareSurface hardwareSurface)
   {
      final Layer layer = new Layer(layers, "Debug");

      final ControllerHost host = layers.getControllerExtension().getHost();

      for (final HardwareControl control : hardwareSurface.getHardwareControls())
      {
         String controlName = control.getLabel();

         if (controlName.isEmpty())
            controlName = "<no name>";

         final String prefix = controlName + ": ";

         layer.bind(control, control.beginTouchAction(), () -> host.println(prefix + "begin touch"));
         layer.bind(control, control.endTouchAction(), () -> host.println(prefix + "end touch"));

         if (control instanceof HardwareButton)
         {
            final HardwareButton button = (HardwareButton)control;

            layer.bindPressed(button, () -> host.println(prefix + "pressed"));
            layer.bindReleased(button, () -> host.println(prefix + "released"));
         }
         else if (control instanceof AbsoluteHardwareControl)
         {
            final AbsoluteHardwareControl absControl = (AbsoluteHardwareControl)control;

            layer.bind(absControl, value -> host.println(prefix + " value " + value));
         }
         else if (control instanceof RelativeHardwareControl)
         {
            final RelativeHardwareControl relControl = (RelativeHardwareControl)control;

            layer.bind(relControl, amount -> host.println(prefix + " adjusted " + amount));
         }
      }

      return layer;
   }
}
