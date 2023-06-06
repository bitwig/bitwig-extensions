package com.bitwig.extensions.controllers.nativeinstruments.maschine.modes;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.*;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.buttons.PadButton;

public class SessionMode extends PadMode {
   private final MaschineLayer selectLayer;
   private final MaschineLayer eraseLayer;
   private final MaschineLayer duplicateLayer;
   private final MaschineLayer colorChooseLayer;

   private ModifierState modstate = ModifierState.NONE;

   private final ClipLauncherSlot[] slotMappingVertical = new ClipLauncherSlot[16];
   private final ClipLauncherSlot[] slotMappingHorizontal = new ClipLauncherSlot[16];
   private final int[] colorVertical = new int[16];
   private final int[] colorHorizontal = new int[16];

   private ClipLauncherSlot[] currentSlotMapping;
   private int[] gridColors = new int[16];

   private LayoutType layout = LayoutType.LAUNCHER;
   private final TrackBank trackBank;

   public SessionMode(final MaschineExtension driver, final String name) {
      super(driver, name, true);
      selectLayer = new MaschineLayer(driver, "select-" + name);
      eraseLayer = new MaschineLayer(driver, "clear-" + name);
      duplicateLayer = new MaschineLayer(driver, "duplicate-" + name);
      colorChooseLayer = new MaschineLayer(driver, "colorpick-" + name);
      trackBank = driver.getTrackBank();
      currentSlotMapping = slotMappingVertical;
      gridColors = colorVertical;
      doGridBinding(driver);

      trackBank.canScrollChannelsDown().addValueObserver(newValue -> dKnobLedUpdateChannelDown(driver, newValue));
      trackBank.canScrollChannelsUp().addValueObserver(newValue -> dKnobLedUpdateChannelUp(driver, newValue));
      trackBank.sceneBank()
         .canScrollForwards()
         .addValueObserver(newValue -> dKnobLedUpdateSceneForward(driver, newValue));
      trackBank.sceneBank()
         .canScrollBackwards()
         .addValueObserver(newValue -> dKnobLedUpdateSceneBackwards(driver, newValue));
   }

   private void dKnobLedUpdateSceneBackwards(MaschineExtension driver, boolean newValue) {
      if (layout == LayoutType.LAUNCHER) {
         driver.sendLedUpdate(CcAssignment.DKNOB_UP, newValue ? 127 : 0);
      } else {
         driver.sendLedUpdate(CcAssignment.DKNOB_LEFT, newValue ? 127 : 0);
      }
   }

   private void dKnobLedUpdateSceneForward(MaschineExtension driver, boolean newValue) {
      if (layout == LayoutType.LAUNCHER) {
         driver.sendLedUpdate(CcAssignment.DKNOB_DOWN, newValue ? 127 : 0);
      } else {
         driver.sendLedUpdate(CcAssignment.DKNOB_RIGHT, newValue ? 127 : 0);
      }
   }

   private void dKnobLedUpdateChannelUp(MaschineExtension driver, boolean newValue) {
      if (layout == LayoutType.LAUNCHER) {
         driver.sendLedUpdate(CcAssignment.DKNOB_LEFT, newValue ? 127 : 0);
      } else {
         driver.sendLedUpdate(CcAssignment.DKNOB_UP, newValue ? 127 : 0);
      }
   }

   private void dKnobLedUpdateChannelDown(MaschineExtension driver, boolean newValue) {
      if (layout == LayoutType.LAUNCHER) {
         driver.sendLedUpdate(CcAssignment.DKNOB_RIGHT, newValue ? 127 : 0);
      } else {
         driver.sendLedUpdate(CcAssignment.DKNOB_DOWN, newValue ? 127 : 0);
      }
   }

   private void doGridBinding(final MaschineExtension driver) {
      trackBank.setChannelScrollStepSize(1);
      final PadButton[] buttons = driver.getPadButtons();
      trackBank.setShouldShowClipLauncherFeedback(true);

      for (int trackIndex = 0; trackIndex < trackBank.getSizeOfBank(); trackIndex++) {
         int ti = trackIndex;
         final Track track = trackBank.getItemAt(trackIndex);
         final ClipLauncherSlotBank slotBank = track.clipLauncherSlotBank();

         for (int slotIndex = 0; slotIndex < 4; slotIndex++) {
            final int si = slotIndex;
            final int buttonIndex = slotIndex * 4 + trackIndex;
            final int buttonIndexHorizontal = (3 - trackIndex) * 4 + slotIndex;
            slotMappingVertical[buttonIndex] = slotBank.getItemAt(3 - slotIndex);
            slotMappingHorizontal[buttonIndexHorizontal] = slotBank.getItemAt(slotIndex);
            int indexHorizontal = (3 - trackIndex) * 4 + (3 - slotIndex);
            slotMappingVertical[buttonIndex].color().addValueObserver((r, g, b) -> {
               colorVertical[buttonIndex] = NIColorUtil.convertColorX(r, g, b);
               colorHorizontal[indexHorizontal] = colorVertical[buttonIndex];
               //DebugOutMs.println("Color %d %d  => %d", ti, si, colorVertical[buttonIndex]);
            });
            final PadButton button = buttons[buttonIndex];
            bindPressed(button, () -> handleLaunch(buttonIndex));
            bindReleased(button, () -> handleLaunchRelease(buttonIndex));
            bindShift(button);
            selectLayer.bindPressed(button, () -> handleSelect(buttonIndex));
            eraseLayer.bindPressed(button, () -> handleErase(buttonIndex));
            duplicateLayer.bindPressed(button, () -> handleDuplicate(buttonIndex));
            colorChooseLayer.bindPressed(button, () -> handleColorPick(buttonIndex));
            bindLightState(() -> computeGridLedState(buttonIndex, ti, si), button);
         }
      }
   }

