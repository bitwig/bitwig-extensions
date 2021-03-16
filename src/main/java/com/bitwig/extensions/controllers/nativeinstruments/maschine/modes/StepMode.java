package com.bitwig.extensions.controllers.nativeinstruments.maschine.modes;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.NoteStep.State;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.ColorBrightness;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.Colors;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineExtension;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.NIColorUtil;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.RgbLed;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.RgbLedState;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.buttons.PadButton;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.display.DisplayUtil;

public class StepMode extends PadMode implements NoteFocusHandler {

	private final Clip clip;
	private int playingStep;

	private int focusNote = 60;
	private DrumPad focusPad;

	private int refVelocity = 100;

	private Consumer<String> focusChangerListener;

	private final NoteStep[] assignments = new NoteStep[16];
	private final PressState[] pressStates = new PressState[16];

	private int padColor;
	private int clipColor;
	private DrumPadMode drumLayer;
	private KeyboardMode keyLayer;

	private final StepViewPosition positionHandler;

	private boolean isDrumEdit = false;

	private int selectPadIndex = 0;
	private String currentTrackName = "";
	private final Map<String, Integer> lastNotes = new HashMap<String, Integer>();

	private enum PressState {
		None, New, Modify, Delete;
	}

	public StepMode(final MaschineExtension driver, final String name) {
		super(driver, name);
		this.clip = driver.getHost().createLauncherCursorClip(16, 1);
		this.positionHandler = new StepViewPosition(this.clip);

		this.clip.color().addValueObserver(this::handleClipColorChanged);

		this.getDriver().getPrimaryDevice().hasDrumPads().addValueObserver(hasDrumPads -> {
			isDrumEdit = hasDrumPads;
		});

		for (int i = 0; i < 16; i++) {
			pressStates[i] = PressState.None;
		}

		this.getDriver().getCursorTrack().name().addValueObserver(tname -> {
			currentTrackName = tname;
		});

		final PadButton[] buttons = driver.getPadButtons();
		for (int i = 0; i < buttons.length; i++) {
			final int index = i;
			final PadButton button = buttons[i];
			bindPressed(button, () -> handleSelection(index));
			bindReleased(button, () -> handleDeSelection(index));
			bindShift(button);
			bindLightState(() -> computeGridLedState(index), button);
		}

		clip.addNoteStepObserver(this::handleNoteStep);
		clip.playingStep().addValueObserver(this::handlePlayingStep);

		this.clip.scrollToKey(focusNote);
	}

	public Clip getClip() {
		return clip;
	}

	public StepViewPosition getPositionHandler() {
		return positionHandler;
	}

	public void setFocusChangerListener(final Consumer<String> focusChangerListener) {
		this.focusChangerListener = focusChangerListener;
	}

	public int getRefVelocity() {
		return refVelocity;
	}

	public void setRefVelocity(final int refVelocity) {
		this.refVelocity = refVelocity;
	}

	@Override
	public void notifyPadColorChanged(final DrumPad pad, final int index, final float r, final float g, final float b) {
		updatePadColor();
	}

	private void handleSelection(final int index) {
		final NoteStep note = assignments[index];
		if (note == null || note.state() == State.Empty) {
			clip.setStep(index, 0, refVelocity, positionHandler.getGridResolution());
			pressStates[index] = PressState.New;
		} else {
			pressStates[index] = PressState.Delete;
		}
	}

	private void handleDeSelection(final int index) {
		if (!isActive()) {
			return;
		}
		if (pressStates[index] == PressState.Delete) {
			clip.toggleStep(index, 0, refVelocity);
		}
	}

	private void handlePlayingStep(final int playingStep) {
		if (playingStep == -1) {
			this.playingStep = -1;
		}
		this.playingStep = playingStep - positionHandler.getStepOffset();
	}

