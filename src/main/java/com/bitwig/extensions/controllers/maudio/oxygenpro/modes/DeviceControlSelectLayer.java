package com.bitwig.extensions.controllers.maudio.oxygenpro.modes;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.maudio.oxygenpro.HwElements;
import com.bitwig.extensions.controllers.maudio.oxygenpro.OxyConfig;
import com.bitwig.extensions.controllers.maudio.oxygenpro.RgbColor;
import com.bitwig.extensions.controllers.maudio.oxygenpro.ViewControl;
import com.bitwig.extensions.controllers.maudio.oxygenpro.control.PadButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;

import java.util.List;
import java.util.Map;

@Component
public class DeviceControlSelectLayer extends Layer {

   private static Map<String, RgbColor> DEVICE_TYPE_TO_COLOR = Map.of("note-effect", RgbColor.MAGENTA, //
      "instrument", RgbColor.GREEN, "audio_to_audio", RgbColor.BLUE);

   private final DeviceBank deviceBank;
   private final RgbColor[] deviceColors = new RgbColor[8];
   private final CursorRemoteControlsPage parameterBank;
   private final PinnableCursorDevice cursorDevice;
   private int selectedPageIndex;
   private int selectedDeviceIndex;

   public DeviceControlSelectLayer(Layers layers, HwElements hwElements, ViewControl viewControl, OxyConfig config) {
      super(layers, "DEVICE_CONTROL_LAYER");

      CursorTrack cursorTrack = viewControl.getCursorTrack();
      cursorDevice = viewControl.getCursorDevice();
      deviceBank = cursorTrack.createDeviceBank(config.getNumberOfControls());
      deviceBank.scrollPosition().markInterested();
      parameterBank = viewControl.getParameterBank();
      parameterBank.pageCount().markInterested();
      parameterBank.selectedPageIndex().addValueObserver(pageIndex -> this.selectedPageIndex = pageIndex);

      List<PadButton> gridButtons = hwElements.getPadButtons();
      for (int i = 0; i < config.getNumberOfControls(); i++) {
         final int index = i;
         PadButton button = gridButtons.get(i);
         Device device = deviceBank.getDevice(index);
         device.exists().markInterested();
         device.deviceType().addValueObserver(deviceType -> handleDeviceType(index, deviceType));
         BooleanValue onCursor = device.createEqualsValue(cursorDevice);
         onCursor.addValueObserver(isOnCursor -> handleSelected(index, isOnCursor));
         button.bindLight(this, () -> this.getRgbState(index, device));
         button.bindPressed(this, () -> this.handleDeviceSelection(index, device));
      }
      for (int i = config.getNumberOfControls(); i < config.getNumberOfControls() * 2; i++) {
         int index = i - config.getNumberOfControls();
         PadButton button = gridButtons.get(i);
         button.bindLight(this, () -> this.getParameterPageState(index));
         button.bindPressed(this, () -> this.handleParameterPageSelect(index));
      }
   }

   private void handleParameterPageSelect(int index) {
      parameterBank.selectedPageIndex().set(index);
   }

   private InternalHardwareLightState getParameterPageState(int index) {
      if (index < parameterBank.pageCount().get()) {
         if (index == selectedPageIndex) {
            return RgbColor.WHITE;
         }
         return RgbColor.AZURE;
      }
      return RgbColor.OFF;
   }

   private void handleSelected(int index, boolean isOnCursor) {
      if (isOnCursor) {
         this.selectedDeviceIndex = index;
      }
   }

   private void handleDeviceType(int index, String deviceType) {
      deviceColors[index] = DEVICE_TYPE_TO_COLOR.getOrDefault(deviceType, RgbColor.MAGENTA);
   }

   private void handleDeviceSelection(int index, Device device) {
      cursorDevice.selectDevice(device);
      device.selectInEditor();
   }

   private InternalHardwareLightState getRgbState(int index, Device device) {
      if (!device.exists().get()) {
         return RgbColor.OFF;
      }
      if (index == selectedDeviceIndex) {
         return RgbColor.WHITE;
      }
      return deviceColors[index];
   }

   @Override
   protected void onActivate() {
      super.onActivate();
   }

   @Override
   protected void onDeactivate() {
      super.onDeactivate();
   }


}