   public void notifyPanelLayout(final LayoutType layout) {
      if (this.layout != layout) {
         this.layout = layout;
         if (layout == LayoutType.ARRANGER) {
            currentSlotMapping = slotMappingHorizontal;
            gridColors = colorHorizontal;
         } else {
            currentSlotMapping = slotMappingVertical;
            gridColors = colorVertical;
         }
         final PadButton[] buttons = getDriver().getPadButtons();
         for (final PadButton padButton : buttons) {
            getDriver().updatePadLed(padButton);
         }
      }
   }

   private void handleLaunchRelease(final int buttonIndex) {
      if (getDriver().getLaunchModifierSet().get()) {
         currentSlotMapping[buttonIndex].launchReleaseAlt();
      } else {
         currentSlotMapping[buttonIndex].launchRelease();
      }
   }

   private void handleLaunch(final int buttonIndex) {
      if (getDriver().isStopDown()) {
         final int trackIndex = layout == LayoutType.ARRANGER ? 3 - buttonIndex / 4 : buttonIndex % 4;
         trackBank.getItemAt(trackIndex).clipLauncherSlotBank().stop();
      } else {
         if (getDriver().getLaunchModifierSet().get()) {
            currentSlotMapping[buttonIndex].launchAlt();
         } else {
            currentSlotMapping[buttonIndex].launch();
         }
      }
   }

   private void handleSelect(final int buttonIndex) {
      currentSlotMapping[buttonIndex].select();
   }

   private void handleErase(final int buttonIndex) {
      currentSlotMapping[buttonIndex].deleteObject();
   }

   private void handleColorPick(final int buttonIndex) {
      final ClipLauncherSlot slot = currentSlotMapping[buttonIndex];
      if (slot.hasContent().get()) {
         getDriver().enterColorSelection(color -> {
            color.set(slot.color());
         });
      }
   }

   private void handleDuplicate(final int buttonIndex) {
      if (getDriver().isShiftDown()) {
         currentSlotMapping[buttonIndex].select();
         getDriver().getFocusClip().duplicateContent();
      } else {
         currentSlotMapping[buttonIndex].duplicateClip();
      }
   }

   private MaschineLayer getLayer(final ModifierState modstate) {
      switch (modstate) {
         case SHIFT:
            return getShiftLayer();
         case DUPLICATE:
            return duplicateLayer;
         case SELECT:
            return selectLayer;
         case ERASE:
            return eraseLayer;
         case VARIATION:
            return colorChooseLayer;
         default:
            return null;
      }

   }

   @Override
   public void setModifierState(final ModifierState modstate, final boolean active) {
      final MaschineLayer layer = getLayer(modstate);
      if (layer != null) {
         if (active) {
            this.modstate = modstate;
            layer.activate();
         } else {
            this.modstate = ModifierState.NONE;
            layer.deactivate();
         }
      } else {
         this.modstate = ModifierState.NONE;
      }
   }

   @Override
   protected String getModeDescription() {
      return "Clip Launcher";
   }

   private InternalHardwareLightState computeGridLedState(final int buttonIndex, int trackIndex, int slotIndex) {
      final ClipLauncherSlot slot = currentSlotMapping[buttonIndex];
      int color = gridColors[buttonIndex];
      Track track = trackBank.getItemAt(layout == LayoutType.LAUNCHER ? trackIndex : 3 - slotIndex);

      if (modstate == ModifierState.SELECT && slot.isSelected().get()) {
         return RgbLed.of(70);
      } else if (slot.hasContent().get()) {
         if (slot.isRecordingQueued().get()) {
            return blinkSlow(color, 3);
         } else if (slot.isRecording().get()) {
            return blinkSlow(5, 4);
         } else if (slot.isPlaybackQueued().get()) {
            return blinkMid(30, color);
         } else if (slot.isStopQueued().get()) {
            return blinkFast(28, 30); //RgbState.flash(color, 1);
         } else if (slot.isPlaying().get() /*&& track.isQueuedForStop().get()*/) {
            return blinkSlow(30, 28);
         } else if (slot.isPlaying().get()) {
//            if (clipLauncherOverdub.get() && track.arm().get()) {
//               return RgbState.pulse(5);
//            } else {
//               return RgbState.pulse(22);
//            }
         }
         return RgbLed.of(color + 1);
      }
      if (slot.isRecordingQueued().get()) {
         return RgbLed.of(5); // Possibly Track Color
      } else if (track.arm().get()) {
         return RgbLed.of(7);
      }
      return RgbLed.OFF;
   }

   RgbLed blinkSlow(int onColor, int offColor) {
      int phase = getDriver().getBlinkState() / 4;
      return RgbLed.of(phase == 0 ? onColor : offColor);
   }

   RgbLed blinkMid(int onColor, int offColor) {
      int phase = getDriver().getBlinkState() % 4;
      return RgbLed.of(phase < 2 ? onColor : offColor);
   }

   RgbLed blinkFast(int onColor, int offColor) {
      int phase = getDriver().getBlinkState() % 8;
      return RgbLed.of(phase == 0 ? onColor : offColor);
   }

}
