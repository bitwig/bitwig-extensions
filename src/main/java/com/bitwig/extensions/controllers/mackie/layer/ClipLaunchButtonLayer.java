package com.bitwig.extensions.controllers.mackie.layer;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;
import com.bitwig.extensions.controllers.mackie.section.MixControl;
import com.bitwig.extensions.controllers.mackie.section.MixerSectionHardware;
import com.bitwig.extensions.controllers.mackie.value.LayoutType;
import com.bitwig.extensions.controllers.mackie.value.ModifierValueObject;
import com.bitwig.extensions.framework.Layer;

public class ClipLaunchButtonLayer extends Layer {

   private final LayoutType layoutType;
   private TrackBank trackBank;
   private final MackieMcuProExtension driver;
   private final Clip mainCursorClip;
   private int blinkTicks;
   private int trackOffset;
   private final Layer launcherLayer;
   private final Layer arrangerLayer;
   private final SlotHandler slotHandler;

   public ClipLaunchButtonLayer(final String name, final MixControl mixControl, final SlotHandler slotHandler) {
      super(mixControl.getDriver().getLayers(),
         name + "_" + mixControl.getHwControls().getSectionIndex() + "_ClipLaunch");
      driver = mixControl.getDriver();
      this.slotHandler = slotHandler;
      mainCursorClip = driver.getHost().createLauncherCursorClip(16, 1);
      launcherLayer = new Layer(driver.getLayers(), name + "_LAUNCH");
      arrangerLayer = new Layer(driver.getLayers(), name + "_ARRANGE");
      layoutType = LayoutType.LAUNCHER;
   }

   public void initTrackBank(final MixerSectionHardware hwControls, final TrackBank trackBank) {
      trackBank.setChannelScrollStepSize(1);
      this.trackBank = trackBank;
      trackOffset = hwControls.getSectionIndex() * 8;

      for (int index = 0; index < 8; index++) {
         final int trackIndex = index + trackOffset;
         final Track track = trackBank.getItemAt(trackIndex);
         final ClipLauncherSlotBank slotBank = track.clipLauncherSlotBank();
         slotBank.setIndication(false);

         for (int slotIndex = 0; slotIndex < 4; slotIndex++) {
            final ClipLauncherSlot slot = slotBank.getItemAt(slotIndex);
            slot.hasContent().markInterested();
            slot.isPlaying().markInterested();
            slot.isPlaybackQueued().markInterested();
            slot.isRecording().markInterested();
            slot.isRecordingQueued().markInterested();

            //if (slotIndex < 4) {
            final HardwareButton verticalButton = hwControls.getButton(slotIndex, index);
            launcherLayer.bindPressed(verticalButton, () -> handleSlotPressed(track, slot));
            launcherLayer.bind(() -> lightState(slot), (OnOffHardwareLight) verticalButton.backgroundLight());
            //}
            if (index < 4) {
               final HardwareButton horizontalButton = hwControls.getButton(index, slotIndex);
               arrangerLayer.bindPressed(horizontalButton, () -> handleSlotPressed(track, slot));
               arrangerLayer.bind(() -> lightState(slot), (OnOffHardwareLight) horizontalButton.backgroundLight());
            }
         }
      }
   }

   public void handleSlotPressed(final Track track, final ClipLauncherSlot slot) {
      final ModifierValueObject modifier = driver.getModifier();
      slotHandler.handleSlotPressed(track, slot, mainCursorClip, modifier);
   }

   public void notifyBlink(final int ticks) {
      blinkTicks = ticks;
   }

   public boolean lightState(final ClipLauncherSlot slot) {
      if (slot.isPlaybackQueued().get() || slot.isRecordingQueued().get()) {
         return blinkTicks % 2 != 0;
      } else if (slot.isRecording().get()) {
         return blinkTicks % 4 != 0;
      } else if (slot.isPlaying().get()) {
         return blinkTicks % 8 >= 3;
      }
      return slot.hasContent().get();
   }

   public void navigateHorizontal(final int direction, final boolean pressed) {
      if (!pressed) {
         return;
      }
      if (layoutType == LayoutType.LAUNCHER) {
         if (direction > 0) {
            trackBank.scrollForwards();
         } else {
            trackBank.scrollBackwards();
         }
      } else {
         if (direction > 0) {
            trackBank.sceneBank().scrollForwards();
         } else {
            trackBank.sceneBank().scrollBackwards();
         }
      }
   }

   public void navigateVertical(final int direction, final boolean pressed) {
      if (!pressed) {
         return;
      }
      if (layoutType == LayoutType.LAUNCHER) {
         if (direction > 0) {
            trackBank.sceneBank().scrollBackwards();
         } else {
            trackBank.sceneBank().scrollForwards();
         }
      } else {
         if (direction > 0) {
            trackBank.scrollBackwards();
         } else {
            trackBank.scrollForwards();
         }
      }
   }

   @Override
   protected void onActivate() {
      applyLayer();
   }

   private void applyLayer() {
      if (!isActive()) {
         return;
      }
      if (layoutType == LayoutType.ARRANGER) {
         launcherLayer.deactivate();
         arrangerLayer.activate();
      } else {
         arrangerLayer.deactivate();
         launcherLayer.activate();
      }
      setIndication(true);
   }

   @Override
   protected void onDeactivate() {
      setIndication(false);
      launcherLayer.deactivate();
      arrangerLayer.deactivate();
   }

   private void setIndication(final boolean enabled) {
      for (int index = 0; index < 8; index++) {
         final Track track = trackBank.getItemAt(index + trackOffset);
         final ClipLauncherSlotBank slotBank = track.clipLauncherSlotBank();
         slotBank.setIndication(enabled);
         for (int slotIndex = 0; slotIndex < 4; slotIndex++) {
            if (layoutType == LayoutType.LAUNCHER) {
               slotBank.getItemAt(slotIndex).setIndication(enabled);
            } else {
               slotBank.getItemAt(slotIndex).setIndication(index < 4 && enabled);
            }
         }
      }
   }

}
