package com.bitwig.extensions.controllers.nativeinstruments.maschine.modes;

import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PlayingNote;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineExtension;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineLayer;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.ModifierState;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.buttons.PadButton;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.display.DisplayLayer;

public abstract class BasicKeyPlayingMode extends PadMode {
	protected final Integer[] deactivationTable = new Integer[128];
	protected final Integer[] noteTable = new Integer[128];

	protected final int[] noteToPad = new int[128];
	protected final boolean[] playing = new boolean[16];
	protected final NoteFocusHandler noteFocusHandler;
	protected MaschineLayer selectLayer;

	public BasicKeyPlayingMode(final MaschineExtension driver, final String name,
			final NoteFocusHandler noteFocusHandler, //
			final VeloctiyHandler velocityHandler, final DisplayLayer associatedDisplay) {
		super(driver, name);

		this.noteFocusHandler = noteFocusHandler;
		this.selectLayer = new MaschineLayer(driver, "select-" + name);
		this.associatedDisplay = associatedDisplay;

		for (int i = 0; i < 128; i++) {
			deactivationTable[i] = Integer.valueOf(-1);
			noteTable[i] = Integer.valueOf(-1);
			noteToPad[i] = -1;
		}
		for (int i = 0; i < 16; i++) {
			playing[i] = false;
		}
		driver.getCursorTrack().playingNotes().addValueObserver(notes -> {
			if (isActive()) {
				for (int i = 0; i < 16; i++) {
					playing[i] = false;
				}
				for (final PlayingNote playingNote : notes) {
					final int padIndex = noteToPad[playingNote.pitch()];
					if (padIndex != -1) {
						playing[padIndex] = true;
					}
				}
			}
		});

	}

	@Override
	public void setModifierState(final ModifierState modstate, final boolean active) {
		if (modstate == ModifierState.SELECT) {
			enableLayer(selectLayer, active);
		} else if (modstate == ModifierState.SHIFT) {
			enableLayer(getShiftLayer(), active);
		}
	}

	private void enableLayer(final MaschineLayer layer, final boolean active) {
		final NoteInput noteInput = getDriver().getNoteInput();
		if (active) {
			noteInput.setKeyTranslationTable(deactivationTable);
			layer.activate();
		} else {
			layer.deactivate();
			noteInput.setKeyTranslationTable(noteTable);
		}
	}

	abstract void applyScale();

	@Override
	public void doActivate() {
		super.doActivate();
		applyScale();
	}

	@Override
	public void doDeactivate() {
		super.doDeactivate();
		for (int i = 0; i < 16; i++) {
			noteTable[i + PadButton.PAD_NOTE_OFFSET] = -1;
		}
		getDriver().getNoteInput().setKeyTranslationTable(noteTable);
	}

}
