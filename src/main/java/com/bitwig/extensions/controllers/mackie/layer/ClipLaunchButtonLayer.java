package com.bitwig.extensions.controllers.mackie.layer;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;
import com.bitwig.extensions.controllers.mackie.value.ModifierValueObject;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.remoteconsole.RemoteConsole;

public class ClipLaunchButtonLayer extends Layer {

	private TrackBank trackBank;
	private final MackieMcuProExtension driver;
	private final Clip mainCursorClip;
	private int blinkTicks;

	public ClipLaunchButtonLayer(final String name, final MixControl mixControl) {
		super(mixControl.getDriver().getLayers(),
				name + "_" + mixControl.getHwControls().getSectionIndex() + "_ClipLaunch");
		driver = mixControl.getDriver();
		mainCursorClip = driver.getHost().createLauncherCursorClip(16, 1);
	}

	public void initTrackBank(final MixerSectionHardware hwControls, final TrackBank trackBank) {
		trackBank.setChannelScrollStepSize(1);
		this.trackBank = trackBank;
		final int offset = hwControls.getSectionIndex() * 8;

		for (int trackIndex = offset; trackIndex < trackBank.getSizeOfBank(); trackIndex++) {
			final Track track = trackBank.getItemAt(trackIndex);
			final ClipLauncherSlotBank slotBank = track.clipLauncherSlotBank();
			slotBank.setIndication(true);

			for (int slotIndex = 0; slotIndex < 4; slotIndex++) {
				final HardwareButton button = hwControls.getButton(slotIndex, trackIndex);
				final OnOffHardwareLight light = (OnOffHardwareLight) button.backgroundLight();
				final ClipLauncherSlot slot = slotBank.getItemAt(slotIndex);
				slot.hasContent().markInterested();
				slot.isPlaying().markInterested();
				slot.isPlaybackQueued().markInterested();
				slot.isRecording().markInterested();
				slot.isRecordingQueued().markInterested();
				bindPressed(button, () -> handleSlotPressed(track, slot));
				bind(() -> lightState(slot), light);
			}
		}
	}

	public void handleSlotPressed(final Track track, final ClipLauncherSlot slot) {
		final ModifierValueObject modifier = driver.getModifier();
		if (modifier.isAlt()) {
			track.stop();
		} else if (modifier.isSet(ModifierValueObject.SHIFT, ModifierValueObject.OPTION)) {
			slot.deleteObject();
		} else if (modifier.isSet(ModifierValueObject.SHIFT, ModifierValueObject.ALT)) {
			slot.select();
			mainCursorClip.duplicateContent();
		} else if (modifier.isOption()) {
			slot.duplicateClip();
		} else {
			slot.launch();
		}
	}

	public void notifyBlink(final int ticks) {
		blinkTicks = ticks;
	}

	public boolean lightState(final ClipLauncherSlot slot) {
		if (slot.isPlaybackQueued().get() || slot.isRecordingQueued().get()) {
			if (blinkTicks % 2 == 0) {
				return false;
			} else {
				return true;
			}
		} else if (slot.isRecording().get()) {
			if (blinkTicks % 4 == 0) {
				return false;
			} else {
				return true;
			}
		} else if (slot.isPlaying().get()) {
			if (blinkTicks % 8 < 3) {
				return false;
			} else {
				return true;
			}
		} else if (slot.hasContent().get()) {
			return true;
		}
		return false;
	}

	public void navigateHorizontal(final int direction) {
		if (direction > 0) {
			trackBank.scrollForwards();
		} else {
			trackBank.scrollBackwards();
		}
	}

	public void navigateVertical(final int direction) {
		RemoteConsole.out.println(" ND {}", direction);
		if (direction > 0) {
			trackBank.sceneBank().scrollBackwards();
		} else {
			trackBank.sceneBank().scrollForwards();
		}
	}

}
