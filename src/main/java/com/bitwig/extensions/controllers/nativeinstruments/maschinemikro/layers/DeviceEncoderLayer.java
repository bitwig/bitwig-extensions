package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.DebugOutMk;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.HwElements;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.RgbColor;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.ViewControl;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.buttons.RgbButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.BooleanValueObject;

import java.util.List;

@Component
public class DeviceEncoderLayer extends Layer {

   private final BooleanValueObject shiftHeld;

   public DeviceEncoderLayer(Layers layers, HwElements hwElements, ViewControl viewControl, ModifierLayer modifierLayer,
                             Transport transport, ControllerHost host) {
      super(layers, "ENCODER_LAYER");
      this.shiftHeld = modifierLayer.getShiftHeld();
      CursorTrack cursorTrack = viewControl.getCursorTrack();
      hwElements.bindEncoder(this, hwElements.getMainEncoder(), dir -> handleEncoder(dir));
      List<RgbButton> gridButtons = hwElements.getPadButtons();
      for (int i = 0; i < 16; i++) {
         final int index = i;
         RgbButton button = gridButtons.get(i);
         button.bindLight(this, () -> RgbColor.of(index));
      }
   }

   private void handleEncoder(int diff) {
      DebugOutMk.println(" TURN %d", diff);
   }
}
