package com.bitwig.extensions.controllers.novation.launchkey_mk3.layer;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.LaunchkeyMk3Extension;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.RgbState;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.control.RgbCcButton;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.control.RgbNoteButton;
import com.bitwig.extensions.framework.Layer;

public class DeviceSelectionLayer extends Layer {

   private final ControllerHost host;
   private String deviceName;
   private String[] pageNames = new String[8];
   private int numberOfPages = 0;
   private int pageIndex;
   private int selectedDeviceIndex = -1;
   private boolean sceneDown = false;
   private boolean modeDown = false;
   private boolean cursorUpDown = false;
   private boolean cursorDownDown = false;

   private class DeviceSlot {
      private final int index;
      public boolean hasDrumPads;
      String name;
      boolean hasDevice;
      String type;
      boolean enabled;
      final Device device;

      public DeviceSlot(final int index, final Device device) {
         this.index = index;
         this.device = device;
      }
   }

   public DeviceSelectionLayer(final LaunchkeyMk3Extension driver) {
      super(driver.getLayers(), "DEVICE_LAYER");

      final RgbNoteButton[] buttons = driver.getHwControl().getDeviceButtons();
      final CursorRemoteControlsPage remoteControlBank = driver.getRemoteControlBank();
      final DeviceBank deviceBank = driver.getDeviceBank();
      final PinnableCursorDevice cursorDevice = driver.getCursorDevice();

      deviceBank.itemCount().addValueObserver(itmes -> driver.getHost().println(" DB ITEMS = " + itmes));

      cursorDevice.name().addValueObserver(name -> {
         deviceName = name;
         if (cursorDownDown || cursorUpDown) {
            driver.setTransientText(deviceName, "");
         }
      });
      remoteControlBank.pageNames().addValueObserver(newNames -> pageNames = newNames);
      remoteControlBank.pageCount().addValueObserver(pages -> numberOfPages = pages);
      remoteControlBank.selectedPageIndex().addValueObserver(selectedIndex -> pageIndex = selectedIndex);
      cursorDevice.hasPrevious().markInterested();
      cursorDevice.hasNext().markInterested();

      for (int i = 0; i < 8; i++) {
         final int index = i;
         final RgbNoteButton pageButton = buttons[i];
         pageButton.bindPressed(this, () -> {
            remoteControlBank.selectedPageIndex().set(index);
            driver.setTransientText(pageNames[index], deviceName);
         }, () -> index < numberOfPages ? (index == pageIndex ? RgbState.of(53) : RgbState.of(55)) : RgbState.OFF);

         final RgbNoteButton deviceButton = buttons[i + 8];
         final Device device = deviceBank.getDevice(index);
         final BooleanValue deviceMatcher = device.createEqualsValue(cursorDevice);
         deviceMatcher.addValueObserver(eqCursor -> {
            if (eqCursor) {
               selectedDeviceIndex = index;
            }
         });
         final DeviceSlot slot = new DeviceSlot(index, device);
         device.hasDrumPads().addValueObserver(hasPads -> slot.hasDrumPads = hasPads);
         device.exists().addValueObserver(ex -> slot.hasDevice = ex);
         device.deviceType().addValueObserver(type -> slot.type = type);
         device.name().addValueObserver(name -> slot.name = name);
         device.isEnabled().addValueObserver(enabled -> slot.enabled = enabled);
         deviceButton.bindPressed(this, () -> selectDevice(driver, cursorDevice, slot), () -> getDeviceColor(slot));
      }

      host = driver.getHost();
      final RgbCcButton navUpButton = driver.getHwControl().getNavUpButton();
      navUpButton.bindIsPressed(this, pressed -> {
         if (pressed) {
            cursorDevice.selectPrevious();
         }
         cursorUpDown = pressed;
      }, () -> cursorDevice.hasPrevious().get() ? RgbState.WHITE : RgbState.OFF);
      final RgbCcButton navDownButton = driver.getHwControl().getNavDownButton();
      navDownButton.bindIsPressed(this, pressed -> {
         if (pressed) {
            cursorDevice.selectNext();
         }
         cursorDownDown = pressed;
      }, () -> cursorDevice.hasNext().get() ? RgbState.WHITE : RgbState.OFF);

      final RgbCcButton sceneLaunchButton = driver.getHwControl().getSceneLaunchButton();
      sceneLaunchButton.bindIsPressed(this, pressed -> {
         sceneDown = pressed;
      }, RgbState.DIM_WHITE, RgbState.OFF);


      final RgbCcButton modeButton = driver.getHwControl().getModeRow2Button();
      modeButton.bindIsPressed(this, pressed -> {
         modeDown = pressed;
      }, RgbState.DIM_WHITE, RgbState.OFF);
   }

   private void selectDevice(final LaunchkeyMk3Extension driver, final PinnableCursorDevice cursorDevice,
                             final DeviceSlot deviceSlot) {
      if (!deviceSlot.hasDevice) {
         return;
      }
      if (modeDown) {
         deviceSlot.device.isEnabled().toggle();
      } else if (sceneDown) {
         deviceSlot.device.deleteObject();
      } else {
         cursorDevice.selectDevice(deviceSlot.device);
         driver.setTransientText(deviceSlot.name, "");
      }
   }

   private RgbState getDeviceColor(final DeviceSlot slot) {
      if (slot.hasDevice) {
         int color = 57;
         switch (slot.type) {
            case "note-effect":
               color = 17;
               break;
            case "audio_to_audio":
               color = 57;
               break;
            case "instrument":
               if (slot.hasDrumPads) {
                  color = 37;
               } else {
                  color = 9;
               }
               break;
         }
         if (!slot.enabled) {
            return RgbState.of(color - 1);
         }
         if (slot.index == selectedDeviceIndex) {
            return RgbState.of(color);
         }
         return RgbState.of(color + 2);

      }
      return RgbState.OFF;
   }

   @Override
   protected void onDeactivate() {
      super.onDeactivate();
      sceneDown = false;
      modeDown = false;
      cursorUpDown = false;
      cursorDownDown = false;
   }
}
