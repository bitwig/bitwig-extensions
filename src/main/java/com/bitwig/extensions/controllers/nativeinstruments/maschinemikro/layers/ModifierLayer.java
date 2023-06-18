package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers;

import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.CcAssignment;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.HwElements;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.BooleanValueObject;

@Component
public class ModifierLayer {

   private final BooleanValueObject shiftHeld = new BooleanValueObject();
   private final BooleanValueObject eraseHeld = new BooleanValueObject();
   private final BooleanValueObject duplicateHeld = new BooleanValueObject();
   private final BooleanValueObject variationHeld = new BooleanValueObject();

   public void init(Layer baseLayer, HwElements hwElements) {
      hwElements.getButton(CcAssignment.VARIATION).bind(baseLayer, variationHeld);
      hwElements.getButton(CcAssignment.VARIATION).bindLightHeld(baseLayer);
      hwElements.getButton(CcAssignment.ERASE).bind(baseLayer, eraseHeld);
      hwElements.getButton(CcAssignment.ERASE).bindLightHeld(baseLayer);
      hwElements.getButton(CcAssignment.DUPLICATE).bind(baseLayer, duplicateHeld);
      hwElements.getButton(CcAssignment.DUPLICATE).bindLightHeld(baseLayer);
      hwElements.getButton(CcAssignment.MASCHINE).bind(baseLayer, shiftHeld);
      hwElements.getButton(CcAssignment.MASCHINE).bindLightHeld(baseLayer);
   }

   public BooleanValueObject getDuplicateHeld() {
      return duplicateHeld;
   }

   public BooleanValueObject getEraseHeld() {
      return eraseHeld;
   }

   public BooleanValueObject getShiftHeld() {
      return shiftHeld;
   }

   public BooleanValueObject getVariationHeld() {
      return variationHeld;
   }
}
