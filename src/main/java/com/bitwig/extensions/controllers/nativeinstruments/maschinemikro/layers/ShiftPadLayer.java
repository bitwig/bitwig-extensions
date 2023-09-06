package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers;

import com.bitwig.extension.controller.api.Application;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.FocusClip;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.HwElements;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.buttons.RgbButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;

import java.util.List;

@Component(priority = 0)
public class ShiftPadLayer extends Layer {

   private final Application application;
   @Inject
   private FocusClip focusClip;
   @Inject
   private ModifierLayer modifierLayer;
   @Inject
   private SessionLayer sessionLayer;

   public ShiftPadLayer(Layers layers, HwElements hwElements, Application application) {
      super(layers, "SHIFT_PAD_LAYER");
      this.application = application;

      List<RgbButton> gridButtons = hwElements.getPadButtons();
      for (int i = 0; i < gridButtons.size(); i++) {
         final int index = (3 - i / 4) * 4 + i % 4;
         RgbButton button = gridButtons.get(i);
         button.bindPressed(this, () -> doShiftDownAction(index));
         button.bindRelease(this, () -> doShiftUpAction(index));
      }
   }

   private void doShiftDownAction(int index) {
      if (modifierLayer.getDuplicateHeld().get()) {
         sessionLayer.invokeDuplicate(index);
      } else {
         switch (index) {
            case 0, 2:
               application.undo();
               break;
            case 1, 3:
               application.redo();
               break;
            case 4:
               focusClip.quantize(1.0);
               break;
            case 5:
               focusClip.quantize(0.5);
               break;
            case 8:
               focusClip.clearSteps();
               break;
            case 10:
               application.copy();
               break;
            case 11:
               application.paste();
               break;
            case 12:
               focusClip.transpose(-1);
               break;
            case 13:
               focusClip.transpose(1);
               break;
            case 14:
               focusClip.transpose(-12);
               break;
            case 15:
               focusClip.transpose(12);
               break;
         }
      }
   }

   private void doShiftUpAction(int index) {

   }

}
