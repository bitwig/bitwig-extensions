package com.bitwig.extensions.controllers.nativeinstruments.maschine.modes;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.CcAssignment;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.LayoutType;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineExtension;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineLayer;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.ModifierState;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.NIColorUtil;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.RgbLedState;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.buttons.PadButton;

public class SessionMode extends PadMode {
	private final MaschineLayer selectLayer;
	private final MaschineLayer eraseLayer;
	private final MaschineLayer duplicateLayer;

	private ModifierState modstate = ModifierState.NONE;

	private final ClipLauncherSlot[] slotMappingVertical = new ClipLauncherSlot[16];
	private final ClipLauncherSlot[] slotMappingHorizontal = new ClipLauncherSlot[16];

	private ClipLauncherSlot[] currentSlotMapping;

	private LayoutType layout = LayoutType.LAUNCHER;
	private final TrackBank trackBank;

	public SessionMode(final MaschineExtension driver, final String name) {
		super(driver, name);
		selectLayer = new MaschineLayer(driver, "select-" + name);
		eraseLayer = new MaschineLayer(driver, "clear-" + name);
		duplicateLayer = new MaschineLayer(driver, "duplicate-" + name);
		trackBank = driver.getTrackBank();
		currentSlotMapping = slotMappingVertical;
		doGridBinding(driver);

		trackBank.canScrollChannelsDown().addValueObserver(newValue -> {
			if (layout == LayoutType.LAUNCHER) {
				driver.sendLedUpdate(CcAssignment.DKNOB_RIGHT, newValue ? 127 : 0);
			} else {
				driver.sendLedUpdate(CcAssignment.DKNOB_DOWN, newValue ? 127 : 0);
			}
		});
		trackBank.canScrollChannelsUp().addValueObserver(newValue -> {
			if (layout == LayoutType.LAUNCHER) {
				driver.sendLedUpdate(CcAssignment.DKNOB_LEFT, newValue ? 127 : 0);
			} else {
				driver.sendLedUpdate(CcAssignment.DKNOB_UP, newValue ? 127 : 0);
			}
		});
		trackBank.sceneBank().canScrollForwards().addValueObserver(newValue -> {
			if (layout == LayoutType.LAUNCHER) {
				driver.sendLedUpdate(CcAssignment.DKNOB_DOWN, newValue ? 127 : 0);
			} else {
				driver.sendLedUpdate(CcAssignment.DKNOB_RIGHT, newValue ? 127 : 0);
			}
		});
		trackBank.sceneBank().canScrollBackwards().addValueObserver(newValue -> {
			if (layout == LayoutType.LAUNCHER) {
				driver.sendLedUpdate(CcAssignment.DKNOB_UP, newValue ? 127 : 0);
			} else {
				driver.sendLedUpdate(CcAssignment.DKNOB_LEFT, newValue ? 127 : 0);
			}
		});
	}

	private void doGridBinding(final MaschineExtension driver) {
		trackBank.setChannelScrollStepSize(1);
		final PadButton[] buttons = driver.getPadButtons();

		for (int trackIndex = 0; trackIndex < trackBank.getSizeOfBank(); trackIndex++) {
			final Track track = trackBank.getItemAt(trackIndex);
			final ClipLauncherSlotBank slotBank = track.clipLauncherSlotBank();
			slotBank.setIndication(true);

			for (int slotIndex = 0; slotIndex < 4; slotIndex++) {
				final int buttonIndex = slotIndex * 4 + trackIndex;
				slotMappingVertical[buttonIndex] = slotBank.getItemAt(3 - slotIndex);
				slotMappingHorizontal[(3 - trackIndex) * 4 + slotIndex] = slotBank.getItemAt(slotIndex);

				final PadButton button = buttons[buttonIndex];
				bindPressed(button, () -> handleLaunch(buttonIndex));
				bindShift(button);
				selectLayer.bindPressed(button, () -> handleSelect(buttonIndex));
				eraseLayer.bindPressed(button, () -> handleErase(buttonIndex));
				duplicateLayer.bindPressed(button, () -> handleDuplicate(buttonIndex));
				bindLightState(() -> computeGridLedState(buttonIndex), button);
			}
		}
	}

	public void notifyPanelLayout(final LayoutType layout) {
		if (this.layout != layout) {
			this.layout = layout;
			if (layout == LayoutType.ARRANGER) {
				currentSlotMapping = slotMappingHorizontal;
			} else {
				currentSlotMapping = slotMappingVertical;
			}
			final PadButton[] buttons = getDriver().getPadButtons();
			for (final PadButton padButton : buttons) {
				getDriver().updatePadLed(padButton);
			}
			updateNavLed();
		}
	}

	private void updateNavLed() {
		final MaschineExtension driver = getDriver();
		if (layout == LayoutType.LAUNCHER) {
			driver.sendLedUpdate(CcAssignment.DKNOB_RIGHT, trackBank.canScrollChannelsDown().get() ? 127 : 0);
			driver.sendLedUpdate(CcAssignment.DKNOB_LEFT, trackBank.canScrollChannelsUp().get() ? 127 : 0);
			driver.sendLedUpdate(CcAssignment.DKNOB_DOWN, trackBank.sceneBank().canScrollForwards().get() ? 127 : 0);
			driver.sendLedUpdate(CcAssignment.DKNOB_UP, trackBank.sceneBank().canScrollBackwards().get() ? 127 : 0);
		} else {
			driver.sendLedUpdate(CcAssignment.DKNOB_DOWN, trackBank.canScrollChannelsDown().get() ? 127 : 0);
			driver.sendLedUpdate(CcAssignment.DKNOB_UP, trackBank.canScrollChannelsDown().get() ? 127 : 0);
			driver.sendLedUpdate(CcAssignment.DKNOB_RIGHT, trackBank.sceneBank().canScrollForwards().get() ? 127 : 0);
			driver.sendLedUpdate(CcAssignment.DKNOB_LEFT, trackBank.sceneBank().canScrollBackwards().get() ? 127 : 0);
		}

	}

