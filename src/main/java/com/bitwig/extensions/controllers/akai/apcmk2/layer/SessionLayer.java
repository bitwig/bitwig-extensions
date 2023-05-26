package com.bitwig.extensions.controllers.akai.apcmk2.layer;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.akai.apcmk2.ApcConfiguration;
import com.bitwig.extensions.controllers.akai.apcmk2.ModifierStates;
import com.bitwig.extensions.controllers.akai.apcmk2.PanelLayout;
import com.bitwig.extensions.controllers.akai.apcmk2.ViewControl;
import com.bitwig.extensions.controllers.akai.apcmk2.control.HardwareElementsApc;
import com.bitwig.extensions.controllers.akai.apcmk2.control.RgbButton;
import com.bitwig.extensions.controllers.akai.apcmk2.control.SingleLedButton;
import com.bitwig.extensions.controllers.akai.apcmk2.led.LedBehavior;
import com.bitwig.extensions.controllers.akai.apcmk2.led.RgbLightState;
import com.bitwig.extensions.controllers.akai.apcmk2.led.SingleLedState;
import com.bitwig.extensions.controllers.novation.commonsmk3.ColorLookup;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Inject;
import com.bitwig.extensions.framework.di.PostConstruct;

public class SessionLayer extends Layer {

   @Inject
   private ViewControl viewControl;
   @Inject
   private Transport transport;
   @Inject
   private ModifierStates modifiers;
   private SettableBooleanValue clipLauncherOverdub;
   private int sceneOffset;
   private PanelLayout panelLayout = PanelLayout.VERTICAL;
   private Layer verticalLayer;
   private Layer horizontalLayer;
   private TrackBank trackBankVertical;
   private TrackBank trackBankHorizontal;

   protected final int[][] colorIndex = new int[8][8];

   public SessionLayer(final Layers layers) {
      super(layers, "SESSION_LAYER");
      this.horizontalLayer = new Layer(layers, "HORIZONTAL_LAYER");
      this.verticalLayer = new Layer(layers, "VERTICAL_LAYER");
   }

   @PostConstruct
   void init(ApcConfiguration configuration, HardwareElementsApc hwElements, Application application) {
      //application.panelLayout().addValueObserver(this::handlePanelLayoutChanged);
      initClipControl(configuration.getSceneRows(), hwElements);
   }

   private void initClipControl(int numberOfScenes, final HardwareElementsApc hwElements) {
      clipLauncherOverdub = transport.isClipLauncherOverdubEnabled();
      clipLauncherOverdub.markInterested();
      transport.isPlaying().markInterested();
      trackBankVertical = viewControl.getTrackBank();
      trackBankVertical.setShouldShowClipLauncherFeedback(true);
      initVerticalControl(numberOfScenes, hwElements, trackBankVertical);
      trackBankHorizontal = viewControl.getTrackBankHorizontal();
      initHorizontalControl(numberOfScenes, hwElements, trackBankHorizontal);

      final SceneBank sceneBank = trackBankVertical.sceneBank();
      final Scene targetScene = trackBankVertical.sceneBank().getScene(0);
      targetScene.clipCount().markInterested();
      sceneBank.setIndication(true);
      sceneBank.scrollPosition().addValueObserver(value -> sceneOffset = value);

      for (int sceneIndex = 0; sceneIndex < numberOfScenes; sceneIndex++) {
         final SingleLedButton sceneButton = hwElements.getSceneButton(sceneIndex);
         final int index = sceneIndex;
         final Scene scene = sceneBank.getScene(index);
         scene.clipCount().markInterested();
         sceneButton.bindPressed(this, () -> handleScenePressed(scene, index));
         sceneButton.bindPressed(this, () -> handleSceneReleased(scene, index));
         sceneButton.bindLight(this, () -> getSceneColor(index, scene));
      }
   }

   private void initVerticalControl(int numberOfScenes, HardwareElementsApc hwElements, TrackBank trackBank) {
      for (int i = 0; i < 8; i++) {
         final int trackIndex = i;
         final Track track = trackBank.getItemAt(trackIndex);
         markTrack(track);
         for (int j = 0; j < numberOfScenes; j++) {
            final int sceneIndex = j;
            final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(sceneIndex);
            prepareSlot(slot, sceneIndex, trackIndex);

            final RgbButton button = hwElements.getGridButton(sceneIndex, trackIndex);
            button.bindPressed(verticalLayer, () -> handleSlotPressed(track, slot));
            button.bindRelease(verticalLayer, () -> handleSlotReleased(track, slot));
            button.bindLight(verticalLayer, () -> getState(track, slot, trackIndex, sceneIndex));
         }
      }
   }

   private void initHorizontalControl(int numberOfScenes, HardwareElementsApc hwElements, TrackBank trackBank) {
      for (int i = 0; i < numberOfScenes; i++) {
         final int trackIndex = i;
         final Track track = trackBank.getItemAt(trackIndex);
         markTrack(track);
         for (int j = 0; j < 8; j++) {
            final int sceneIndex = j;
            final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(sceneIndex);
            prepareSlot(slot, sceneIndex, trackIndex);

            final RgbButton button = hwElements.getGridButton(trackIndex, sceneIndex);
            button.bindPressed(horizontalLayer, () -> handleSlotPressed(track, slot));
            button.bindRelease(horizontalLayer, () -> handleSlotReleased(track, slot));
            button.bindLight(horizontalLayer, () -> getState(track, slot, trackIndex, sceneIndex));
         }
      }
   }

