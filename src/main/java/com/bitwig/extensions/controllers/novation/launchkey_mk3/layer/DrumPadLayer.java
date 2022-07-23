package com.bitwig.extensions.controllers.novation.launchkey_mk3.layer;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.*;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.control.RgbCcButton;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.control.RgbNoteButton;
import com.bitwig.extensions.framework.Layer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DrumPadLayer extends Layer {

   private final DrumPadBank padBank;

   private final NoteInput noteInput;
   private final Set<Integer> padNotes = new HashSet<>();
   private final PinnableCursorDevice cursorDevice;
   private boolean selectModeActive = false;
   private int padsNoteOffset = 32;
   private int keyNoteOffset = 32;
   private final int[] hangingNotes = new int[16];
   private final Integer[] noteTable = new Integer[128];
   private final boolean[] isPlaying = new boolean[128];

   private final PadSlot[] padSlots = new PadSlot[16];
   private int trackColor = 0;

   private boolean focusOnDrums = true;

   public static class PadSlot {
      private final DrumPad pad;
      private final int index;
      private final Device device;
      private int color;

      public PadSlot(final int index, final DrumPad pad) {
         this.index = index;
         this.pad = pad;
         final DeviceBank deviceBank = pad.createDeviceBank(1);
         device = deviceBank.getDevice(0);
      }

      public void setColor(final int color) {
         this.color = color;
      }

      public int getColor() {
         return color;
      }

      public void select(final PinnableCursorDevice cursorDevice) {
         pad.selectInEditor();
         cursorDevice.selectDevice(device);
      }
   }

   public DrumPadLayer(final LaunchkeyMk3Extension driver) {
      super(driver.getLayers(), "DRUM_LAYER");
      Arrays.fill(noteTable, -1);
      Arrays.fill(hangingNotes, -1);
      final HwControls hwControl = driver.getHwControl();
      final CursorTrack cursorTrack = driver.getCursorTrack();
      noteInput = driver.getMidiIn().createNoteInput("MIDI", "89????", "99????", "A9????");
      noteInput.setShouldConsumeEvents(false);
      final PinnableCursorDevice primaryDevice = driver.getPrimaryDevice();
      cursorDevice = driver.getCursorDevice();
      padBank = primaryDevice.createDrumPadBank(16);
      padBank.scrollPosition().addValueObserver(index -> {
         padsNoteOffset = index;
         if (isActive() && focusOnDrums) {
            applyNotes(padsNoteOffset);
         }
      });
      padBank.exists().markInterested();
      padBank.scrollPosition().set(padsNoteOffset);

      primaryDevice.hasDrumPads().addValueObserver(hasDrumPads -> {
         focusOnDrums = hasDrumPads;
         applyNotes(focusOnDrums ? padsNoteOffset : keyNoteOffset);
      });
      final RgbNoteButton[] buttons = hwControl.getDrumButtons();

      padBank.scrollPosition().markInterested();

      for (int i = 0; i < buttons.length; i++) {
         final int index = i;
         final RgbNoteButton button = buttons[i];
         final DrumPad pad = padBank.getItemAt(i);
         padSlots[index] = new PadSlot(index, pad);
         padNotes.add(buttons[i].getNumber());
         pad.exists().markInterested();
         pad.color().addValueObserver((r, g, b) -> padSlots[index].setColor(ColorLookup.toColor(r, g, b)));
         button.bindPressed(this, () -> {
         }, () -> getColor(index, pad));
      }

      final RgbCcButton navUpButton = hwControl.getNavUpButton();
      navUpButton.bindIsPressed(this, pressed -> {
         if (pressed) {
            driver.startHold(() -> scrollPadBank(1));
         } else {
            driver.stopHold();
         }
      }, this::canScrollUp, RgbState.YELLOW, RgbState.YELLOW_LO);

      final RgbCcButton navDownButton = hwControl.getNavDownButton();
      navDownButton.bindIsPressed(this, pressed -> {
         if (pressed) {
            driver.startHold(() -> scrollPadBank(-1));
         } else {
            driver.stopHold();
         }
      }, this::canScrollDown, RgbState.YELLOW, RgbState.YELLOW_LO);
      final RgbCcButton sceneLaunchButton = hwControl.getSceneLaunchButton();
      sceneLaunchButton.bindIsPressed(this, pressed -> selectModeActive = pressed,
         pressed -> pressed ? RgbState.WHITE : RgbState.OFF);
      final RgbCcButton row2ModeButton = hwControl.getModeRow2Button();
      row2ModeButton.bindIsPressed(this, pressed -> {

      }, pressed -> pressed ? RgbState.ORANGE_LO : RgbState.OFF);
      cursorTrack.playingNotes().addValueObserver(this::handleNotes);
      cursorTrack.color().addValueObserver((r, g, b) -> trackColor = ColorLookup.toColor(r, g, b));
   }

   private boolean canScrollUp() {
      if (focusOnDrums) {
         return padsNoteOffset + 16 < 128;
      }
      return keyNoteOffset + 16 < 128;
   }

   private boolean canScrollDown() {
      if (focusOnDrums) {
         return padsNoteOffset - 4 >= 0;
      }
      return keyNoteOffset - 4 >= 0;
   }

   private void scrollPadBank(final int dir) {
      if (padBank.exists().get()) {
         if (dir < 0) {
            final int amount = padsNoteOffset > 8 ? -16 : -4;
            padBank.scrollBy(amount);
         } else {
            final int amount = padsNoteOffset == 0 ? 4 : 16;
            padBank.scrollBy(amount);
         }
      } else {
         final int newPos;
         if (dir < 0) {
            final int amount = keyNoteOffset > 8 ? -16 : -4;
            newPos = Math.max(0, keyNoteOffset + amount);
         } else {
            final int amount = keyNoteOffset == 0 ? 4 : 16;
            newPos = Math.min(124, keyNoteOffset + amount);
         }
         if (newPos != keyNoteOffset) {
            keyNoteOffset = newPos;
            applyNotes(keyNoteOffset);
         }
      }
   }

   private RgbState getColor(final int index, final DrumPad pad) {
      final int noteValue = (focusOnDrums ? padsNoteOffset : keyNoteOffset) + index;
      if (!focusOnDrums) {
         return RgbState.of(isPlaying[noteValue] ? trackColor - 2 : trackColor + 1);
      } else if (pad.exists().get()) {
         final int color = padSlots[index].getColor();
         if (color == 0) {
            return RgbState.of(isPlaying[noteValue] ? trackColor - 2 : trackColor + 1);
         }
         return RgbState.of(isPlaying[noteValue] ? color - 1 : color);
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

   public void notifyNote(final int sb, final int noteValue, final int vel) {
      if (!isActive() || !padNotes.contains(
         noteValue) || (sb != LaunchkeyConstants.NOTE_ON && sb != LaunchkeyConstants.NOTE_OFF)) {
         return;
      }
      final int index = noteValue - 36;
      final int notePlayed = focusOnDrums ? padsNoteOffset + index : keyNoteOffset + index;
      if (sb == LaunchkeyConstants.NOTE_ON && vel > 0) {
         hangingNotes[index] = notePlayed;
         if (selectModeActive) {
            padSlots[index].select(cursorDevice);
         }
      } else {
         if (hangingNotes[index] != -1 && hangingNotes[index] != notePlayed) {
            noteInput.sendRawMidiEvent(LaunchkeyConstants.NOTE_OFF | LaunchkeyConstants.DRUM_CHANNEL,
               hangingNotes[index], 0);
         }
         hangingNotes[index] = -1;
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
      applyNotes(focusOnDrums ? padsNoteOffset : keyNoteOffset);
   }

   @Override
   protected void onDeactivate() {
      super.onDeactivate();
      resetNotes();
   }

}
