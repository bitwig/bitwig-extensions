package com.bitwig.extensions.controllers.novation.launchpadmini3;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.novation.commonsmk3.OverviewGrid;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.DebugOutLp;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.FocusSlot;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;

import java.util.Optional;

@Component
public class ViewCursorControl {
   @Inject
   Application application;

   private final CursorTrack cursorTrack;
   private final PinnableCursorDevice primaryDevice;
   private final TrackBank trackBank;
   private final PinnableCursorDevice cursorDevice;
   private final Clip cursorClip;

   private final Track rootTrack;
   private final ClipLauncherSlotBank mainTrackSlotBank;
   private final Track largeFocusTrack;
   private FocusSlot focusSlot;
   private final TrackBank maxTrackBank;
   private int queuedForPlaying = 0;
   private int focusSceneIndex;

   private final OverviewGrid overviewGrid = new OverviewGrid();

   public ViewCursorControl(final ControllerHost host) {
      rootTrack = host.getProject().getRootTrackGroup();
      rootTrack.arm().markInterested();

      maxTrackBank = host.createTrackBank(64, 1, 1);

      setUpFocusScene();

      trackBank = host.createTrackBank(8, 1, 8);

      trackBank.sceneBank().itemCount().addValueObserver(overviewGrid::setNumberOfScenes);
      trackBank.channelCount().addValueObserver(overviewGrid::setNumberOfTracks);
      trackBank.scrollPosition().addValueObserver(overviewGrid::setTrackPosition);
      trackBank.sceneBank().scrollPosition().addValueObserver(overviewGrid::setScenePosition);

      cursorTrack = host.createCursorTrack(8, 8);
      for (int i = 0; i < 8; i++) {
         prepareTrack(trackBank.getItemAt(i));
      }

      cursorDevice = cursorTrack.createCursorDevice();
      cursorClip = host.createLauncherCursorClip(32 * 6, 127);

      cursorTrack.clipLauncherSlotBank().cursorIndex().addValueObserver(index -> {
         // RemoteConsole.out.println(" => {}", index);
      });
      prepareTrack(cursorTrack);

      primaryDevice = cursorTrack.createCursorDevice("drumdetection", "Pad Device", 8,
         CursorDeviceFollowMode.FIRST_INSTRUMENT);
      primaryDevice.hasDrumPads().markInterested();
      primaryDevice.exists().markInterested();


      final TrackBank singleTrackBank = host.createTrackBank(1, 0, 16);
      singleTrackBank.scrollPosition().markInterested();
      singleTrackBank.followCursorTrack(cursorTrack);
      largeFocusTrack = singleTrackBank.getItemAt(0);
      prepareTrack(largeFocusTrack);

      mainTrackSlotBank = largeFocusTrack.clipLauncherSlotBank();
      final BooleanValue equalsToCursorTrack = largeFocusTrack.createEqualsValue(cursorTrack);
      equalsToCursorTrack.markInterested();


      for (int i = 0; i < 16; i++) {
         final int index = i;
         final ClipLauncherSlot slot = mainTrackSlotBank.getItemAt(i);
         prepareSlot(slot);

         slot.isSelected().addValueObserver(selected -> {
            if (selected) {
               DebugOutLp.println("Select Slot MAIN %d", index);
               focusSlot = new FocusSlot(largeFocusTrack, slot, index, equalsToCursorTrack);
            }
         });
      }
   }

   private void setUpFocusScene() {
      maxTrackBank.sceneBank().scrollPosition().addValueObserver(scrollPos -> focusSceneIndex = scrollPos);
      for (int i = 0; i < 64; i++) {
         final Track track = maxTrackBank.getItemAt(i);
         final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(0);
         slot.isPlaybackQueued().addValueObserver(queued -> {
            if (queued) {
               queuedForPlaying++;
            } else if (queuedForPlaying > 0) {
               queuedForPlaying--;
            }
         });
      }
   }

   public boolean hasQueuedForPlaying() {
      return queuedForPlaying > 0;
   }

   public void focusScene(final int sceneIndex) {
      maxTrackBank.sceneBank().scrollPosition().set(sceneIndex);
   }

   private void prepareTrack(final Track track) {
      track.arm().markInterested();
      track.monitorMode().markInterested();
      track.sourceSelector().hasAudioInputSelected().markInterested();
      track.sourceSelector().hasNoteInputSelected().markInterested();
   }

   private void prepareSlot(final ClipLauncherSlot slot) {
      slot.isRecording().markInterested();
      slot.isRecordingQueued().markInterested();
      slot.hasContent().markInterested();
      slot.name().markInterested();
      slot.isPlaying().markInterested();
      slot.isSelected().markInterested();
   }

   public void scrollToOverview(final int trackIndex, final int sceneIndex) {
      final int posX = trackIndex * 8;
      final int posY = sceneIndex * 8;
      if (posX < overviewGrid.getNumberOfTracks() && posY < overviewGrid.getNumberOfScenes()) {
         trackBank.scrollPosition().set(posX);
         trackBank.sceneBank().scrollPosition().set(posY);
      }
   }

   public boolean inOverviewGrid(final int trackIndex, final int sceneIndex) {
      final int posX = trackIndex * 8;
      final int posY = sceneIndex * 8;
      return posX < overviewGrid.getNumberOfTracks() && posY < overviewGrid.getNumberOfScenes();
   }

