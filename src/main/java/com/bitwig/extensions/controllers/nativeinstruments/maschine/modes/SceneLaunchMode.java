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

public class SceneLaunchMode extends PadMode implements JogWheelDestination {

	private final MaschineLayer selectLayer;
	private final MaschineLayer eraseLayer;
	private final MaschineLayer duplicateLayer;
	private final SceneBank sceneBank;

	private final boolean[] isSelected = new boolean[16];

	public SceneLaunchMode(final MaschineExtension driver, final String name) {
		super(driver, name, true);
		selectLayer = new MaschineLayer(driver, "select-" + name);
		eraseLayer = new MaschineLayer(driver, "clear-" + name);
		duplicateLayer = new MaschineLayer(driver, "duplicate-" + name);

		sceneBank = driver.getHost().createSceneBank(16);
		sceneBank.canScrollBackwards().markInterested();
		sceneBank.canScrollForwards().markInterested();
		sceneBank.itemCount().markInterested();

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
			bindPressed(button, scene.launchAction());
			bindShift(button);
			selectLayer.bindPressed(button, () -> handleSelect(scene, index));
			eraseLayer.bindPressed(button, () -> handleErase(scene));
			duplicateLayer.bindPressed(button, () -> handleDuplicate(scene));
			bindLightState(() -> computeGridLedState(scene, index), button);
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

	private void handleSelect(final Scene scene, final int index) {
		if (scene.exists().get()) {
			scene.selectInEditor();
		}
	}

	private InternalHardwareLightState computeGridLedState(final Scene scene, final int index) {
		assert scene.isSubscribed();
		final int color = NIColorUtil.convertColor(scene.color()) + (isSelected[index] ? 2 : 0);

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
		sceneBank.setIndication(true);

		for (int i = 0; i < 16; ++i) {
			final Scene scene = sceneBank.getItemAt(i);
			scene.subscribe();
			scene.setIndication(true);
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
			scene.setIndication(false);
			scene.unsubscribe();
			scene.color().unsubscribe();
			scene.exists().unsubscribe();
			scene.sceneIndex().unsubscribe();
		}
	}

	@Override
	public void jogWheelAction(final int increment) {
		if (increment > 0) {
			sceneBank.scrollForwards();
		} else {
			sceneBank.scrollBackwards();
		}
	}

	@Override
	public void jogWheelPush(final boolean push) {
	}

}
