package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers;

import com.bitwig.extension.controller.api.Application;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.DebugOutMk;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.FocusClip;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.HwElements;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.buttons.RgbButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;

import java.util.List;

@Component(priority = 0)
public class ShiftPadLayer extends Layer {

   private final Application application;
   private final FocusClip focusClip;

   public ShiftPadLayer(Layers layers, HwElements hwElements, Application application, FocusClip focusClip) {
      super(layers, "SHIFT_PAD_LAYER");
      DebugOutMk.println("Create Shift Pad Layer");
      this.application = application;
      this.focusClip = focusClip;
      List<RgbButton> gridButtons = hwElements.getPadButtons();
      for (int i = 0; i < gridButtons.size(); i++) {
         final int index = (3 - i / 4) * 4 + i % 4;
         RgbButton button = gridButtons.get(i);
         button.bindPressed(this, () -> doShiftDownAction(index));
         button.bindRelease(this, () -> doShiftUpAction(index));
      }
   }

   private void doShiftDownAction(int index) {
      switch (index) {
         case 0, 2:
            application.undo();
            break;
         case 1, 3:
            application.redo();
            break;
      }
   }

   private void doShiftUpAction(int index) {

   }

}
