package com.bitwig.extensions.controllers.reloop;

import java.util.Arrays;
import java.util.List;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.PlayingNote;
import com.bitwig.extensions.controllers.reloop.display.Screen;
import com.bitwig.extensions.controllers.reloop.display.ScreenManager;
import com.bitwig.extensions.controllers.reloop.display.ScreenMode;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;
import com.bitwig.extensions.framework.values.PadScaleHandler;

@Component
public class DrumPadLayer extends Layer {
    private static final int PADS = 64;
    private static final int NOTE_OFFSET = 0x20;
    private static final int[] MAPPING = {
        4, 5, 6, 7, 12, 13, 14, 15, 0, 1, 2, 3, 8, 9, 10, 11
    };
    private static final int[] KEY_MAPPING = {
        8, 9, 10, 11, 12, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7
    };
    
    private final boolean[] isBaseNote = new boolean[PADS];
    private final boolean[] playing = new boolean[128];
    private final boolean[] isSelected = new boolean[PADS];
    private final int[] padColors = new int[PADS];
    private final NoteInput noteInput;
    private final DrumPadBank drumPadBank;
    private final Integer[] noteTable = new Integer[128];
    private final PinnableCursorDevice primaryDevice;
    private final List<RgbButton> buttons;
    private boolean hasPads = false;
    private int cursorTrackColor;
    private int padOffset = 36;
    private final int noteOffset = 48;
    private final PadScaleHandler scaleHandler;
    private final ScreenManager screenManager;
    private boolean encoderPressed;
    protected final int[] noteToPad = new int[128];
    protected final int[] padToNote = new int[PADS];
    protected final Integer[] deactivationTable = new Integer[128];
    
    @Inject
    GlobalStates globalStates;
    
    public DrumPadLayer(final Layers layers, final MidiProcessor midiProcessor, final BitwigControl viewControl,
        final HwElements hwElements, final ControllerHost host, final ScreenManager screenManager) {
        super(layers, "DRUM PAD Layer");
        noteInput = midiProcessor.getMidiIn().createNoteInput("Drum", "A9????", "89????", "99????");
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        primaryDevice =
            cursorTrack.createCursorDevice("DrumDetection", "Pad Device", 2, CursorDeviceFollowMode.FIRST_INSTRUMENT);
        primaryDevice.name().markInterested();
        cursorTrack.color()
            .addValueObserver(((red, green, blue) -> this.cursorTrackColor = ReloopRgb.toColorValue(red, green, blue)));
        drumPadBank = primaryDevice.createDrumPadBank(16);
        drumPadBank.setIndication(true);
        scaleHandler = new PadScaleHandler(host,
            List.of(KeypadScale.CHROMATIC, KeypadScale.MAJOR_8, KeypadScale.MINOR_8, KeypadScale.PENTATONIC,
                KeypadScale.PENTATONIC_MINOR, KeypadScale.DORIAN_8, KeypadScale.MIXOLYDIAN_8, KeypadScale.MAJOR_BLUES_8,
                KeypadScale.MINOR_BLUES), 16, true);
        scaleHandler.addStateChangedListener(this::handleScaleChange);
        this.screenManager = screenManager;
        drumPadBank.scrollPosition().set(padOffset);
        drumPadBank.scrollPosition().addValueObserver(this::handlePadBankScrolling);
        cursorTrack.playingNotes().addValueObserver(this::handleNotePlaying);
        hwElements.get(Assignment.SCENE_UP).bindPressed(this, () -> octaveScroll(1));
        hwElements.get(Assignment.SCENE_DOWN).bindPressed(this, () -> octaveScroll(-1));
        
        Arrays.fill(padColors, 0);
        Arrays.fill(deactivationTable, -1);
        Arrays.fill(noteTable, -1);
        Arrays.fill(noteToPad, -1);
        Arrays.fill(padToNote, -1);
        
        primaryDevice.hasDrumPads().addValueObserver(hasPads -> {
            this.hasPads = hasPads;
            applyScale();
        });
        
        this.buttons = hwElements.getNoteButtons();
        for (int i = 0; i < 64; i++) {
            final int index = i;
            final RgbButton button = buttons.get(i);
            final int drumPadIndex = MAPPING[i % 16];
            final DrumPad pad = drumPadBank.getItemAt(drumPadIndex);
            setUpPad(drumPadIndex, pad);
            button.bindLight(this, () -> getPadState(pad, index));
        }
    }
    
    private void updateScaleScreen() {
        final Screen screen = screenManager.getScreen(ScreenMode.SCALE);
        screen.setText(0, "Pad Scale");
        screen.setText(1, scaleHandler.getCurrentScale().getName());
        screen.setText(2, "Root Note: " + scaleHandler.getBaseNoteStr());
        screen.setText(3, "Offset: " + scaleHandler.getOctaveOffset());
        screen.notifyUpdated();
        screenManager.setMode(ScreenMode.SCALE);
    }
    
