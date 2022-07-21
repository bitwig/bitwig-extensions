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

public class KnobLayer extends Layer {
   private final Layer deviceLayer;
   private final Layer volumeLayer;
   private final Layer sendsLayer;
   private final Layer panLayer;

   private Layer currentLayer;
   private ControlMode mode = ControlMode.VOLUME;
   private final ControllerHost host;
   private final Map<ControlMode, Layer> layerMap = new HashMap<>();

   public KnobLayer(final LaunchkeyMk3Extension driver) {
      super(driver.getLayers(), "CONTROL_LAYER");
      final HwControls hwControl = driver.getHwControl();
      final LcdDisplay lcdDisplay = driver.getLcdDisplay();
      host = driver.getHost();
      final TrackBank trackBank = driver.getTrackBank();
      final CursorTrack cursorTrack = driver.getCursorTrack();
      deviceLayer = new Layer(driver.getLayers(), "KNOB_DEVICE_LAYER");
      volumeLayer = new Layer(driver.getLayers(), "KNOB_VOLUME_LAYER");
      sendsLayer = new Layer(driver.getLayers(), "KNOB_SEND_LAYER");
      panLayer = new Layer(driver.getLayers(), "KNOB_PAN_LAYER");

      final ModeButton deviceModeButton = new ModeButton("KNOB", driver, 9, ControlMode.DEVICE);
      final ModeButton volumeModeButton = new ModeButton("KNOB", driver, 9, ControlMode.VOLUME);
      final ModeButton sendAModeButton = new ModeButton("KNOB", driver, 9, ControlMode.SEND_A);
      final ModeButton panModeButton = new ModeButton("KNOB", driver, 9, ControlMode.PAN);

      deviceModeButton.bind(this, this::changeMode);
      volumeModeButton.bind(this, this::changeMode);
      sendAModeButton.bind(this, this::changeMode);
      panModeButton.bind(this, this::changeMode);
      layerMap.put(ControlMode.DEVICE, deviceLayer);
      layerMap.put(ControlMode.VOLUME, volumeLayer);
      layerMap.put(ControlMode.SEND_A, sendsLayer);
      layerMap.put(ControlMode.PAN, panLayer);

      final CursorRemoteControlsPage remoteParameters = driver.getRemoteControlBank();
      for (int i = 0; i < 8; i++) {
         final AbsoluteHardwareKnob knob = hwControl.getKnobs()[i];
         final Parameter parameter = remoteParameters.getParameter(i);
         final int index = 56 + i;
         final Track track = trackBank.getItemAt(i);
         final Send send1 = track.sendBank().getItemAt(0);
         volumeLayer.bind(knob, track.volume());
         volumeLayer.addBinding(new LcdTrackParameterBinding("Vol", track, track.volume(), lcdDisplay, index));

         panLayer.bind(knob, track.pan());
         panLayer.addBinding(new LcdTrackParameterBinding("Pan", track, track.pan(), lcdDisplay, index));

         sendsLayer.bind(knob, send1);
         sendsLayer.addBinding(new LcdTrackParameterBinding("SendA", track, send1, lcdDisplay, index));

         deviceLayer.bind(knob, parameter);
         deviceLayer.addBinding(new LcdDeviceParameterBinding(remoteParameters, parameter, lcdDisplay, index));
      }


      currentLayer = panLayer;
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
