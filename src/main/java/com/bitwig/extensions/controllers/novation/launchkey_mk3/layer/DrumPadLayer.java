package com.bitwig.extensions.controllers.novation.launchkey_mk3.layer;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.ColorLookup;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.HwControls;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.LaunchkeyMk3Extension;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.RgbState;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.control.RgbCcButton;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.control.RgbNoteButton;
import com.bitwig.extensions.framework.Layer;

import java.util.Arrays;

public class DrumPadLayer extends Layer {
   private final DrumPadBank padBank;
   private final NoteInput noteInput;
   private final int keysNoteOffset = 60;
   private int padsNoteOffset = 32;
   private final int[] hangingNotes = new int[16];
   private final Integer[] noteTable = new Integer[128];
   private final boolean[] isPlaying = new boolean[128];
   private final int padColors[] = new int[16];
   private int trackColor = 0;
   private final boolean focusOnDrums = true;

   public DrumPadLayer(final LaunchkeyMk3Extension driver) {
      super(driver.getLayers(), "DRUM_LAYER");
      Arrays.fill(noteTable, -1);
      Arrays.fill(hangingNotes, -1);
      final HwControls hwControl = driver.getHwControl();
      final CursorTrack cursorTrack = driver.getCursorTrack();
      noteInput = driver.getMidiIn().createNoteInput("MIDI", "89????", "99????", "A9????");
      noteInput.setShouldConsumeEvents(false);
      final PinnableCursorDevice primaryDevice = driver.getPrimaryDevice();
      padBank = primaryDevice.createDrumPadBank(16);
      padBank.scrollPosition().addValueObserver(index -> {
         padsNoteOffset = index;
         if (isActive()) {
            applyNotes(padsNoteOffset);
         }
      });
      padBank.scrollPosition().set(30);
      padBank.canScrollChannelsUp().markInterested();
      padBank.canScrollChannelsDown().markInterested();

      primaryDevice.hasDrumPads().addValueObserver(hasDrumPads -> {
         if (isActive()) {
            applyNotes(hasDrumPads ? padsNoteOffset : keysNoteOffset);
         }
      });
      final RgbNoteButton[] buttons = hwControl.getDrumButtons();

      padBank.scrollPosition().markInterested();

      for (int i = 0; i < buttons.length; i++) {
         final int index = i;
         final RgbNoteButton button = buttons[i];
         final DrumPad pad = padBank.getItemAt(i);
         pad.exists().markInterested();
         pad.color().addValueObserver((r, g, b) -> {
            padColors[index] = ColorLookup.toColor(r, g, b);
            driver.getHost().println("COOR PA " + index + " " + padColors[index]);
         });
         button.bindPressed(this, () -> {
         }, () -> getColor(index, pad));
      }

      final RgbCcButton navUpButton = hwControl.getNavUpButton();
      navUpButton.bindIsPressed(this, pressed -> {
         if (pressed) {
            driver.startHold(() -> padBank.scrollBy(4));
         } else {
            driver.stopHold();
         }
      }, () -> {
         return padBank.scrollPosition().get() + 4 < 128 ? RgbState.DIM_WHITE : RgbState.OFF;
      });
      final RgbCcButton navDownButton = hwControl.getNavDownButton();
      navDownButton.bindIsPressed(this, pressed -> {
         if (pressed) {
            driver.startHold(() -> padBank.scrollBy(-4));
         } else {
            driver.stopHold();
         }
      }, () -> {
         return padBank.scrollPosition().get() - 4 >= 0 ? RgbState.DIM_WHITE : RgbState.OFF;
      });
      final RgbCcButton sceneLaunchButton = hwControl.getSceneLaunchButton();
      sceneLaunchButton.bindPressed(this, () -> {

      }, () -> RgbState.WHITE);
      final RgbCcButton row2ModeButton = hwControl.getModeRow2Button();
      row2ModeButton.bindPressed(this, () -> {

      }, () -> RgbState.RED_LO);
      cursorTrack.playingNotes().addValueObserver(this::handleNotes);
      cursorTrack.color().addValueObserver((r, g, b) -> trackColor = ColorLookup.toColor(r, g, b));
   }

   private RgbState getColor(final int index, final DrumPad pad) {
      if (pad.exists().get()) {
         final int noteValue = (focusOnDrums ? padsNoteOffset : keysNoteOffset) + index;
         if (padColors[index] == 0) {
            return RgbState.of(isPlaying[noteValue] ? trackColor : trackColor + 2);
         }
         return RgbState.of(isPlaying[noteValue] ? padColors[index] - 1 : padColors[index]);
      }
      return RgbState.OFF;
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

   public void applyNotes(final int noteOffset) {
      Arrays.fill(noteTable, -1);
      for (int note = 0; note < 16; note++) {
         final int value = noteOffset + note;
         noteTable[36 + note] = value < 128 ? value : -1;
      }
      noteInput.setKeyTranslationTable(noteTable);
   }

   private void resetNotes() {
      Arrays.fill(noteTable, -1);
      noteInput.setKeyTranslationTable(noteTable);
   }

   @Override
   protected void onActivate() {
      super.onActivate();
      applyNotes(focusOnDrums ? padsNoteOffset : keysNoteOffset);
   }

   @Override
   protected void onDeactivate() {
      super.onDeactivate();
      resetNotes();
   }

}