	private void handleLaunch(final int buttonIndex) {
		currentSlotMapping[buttonIndex].launch();
	}

	private void handleSelect(final int buttonIndex) {
		currentSlotMapping[buttonIndex].select();
	}

	private void handleErase(final int buttonIndex) {
		currentSlotMapping[buttonIndex].deleteObject();
	}

	private void handleDuplicate(final int buttonIndex) {
		if (getDriver().isShiftDown()) {
			currentSlotMapping[buttonIndex].select();
			getDriver().getFocusClip().duplicateContent();
		} else {
			currentSlotMapping[buttonIndex].duplicateClip();
		}
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
				this.modstate = modstate;
				layer.activate();
			} else {
				this.modstate = ModifierState.NONE;
				layer.deactivate();
			}
		} else {
			this.modstate = ModifierState.NONE;
		}
	}

	@Override
	protected String getModeDescription() {
		return "Clip Launcher";
	}

	public void navLeft() {
		if (layout == LayoutType.LAUNCHER) {
			getDriver().getTrackBank().scrollBackwards();
		} else {
			getDriver().getTrackBank().sceneBank().scrollBackwards();
		}
	}

	public void navRight() {
		if (layout == LayoutType.LAUNCHER) {
			getDriver().getTrackBank().scrollForwards();
		} else {
			getDriver().getTrackBank().sceneBank().scrollForwards();
		}
	}

	public void navUp() {
		if (layout == LayoutType.LAUNCHER) {
			getDriver().getTrackBank().sceneBank().scrollBackwards();
		} else {
			getDriver().getTrackBank().scrollBackwards();
		}
	}

	public void navDown() {
		if (layout == LayoutType.LAUNCHER) {
			getDriver().getTrackBank().sceneBank().scrollForwards();
		} else {
			getDriver().getTrackBank().scrollForwards();
		}
	}

	private InternalHardwareLightState computeGridLedState(final int buttonIndex) {
		final ClipLauncherSlot slot = currentSlotMapping[buttonIndex];
		assert slot.isSubscribed();
		int color = NIColorUtil.convertColor(slot.color());
		int pulse = 0;
		int offColor = color;

		if (modstate == ModifierState.SELECT) {
			if (slot.isSelected().get()) {
				if (color == 0) {
					color = 70;
				} else {
					color += 2;
				}
			}
		} else {
			if (slot.isRecordingQueued().get()) {
				pulse = 1;
				offColor = 5;
			} else if (slot.isRecording().get()) {
				pulse = 1;
				color += 2;
				offColor = 5;
			} else if (slot.isPlaybackQueued().get()) {
				pulse = 1;
				offColor = color + 2;
			} else if (slot.isPlaying().get()) {
				color += 2;
			}
		}
		return new RgbLedState(color, offColor, pulse);
	}

	@Override
	public void doActivate() {
		super.doActivate();
		final TrackBank trackBank = getDriver().getTrackBank();
		final SceneBank sceneBank = getDriver().getTrackBank().sceneBank();
		sceneBank.setIndication(true);

		for (int i = 0; i < 4; ++i) {
			final Track track = trackBank.getItemAt(i);
			track.subscribe();

			final ClipLauncherSlotBank slotBank = track.clipLauncherSlotBank();
			slotBank.setIndication(true);

			for (int j = 0; j < 4; ++j) {
				final ClipLauncherSlot slot = slotBank.getItemAt(j);

				slot.subscribe();
				slot.color().subscribe();
				slot.isSelected().subscribe();
				slot.isPlaying().subscribe();
				slot.isPlaybackQueued().subscribe();
				slot.isRecording().subscribe();
				slot.isRecordingQueued().subscribe();
				slot.hasContent().subscribe();
			}
		}
	}

	@Override
	public void doDeactivate() {
//		mQuantizeLayer.deactivate();
//		mDeleteLayer.deactivate();
		super.doDeactivate();
		final TrackBank trackBank = getDriver().getTrackBank();
		final SceneBank sceneBank = getDriver().getTrackBank().sceneBank();
		sceneBank.setIndication(false);

		for (int i = 0; i < 4; ++i) {
			final Track channel = trackBank.getItemAt(i);
			channel.unsubscribe();

			final ClipLauncherSlotBank slotBank = channel.clipLauncherSlotBank();
			slotBank.setIndication(false);

			for (int j = 0; j < 4; ++j) {
				final ClipLauncherSlot slot = slotBank.getItemAt(j);

				slot.color().unsubscribe();
				slot.isSelected().unsubscribe();
				slot.isPlaying().unsubscribe();
				slot.isPlaybackQueued().unsubscribe();
				slot.isRecording().unsubscribe();
				slot.isRecordingQueued().unsubscribe();
				slot.hasContent().unsubscribe();
				slot.unsubscribe();
			}
		}

	}

}
