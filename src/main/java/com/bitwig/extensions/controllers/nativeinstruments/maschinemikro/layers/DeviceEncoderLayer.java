package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.nativeinstruments.commons.ColorBrightness;
import com.bitwig.extensions.controllers.nativeinstruments.commons.Colors;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.CcAssignment;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.HwElements;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.RgbColor;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.ViewControl;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.buttons.RgbButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;
import com.bitwig.extensions.framework.values.BooleanValueObject;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class DeviceEncoderLayer extends Layer {

   private static final RgbColor[] PARAM_COLORS = {RgbColor.PURPLE, RgbColor.PURPLE, RgbColor.PURPLE, RgbColor.PURPLE, RgbColor.PURPLE, RgbColor.PURPLE, RgbColor.PURPLE, RgbColor.PURPLE};
   private static Map<String, RgbColor> DEVICE_TYPE_TO_COLOR = Map.of("note_effect", RgbColor.RED, //
      "instrument", RgbColor.GREEN, "audio_to_audio", RgbColor.of(Colors.CYAN));

   private final BooleanValueObject shiftHeld;
   private final PinnableCursorDevice cursorDevice;
   private final Layer pageLayer;
   private int pageCount = 0;
   private int pageIndex = -1;
   private int selectedDeviceIndex;
   private boolean encoderIsTouched;
   private int selectedParameter = -1;
   private final CursorRemoteControlsPage remoteBank;
   private final RgbColor[] deviceColors = new RgbColor[8];
   private TouchStripLayer touchStripLayer;

   public DeviceEncoderLayer(Layers layers, HwElements hwElements, ViewControl viewControl, ModifierLayer modifierLayer,
                             Transport transport, ControllerHost host) {
      super(layers, "DEVICE_ENCODER_LAYER");
      pageLayer = new Layer(layers, "ENCODER_PARAMETER_PAGE_LAYER");
      Arrays.fill(deviceColors, RgbColor.OFF);

      this.shiftHeld = modifierLayer.getShiftHeld();
      CursorTrack cursorTrack = viewControl.getCursorTrack();
      hwElements.bindEncoder(this, hwElements.getMainEncoder(), dir -> handleEncoder(dir));
      hwElements.getButton(CcAssignment.ENCODER_TOUCH).bindIsPressed(this, this::handleEncoderTouch);
      hwElements.getButton(CcAssignment.ENCODER_PRESS).bindIsPressed(this, this::handleEncoderPressed);

      cursorDevice = viewControl.getCursorDevice();
      remoteBank = cursorDevice.createCursorRemoteControlsPage(8);
      remoteBank.selectedPageIndex().addValueObserver(pageIndex -> this.pageIndex = pageIndex);
      remoteBank.pageCount().addValueObserver(pageCount -> this.pageCount = pageCount);
      DeviceBank deviceBank = cursorTrack.createDeviceBank(8);

      List<RgbButton> gridButtons = hwElements.getPadButtons();
      for (int i = 0; i < 8; i++) {
         final int index = i;
         Device bankDevice = deviceBank.getDevice(index);
         bankDevice.exists().markInterested();
         bankDevice.deviceType().addValueObserver(deviceType -> handleDeviceTypeChanged(index, deviceType));
         BooleanValue onCursor = bankDevice.createEqualsValue(cursorDevice);
         onCursor.addValueObserver(isOnCursor -> handleDeviceSelection(index, isOnCursor));
         RgbButton button = gridButtons.get(i);
         button.bindLight(this, () -> deviceColor(index, bankDevice));
         button.bindPressed(this, () -> selectDevice(index, bankDevice));

         button.bindLight(pageLayer, () -> parameterPageColor(index));
         button.bindPressed(pageLayer, () -> selectRemotePage(index));
      }

      for (int i = 8; i < 16; i++) {
         final int index = i - 8;
         RgbButton button = gridButtons.get(i);
         RemoteControl parameter = remoteBank.getParameter(index);
         parameter.value().markInterested();
         parameter.exists().markInterested();
         button.bindLight(this, () -> paramSelectionColor(index, parameter));
         button.bindPressed(this, () -> selectedParameter(index));
      }
   }

   @Inject
   public void setTouchStripLayer(TouchStripLayer touchStripLayer) {
      this.touchStripLayer = touchStripLayer;
   }

   private void handleEncoderPressed(Boolean pressed) {
      if (!pressed) {
         return;
      }
      if (selectedParameter == -1) {
         pageLayer.toggleIsActive();
      } else {
         RemoteControl parameter = remoteBank.getParameter(selectedParameter);
         parameter.reset();
      }
   }

   private void selectRemotePage(int index) {
      remoteBank.selectedPageIndex().set(index);
   }

   private RgbColor parameterPageColor(int index) {
      if (index >= pageCount) {
         return RgbColor.OFF;
      }
      if (index == pageIndex) {
         return RgbColor.WHITE.brightness(ColorBrightness.BRIGHT);
      }
      return RgbColor.WHITE.brightness(ColorBrightness.DARKENED);
   }

   private void handleDeviceTypeChanged(int index, String deviceType) {
      deviceColors[index] = DEVICE_TYPE_TO_COLOR.getOrDefault(deviceType, RgbColor.ORANGE);
   }

   private void handleDeviceSelection(int index, boolean isOnCursor) {
      if (isOnCursor) {
         this.selectedDeviceIndex = index;
      }
   }

   private void handleEncoderTouch(boolean touched) {
      encoderIsTouched = touched;
      if (selectedParameter != -1) {
         RemoteControl parameter = remoteBank.getParameter(selectedParameter);
         parameter.touch(touched);
      }
   }

   private void selectedParameter(int index) {
      if (selectedParameter != -1) {
         RemoteControl previousParameter = remoteBank.getParameter(selectedParameter);
         previousParameter.touch(false);
      }
      if (selectedParameter == index) {
         selectedParameter = -1;
      } else {
         selectedParameter = index;
         this.touchStripLayer.setCurrentParameter(remoteBank.getParameter(selectedParameter));
      }
   }

   private RgbColor paramSelectionColor(int index, Parameter parameter) {
      if (parameter.exists().get()) {
         return selectedParameter == index ? PARAM_COLORS[index].brightness(
            ColorBrightness.BRIGHT) : PARAM_COLORS[index];
      }
      return RgbColor.WHITE;
   }

   private RgbColor deviceColor(int index, Device device) {
      if (device.exists().get()) {
         return index == selectedDeviceIndex ? deviceColors[index].brightness(
            ColorBrightness.BRIGHT) : deviceColors[index];
      }
      return RgbColor.WHITE;
   }

   private void selectDevice(int index, Device bankDevice) {
      cursorDevice.selectDevice(bankDevice);
      bankDevice.selectInEditor();
   }

   private void handleEncoder(int diff) {
      if (selectedParameter != -1) {
         RemoteControl parameter = remoteBank.getParameter(selectedParameter);
         parameter.touch(encoderIsTouched);
         if (shiftHeld.get()) {
            parameter.value().inc(diff * 0.01);
         } else {
            parameter.value().inc(diff * 0.025);
         }
      } else {
         if (diff > 0) {
            remoteBank.selectNext();
         } else {
            remoteBank.selectPrevious();
         }
      }
   }

   @Override
   protected void onDeactivate() {
      super.onDeactivate();
      if (selectedParameter != -1) {
         RemoteControl previousParameter = remoteBank.getParameter(selectedParameter);
         previousParameter.touch(false);
      }
   }
}