   private void handlePanelLayoutChanged(final String panelLayout) {
      if (panelLayout.equals("MIX")) {
         setLayout(PanelLayout.VERTICAL);
         trackBankHorizontal.setShouldShowClipLauncherFeedback(false);
         trackBankVertical.setShouldShowClipLauncherFeedback(true);
      } else {
         setLayout(PanelLayout.HORIZONTAL);
         trackBankHorizontal.setShouldShowClipLauncherFeedback(true);
         trackBankVertical.setShouldShowClipLauncherFeedback(false);
      }
   }

   public void setLayout(final PanelLayout layout) {
      panelLayout = layout;
      horizontalLayer.setIsActive(panelLayout == PanelLayout.HORIZONTAL);
      verticalLayer.setIsActive(panelLayout == PanelLayout.VERTICAL);
   }

   private void handleScenePressed(final Scene scene, final int index) {
      viewControl.focusScene(index + sceneOffset);
      if (modifiers.isShift()) {
         scene.launchAlt();
      } else {
         scene.launch();
      }
   }

   private void handleSceneReleased(final Scene scene, final int index) {
      if (modifiers.isShift()) {
         scene.launchReleaseAlt();
      } else {
         scene.launchRelease();
      }
   }

   private SingleLedState getSceneColor(final int index, final Scene scene) {
      if (scene.clipCount().get() > 0) {
         if (sceneOffset + index == viewControl.getFocusSceneIndex() && viewControl.hasQueuedForPlaying()) {
            return SingleLedState.BLINK;
         }
         return SingleLedState.OFF;
      }
      return SingleLedState.OFF;
   }

   private void handleSlotPressed(final Track track, final ClipLauncherSlot slot) {
      if (modifiers.isShift()) {
         slot.launchAlt();
      } else {
         slot.launch();
      }
   }

   private void handleSlotReleased(final Track track, final ClipLauncherSlot slot) {
      if (modifiers.isShift()) {
         slot.launchReleaseAlt();
      } else {
         slot.launchRelease();
      }
   }

   private RgbLightState getState(final Track track, final ClipLauncherSlot slot, final int trackIndex,
                                  final int sceneIndex) {
      if (slot.hasContent().get()) {
         final int color = colorIndex[sceneIndex][trackIndex];
         if (slot.isRecordingQueued().get()) {
            return RgbLightState.RED.behavior(LedBehavior.BLINK_8);
         } else if (slot.isRecording().get()) {
            return RgbLightState.RED.behavior(LedBehavior.PULSE_2);
         } else if (slot.isPlaybackQueued().get()) {
            return RgbLightState.of(color, LedBehavior.BLINK_8);
         } else if (slot.isStopQueued().get()) {
            return RgbLightState.GREEN_PLAY.behavior(LedBehavior.BLINK_16);
         } else if (slot.isPlaying().get() && track.isQueuedForStop().get()) {
            return RgbLightState.GREEN.behavior(LedBehavior.BLINK_16);
         } else if (slot.isPlaying().get()) {
            if (clipLauncherOverdub.get() && track.arm().get()) {
               return RgbLightState.RED.behavior(LedBehavior.PULSE_8);
            } else {
               if (transport.isPlaying().get()) {
                  return RgbLightState.GREEN_PLAY;
               }
               return RgbLightState.GREEN;
            }
         }
         return RgbLightState.of(color);
      }
      if (slot.isRecordingQueued().get()) {
         return RgbLightState.RED.behavior(LedBehavior.BLINK_8); // Possibly Track Color
      } else if (track.arm().get()) {
         return RgbLightState.RED.behavior(LedBehavior.LIGHT_25);
      }
      return RgbLightState.OFF;
   }


   private void markTrack(final Track track) {
      track.isStopped().markInterested();
      track.mute().markInterested();
      track.solo().markInterested();
      track.isQueuedForStop().markInterested();
      track.arm().markInterested();
   }

   private void prepareSlot(final ClipLauncherSlot slot, final int sceneIndex, final int trackIndex) {
      slot.hasContent().markInterested();
      slot.isPlaying().markInterested();
      slot.isStopQueued().markInterested();
      slot.isRecordingQueued().markInterested();
      slot.isRecording().markInterested();
      slot.isPlaybackQueued().markInterested();
      slot.color().addValueObserver((r, g, b) -> colorIndex[sceneIndex][trackIndex] = ColorLookup.toColor(r, g, b));
   }

   @Override
   protected void onActivate() {
      super.onActivate();
      horizontalLayer.setIsActive(panelLayout == PanelLayout.HORIZONTAL);
      verticalLayer.setIsActive(panelLayout == PanelLayout.VERTICAL);
   }

   @Override
   protected void onDeactivate() {
      super.onDeactivate();
      horizontalLayer.setIsActive(false);
      verticalLayer.setIsActive(false);
   }

}
