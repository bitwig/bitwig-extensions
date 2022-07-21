package com.bitwig.extensions.controllers.novation.launchkey_mk3.layer;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.HwControls;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.LaunchkeyMk3Extension;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.LcdDisplay;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.control.LcdDeviceParameterBinding;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.control.LcdTrackParameterBinding;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.control.ModeButton;
import com.bitwig.extensions.framework.Layer;

import java.util.HashMap;
import java.util.Map;

public class FaderLayer extends Layer {
   private final Layer deviceLayer;
   private final Layer volumeLayer;
   private final Layer sendALayer;
   private final Layer sendBLayer;

   private Layer currentLayer;
   private ControlMode mode = ControlMode.VOLUME;
   private final ControllerHost host;
   private final Map<ControlMode, Layer> layerMap = new HashMap<>();

   public FaderLayer(final LaunchkeyMk3Extension driver) {
      super(driver.getLayers(), "CONTROL_LAYER");
      final HwControls hwControl = driver.getHwControl();
      final LcdDisplay lcdDisplay = driver.getLcdDisplay();
      host = driver.getHost();
      final TrackBank trackBank = driver.getTrackBank();
      final CursorTrack cursorTrack = driver.getCursorTrack();
      deviceLayer = new Layer(driver.getLayers(), "DEVICE_LAYER");
      volumeLayer = new Layer(driver.getLayers(), "VOLUME_LAYER");
      sendALayer = new Layer(driver.getLayers(), "SEND_A_LAYER");
      sendBLayer = new Layer(driver.getLayers(), "SEND_B_LAYER");

      final ModeButton deviceModeButton = new ModeButton("FADER", driver, 10, ControlMode.DEVICE);
      final ModeButton volumeModeButton = new ModeButton("FADER", driver, 10, ControlMode.VOLUME);
      final ModeButton sendAModeButton = new ModeButton("FADER", driver, 10, ControlMode.SEND_A);
      final ModeButton sendBModeButton = new ModeButton("FADER", driver, 10, ControlMode.SEND_B);
      deviceModeButton.bind(this, this::changeMode);
      volumeModeButton.bind(this, this::changeMode);
      sendAModeButton.bind(this, this::changeMode);
      sendBModeButton.bind(this, this::changeMode);
      layerMap.put(ControlMode.DEVICE, deviceLayer);
      layerMap.put(ControlMode.VOLUME, volumeLayer);
      layerMap.put(ControlMode.SEND_A, sendALayer);
      layerMap.put(ControlMode.SEND_B, sendBLayer);

      final CursorRemoteControlsPage remoteParameters = driver.getRemoteControlBank();
      for (int i = 0; i < 8; i++) {
         final HardwareSlider slider = hwControl.getSliders()[i];
         final Parameter parameter = remoteParameters.getParameter(i);
         final int index = 80 + i;
         final Track track = trackBank.getItemAt(i);
         final Send send1 = track.sendBank().getItemAt(0);
         final Send send2 = track.sendBank().getItemAt(1);
         volumeLayer.bind(slider, track.volume());
         volumeLayer.addBinding(new LcdTrackParameterBinding("Vol", track, track.volume(), lcdDisplay, index));

         sendALayer.bind(slider, send1);
         sendALayer.addBinding(new LcdTrackParameterBinding("SendA", track, send1, lcdDisplay, index));

         sendBLayer.bind(slider, send2);
         sendBLayer.addBinding(new LcdTrackParameterBinding("SendB", track, send2, lcdDisplay, index));

         deviceLayer.bind(slider, parameter);
         deviceLayer.addBinding(new LcdDeviceParameterBinding(remoteParameters, parameter, lcdDisplay, index));
      }

      currentLayer = volumeLayer;
   }

   private void changeMode(final ControlMode newMode) {
      if (mode != newMode) {
         mode = newMode;
         currentLayer.setIsActive(false);
         currentLayer = layerMap.get(newMode);
         currentLayer.setIsActive(true);
      }
   }

   @Override
   protected void onActivate() {
      super.onActivate();
      currentLayer.setIsActive(true);
   }

   @Override
   protected void onDeactivate() {
      super.onDeactivate();
      currentLayer.setIsActive(false);
   }

}
