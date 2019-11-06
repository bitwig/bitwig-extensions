package com.bitwig.extensions.framework;

import com.bitwig.extension.controller.api.AbsoluteHardwareControl;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareControl;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.RelativeHardwareControl;

public class DebugUtilities
{
   public static Layer createDebugLayer(Layers layers, HardwareSurface hardwareSurface)
   {
      Layer layer = new Layer(layers, "Debug");

      ControllerHost host = layers.getControllerExtension().getHost();

      for (HardwareControl control : hardwareSurface.getHardwareControls())
      {
         String controlName = control.getLabel();

         if (controlName.isEmpty())
            controlName = "<no name>";

         final String prefix = controlName + ": ";

         layer.bind(control, control.beginTouchAction(), () -> host.println(prefix + "begin touch"));
         layer.bind(control, control.endTouchAction(), () -> host.println(prefix + "end touch"));

         if (control instanceof HardwareButton)
         {
            HardwareButton button = (HardwareButton)control;

            layer.bindPressed(button, () -> host.println(prefix + "pressed"));
            layer.bindReleased(button, () -> host.println(prefix + "released"));
         }
         else if (control instanceof AbsoluteHardwareControl)
         {
            AbsoluteHardwareControl absControl = (AbsoluteHardwareControl)control;

            // TODO
         }
         else if (control instanceof RelativeHardwareControl)
         {
            RelativeHardwareControl relControl = (RelativeHardwareControl)control;

            layer.bind(relControl, host.createRelativeHardwareControlAdjustmentTarget(
               amount -> host.println(prefix + " adjusted " + amount)));
         }
      }

      return layer;
   }
}