    private void updateDrumScreen() {
        final Screen screen = screenManager.getScreen(ScreenMode.DRUM);
        final int base = padOffset - 4;
        screen.setText(0, "%s".formatted(primaryDevice.name().get()));
        screen.setText(1, "Grid = %02d".formatted(base / 16 - 1));
        screen.setText(2, "Row  = %02d".formatted((base % 16) / 4 + 1));
        screen.notifyUpdated();
        screenManager.setMode(ScreenMode.DRUM);
    }
    
    private void handleScaleChange() {
        if (isActive() && !hasPads) {
            applyScale();
        }
    }
    
    public void setEncoderPressed(final boolean encoderPressed) {
        this.encoderPressed = encoderPressed;
    }
    
    private void handlePadBankScrolling(final int scrollPos) {
        padOffset = scrollPos;
        if (isActive()) {
            if (hasPads) {
                updateDrumScreen();
            }
            applyScale();
        }
    }
    
    private void handleNotePlaying(final PlayingNote[] notes) {
        if (isActive()) {
            Arrays.fill(playing, false);
            for (final PlayingNote playingNote : notes) {
                playing[playingNote.pitch()] = true;
            }
        }
    }
    
    private void octaveScroll(final int dir) {
        if (hasPads) {
            navigatePadScale(dir);
            updateDrumScreen();
        } else {
            scaleHandler.incrementNoteOffset(dir);
            updateScaleScreen();
        }
    }
    
    private void setUpPad(final int index, final DrumPad pad) {
        pad.color().addValueObserver((r, g, b) -> padColors[index] = ReloopRgb.toColorValue(r, g, b));
        pad.name().markInterested();
        pad.exists().markInterested();
        pad.solo().markInterested();
        pad.mute().markInterested();
        pad.addIsSelectedInEditorObserver(selected -> {
            isSelected[index] = selected;
        });
    }
    
    public ReloopRgb getPadState(final DrumPad pad, final int index) {
        if (hasPads) {
            final int drumPadIndex = MAPPING[index % 16];
            final int color = padColors[drumPadIndex] > 0 ? padColors[drumPadIndex] : cursorTrackColor;
            if (playing[padToNote[drumPadIndex]]) {
                return ReloopRgb.ofBright(color);
            }
            return ReloopRgb.of(color);
        } else {
            final int drumPadIndex = KEY_MAPPING[index % 16];
            if (isBaseNote[drumPadIndex]) {
                return playing[padToNote[drumPadIndex]] ? ReloopRgb.WHITE_BRIGHT : ReloopRgb.WHITE_DIM;
            } else {
                final int color = padColors[drumPadIndex] > 0 ? padColors[drumPadIndex] : cursorTrackColor;
                return playing[padToNote[drumPadIndex]] ? ReloopRgb.ofBright(color) : ReloopRgb.of(color);
            }
        }
    }
    
    public void navigatePadScale(final int dir) {
        if (hasPads) {
            final int newOffset = padOffset + dir * (encoderPressed ? 4 : 16);
            if (newOffset >= 0 && newOffset < 116) {
                navigateBy(dir * (encoderPressed ? 4 : 16));
            }
            updateDrumScreen();
        } else {
            if (encoderPressed) {
                scaleHandler.incBaseNote(dir);
            } else {
                scaleHandler.incScaleSelection(dir);
            }
            updateScaleScreen();
        }
    }
    
    public void navigateBy(final int amount) {
        drumPadBank.scrollBy(amount);
    }
    
    @Override
    protected void onActivate() {
        super.onActivate();
        refreshAll();
        applyScale();
    }
    
    public void refreshAll() {
        buttons.forEach(button -> button.restoreLastColor());
    }
    
    private void applyScale() {
        Arrays.fill(noteTable, -1);
        if (hasPads) {
            for (int i = 0; i < PADS; i++) {
                final int drumPadIndex = MAPPING[i % 16];
                noteTable[i + NOTE_OFFSET] = drumPadIndex + padOffset;
                noteToPad[drumPadIndex + padOffset] = drumPadIndex;
                padToNote[i] = padOffset + i;
            }
        } else {
            final int startNote = scaleHandler.getStartNote(); //baseNote;
            final int[] intervals = scaleHandler.getCurrentScale().getIntervals();
            for (int i = 0; i < PADS; i++) {
                final int drumPadIndex = KEY_MAPPING[i % 16];
                final int index = drumPadIndex % intervals.length;
                final int oct = drumPadIndex / intervals.length;
                int note = startNote + intervals[index] + 12 * oct;
                note = note < 0 || note > 127 ? -1 : note;
                if (note < 0 || note > 127) {
                    noteTable[i + NOTE_OFFSET] = -1;
                    isBaseNote[drumPadIndex] = false;
                    padToNote[drumPadIndex] = -1;
                } else {
                    noteTable[i + NOTE_OFFSET] = note;
                    noteToPad[note] = drumPadIndex;
                    isBaseNote[drumPadIndex] = note % 12 == scaleHandler.getBaseNote();
                    padToNote[drumPadIndex] = note;
                }
            }
        }
        if (isActive()) {
            noteInput.setKeyTranslationTable(noteTable);
            this.noteInput.setShouldConsumeEvents(false);
        }
    }
    
    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        Arrays.fill(noteTable, -1);
        Arrays.fill(playing, false);
        noteInput.setKeyTranslationTable(noteTable);
    }
}
