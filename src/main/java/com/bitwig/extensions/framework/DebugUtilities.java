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
      layer.setShouldReplaceBindingsInLayersBelow(false);

      addDebugBindings(layer, hardwareSurface);

      return layer;
   }

   public static void addDebugBindings(final Layer layer, final HardwareSurface hardwareSurface)
   {
      final ControllerHost host = layer.getLayers().getControllerExtension().getHost();

      for (final HardwareControl control : hardwareSurface.hardwareControls())
      {
         String controlName = control.getId();

         if (!control.getLabel().isEmpty())
            controlName += " " + control.getLabel();

         final String prefix = controlName + ": ";

         layer.bind(control, control.beginTouchAction(), () -> host.println(prefix + "begin touch"));
         layer.bind(control, control.endTouchAction(), () -> host.println(prefix + "end touch"));

         if (control instanceof HardwareButton)
         {
            final HardwareButton button = (HardwareButton)control;

            layer.bindPressed(button, pressure -> host.println(prefix + "pressed with pressure " + pressure));
            layer.bindReleased(button,
               pressure -> host.println(prefix + "released with pressure " + pressure));
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
   }
}
