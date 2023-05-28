package com.bitwig.extensions.controllers.nativeinstruments.maschine.modes;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineExtension;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineLayer;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.buttons.PadButton;
import com.bitwig.extensions.framework.values.BooleanValueObject;

public abstract class VuMode extends MaschineLayer {

   private final MaschineLayer shiftLayer;
   private final BooleanValueObject active = new BooleanValueObject();
   final MaschineExtension driver;

   public VuMode(final MaschineExtension driver, final String name) {
      super(driver, name);
      this.driver = driver;
      shiftLayer = new MaschineLayer(driver, "shift-" + name);
   }

   protected abstract String getModeDescription();

   void bindShift(final PadButton button) {
      shiftLayer.bindPressed(button, () -> driver.handleShiftAction(button.getIndex()));
   }

   public void activateShiftMode() {
      shiftLayer.activate();
   }

   public void deactivateShiftMode() {
      shiftLayer.deactivate();
   }

   @Override
   final protected void onActivate() {
      doActivate();
      active.set(true);
//
//		final String modeDescription = getModeDescription();
//		if (modeDescription != null)
//			mDriver.getHost().showPopupNotification(modeDescription);
   }

   protected void doActivate() {
      /* for subclasses */
   }

   @Override
   final protected void onDeactivate() {
      doDeactivate();
      active.set(false);
   }

   protected void doDeactivate() {
      shiftLayer.deactivate();
   }

   public BooleanValue getActive() {
      return active;
   }

}
