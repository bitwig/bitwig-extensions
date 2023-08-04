package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class FocusClip {
   private static final int SINGLE_SLOT_RANGE = 8;
   private double playHeadLength;
   private final Clip mainCursoClip;
   private final CursorTrack cursorTrack;
   private final Application application;
   private final Transport transport;

   private int selectedSlotIndex = -1;

   private String currentTrackName = "";

   private final Map<String, Integer> indexMemory = new HashMap<>();
   private final ClipLauncherSlotBank slotBank;
   private int playHeadPosition = 0;

   @Inject
   private MidiProcessor midiProcessor;

   public FocusClip(ControllerHost host, Application application, Transport transport, ViewControl viewControl) {
      this.cursorTrack = viewControl.getCursorTrack();

      slotBank = cursorTrack.clipLauncherSlotBank();
      for (int i = 0; i < slotBank.getSizeOfBank(); i++) {
         final ClipLauncherSlot slot = slotBank.getItemAt(i);
         slot.isRecording().markInterested();
         slot.isPlaying().markInterested();
         slot.hasContent().markInterested();
      }
      this.application = application;
      this.transport = transport;

      slotBank.addPlaybackStateObserver((slotIndex, playbackState, isQueued) -> {
         if (playbackState != 0 && !isQueued) {
            slotBank.select(slotIndex);
         }
      });
      slotBank.addIsSelectedObserver((index, selected) -> {
         if (selected) {
            selectedSlotIndex = index;
            indexMemory.put(currentTrackName, selectedSlotIndex);
         }
      });
      this.cursorTrack.name().addValueObserver(name -> {
         selectedSlotIndex = -1;
         currentTrackName = name;
         final Integer index = indexMemory.get(name);
         if (index != null) {
            selectedSlotIndex = index.intValue();
         }
      });

      mainCursoClip = host.createLauncherCursorClip(16, 1);

      mainCursoClip.setStepSize(0.125);
      mainCursoClip.getLoopLength().addValueObserver(v -> {
         playHeadLength = v * 8;
      });
      mainCursoClip.playingStep().addValueObserver(v -> {
         if (playHeadLength > 0) {
            if (v == -1) {
               playHeadPosition = 0;
            } else {
               playHeadPosition = Math.min((int) (v / playHeadLength * 127), 127);
            }
         }
      });
   }

   public int getPlayPosition() {
      return playHeadPosition;
   }

   public void invokeRecord() {
      if (selectedSlotIndex != -1) {
         final ClipLauncherSlot slot = slotBank.getItemAt(selectedSlotIndex);
         if (slot.isRecording().get()) {
            slot.launch();
            transport.isClipLauncherOverdubEnabled().set(false);
         } else if (slot.isPlaying().get()) {
            transport.isClipLauncherOverdubEnabled().toggle();
         } else {
            slot.launch();
            transport.isClipLauncherOverdubEnabled().set(true);
         }
      } else {
         findNextEmptySlot(true).ifPresent(slot -> {
            slot.launch();
            transport.isClipLauncherOverdubEnabled().set(true);
         });

      }
   }

   private Optional<ClipLauncherSlot> findNextEmptySlot(final boolean select) {
      ClipLauncherSlot lastEmpty = null;
      int lastEmptyIndex = -1;
      for (int i = SINGLE_SLOT_RANGE - 1; i >= 0; i--) {
         final ClipLauncherSlot slot = slotBank.getItemAt(i);
         if (slot.hasContent().get()) {
            if (lastEmpty != null) {
               break;
            }
         } else {
            lastEmpty = slot;
            lastEmptyIndex = i;
         }
      }
      if (lastEmpty != null) {
         if (select) {
            slotBank.select(lastEmptyIndex);
         }
         return Optional.of(lastEmpty);
      }
      return Optional.empty();
   }

   public Clip getMainCursoClip() {
      return mainCursoClip;
   }

   public void duplicateContent() {
      mainCursoClip.duplicateContent();
      application.focusPanelBelow();
      application.toggleDevices();
      application.toggleNoteEditor();
      application.zoomToFit();
   }

   public void quantize(final double amount) {
      mainCursoClip.quantize(amount);
   }

   public void clearSteps() {
      mainCursoClip.clearSteps();
   }

   public void transpose(final int semitones) {
      mainCursoClip.transpose(semitones);
   }

   public void clearNotes(final int noteToClear) {
      mainCursoClip.scrollToKey(noteToClear);
      mainCursoClip.clearStepsAtY(0, 0);
   }


}