   public boolean inOverviewGridFocus(final int trackIndex, final int sceneIndex) {
      final int locX = overviewGrid.getTrackPosition() / 8;
      final int locY = overviewGrid.getScenePosition() / 8;
      return locX == trackIndex && locY == sceneIndex;
   }

   public TrackBank getTrackBank() {
      return trackBank;
   }

   public CursorTrack getCursorTrack() {
      return cursorTrack;
   }

   public PinnableCursorDevice getPrimaryDevice() {
      return primaryDevice;
   }

   public PinnableCursorDevice getCursorDevice() {
      return cursorDevice;
   }

   public Clip getCursorClip() {
      return cursorClip;
   }

   public FocusSlot getFocusSlot() {
      return focusSlot;
   }

   private boolean trackOfFocusSlotArmed() {
      return focusSlot != null && focusSlot.getTrack().arm().get();
   }

   public void globalRecordAction(final Transport transport) {
      if (largeFocusTrack.arm().get() || trackOfFocusSlotArmed()) {
         if (focusSlot != null) {
            handleFocusedSlotOnArmedTrack(transport);
         } else {
            handleNoFocusSlotOnArmedTrack(transport);
         }
      } else if (focusSlot != null) {
         handleRecordFocusSlotNotArmed(transport);
      } else {
         transport.isClipLauncherOverdubEnabled().toggle();
      }
   }

   private void handleNoFocusSlotOnArmedTrack(final Transport transport) {
      final Optional<ClipLauncherSlot> playingSlot = findPlayingSlot();
      if (playingSlot.isPresent()) {
         toggleRecording(playingSlot.get(), transport);
      } else {
         findEmptySlotAndLaunch(transport, -1);
      }
   }

   private void handleFocusedSlotOnArmedTrack(final Transport transport) {
      if (focusSlot.getTrack().arm().get()) {
         if (focusSlot.isEmpty()) {
            recordToEmptySlot(focusSlot.getSlot(), transport);
         } else {
            toggleRecording(focusSlot.getSlot(), transport);
         }
      } else {
         findEmptySlotAndLaunch(transport, focusSlot.getSlotIndex());
      }
   }

   private void handleRecordFocusSlotNotArmed(final Transport transport) {
      final Track track = focusSlot.getTrack();
      if (canRecord(focusSlot.getTrack())) {
         track.arm().set(true);
         track.selectInEditor();
         if (!focusSlot.isEmpty()) {
            toggleRecording(focusSlot.getSlot(), transport);
         } else {
            recordToEmptySlot(focusSlot.getSlot(), transport);
         }
      } else {
         transport.isClipLauncherOverdubEnabled().toggle();
      }
   }

   private void findEmptySlotAndLaunch(final Transport transport, final int slotIndex) {
      final Optional<ClipLauncherSlot> slot = findCursorFirstEmptySlot(slotIndex);
      if (slot.isPresent()) {
         recordToEmptySlot(slot.get(), transport);
      } else {
         transport.isClipLauncherOverdubEnabled().toggle();
      }
   }

   private boolean canRecord(final Track track) {
      return track.sourceSelector().hasNoteInputSelected().get() || track.sourceSelector()
         .hasAudioInputSelected()
         .get();
   }

   public int getFocusSceneIndex() {
      return focusSceneIndex;
   }

   public Optional<ClipLauncherSlot> findCursorFirstEmptySlot(final int firstIndex) {
      if (firstIndex >= 0 && firstIndex < 16) {
         final ClipLauncherSlot slot = mainTrackSlotBank.getItemAt(firstIndex);
         if (!slot.hasContent().get()) {
            return Optional.of(slot);
         }
      }
      for (int i = 0; i < 16; i++) {
         final ClipLauncherSlot slot = mainTrackSlotBank.getItemAt(i);
         if (!slot.hasContent().get()) {
            return Optional.of(slot);
         }
      }
      return Optional.empty();
   }

   private void toggleRecording(final ClipLauncherSlot slot, final Transport transport) {
      if (slot.isRecordingQueued().get() || slot.isRecording().get()) {
         slot.launch();
         transport.isClipLauncherOverdubEnabled().set(false);
      } else if (!slot.isPlaying().get()) {
         slot.launch();
         transport.isClipLauncherOverdubEnabled().set(true);
      } else if (transport.isPlaying().get()) {
         transport.isClipLauncherOverdubEnabled().toggle();
      } else {
         transport.restart();
         slot.launch();
         transport.isClipLauncherOverdubEnabled().set(true);
      }
   }

   private void recordToEmptySlot(final ClipLauncherSlot slot, final Transport transport) {
      if (!transport.isPlaying().get()) {
         transport.restart();
      }
      slot.select();
      slot.launch();
      transport.isClipLauncherOverdubEnabled().set(true);
   }

   public Optional<ClipLauncherSlot> findPlayingSlot() {
      for (int i = 0; i < 16; i++) {
         final ClipLauncherSlot slot = mainTrackSlotBank.getItemAt(i);
         if (slot.hasContent().get() && slot.isPlaying().get()) {
            return Optional.of(slot);
         }
      }
      return Optional.empty();
   }

}
