package com.bitwig.extensions.controllers.novation.launchpadmini3.layers;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.novation.commonsmk3.*;
import com.bitwig.extensions.controllers.novation.launchpadmini3.*;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.FocusSlot;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Inject;
import com.bitwig.extensions.framework.di.PostConstruct;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SessionLayer extends AbstractLpSessionLayer {

   public static final int MOMENTARY_TIME = 500;

   private final int[] sceneColorIndex = new int[8];
   private int soloHeldCount = 0;
   private int armHeldCount = 0;
   private final Map<ControlMode, AbstractSliderLayer> controlSliderLayers = new HashMap<>();
   private int sceneOffset;

   private LpMode lpMode = LpMode.SESSION;
   private ControlMode controlMode = ControlMode.NONE;
   private TrackMode trackMode = TrackMode.NONE;

   private ControlMode stashedControlMode = ControlMode.NONE;
   private TrackMode stashedTrackMode = TrackMode.NONE;

   private final Layer muteLayer;
   private final Layer soloLayer;
   private final Layer armLayer;
   private final Layer stopLayer;
   private final Layer controlLayer;

   private final Layer sceneTrackControlLayer;
   private final Layer sceneControlLayer;

   private Layer currentBottomRowLayer;
   private SendsSliderLayer sendsSliderLayer;

   private Clip cursorClip;

   @Inject
   private ViewCursorControl viewCursorControl;
   @Inject
   private MidiProcessor midiProcessor;
   @Inject
   private LaunchpadDeviceConfig config;
   @Inject
   private Transport transport;

   private boolean shiftHeld;

   private Project project;
   private final Map<TrackMode, Layer> trackModeLayerMap = new HashMap<>();
   private LabeledButton modeButton = null;

   private final List<TrackMode> miniModeSequenceXtra = List.of(TrackMode.NONE, TrackMode.STOP, TrackMode.SOLO,
      TrackMode.MUTE, TrackMode.CONTROL);
   private final List<TrackMode> miniModeSequence = List.of(TrackMode.NONE, TrackMode.STOP, TrackMode.SOLO,
      TrackMode.MUTE);

   public SessionLayer(final Layers layers) {
      super(layers);
      sceneControlLayer = new Layer(layers, "SCENE_CONTROL");
      sceneTrackControlLayer = new Layer(layers, "SCENE_TRACK_CONTROL");

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
      currentBottomRowLayer = null;
   }

   @PostConstruct
   protected void init(final ControllerHost host, final Transport transport, final HwElements hwElements) {
      clipLauncherOverdub = transport.isClipLauncherOverdubEnabled();
      clipLauncherOverdub.markInterested();
      project = host.getProject();
      cursorClip = viewCursorControl.getCursorClip();

      final Clip cursorClip = viewCursorControl.getCursorClip();
      cursorClip.getLoopLength().markInterested();

      final TrackBank trackBank = viewCursorControl.getTrackBank();
      trackBank.setShouldShowClipLauncherFeedback(true);

      final SceneBank sceneBank = trackBank.sceneBank();
      final Scene targetScene = trackBank.sceneBank().getScene(0);
      targetScene.clipCount().markInterested();
      initClipControl(hwElements, trackBank);
      initNavigation(hwElements, trackBank, sceneBank);
      trackBank.setShouldShowClipLauncherFeedback(true);
      initSceneControl(hwElements, sceneBank);
      if (config.isMiniVersion()) {
         initTrackControlSceneButtons(hwElements, sceneTrackControlLayer);
      } else {
         initTrackControlXSceneButtons(hwElements, sceneTrackControlLayer);
      }
   }

   @Activate
   public void activation() {
      setIsActive(true);
      if (modeButton != null) {
         modeButton.refresh();
      }
   }

   public void setShiftHeld(boolean value) {
      this.shiftHeld = value;
   }

   public void registerControlLayer(final ControlMode controlMode, final AbstractSliderLayer sliderLayer) {
      controlSliderLayers.put(controlMode, sliderLayer);
      if (controlMode == ControlMode.SENDS && sliderLayer instanceof SendsSliderLayer) {
         sendsSliderLayer = (SendsSliderLayer) sliderLayer;
         sendsSliderLayer.addControlModeRemoveListener(this::sendRemoved);
      }
   }

   private void initNavigation(final HwElements hwElements, final TrackBank trackBank, final SceneBank sceneBank) {
      final LabeledButton upButton = hwElements.getLabeledButton(LabelCcAssignmentsMini.UP);
      final LabeledButton downButton = hwElements.getLabeledButton(LabelCcAssignmentsMini.DOWN);
      final LabeledButton leftButton = hwElements.getLabeledButton(LabelCcAssignmentsMini.LEFT);
      final LabeledButton rightButton = hwElements.getLabeledButton(LabelCcAssignmentsMini.RIGHT);
      sceneBank.canScrollForwards().markInterested();
      sceneBank.canScrollBackwards().markInterested();
      trackBank.canScrollForwards().markInterested();
      trackBank.canScrollBackwards().markInterested();
      final RgbState baseColor = RgbState.of(1);
      final RgbState pressedColor = RgbState.of(3);

      downButton.bindRepeatHold(this, () -> sceneBank.scrollBy(1));
      downButton.bindHighlightButton(this, sceneBank.canScrollForwards(), baseColor, pressedColor);

      upButton.bindRepeatHold(this, () -> sceneBank.scrollBy(-1));
      upButton.bindHighlightButton(this, sceneBank.canScrollBackwards(), baseColor, pressedColor);

      leftButton.bindRepeatHold(this, () -> trackBank.scrollBy(-1));
      leftButton.bindHighlightButton(this, trackBank.canScrollBackwards(), baseColor, pressedColor);

      rightButton.bindRepeatHold(this, () -> trackBank.scrollBy(1));
      rightButton.bindHighlightButton(this, trackBank.canScrollForwards(), baseColor, pressedColor);
   }

   private void initClipControl(final HwElements hwElements, final TrackBank trackBank) {
      for (int i = 0; i < 8; i++) {
         final int trackIndex = i;
         final Track track = trackBank.getItemAt(trackIndex);
         markTrack(track);
         for (int j = 0; j < 8; j++) {
            final int sceneIndex = j;
            final ClipLauncherSlot slot = track.clipLauncherSlotBank().getItemAt(sceneIndex);
            prepareSlot(slot, sceneIndex, trackIndex);
            final GridButton button = hwElements.getGridButton(sceneIndex, trackIndex);
            button.bindPressed(this, pressed -> handleSlot(pressed, track, slot));
            button.bindLight(this, () -> getState(track, slot, trackIndex, sceneIndex));
         }
      }
      for (int i = 0; i < 8; i++) {
         final Track track = trackBank.getItemAt(i);
         final GridButton button = hwElements.getGridButton(7, i);
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
      initControlLayer(hwElements);
   }

   private void initControlLayer(final HwElements hwElements) {
      transport.isPlaying().markInterested();
      transport.isClipLauncherOverdubEnabled().markInterested();
      transport.isMetronomeEnabled().markInterested();
      int index = 0;
      final GridButton playButton = hwElements.getGridButton(7, index++);
      playButton.bindPressed(controlLayer, this::togglePlay);
      playButton.bindLight(controlLayer, () -> transport.isPlaying().get() ? RgbState.of(21) : RgbState.of(23));
      final GridButton overButton = hwElements.getGridButton(7, index++);

      overButton.bindPressed(controlLayer, () -> viewCursorControl.globalRecordAction(transport));
      overButton.bindLight(controlLayer, this::getRecordButtonColorRegular);

      final GridButton metroButton = hwElements.getGridButton(7, index++);
      metroButton.bindPressed(controlLayer, () -> transport.isMetronomeEnabled().toggle());
      metroButton.bindLight(controlLayer,
         () -> transport.isMetronomeEnabled().get() ? RgbState.of(37) : RgbState.of(39));
      for (int i = 0; i < 4; i++) {
         final GridButton emptyButton = hwElements.getGridButton(7, index++);
         emptyButton.bindPressed(controlLayer, () -> {
         });
         emptyButton.bindLight(controlLayer, () -> RgbState.OFF);
      }
      final GridButton shiftButton = hwElements.getGridButton(7, index);
      shiftButton.bindPressed(controlLayer, pressed -> shiftHeld = pressed);
      shiftButton.bindLightPressed(controlLayer, RgbState.of(1), RgbState.of(3));
   }

   private void togglePlay() {
      if (shiftHeld) {
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

   private void initSceneControl(final HwElements hwElements, final SceneBank sceneBank) {
      sceneBank.setIndication(true);
      sceneBank.scrollPosition().addValueObserver(value -> sceneOffset = value);
      final int n = config.isMiniVersion() ? 7 : 8;
      for (int i = 0; i < n; i++) {
         final int index = i;
         final Scene scene = sceneBank.getScene(index);
         final LabeledButton sceneButton = hwElements.getSceneLaunchButtons().get(index);
         scene.clipCount().markInterested();
         scene.color().addValueObserver((r, g, b) -> sceneColorIndex[index] = ColorLookup.toColor(r, g, b));
         sceneButton.bindPressed(sceneControlLayer, pressed -> handleScene(pressed, scene, index));
         sceneButton.bindLight(sceneControlLayer, () -> getSceneColor(index, scene));
      }
      if (config.isMiniVersion()) {
         modeButton = hwElements.getSceneLaunchButtons().get(7);
         modeButton.bindPressed(sceneControlLayer, this::changeModeMini);
         modeButton.bindLight(sceneControlLayer, () -> RgbState.of(trackMode.getColorIndex()));
      }
   }

   private void initTrackControlXSceneButtons(final HwElements hwElements, final Layer layer) {
      initVolumeControl(hwElements, layer, 0);
      initPanControl(hwElements, layer, 1);
      initSendsAControl(hwElements, layer, 2);
      initSendsBControl(hwElements, layer, 3);
      initStopControl(hwElements, layer, 4);
      initMuteControl(hwElements, layer, 5);
      initSoloControl(hwElements, layer, 6);
      initArmControl(hwElements, layer, 7);
   }

   private void initTrackControlSceneButtons(final HwElements hwElements, final Layer layer) {
      initVolumeControl(hwElements, layer, 0);
      initPanControl(hwElements, layer, 1);
      initSendsAControl(hwElements, layer, 2);
      initSendsBControl(hwElements, layer, 3);
      initDeviceControl(hwElements, layer, 4);
      initStopControl(hwElements, layer, 5);
      initMuteControl(hwElements, layer, 6);
      initSoloControl(hwElements, layer, 7);
   }

   private void initVolumeControl(final HwElements hwElements, final Layer layer, final int index) {
      final LabeledButton volumeButton = hwElements.getSceneLaunchButtons().get(index);
      volumeButton.bindPressReleaseAfter(this, () -> intoControlMode(ControlMode.VOLUME), this::returnToPreviousMode,
         MOMENTARY_TIME);
      volumeButton.bindLight(layer, () -> controlMode == ControlMode.VOLUME ? RgbState.of(9) : RgbState.of(11));
   }

   private void initPanControl(final HwElements hwElements, final Layer layer, final int index) {
      final LabeledButton panButton = hwElements.getSceneLaunchButtons().get(index);
      panButton.bindPressReleaseAfter(this, () -> intoControlMode(ControlMode.PAN), this::returnToPreviousMode,
         MOMENTARY_TIME);
      panButton.bindLight(layer, () -> controlMode == ControlMode.PAN ? RgbState.of(9) : RgbState.of(11));
   }

   private void initSendsAControl(final HwElements hwElements, final Layer layer, final int index) {
      final LabeledButton sendsAButton = hwElements.getSceneLaunchButtons().get(index);
      sendsAButton.bindPressReleaseAfter(this, () -> intoControlMode(ControlMode.SENDS_A), this::returnToPreviousMode,
         MOMENTARY_TIME);
      sendsAButton.bindLight(layer, () -> getSendsState(ControlMode.SENDS_A));
   }

   private void initSendsBControl(final HwElements hwElements, final Layer layer, final int index) {
      final LabeledButton sendsBButton = hwElements.getSceneLaunchButtons().get(index);
      sendsBButton.bindPressReleaseAfter(this, () -> intoControlMode(ControlMode.SENDS_B), this::returnToPreviousMode,
         MOMENTARY_TIME);
      sendsBButton.bindLight(layer, () -> getSendsState(ControlMode.SENDS_B));
   }

   private void initDeviceControl(final HwElements hwElements, final Layer layer, final int index) {
      final LabeledButton deviceButton = hwElements.getSceneLaunchButtons().get(index);
      deviceButton.bindPressReleaseAfter(this, () -> intoControlMode(ControlMode.DEVICE), this::returnToPreviousMode,
         MOMENTARY_TIME);
      deviceButton.bindLight(layer, () -> controlMode == ControlMode.DEVICE ? RgbState.of(33) : RgbState.of(31));
   }

   private void initStopControl(final HwElements hwElements, final Layer layer, final int index) {
      final LabeledButton stopButton = hwElements.getSceneLaunchButtons().get(index);
      stopButton.bindPressReleaseAfter(this, () -> intoTrackMode(TrackMode.STOP), this::returnToPreviousMode,
         MOMENTARY_TIME);
      stopButton.bindLight(layer, () -> trackMode == TrackMode.STOP ? RgbState.of(5) : RgbState.of(7));
   }

   private void initMuteControl(final HwElements hwElements, final Layer layer, final int index) {
      final LabeledButton muteButton = hwElements.getSceneLaunchButtons().get(index);
      muteButton.bindPressReleaseAfter(this, () -> intoTrackMode(TrackMode.MUTE), this::returnToPreviousMode,
         MOMENTARY_TIME);
      muteButton.bindLight(layer, () -> trackMode == TrackMode.MUTE ? RgbState.of(9) : RgbState.of(10));
   }

   private void initSoloControl(final HwElements hwElements, final Layer layer, final int index) {
      final LabeledButton soloButton = hwElements.getSceneLaunchButtons().get(index);
      soloButton.bindPressReleaseAfter(this, () -> intoTrackMode(TrackMode.SOLO), this::returnToPreviousMode,
         MOMENTARY_TIME);
      soloButton.bindLight(layer, () -> trackMode == TrackMode.SOLO ? RgbState.of(13) : RgbState.of(15));
   }

   private void initArmControl(final HwElements hwElements, final Layer layer, final int index) {
      final LabeledButton soloButton = hwElements.getSceneLaunchButtons().get(index);
      soloButton.bindPressReleaseAfter(this, () -> intoTrackMode(TrackMode.ARM), this::returnToPreviousMode,
         MOMENTARY_TIME);
      soloButton.bindLight(layer, () -> trackMode == TrackMode.ARM ? RgbState.of(5) : RgbState.of(7));
   }

   private void sendRemoved(final ControlMode modeRemoved) {
      if (controlMode == modeRemoved) {
         final AbstractSliderLayer currentMode = controlSliderLayers.get(controlMode.getRefMode());
         if (currentMode != null) {
            currentMode.setIsActive(false);
         }
         controlMode = ControlMode.NONE;
      }
   }

   public RgbState getSendsState(final ControlMode mode) {
      if (sendsSliderLayer.canBeEntered(mode)) {
         return controlMode == mode ? RgbState.of(13) : RgbState.of(15);
      }
      return RgbState.OFF;
   }

   private void intoTrackMode(final TrackMode mode) {
      if (controlMode != ControlMode.NONE) {
         switchToMode(ControlMode.NONE);
      }
      if (trackMode == mode) {
         trackMode = TrackMode.NONE;
      } else {
         trackMode = mode;
         soloHeldCount = 0;
      }
      applyTrackMode();
   }

   private void applyTrackMode() {
      switchToBottomLayer(trackModeLayerMap.get(trackMode));
   }

   public void intoControlMode(final ControlMode mode) {
      final AbstractSliderLayer currentMode = controlSliderLayers.get(controlMode.getRefMode());
      final AbstractSliderLayer modeLayer = controlSliderLayers.get(mode.getRefMode());
      if (modeLayer != null && !modeLayer.canBeEntered(mode)) {
         return;
      }
      if (currentMode != null) {
         currentMode.setIsActive(false);
      }
      if (controlMode == mode) {
         controlMode = ControlMode.NONE;
         midiProcessor.toLayout(0x00);
      } else {
         controlMode = mode;
         if (modeLayer != null) {
            modeLayer.setIsActive(true);
         }
      }
      trackMode = TrackMode.NONE;
      applyTrackMode();
   }

   public void returnToPreviousMode(final boolean longPress) {
      if (longPress) {
         if (stashedControlMode != controlMode) {
            switchToMode(stashedControlMode);
         }
         if (stashedTrackMode != trackMode) {
            trackMode = stashedTrackMode;
            applyTrackMode();
         }
      } else {
         stashedControlMode = controlMode;
         stashedTrackMode = trackMode;
      }
   }

   private void switchToMode(final ControlMode newMode) {
      final AbstractSliderLayer currentMode = controlSliderLayers.get(controlMode.getRefMode());
      final AbstractSliderLayer modeLayer = controlSliderLayers.get(newMode.getRefMode());
      if (modeLayer != null && !modeLayer.canBeEntered(newMode)) {
         return;
      }
      if (currentMode != null) {
         currentMode.setIsActive(false);
      }
      if (modeLayer instanceof SendsSliderLayer) {
         final SendsSliderLayer sendingLayer = (SendsSliderLayer) modeLayer;
         sendingLayer.setControl(newMode);
      }
      if (modeLayer != null) {
         modeLayer.setIsActive(true);
      }
      if (newMode == ControlMode.NONE && controlMode != ControlMode.NONE) {
         midiProcessor.toLayout(0x00);
      }
      controlMode = newMode;
   }

   public void setMode(final LpMode lpMode) {
      this.lpMode = lpMode;
      sceneTrackControlLayer.setIsActive(lpMode == LpMode.MIXER);
      sceneControlLayer.setIsActive(lpMode == LpMode.SESSION);
      final AbstractSliderLayer currentSliderMode = controlSliderLayers.get(controlMode.getRefMode());
      applyTrackMode();
      if (currentSliderMode != null) {
         currentSliderMode.setIsActive(lpMode != LpMode.SESSION);
      }
      if (currentBottomRowLayer != null) {
         currentBottomRowLayer.setIsActive(lpMode == LpMode.MIXER);
      }
   }

   private void changeModeMini() { // change to sequence
      final TrackMode nextMode = getNextMode(miniModeSequenceXtra);
      final Layer layer = trackModeLayerMap.get(nextMode);
      trackMode = nextMode;
      switchToBottomLayer(layer);
   }

   private TrackMode getNextMode(final List<TrackMode> sequence) {
      final int index = sequence.indexOf(trackMode);
      if (index == -1) {
         return sequence.get(0);
      }
      return sequence.get((index + 1) % sequence.size());
   }

   private void switchToBottomLayer(final Layer nextLayer) {
      if (currentBottomRowLayer != null) {
         currentBottomRowLayer.setIsActive(false);
      }
      currentBottomRowLayer = nextLayer;
      if (currentBottomRowLayer != null) {
         currentBottomRowLayer.setIsActive(true);
      }
   }

   private void markTrack(final Track track) {
      track.exists().markInterested();
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

   private void handleSlot(final boolean pressed, final Track track, final ClipLauncherSlot slot) {
      if (pressed) {
         if (shiftHeld) {
            track.selectInMixer();
            slot.select();
         } else {
            slot.select();
            slot.launch();
         }
      } else {
         if (shiftHeld) {
            slot.launchReleaseAlt();
         } else {
            slot.launchRelease();
         }
      }
   }

   private void handleScene(final boolean pressed, final Scene scene, final int sceneIndex) {
      if (pressed) {
         viewCursorControl.focusScene(sceneIndex + sceneOffset);
         if (shiftHeld) {
            scene.launchAlt();
         } else {
            scene.launch();
         }
      } else {
         if (shiftHeld) {
            scene.launchReleaseAlt();
         } else {
            scene.launchRelease();
         }
      }
   }

   private RgbState getSceneColor(final int sceneIndex, final Scene scene) {
      if (scene.clipCount().get() > 0) {
         if (sceneOffset + sceneIndex == viewCursorControl.getFocusSceneIndex() && viewCursorControl.hasQueuedForPlaying()) {
            return RgbState.GREEN_FLASH;
         }
         return RgbState.of(sceneColorIndex[sceneIndex]);
      }
      return RgbState.OFF;
   }

   @Override
   protected void onActivate() {
      super.onActivate();
      sceneTrackControlLayer.setIsActive(lpMode == LpMode.MIXER);
      sceneControlLayer.setIsActive(lpMode == LpMode.SESSION);
      if (currentBottomRowLayer != null) {
         currentBottomRowLayer.setIsActive(lpMode == LpMode.MIXER);
      }
   }

   @Override
   protected void onDeactivate() {
      super.onDeactivate();
      sceneControlLayer.setIsActive(false);
      sceneTrackControlLayer.setIsActive(false);
      if (currentBottomRowLayer != null) {
         currentBottomRowLayer.setIsActive(false);
      }
   }

   public RgbState getRecordButtonColorRegular() {
      final FocusSlot focusSlot = viewCursorControl.getFocusSlot();
      if (focusSlot != null) {
         final ClipLauncherSlot slot = focusSlot.getSlot();
         if (slot.isRecordingQueued().get()) {
            return RgbState.flash(5, 0);
         }
         if (slot.isRecording().get() || slot.isRecordingQueued().get()) {
            return RgbState.pulse(5);
         }
      }
      if (transport.isClipLauncherOverdubEnabled().get()) {
         return RgbState.RED;
      } else {
         return RgbState.of(7);
      }
   }
}
