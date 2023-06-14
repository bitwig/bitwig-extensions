package com.bitwig.extensions.controllers.novation.launchpadmini3.layers;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.novation.commonsmk3.GridButton;
import com.bitwig.extensions.controllers.novation.commonsmk3.PanelLayout;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.launchpadmini3.HwElements;
import com.bitwig.extensions.controllers.novation.launchpadmini3.TrackMode;
import com.bitwig.extensions.controllers.novation.launchpadmini3.ViewCursorControl;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

import java.util.HashMap;
import java.util.Map;

public class TrackControlLayer {
   private final Layer muteLayer;
   private final Layer soloLayer;
   private final Layer armLayer;
   private final Layer stopLayer;
   private final Layer controlLayer;
   private final Map<TrackMode, Layer> trackModeLayerMap = new HashMap<>();
   private final PanelLayout layoutType;
   private final Project project;
   private final Transport transport;
   private final SessionLayer sessionLayer;
   private Layer currentControlGridLayer;
   private int soloHeldCount = 0;
   private int armHeldCount = 0;

   TrackControlLayer(final Layers layers, SessionLayer sessionLayer, Transport transport, ControllerHost host,
                     PanelLayout layoutType) {
      this.transport = transport;
      this.sessionLayer = sessionLayer;
      transport.isPlaying().markInterested();
      transport.isClipLauncherOverdubEnabled().markInterested();
      transport.isMetronomeEnabled().markInterested();
      muteLayer = new Layer(layers, "MUTE_LAYER");
      soloLayer = new Layer(layers, "SOLO_LAYER");
      stopLayer = new Layer(layers, "STOP_LAYER");
      armLayer = new Layer(layers, "ARM_LAYER");
      controlLayer = new Layer(layers, "CONTROL_LAYER");
      trackModeLayerMap.put(TrackMode.NONE, null);
      trackModeLayerMap.put(TrackMode.SOLO, soloLayer);
      trackModeLayerMap.put(TrackMode.MUTE, muteLayer);
      trackModeLayerMap.put(TrackMode.ARM, armLayer);
      trackModeLayerMap.put(TrackMode.STOP, stopLayer);
      trackModeLayerMap.put(TrackMode.CONTROL, controlLayer);
      currentControlGridLayer = null;
      project = host.getProject();
      this.layoutType = layoutType;
   }

   void initClipControl(final HwElements hwElements, final TrackBank trackBank) {
      for (int i = 0; i < 8; i++) {
         final Track track = trackBank.getItemAt(i);
         final GridButton button = getButton(hwElements, i);
         button.bindPressed(stopLayer, track::stop);
         button.bindLight(stopLayer, () -> getStopState(track));
         button.bindPressed(muteLayer, () -> track.mute().toggle());
         button.bindLight(muteLayer, () -> getMuteState(track));
         button.bindPressed(soloLayer, () -> handleSolo(true, track));
         button.bindRelease(soloLayer, () -> handleSolo(false, track));
         button.bindLight(soloLayer, () -> getSoloState(track));
         button.bindPressed(armLayer, () -> handleArm(true, track));
         button.bindRelease(armLayer, () -> handleArm(false, track));
         button.bindLight(armLayer, () -> getArmState(track));
      }
   }

   void initControlLayer(final HwElements hwElements, ViewCursorControl viewCursorControl) {
      int index = 0;
      final GridButton playButton = getButton(hwElements, index++);
      playButton.bindPressed(controlLayer, this::togglePlay);
      playButton.bindLight(controlLayer, () -> transport.isPlaying().get() ? RgbState.of(21) : RgbState.of(23));
      final GridButton overButton = getButton(hwElements, index++);

      overButton.bindPressed(controlLayer, () -> viewCursorControl.globalRecordAction(transport));
      overButton.bindLight(controlLayer, sessionLayer::getRecordButtonColorRegular);

      final GridButton metroButton = getButton(hwElements, index++);
      metroButton.bindPressed(controlLayer, () -> transport.isMetronomeEnabled().toggle());
      metroButton.bindLight(controlLayer,
         () -> transport.isMetronomeEnabled().get() ? RgbState.of(37) : RgbState.of(39));
      for (int i = 0; i < 4; i++) {
         final GridButton emptyButton = getButton(hwElements, index++);
         emptyButton.bindPressed(controlLayer, () -> {
         });
         emptyButton.bindLight(controlLayer, () -> RgbState.OFF);
      }
      final GridButton shiftButton = getButton(hwElements, index);
      shiftButton.bindPressed(controlLayer, sessionLayer::setShiftHeld);
      shiftButton.bindLightPressed(controlLayer, RgbState.pulse(2), RgbState.of(3));
   }

   private GridButton getButton(final HwElements hwElements, int index) {
      if (layoutType == PanelLayout.VERTICAL) {
         return hwElements.getGridButton(7, index);
      }
      return hwElements.getGridButton(index, 7);
   }

   public void applyMode(TrackMode trackMode) {
      activateLayer(trackModeLayerMap.get(trackMode));
   }

   void activateLayer(final Layer nextLayer) {
      if (currentControlGridLayer != null) {
         currentControlGridLayer.setIsActive(false);
      }
      currentControlGridLayer = nextLayer;
      if (currentControlGridLayer != null) {
         currentControlGridLayer.setIsActive(true);
      }
   }

   void activateControlLayer(boolean active) {
      if (currentControlGridLayer != null) {
         currentControlGridLayer.setIsActive(active);
      }
   }

   public void deactivateLayer() {
      if (currentControlGridLayer != null) {
         currentControlGridLayer.setIsActive(false);
      }
   }

   public void reset() {
      soloHeldCount = 0;
      armHeldCount = 0;
      deactivateLayer();
      currentControlGridLayer = null;
   }

   private RgbState getMuteState(final Track track) {
      if (track.exists().get()) {
         return track.mute().get() ? RgbState.of(9) : RgbState.of(11);
      }
      return RgbState.OFF;
   }

   private RgbState getSoloState(final Track track) {
      if (track.exists().get()) {
         return track.solo().get() ? RgbState.of(13) : RgbState.of(15);
      }
      return RgbState.OFF;
   }

   private RgbState getArmState(final Track track) {
      if (track.exists().get()) {
         return track.arm().get() ? RgbState.of(5) : RgbState.of(7);
      }
      return RgbState.OFF;
   }


   private RgbState getStopState(final Track track) {
      if (track.exists().get()) {
         if (track.isQueuedForStop().get()) {
            return RgbState.flash(5, 0);
         } else if (track.isStopped().get()) {
            return RgbState.of(7);
         } else {
            return RgbState.RED;
         }
      }
      return RgbState.OFF;
   }

   private void togglePlay() {
      if (sessionLayer.isShiftHeld()) {
         transport.continuePlayback();
      } else {
         if (transport.isPlaying().get()) {
            transport.stop();
         } else {
            transport.togglePlay();
         }

      }
   }

   private void handleSolo(final boolean pressed, final Track track) {
      if (pressed) {
         track.solo().toggle(soloHeldCount == 0);
         soloHeldCount++;
      } else {
         if (soloHeldCount > 0) {
            soloHeldCount--;
         }
      }
   }

   private void handleArm(final boolean pressed, final Track track) {
      if (pressed) {
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
      } else {
         if (armHeldCount > 0) {
            armHeldCount--;
         }
      }
   }

   public void resetCounts() {
      soloHeldCount = 0;
      armHeldCount = 0;
   }


}
