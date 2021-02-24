package com.bitwig.extensions.controllers.nativeinstruments.maschine.modes;

import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineExtension;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineLayer;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.ModifierState;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.NIColorUtil;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.RgbLedState;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.buttons.PadButton;

public class SceneLaunchMode extends PadMode {

	private final MaschineLayer selectLayer;
	private final MaschineLayer eraseLayer;
	private final MaschineLayer duplicateLayer;
	private final SceneBank sceneBank;

	public SceneLaunchMode(final MaschineExtension driver, final String name) {
		super(driver, name);
		selectLayer = new MaschineLayer(driver, "select-" + name);
		eraseLayer = new MaschineLayer(driver, "clear-" + name);
		duplicateLayer = new MaschineLayer(driver, "duplicate-" + name);

		sceneBank = driver.getHost().createSceneBank(16);
		sceneBank.canScrollBackwards().markInterested();
		sceneBank.canScrollForwards().markInterested();
		sceneBank.itemCount().markInterested();
		final PadButton[] buttons = driver.getPadButtons();
		for (int i = 0; i < 16; ++i) {
			final PadButton button = buttons[i];
			final Scene scene = sceneBank.getItemAt(i);
			scene.subscribe();
			scene.color().markInterested();
			scene.exists().markInterested();
			bindPressed(button, scene.launchAction());
			bindShift(button);
			selectLayer.bindPressed(button, () -> handleSelect(scene));
			eraseLayer.bindPressed(button, () -> handleErase(scene));
			duplicateLayer.bindPressed(button, () -> handleDuplicate(scene));
			bindLightState(() -> computeGridLedState(scene), button);
		}

	}

	private void handleDuplicate(final Scene scene) {
//		scene.selectInEditor();
//		scene.showInEditor();
//		getDriver().getApplication().duplicate();
	}

	private void handleErase(final Scene scene) {
		scene.deleteObject();
	}

	private void handleSelect(final Scene scene) {
		scene.selectInEditor();
	}

	private InternalHardwareLightState computeGridLedState(final Scene scene) {
		assert scene.isSubscribed();
		final int color = NIColorUtil.convertColor(scene.color());

		return new RgbLedState(color, color, 0);
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
		for (int i = 0; i < 16; ++i) {
			final Scene scene = sceneBank.getItemAt(i);
			scene.unsubscribe();
			scene.color().unsubscribe();
			scene.exists().unsubscribe();
			scene.sceneIndex().unsubscribe();
		}

	}

}
