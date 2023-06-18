package com.bitwig.extensions.controllers.maudio.oxygenpro;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.maudio.oxygenpro.control.CcButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component
public class TrackControl {
   private Layer mainLayer;
   private Layer currentTrackControlLayer;
   private Layer currentKnobControlLayer;

   private RgbColor[] slotColors = new RgbColor[16];
   private RgbColor trackColor = RgbColor.OFF;
   private int soloHeldCount = 0;
   private int armHeldCount = 0;
   final boolean[] trackSelectionState = new boolean[8];

   private enum KnobMode {
      PAN(OxygenCcAssignments.PAN_MODE),
      DEVICE(OxygenCcAssignments.DEVICE_MODE),
      SENDS(OxygenCcAssignments.SENDS_MODE);
      private OxygenCcAssignments assignment;

      KnobMode(OxygenCcAssignments assignment) {
         this.assignment = assignment;
      }
   }

   private enum TrackButtonMode {
      REC(OxygenCcAssignments.REC_MODE),
      SELECT(OxygenCcAssignments.SELECT_MODE),
      MUTE(OxygenCcAssignments.MUTE_MODE),
      SOLO(OxygenCcAssignments.SOLO_MODE),
      OXY(OxygenCcAssignments.OXY_MODE);

      private OxygenCcAssignments assignment;

      TrackButtonMode(OxygenCcAssignments assignment) {
         this.assignment = assignment;
      }
   }

   private Map<TrackButtonMode, Layer> trackLayers = new HashMap<>();
   private Map<KnobMode, Layer> knobLayers = new HashMap<>();

   public TrackControl(Layers layers, HwElements hwElements, ViewControl viewControl, OxyConfig config) {
      Arrays.fill(slotColors, RgbColor.OFF);
      this.mainLayer = new Layer(layers, "SESSION_LAYER");
      TrackBank trackBank = viewControl.getMixerTrackBank();
      Arrays.stream(TrackButtonMode.values()).forEach(mode -> addTrackMode(layers, hwElements, mode));
      Arrays.stream(KnobMode.values()).forEach(mode -> addKnobMode(layers, hwElements, mode));

      currentTrackControlLayer = trackLayers.get(TrackButtonMode.REC);
      currentKnobControlLayer = knobLayers.get(KnobMode.DEVICE);

      int numberOfControls = config.getNumberOfControls();
      for (int ti = 0; ti < numberOfControls; ti++) {
         final int trackIndex = ti;
         Track track = trackBank.getItemAt(trackIndex);
         track.arm().markInterested();
         track.mute().markInterested();
         track.solo().markInterested();
         track.exists().markInterested();
         track.addIsSelectedInMixerObserver(selected -> this.trackSelectionState[trackIndex] = selected);
         SendBank sendBank = track.sendBank();

         mainLayer.bind(hwElements.getSlider(trackIndex), track.volume());
         knobLayers.get(KnobMode.PAN).bind(hwElements.getKnob(trackIndex), track.pan());
         knobLayers.get(KnobMode.SENDS).bind(hwElements.getKnob(trackIndex), track.sendBank().getItemAt(0));
         CcButton button = hwElements.getTrackButtons().get(trackIndex);
         button.bind(trackLayers.get(TrackButtonMode.REC), () -> this.handleRecPressed(track));
         button.bindLight(trackLayers.get(TrackButtonMode.REC), track.arm());
         button.bind(trackLayers.get(TrackButtonMode.SELECT), () -> this.handleSelectPressed(track));
         button.bindLight(trackLayers.get(TrackButtonMode.SELECT), () -> trackSelectionState(track, trackIndex));
         button.bind(trackLayers.get(TrackButtonMode.SOLO), () -> this.handleSoloPressed(track));
         button.bindLight(trackLayers.get(TrackButtonMode.SOLO), track.solo());
         button.bind(trackLayers.get(TrackButtonMode.MUTE), () -> this.handleMutePressed(track));
         button.bindLight(trackLayers.get(TrackButtonMode.MUTE), track.mute());
      }

      final PinnableCursorDevice cursorDevice = viewControl.getCursorDevice();
      CursorRemoteControlsPage parameterBank = cursorDevice.createCursorRemoteControlsPage(numberOfControls);
      for (int i = 0; i < numberOfControls; i++) {
         AbsoluteHardwareKnob knob = hwElements.getKnob(i);
         knobLayers.get(KnobMode.DEVICE).bind(knob, parameterBank.getParameter(i));
      }
      HardwareSlider masterSlider = hwElements.getMasterSlider();
      if (masterSlider != null) {
         mainLayer.bind(masterSlider, viewControl.getMasterTrack().volume());
      }
   }

   private void handleMutePressed(Track track) {
      track.mute().toggle();
   }

   private void handleRecPressed(Track track) {
      track.arm().toggle();
   }

   private void handleSoloPressed(Track track) {
      track.solo().toggle();
   }

   private void handleSelectPressed(Track track) {
      track.selectInEditor();
   }

   private boolean trackSelectionState(Track track, int index) {
      if (!track.exists().get()) {
         return false;
      }
      return trackSelectionState[index];
   }

   private void addTrackMode(Layers layers, HwElements hwElements, TrackButtonMode mode) {
      Layer layer = new Layer(layers, mode.toString() + "_MODE");
      trackLayers.put(mode, layer);
      CcButton button = hwElements.getButton(mode.assignment);
      button.bindPressed(mainLayer, () -> setToTrackMode(mode));
   }

   private void addKnobMode(Layers layers, HwElements hwElements, KnobMode mode) {
      Layer layer = new Layer(layers, mode.toString() + "_MODE");
      knobLayers.put(mode, layer);
      CcButton button = hwElements.getButton(mode.assignment);
      button.bindPressed(mainLayer, () -> selectKnobMode(mode));
   }

   private void selectKnobMode(KnobMode mode) {
      currentKnobControlLayer.setIsActive(false);
      currentTrackControlLayer = knobLayers.get(mode);
      currentTrackControlLayer.setIsActive(true);
      // TODO check how Ableton handles sends
   }

   private void setToTrackMode(TrackButtonMode mode) {
      currentTrackControlLayer.setIsActive(false);
      currentTrackControlLayer = trackLayers.get(mode);
      currentTrackControlLayer.setIsActive(true);
   }

   @Activate
   public void onActivate() {
      this.mainLayer.setIsActive(true);
      currentTrackControlLayer.setIsActive(true);
      currentKnobControlLayer.setIsActive(true);
   }
}
