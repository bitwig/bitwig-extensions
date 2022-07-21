package com.bitwig.extensions.controllers.novation.launchkey_mk3.layer;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.LaunchkeyMk3Extension;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.LcdDisplay;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.control.LcdDeviceParameterBinding;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.control.LcdTrackParameterBinding;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.control.ModeButton;
import com.bitwig.extensions.framework.Layer;

import java.util.HashMap;
import java.util.Map;

public class ControlLayer extends Layer {
   private final Layer deviceLayer;
   private final Layer volumeLayer;
   private final Layer sendsALayer;
   private final Layer sendsBLayer;
   private final Layer panLayer;

   private Layer currentLayer;
   private ControlMode mode = ControlMode.VOLUME;
   private final ControllerHost host;
   private final Map<ControlMode, Layer> layerMap = new HashMap<>();

   public ControlLayer(final String id, final LaunchkeyMk3Extension driver, final AbsoluteHardwareControl[] controls,
                       final int modeButtonCc, final int paramIndex, final ControlMode initMode) {
      super(driver.getLayers(), id + "_CONTROL_LAYER");
      final LcdDisplay lcdDisplay = driver.getLcdDisplay();
      host = driver.getHost();
      final TrackBank trackBank = driver.getTrackBank();
      mode = initMode;
      deviceLayer = new Layer(driver.getLayers(), id + "_DEVICE_LAYER");
      volumeLayer = new Layer(driver.getLayers(), id + "_VOLUME_LAYER");
      sendsALayer = new Layer(driver.getLayers(), id + "_SEND_A_LAYER");
      sendsBLayer = new Layer(driver.getLayers(), id + "_SEND_B_LAYER");
      panLayer = new Layer(driver.getLayers(), id + "_PAN_LAYER");

      final ModeButton deviceModeButton = new ModeButton(id, driver, modeButtonCc, ControlMode.DEVICE);
      final ModeButton volumeModeButton = new ModeButton(id, driver, modeButtonCc, ControlMode.VOLUME);
      final ModeButton sendAModeButton = new ModeButton(id, driver, modeButtonCc, ControlMode.SEND_A);
      final ModeButton sendBModeButton = new ModeButton(id, driver, modeButtonCc, ControlMode.SEND_B);
      final ModeButton panModeButton = new ModeButton(id, driver, modeButtonCc, ControlMode.PAN);

      deviceModeButton.bind(this, this::changeMode);
      volumeModeButton.bind(this, this::changeMode);
      sendAModeButton.bind(this, this::changeMode);
      sendBModeButton.bind(this, this::changeMode);
      panModeButton.bind(this, this::changeMode);
      layerMap.put(ControlMode.DEVICE, deviceLayer);
      layerMap.put(ControlMode.VOLUME, volumeLayer);
      layerMap.put(ControlMode.SEND_A, sendsALayer);
      layerMap.put(ControlMode.SEND_B, sendsBLayer);
      layerMap.put(ControlMode.PAN, panLayer);

      final CursorRemoteControlsPage remoteParameters = driver.getRemoteControlBank();
      for (int i = 0; i < 8; i++) {
         final AbsoluteHardwareControl knob = controls[i];
         final Parameter parameter = remoteParameters.getParameter(i);
         final int index = paramIndex + i;
         final Track track = trackBank.getItemAt(i);
         final Send send1 = track.sendBank().getItemAt(0);
         final Send send2 = track.sendBank().getItemAt(1);
         volumeLayer.bind(knob, track.volume());
         volumeLayer.addBinding(new LcdTrackParameterBinding("Vol", track, track.volume(), lcdDisplay, index));

         panLayer.bind(knob, track.pan());
         panLayer.addBinding(new LcdTrackParameterBinding("Pan", track, track.pan(), lcdDisplay, index));

         sendsALayer.bind(knob, send1);
         sendsALayer.addBinding(new LcdTrackParameterBinding("SendA", track, send1, lcdDisplay, index));

         sendsBLayer.bind(knob, send2);
         sendsBLayer.addBinding(new LcdTrackParameterBinding("SendB", track, send2, lcdDisplay, index));

         deviceLayer.bind(knob, parameter);
         deviceLayer.addBinding(new LcdDeviceParameterBinding(remoteParameters, parameter, lcdDisplay, index));
      }

      currentLayer = layerMap.get(initMode);
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