	private InternalHardwareLightState computeGridLedState(final int index) {
		if (assignments[index] == null || assignments[index].state() == State.Empty) {
			if (index == playingStep) {
				return RgbLedState.WHITE_BRIGHT;
			}
			return RgbLedState.OFF;
		} else if (isDrumEdit) {
			if (index == playingStep) {
				return RgbLed.colorOf(padColor + 2);
			} else if (assignments[index].state() == State.NoteSustain) {
				return RgbLed.colorOf(padColor);
			}
			return RgbLed.colorOf(padColor + 1);
		} else {
			if (index == playingStep) {
				return RgbLed.colorOf(clipColor + 2);
			} else if (assignments[index].state() == State.NoteSustain) {
				return RgbLed.colorOf(clipColor);
			}
			return RgbLed.colorOf(clipColor + 1);
		}
	}

	private void handleClipColorChanged(final float red, final float green, final float blue) {
		updatePadColor();
	}

	@Override
	public void notifyDrumPadSelected(final DrumPad pad, final int padOffset, final int padIndex) {
		this.focusNote = padOffset + padIndex;
		this.focusPad = pad;
		this.selectPadIndex = padIndex;

		updatePadColor();

		this.clip.scrollToKey(this.focusNote);
		if (this.focusChangerListener != null) {
			this.focusChangerListener.accept(getFocus());
		}
	}

	private void initPadColor() {
		if (focusPad == null && !isDrumEdit) {
			return;
		} else if (focusPad == null && isDrumEdit) {
			drumLayer.selectPad(0);
			return;
		}
		updatePadColor();
	}

	private void updatePadColor() {
		if (focusPad == null) {
			return;
		}
		if (NIColorUtil.isOff(focusPad.color())) {
			padColor = Colors.WHITE.getIndexValue(ColorBrightness.DARKENED);
		} else {
			padColor = NIColorUtil.convertColor(focusPad.color());
		}
	}

	@Override
	public void notifyNoteSelected(final int note) {
		this.focusNote = note;
		this.focusPad = null;
		this.clip.scrollToKey(focusNote);
		lastNotes.put(currentTrackName, focusNote);
		if (this.focusChangerListener != null) {
			this.focusChangerListener.accept(getFocus());
		}
	}

	public int getFocusNote() {
		return focusNote;
	}

	public DrumPad getFocusPad() {
		return focusPad;
	}

	public String getFocus() {
		if (focusPad != null && isDrumEdit) {
			return focusPad.name().get() + " " + DisplayUtil.toNote(focusNote);
		} else if (focusNote != -1) {
			return DisplayUtil.toNote(focusNote);
		}
		return "ALL";
	}

	private void handleNoteStep(final NoteStep noteStep) {
		// Which grid step contains Notes
		assignments[noteStep.x()] = noteStep;
	}

	@Override
	protected String getModeDescription() {
		return "Step Mode";
	}

	@Override
	public boolean hasMomentarySelectMode() {
		return true;
	}

	@Override
	public PadMode getMomentarySwitchMode() {
		return isDrumEdit ? this.drumLayer : this.keyLayer;
	}

	public void findeActiveNote() {

	}

	@Override
	protected void onActivate() {
		super.onActivate();
		initPadColor();
		clip.getTrack().selectInEditor();
		clip.getTrack().isActivated().set(true);
	}

	@Override
	protected void onDeactivate() {
		super.onDeactivate();
		for (int i = 0; i < 16; i++) {
			pressStates[i] = PressState.None;
		}
	}

	public void setSelectLayers(final DrumPadMode padMode, final KeyboardMode keyboardMode) {
		this.drumLayer = padMode;
		this.keyLayer = keyboardMode;
	}

	public void changeFocusNote(final int amount) {
		if (isDrumEdit) {
			final int newValue = selectPadIndex + amount;
			if (newValue >= 0 && newValue < 16) {
				drumLayer.selectPad(newValue);
			}
		} else {
			final int newValue = keyLayer.getNextNote(focusNote, amount);
			// final int newValue = focusNote + amount;
			if (newValue >= 0 && newValue < 128) {
				focusNote = newValue;
				this.clip.scrollToKey(focusNote);
				if (this.focusChangerListener != null) {
					this.focusChangerListener.accept(getFocus());
				}
			}
		}
	}
}
