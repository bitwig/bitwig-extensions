package com.bitwig.extensions.controllers.nativeinstruments.maschine.modes;

import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.Project;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.*;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.buttons.PadButton;
import com.bitwig.extensions.framework.values.BooleanValueObject;

public class SceneLaunchMode extends PadMode implements JogWheelDestination {

   private final MaschineLayer selectLayer;
   private final MaschineLayer eraseLayer;
   private final MaschineLayer duplicateLayer;
   private final MaschineLayer colorChooseLayer;

   private final SceneBank sceneBank;

   private final boolean[] isSelected = new boolean[16];

   public SceneLaunchMode(final MaschineExtension driver, final String name) {
      super(driver, name, true);
      selectLayer = new MaschineLayer(driver, "select-" + name);
      eraseLayer = new MaschineLayer(driver, "clear-" + name);
      duplicateLayer = new MaschineLayer(driver, "duplicate-" + name);
      colorChooseLayer = new MaschineLayer(driver, "variation-" + name);

      sceneBank = driver.getHost().createSceneBank(16);
      sceneBank.canScrollBackwards().markInterested();
      sceneBank.canScrollForwards().markInterested();
      sceneBank.itemCount().markInterested();
      sceneBank.setSizeOfBank(16);
      Project project = driver.getProject();
      BooleanValueObject launchAlt = driver.getLaunchModifierSet();

      final PadButton[] buttons = driver.getPadButtons();
      for (int i = 0; i < 16; ++i) {
         final int index = i;
         final PadButton button = buttons[i];
         final Scene scene = sceneBank.getItemAt(i);
         scene.subscribe();
         scene.color().markInterested();
         scene.exists().markInterested();

         scene.addIsSelectedInEditorObserver(selected -> {
            isSelected[index] = selected;
         });
         bindPressed(button, () -> handleSceneLaunch(project, scene, launchAlt));
         bindReleased(button, () -> handleSceneRelease(scene, launchAlt));
         bindShift(button);
         selectLayer.bindPressed(button, () -> handleSelect(scene, index));
         eraseLayer.bindPressed(button, () -> handleErase(scene));
         duplicateLayer.bindPressed(button, () -> handleDuplicate(scene));
         colorChooseLayer.bindPressed(button, () -> handleColorSelection(scene));
         bindLightState(() -> computeGridLedState(scene, index), button);
      }

   }

   private void handleSceneLaunch(Project project, Scene scene, BooleanValueObject launchAlt) {
      if (scene.exists().get()) {
         if (launchAlt.get()) {
            scene.launchAlt();
         } else {
            scene.launch();
         }
      } else {
         project.createScene();
      }
   }

   private void handleSceneRelease(Scene scene, BooleanValueObject launchAlt) {
      if (scene.exists().get()) {
         if (launchAlt.get()) {
            scene.launchReleaseAlt();
         } else {
            scene.launchRelease();
         }
      }
   }

   private void handleDuplicate(final Scene scene) {
//		scene.selectInEditor();
//		scene.showInEditor();
//		getDriver().getApplication().duplicate();
   }

   private void handleColorSelection(final Scene scene) {
      if (scene.exists().get()) {
         getDriver().enterColorSelection(color -> {
            color.set(scene.color());
         });
      }
   }

   private void handleErase(final Scene scene) {
      scene.deleteObject();
   }

   private void handleSelect(final Scene scene, final int index) {
      if (scene.exists().get()) {
         scene.selectInEditor();
      }
   }

   private InternalHardwareLightState computeGridLedState(final Scene scene, final int index) {
      assert scene.isSubscribed();
      final int color = NIColorUtil.convertColor(scene.color()) + (isSelected[index] ? 2 : 0);

      return RgbLedState.colorOf(color);
   }

   @Override
   protected String getModeDescription() {
      return "Scene Launch Mode";
   }

   private MaschineLayer getLayer(final ModifierState modstate) {
      switch (modstate) {
         case SHIFT:
            return getShiftLayer();
         case DUPLICATE:
            return duplicateLayer;
         case SELECT:
            return selectLayer;
         case ERASE:
            return eraseLayer;
         case VARIATION:
            return colorChooseLayer;
         default:
            return null;
      }

   }

   @Override
   public void setModifierState(final ModifierState modstate, final boolean active) {
      final MaschineLayer layer = getLayer(modstate);
      if (layer != null) {
         if (active) {
            layer.activate();
         } else {
            layer.deactivate();
         }
      }
   }

   @Override
   public void doActivate() {
      super.doActivate();
      sceneBank.setIndication(true);

      for (int i = 0; i < 16; ++i) {
         final Scene scene = sceneBank.getItemAt(i);
         scene.subscribe();
         scene.color().subscribe();
         scene.exists().subscribe();
         scene.sceneIndex().subscribe();
      }
   }

   @Override
   public void doDeactivate() {
      super.doDeactivate();
      selectLayer.deactivate();
      duplicateLayer.deactivate();
      eraseLayer.deactivate();
      sceneBank.setIndication(false);
      for (int i = 0; i < 16; ++i) {
         final Scene scene = sceneBank.getItemAt(i);
         scene.unsubscribe();
         scene.color().unsubscribe();
         scene.exists().unsubscribe();
         scene.sceneIndex().unsubscribe();
      }
   }

   @Override
   public void jogWheelAction(final int increment) {
      if (increment > 0) {
         sceneBank.scrollBy(4);
      } else {
         sceneBank.scrollBy(-4);
      }
   }

   @Override
   public void jogWheelPush(final boolean push) {
   }

}
