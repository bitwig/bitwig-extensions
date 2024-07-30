package com.bitwig.extensions.controllers.akai.apcmk2.layer;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.akai.apc.common.PanelLayout;
import com.bitwig.extensions.controllers.akai.apcmk2.ViewControl;
import com.bitwig.extensions.controllers.akai.apcmk2.HardwareElementsApc;
import com.bitwig.extensions.controllers.akai.apc.common.control.SingleLedButton;
import com.bitwig.extensions.controllers.akai.apc.common.led.SingleLedState;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.PostConstruct;

import java.util.HashMap;
import java.util.Map;

public class TrackControlLayer {
   private Project project;
   private TrackMode mode = TrackMode.STOP;
   private final Map<TrackMode, Layer> layerMap = new HashMap<>();
   private int soloHeldCount = 0;
   private int armHeldCount = 0;
   final boolean[] trackSelectionState = new boolean[8];
   private PanelLayout panelLayout = PanelLayout.VERTICAL;

   public TrackControlLayer(Layers layers) {
      layerMap.put(TrackMode.STOP, new Layer(layers, "STOP_LAYER"));
      layerMap.put(TrackMode.SOLO, new Layer(layers, "SOLO_LAYER"));
      layerMap.put(TrackMode.MUTE, new Layer(layers, "MUTE_LAYER"));
      layerMap.put(TrackMode.ARM, new Layer(layers, "ARM_LAYER"));
      layerMap.put(TrackMode.SELECT, new Layer(layers, "SELECT_LAYER"));
   }

   @PostConstruct
   public void init(HardwareElementsApc hwElements, ViewControl viewControl, ControllerHost host,
                    Application application) {
      //application.panelLayout().addValueObserver(this::handlePanelLayoutChanged);

      final TrackBank trackBank = viewControl.getTrackBank();
      project = host.getProject();

      for (int i = 0; i < 8; i++) {
         final int index = i;
         final SingleLedButton trackButton = hwElements.getTrackButton(i);
         final Track track = trackBank.getItemAt(i);
         track.mute().markInterested();
         track.solo().markInterested();
         track.arm().markInterested();
         track.isStopped().markInterested();
         track.isQueuedForStop().markInterested();
         track.exists().markInterested();
         track.addIsSelectedInMixerObserver(selected -> this.trackSelectionState[index] = selected);

         final Layer selectLayer = layerMap.get(TrackMode.SELECT);
         trackButton.bindPressed(selectLayer, track::selectInEditor);
         trackButton.bindLight(selectLayer, () -> trackSelectionState(track, index));
         final Layer soloLayer = layerMap.get(TrackMode.SOLO);
         trackButton.bindPressed(soloLayer, () -> handleSoloDown(track));
         trackButton.bindRelease(soloLayer, () -> handleSoloUp(track));
         trackButton.bindLight(soloLayer, () -> track.solo().get() ? SingleLedState.ON : SingleLedState.OFF);
         final Layer muteLayer = layerMap.get(TrackMode.MUTE);
         trackButton.bindPressed(muteLayer, () -> track.mute().toggle());
         trackButton.bindLight(muteLayer, () -> trackMuteState(track));
         final Layer armLayer = layerMap.get(TrackMode.ARM);
         trackButton.bindPressed(armLayer, () -> handleArmDown(track));
         trackButton.bindRelease(armLayer, () -> handleArmUp(track));
         trackButton.bindLight(armLayer, () -> track.arm().get() ? SingleLedState.ON : SingleLedState.OFF);
         final Layer stopLayer = layerMap.get(TrackMode.STOP);
         trackButton.bindPressed(stopLayer, track::stop);
         trackButton.bindLight(stopLayer, () -> trackRunningState(track));
      }
   }

   private void handlePanelLayoutChanged(String panelLayout) {
      if (panelLayout.equals("MIX")) {
         setLayout(PanelLayout.VERTICAL);
      } else {
         setLayout(PanelLayout.HORIZONTAL);
      }
   }

   private void setLayout(PanelLayout layout) {
      panelLayout = layout;
   }

   private SingleLedState trackRunningState(Track track) {
      if (!track.exists().get() || track.isStopped().get()) {
         return SingleLedState.OFF;
      }
      if (track.isQueuedForStop().get()) {
         return SingleLedState.BLINK;
      }
      return SingleLedState.ON;
   }

   private SingleLedState trackMuteState(Track track) {
      if (track.exists().get() && track.mute().get()) {
         return SingleLedState.ON;
      }
      return SingleLedState.OFF;
   }

   private SingleLedState trackSelectionState(Track track, int index) {
      if (!track.exists().get()) {
         return SingleLedState.OFF;
      }
      return trackSelectionState[index] ? SingleLedState.ON : SingleLedState.OFF;
   }

   private void handleSoloDown(Track track) {
      track.solo().toggle(soloHeldCount == 0);
      soloHeldCount++;
   }

   private void handleSoloUp(Track track) {
      if (soloHeldCount > 0) {
         soloHeldCount--;
      }
   }

   private void handleArmDown(Track track) {
      if (armHeldCount == 0) {
         final boolean isArmed = track.arm().get();
         project.unarmAll();
         if (isArmed) {
            track.arm().set(false);
         } else {
            track.arm().set(true);
            track.selectInEditor();
         }
      } else {
         track.arm().toggle();
      }
      armHeldCount++;
   }

   private void handleArmUp(Track track) {
      if (armHeldCount > 0) {
         armHeldCount--;
      }
   }

   @Activate
   public void activate() {
      layerMap.get(mode).setIsActive(true);
   }


   public TrackMode getMode() {
      return mode;
   }

   public void setMode(final TrackMode mode) {
      if (this.mode == mode) {
         return;
      }
      layerMap.get(this.mode).setIsActive(false);
      layerMap.get(mode).setIsActive(true);
      this.mode = mode;
   }

}
