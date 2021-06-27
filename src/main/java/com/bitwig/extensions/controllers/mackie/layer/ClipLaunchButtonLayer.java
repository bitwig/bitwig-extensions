package com.bitwig.extensions.controllers.mackie.layer;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.framework.Layer;

public class ClipLaunchButtonLayer extends Layer {

	private int blinkTicks = 0;

	public ClipLaunchButtonLayer(final String name, final MixControl mixControl) {
		super(mixControl.getDriver().getLayers(),
				name + "_" + mixControl.getHwControls().getSectionIndex() + "_ClipLaunch");
	}

	public void initTrackBank(final MixerSectionHardware hwControls, final TrackBank trackBank) {
		trackBank.setChannelScrollStepSize(1);
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
				bindPressed(button, () -> slot.launch());
				bind(() -> lightState(slot), light);
			}
		}
	}

	public void notifyBlink() {
		blinkTicks++;
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

}
