package com.bitwig.extensions.controllers.novation.launchpadpromk3.layers;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.novation.commonsmk3.*;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.DebugOutLp;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.HwElements;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.LabelCcAssignments;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.ViewCursorControl;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Inject;
import com.bitwig.extensions.framework.di.PostConstruct;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DrumLayer extends Layer {

   private DrumPadBank drumPadBank;
   private NoteInput noteInput;
   private final int[] padColors = new int[64];
   private final Integer[] noteTable = new Integer[128];
   private final boolean[] isPlaying = new boolean[128];
   private final Set<Integer> padNotes = new HashSet<>();
   @Inject
   private ViewCursorControl viewCursorControl;
   @Inject
   private TrackState trackState;
   private int padsNoteOffset;

   public DrumLayer(final Layers layers) {
      super(layers, "DRUM_PAD_LAYER");
   }

   @PostConstruct
   public void init(final ControllerHost host, final MidiProcessor midiProcessor, final HwElements hwElements) {
      DebugOutLp.println("Drum Pad Bank");

      noteInput = midiProcessor.getMidiIn().createNoteInput("MIDI", "88????", "98????");
      noteInput.setShouldConsumeEvents(false);
      final CursorTrack cursorTrack = viewCursorControl.getCursorTrack();
      cursorTrack.playingNotes().addValueObserver(this::handleNotes);

      final PinnableCursorDevice primaryDevice = viewCursorControl.getPrimaryDevice();
      final DeviceBank drumBank = cursorTrack.createDeviceBank(1);
      final DeviceMatcher drumMatcher = host.createBitwigDeviceMatcher(SpecialDevices.DRUM.getUuid());
      drumBank.setDeviceMatcher(drumMatcher);
      drumPadBank = primaryDevice.createDrumPadBank(64);
      drumPadBank.scrollPosition().addValueObserver(index -> {
         padsNoteOffset = index;
         if (isActive()) {
            applyNotes(padsNoteOffset);
         }
      });

      final List<DrumButton> drumButtons = hwElements.getDrumGridButtons();
      for (int i = 0; i < drumButtons.size(); i++) {
         final int index = i;
         final DrumPad pad = drumPadBank.getItemAt(i);
         pad.exists().markInterested();
         pad.color().addValueObserver((r, g, b) -> padColors[index] = ColorLookup.toColor(r, g, b));
         final DrumButton button = drumButtons.get(i);
         button.bindLight(this, () -> getPadState(index, pad));
      }
      initNavigation(hwElements);
   }

   private void initNavigation(final HwElements hwElements) {
      final LabeledButton upButton = hwElements.getLabeledButton(LabelCcAssignments.UP);
      final LabeledButton downButton = hwElements.getLabeledButton(LabelCcAssignments.DOWN);
      final LabeledButton leftButton = hwElements.getLabeledButton(LabelCcAssignments.LEFT);
      final LabeledButton rightButton = hwElements.getLabeledButton(LabelCcAssignments.RIGHT);
      final RgbState pressedColor = RgbState.of(21);
      final RgbState baseColor = RgbState.of(1);

      leftButton.bindRepeatHold(this, () -> handleNavigateVertical(-4));
      leftButton.bindHighlightButton(this, () -> (padsNoteOffset - 4) >= 4, baseColor, pressedColor);
      rightButton.bindRepeatHold(this, () -> handleNavigateVertical(4));
      rightButton.bindHighlightButton(this, () -> (padsNoteOffset + 4 + 64) < 128, baseColor, pressedColor);

      downButton.bindRepeatHold(this, () -> handleNavigateVertical(-16));
      downButton.bindHighlightButton(this, () -> (padsNoteOffset - 16) >= 4, baseColor, pressedColor);
      upButton.bindRepeatHold(this, () -> handleNavigateVertical(16));
      upButton.bindHighlightButton(this, () -> (padsNoteOffset + 16 + 64) < 128, baseColor, pressedColor);
   }

   private void handleNavigateVertical(final int direction) {
      final int newPosition = padsNoteOffset + direction;
      if (newPosition >= 4 && newPosition + 64 < 128) {
         drumPadBank.scrollBy(direction);
      }
   }


   private RgbState getPadState(final int index, final DrumPad pad) {
      final boolean playing = isPlaying(index);
      if (pad.exists().get()) {
         if (playing) {
            return RgbState.WHITE;
         }
         if (padColors[index] == 0) {
            return RgbState.of(trackState.getCursorColor());
         }
         return RgbState.of(padColors[index]);
      }
      return playing ? RgbState.DIM_WHITE : RgbState.OFF;
   }

   public boolean isPlaying(final int index) {
      final int offset = padsNoteOffset + index;
      if (offset < 128) {
         return isPlaying[offset];
      }
      return false;
   }

   public void applyNotes(final int noteOffset) {
      Arrays.fill(noteTable, -1);
      for (int note = 0; note < 64; note++) {
         final int value = noteOffset + note;
         noteTable[36 + note] = value < 128 ? value : -1;
      }
      noteInput.setKeyTranslationTable(noteTable);
   }

   private void handleNotes(final PlayingNote[] playingNotes) {
      if (!isActive()) {
         return;
      }
      Arrays.fill(isPlaying, false);
      for (final PlayingNote playingNote : playingNotes) {
         isPlaying[playingNote.pitch()] = true;
      }
   }

}
