package com.bitwig.extensions.controllers.mackie.layer;

import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extensions.controllers.mackie.NoteOnAssignment;
import com.bitwig.extensions.controllers.mackie.section.MixControl;
import com.bitwig.extensions.controllers.mackie.section.MixerSectionHardware;
import com.bitwig.extensions.controllers.mackie.section.NoteState;
import com.bitwig.extensions.controllers.mackie.section.ScaleNoteHandler;
import com.bitwig.extensions.controllers.mackie.value.ValueObject;

public class NotePlayingButtonLayer extends ButtonLayer {

	private ScaleNoteHandler scaleHandler;
	private int blinkTicks;

	public NotePlayingButtonLayer(final MixControl mixControl, final int layerOffset) {
		super("NOTEPLAYER", mixControl, NoteOnAssignment.REC_BASE, layerOffset);
	}

	public void init(final ScaleNoteHandler scaleNoteHandler, final MixerSectionHardware hwControls) {
		this.scaleHandler = scaleNoteHandler;
		for (int i = 0; i < 32; i++) {
			final HardwareButton button = hwControls.getButton(i);
			final OnOffHardwareLight light = (OnOffHardwareLight) button.backgroundLight();
			final ValueObject<NoteState> state = scaleHandler.isPlaying(i);
			this.bind(() -> getLightState(state), light);
		}
	}

	public boolean getLightState(final ValueObject<NoteState> state) {
		switch (state.get()) {
		case BASENOTE:
			if (blinkTicks % 4 == 0) {
				return false;
			}
			return true;
		case OFF:
			return false;
		case PLAYING:
			return true;
		default:
			return false;
		}
	}

	@Override
	protected void onActivate() {
		scaleHandler.activate();
	}

	@Override
	protected void onDeactivate() {
		scaleHandler.deactivate();
	}

	public void notifyBlink(final int ticks) {
		blinkTicks = ticks;
	}

	public void navigateHorizontal(final int direction, final boolean pressed) {
		if (!isActive()) {
			return;
		}
		scaleHandler.navigateHorizontal(direction, pressed);
	}

	public void navigateVertical(final int direction, final boolean pressed) {
		if (!isActive()) {
			return;
		}
		scaleHandler.navigateVertical(direction, pressed);
	}

}
