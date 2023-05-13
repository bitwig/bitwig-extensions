package com.bitwig.extensions.controllers.mackie;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.mackie.configurations.BrowserConfiguration;
import com.bitwig.extensions.controllers.mackie.configurations.LayerConfiguration;
import com.bitwig.extensions.controllers.mackie.configurations.MenuModeLayerConfiguration;
import com.bitwig.extensions.controllers.mackie.definition.ControllerConfig;
import com.bitwig.extensions.controllers.mackie.definition.ManufacturerType;
import com.bitwig.extensions.controllers.mackie.definition.SubType;
import com.bitwig.extensions.controllers.mackie.devices.CursorDeviceControl;
import com.bitwig.extensions.controllers.mackie.devices.DeviceTypeBank;
import com.bitwig.extensions.controllers.mackie.devices.SpecialDevices;
import com.bitwig.extensions.controllers.mackie.display.MainUnitButton;
import com.bitwig.extensions.controllers.mackie.display.MotorSlider;
import com.bitwig.extensions.controllers.mackie.display.TimeCodeLed;
import com.bitwig.extensions.controllers.mackie.display.VuMode;
import com.bitwig.extensions.controllers.mackie.layer.SlotHandler;
import com.bitwig.extensions.controllers.mackie.layer.SlotHandlerIcon;
import com.bitwig.extensions.controllers.mackie.section.*;
import com.bitwig.extensions.controllers.mackie.value.*;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.remoteconsole.RemoteConsole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public class MackieMcuProExtension extends ControllerExtension {

   static final String MAIN_UNIT_SYSEX_HEADER = "F0 00 00 66 14 ";
   private static final String SYSEX_DEVICE_RELOAD = "f0000066140158595a";
   private static final double[] FFWD_SPEEDS = {0.0625, 0.25, 1.0, 4.0};
   private static final double[] FFWD_SPEEDS_SHIFT = {0.25, 1.0, 4.0, 16.0};
   private static final long[] FFWD_TIMES = {500, 1000, 2000, 3000, 4000};

   private Layers layers;
   private Layer mainLayer;
   private Layer shiftLayer;
   private Layer optionLayer;

   private Layer cueMarkerModeLayer;

   private HardwareSurface surface;
   private Transport transport;
   private Application application;
   private Project project;
   private MidiOut midiOut;
   private MidiIn midiIn;
   private CursorTrack cursorTrack;
   private TrackBank mixerTrackBank;
   private TrackBank globalTrackBank;
   private ControllerHost host;
   private TimeCodeLed ledDisplay;
   private MasterTrack masterTrack;
   private final BooleanValueObject flipped = new BooleanValueObject();
   private final BooleanValueObject zoomActive = new BooleanValueObject();
   // private final BooleanValueObject scrubActive = new BooleanValueObject();

   private final ValueObject<MixerMode> mixerMode = new ValueObject<>(MixerMode.MAIN);
   private MixerMode previousOverallMode = MixerMode.MAIN;
   private boolean forcedIntoDrumMixMode = false;

   private final ValueObject<ButtonViewState> buttonViewMode = new ValueObject<>(ButtonViewState.MIXER);
   private int blinkTicks = 0;

   private final ModifierValueObject modifier = new ModifierValueObject();
   private final TrackModeValue trackChannelMode = new TrackModeValue();

   private VuMode vuMode = VuMode.LED;
   private final int nrOfExtenders;
   private DelayAction delayedAction = null; // TODO this needs to be a queue

   private final HoldMenuButtonState holdAction = new HoldMenuButtonState();
   private final int[] lightStatusMap = new int[127];

   private LayoutType currentLayoutType;

   private MixControl mainSection;
   private final List<MixControl> sections = new ArrayList<>();

   private final HoldCapture holdState = new HoldCapture();
   private ActionSet actionSet;
   private Arranger arranger;
   private BrowserConfiguration browserConfiguration;

   private CursorDeviceControl cursorDeviceControl;
   private MainUnitButton enterButton;
   private MainUnitButton cancelButton;
   private DeviceMatcher drumMatcher;
   private MainUnitButton noteFxButton;

   private final ControllerConfig controllerConfig;
   private NotePlayingSetup notePlayingSetup;
   private FocusClipView followClip;
   private MotorSlider masterSlider;
   private MenuCreator menuCreator;

   public MackieMcuProExtension(final ControllerExtensionDefinition definition, final ControllerHost host,
                                final ControllerConfig controllerConfig, final int extenders) {
      super(definition, host);
      nrOfExtenders = extenders;
      this.controllerConfig = controllerConfig;
   }

   @Override
   public void init() {
      host = getHost();
      DebugUtil.host = host;
      RemoteConsole.init(host);
      surface = host.createHardwareSurface();
      transport = host.createTransport();
      application = host.createApplication();
      arranger = host.createArranger();
      project = host.getProject();
      layers = new Layers(this);
      mainLayer = new Layer(layers, "MainLayer");
      shiftLayer = new Layer(layers, "GlobalShiftLayer");
      optionLayer = new Layer(layers, "OptionLayer");
      cueMarkerModeLayer = new Layer(layers, "Cue Marker Layer");
      notePlayingSetup = new NotePlayingSetup();
      actionSet = new ActionSet(application);
      controllerConfig.getSimulationLayout().setSurfaceSize(surface, nrOfExtenders);

      modifier.setUsesDuplicateClear(controllerConfig.isUseClearDuplicateModifiers());

      Arrays.fill(lightStatusMap, -1);

      midiOut = host.getMidiOutPort(0);
      midiIn = host.getMidiInPort(0);

      ledDisplay = new TimeCodeLed(midiOut);
      drumMatcher = host.createBitwigDeviceMatcher(SpecialDevices.DRUM.getUuid());

      enterButton = new MainUnitButton(this, BasicNoteOnAssignment.ENTER, controllerConfig.getSimulationLayout());

      final BasicNoteOnAssignment cancelAssignment = controllerConfig.getSubType() == SubType.G2 ? //
         BasicNoteOnAssignment.GV_USER_LF8_G2 : BasicNoteOnAssignment.CANCEL;
      cancelButton = new MainUnitButton(this, cancelAssignment, controllerConfig.getSimulationLayout());
      cancelButton.activateHoldState();

      final PopupBrowser browser = host.createPopupBrowser();
      cursorTrack = getHost().createCursorTrack(8, 4);
      followClip = new FocusClipView(host, cursorTrack);

      initJogWheel();
      initMasterSection();
      initChannelSections();

      menuCreator = new MenuCreator(application, mainSection, actionSet);
      browserConfiguration = new BrowserConfiguration("BROWSER", mainSection, host, browser);
      intiVPotModes();

      initTransport();
      initTrackBank(4);
      initModifiers();

      initCursorSection();

      midiIn.setMidiCallback((ShortMidiMessageReceivedCallback) this::onMidi0);

      setUpMidiSysExCommands();
      mainLayer.activate();
      sections.forEach(MixControl::init);
      host.showPopupNotification(
         " Initialized " + getExtensionDefinition().getName() + " " + getExtensionDefinition().getVersion());
      sections.forEach(MixControl::resetFaders);
      sections.forEach(MixControl::clearAll);
      ledDisplay.refreshMode();
      host.scheduleTask(this::handlePing, 100);

//		final Action[] as = application.getActions();
//		for (final Action action : as) {
//			host.println("ACTION > [ " + action.getId() + "]");
//		}
   }

   public MainUnitButton getEnterButton() {
      return enterButton;
   }

   public MainUnitButton getCancelButton() {
      return cancelButton;
   }

   public void initChannelSections() {
      mainSection = new MixControl(this, midiIn, midiOut, nrOfExtenders, SectionType.MAIN, false);
      sections.add(mainSection);
      for (int i = 0; i < nrOfExtenders; i++) {
         final MidiOut extMidiOut = host.getMidiOutPort(i + 1);
         final MidiIn extMidiIn = host.getMidiInPort(i + 1);
         if (extMidiIn != null && extMidiOut != null) {
            final MixControl extenderSection = new ExtenderMixControl(this, extMidiIn, extMidiOut, i, false);
            sections.add(extenderSection);
         }
      }
   }

   public void doActionImmediate(final String actionId) {
      if (delayedAction != null && actionId.equals(delayedAction.getActionId())) {
         delayedAction.run();
         delayedAction = null;
      }
   }

   public void cancelAction(final String actionId) {
      if (delayedAction != null && actionId.equals(delayedAction.getActionId())) {
         delayedAction = null;
      }
   }

   public void scheduleAction(final String actionId, final int duration, final Runnable action) {
      delayedAction = new DelayAction(duration, actionId, action);
   }

   private void handlePing() {
      if (delayedAction != null && delayedAction.isReady()) {
         delayedAction.run();
         delayedAction = null;
      }
      if (holdAction.isRunning()) {
         holdAction.execute();
      }
      sections.forEach(section -> section.notifyBlink(blinkTicks));
      blinkTicks++;
      host.scheduleTask(this::handlePing, 100);
   }

   private void initJogWheel() {
      final RelativeHardwareKnob fourDKnob = surface.createRelativeHardwareKnob("JOG_WHEEL");
      fourDKnob.setAdjustValueMatcher(midiIn.createRelativeSignedBitCCValueMatcher(0, 60, 128));
      fourDKnob.setStepSize(1 / 128.0);

      final HardwareActionBindable incAction = host.createAction(() -> jogWheelPlayPosition(1), () -> "+");
      final HardwareActionBindable decAction = host.createAction(() -> jogWheelPlayPosition(-1), () -> "-");
      mainLayer.bind(fourDKnob, host.createRelativeHardwareControlStepTarget(incAction, decAction));
      controllerConfig.getSimulationLayout().layoutJogwheel(fourDKnob);
   }

   private void jogWheelPlayPosition(final int dir) {
      double resolution = 0.25;
      if (modifier.isOptionSet()) {
         resolution = 4.0;
      } else if (modifier.isShiftSet()) {
         resolution = 1.0;
      }
      changePlayPosition(dir, resolution, !modifier.isOptionSet(), !modifier.isControlSet());
   }

   private void changePlayPosition(final int inc, final double resolution, final boolean restrictToStart,
                                   final boolean quantize) {

      final double position = transport.playStartPosition().get();
      double newPos = position + resolution * inc;

      if (restrictToStart && newPos < 0) {
         newPos = 0;
      }

      if (position != newPos) {
         if (quantize) {
            final double intPosition = Math.floor(newPos / resolution);
            newPos = intPosition * resolution;
         }
         transport.playStartPosition().set(newPos);
         if (transport.isPlaying().get()) {
            transport.jumpToPlayStartPosition();
         }
      }
   }

   public VuMode getVuMode() {
      return vuMode;
   }

   public DeviceMatcher getDrumMatcher() {
      return drumMatcher;
   }

   public MasterTrack getMasterTrack() {
      return masterTrack;
   }

   public MotorSlider getMasterSlider() {
      return masterSlider;
   }

   private void initMasterSection() {
      masterTrack = getHost().createMasterTrack(8);
      final int touchNoteNr = get(BasicNoteOnAssignment.TOUCH_VOLUME).getNoteNo() + 8;
      masterSlider = new MotorSlider("MASTER", 8, touchNoteNr, surface, midiIn, midiOut);
      controllerConfig.getSimulationLayout().layoutMainSlider(masterSlider.getFader());
   }

   private void intiVPotModes() {
      MainUnitButton.assignToggle(this, BasicNoteOnAssignment.V_TRACK, mainLayer, trackChannelMode, controllerConfig);

//      final Action[] allActions = application.getActions();
//      for (final Action action : allActions) {
//         RemoteConsole.out.println(" ACTION [{}] name={} id=[{}] ", action.getCategory().getName(), action.getName(),
//            action.getId());
//   }
      // createOnOfBoolButton(NoteOnAssignment.SOLO, soloExclusive);

      createModeButton(VPotMode.SEND);
      createModeButton(VPotMode.PAN);
      createModeButton(VPotMode.PLUGIN);
      createModeButton(VPotMode.EQ);
      createModeButton(VPotMode.INSTRUMENT);
      noteFxButton = createModeButton(VPotMode.MIDI_EFFECT);
   }

   private void initCursorSection() {
      MainUnitButton //
         .assignIsPressed(this, BasicNoteOnAssignment.TRACK_LEFT, mainLayer, v -> navigateTrack(-1, v),
            controllerConfig);
      MainUnitButton.assignIsPressed(this, BasicNoteOnAssignment.TRACK_RIGHT, mainLayer, v -> navigateTrack(1, v),
         controllerConfig);
      MainUnitButton.assignIsPressed(this, BasicNoteOnAssignment.BANK_RIGHT, mainLayer, v -> navigateBank(1, v),
         controllerConfig);
      MainUnitButton.assignIsPressed(this, BasicNoteOnAssignment.BANK_LEFT, mainLayer, v -> navigateBank(-1, v),
         controllerConfig);
      MainUnitButton.assignIsPressed(this, BasicNoteOnAssignment.CURSOR_UP, mainLayer, v -> navigateUpDown(1, v),
         controllerConfig);
      MainUnitButton.assignIsPressed(this, BasicNoteOnAssignment.CURSOR_DOWN, mainLayer, v -> navigateUpDown(-1, v),
         controllerConfig);
      MainUnitButton.assignIsPressed(this, BasicNoteOnAssignment.CURSOR_LEFT, mainLayer, v -> navigateLeftRight(-1, v),
         controllerConfig);
      MainUnitButton.assignIsPressed(this, BasicNoteOnAssignment.CURSOR_RIGHT, mainLayer, v -> navigateLeftRight(1, v),
         controllerConfig);
      MainUnitButton.assignToggle(this, BasicNoteOnAssignment.ZOOM, mainLayer, zoomActive, controllerConfig);
      // createOnOfBoolButton(NoteOnAssignment.SCRUB, scrubActive);
   }

   private void navigateTrack(final int direction, final boolean isPressed) {
      if (!isPressed) {
         return;
      }
      if (direction > 0) {
         if (mixerMode.get() == MixerMode.DRUM) {
            cursorDeviceControl.getDrumPadBank().scrollForwards();
         } else {
            mixerTrackBank.scrollForwards();
            globalTrackBank.scrollForwards();
         }
      } else {
         if (mixerMode.get() == MixerMode.DRUM) {
            cursorDeviceControl.getDrumPadBank().scrollBackwards();
         } else {
            mixerTrackBank.scrollBackwards();
            globalTrackBank.scrollBackwards();
         }
      }
   }

   private void navigateBank(final int direction, final boolean isPressed) {
      if (!isPressed) {
         return;
      }
      final int numberOfTracksToControl = 8 * (nrOfExtenders + 1);
      if (mixerMode.get() == MixerMode.DRUM) {
         cursorDeviceControl.getDrumPadBank().scrollBy(direction * numberOfTracksToControl);
      } else {
         mixerTrackBank.scrollBy(direction * numberOfTracksToControl);
         globalTrackBank.scrollBy(direction * numberOfTracksToControl);
      }
   }

   private void navigateUpDown(final int direction, final boolean isPressed) {
      if (!zoomActive.get()) {
         if (getModifier().isShift()) {
            followClip.navigateFocusVertical(direction, isPressed);
         } else {
            sections.forEach(section -> section.navigateUpDown(direction, isPressed));
         }
      } else {
         if (!isPressed) {
            return;
         }
         if (direction > 0) {
            arranger.zoomOutLaneHeightsAll();
         } else {
            arranger.zoomInLaneHeightsAll();
         }
      }
   }

   private void navigateLeftRight(final int direction, final boolean isPressed) {
      if (!zoomActive.get()) {
         if (getModifier().isShift()) {
            followClip.navigateFocusHorizontal(direction, isPressed);
         } else {
            sections.forEach(section -> section.navigateLeftRight(direction, isPressed));
         }
      } else {
         if (!isPressed) {
            return;
         }
         if (direction < 0) {
            arranger.zoomOut();
         } else {
            arranger.zoomIn();
         }
      }
   }

   private void initModifiers() {
      MainUnitButton.assignIsPressed(this, BasicNoteOnAssignment.SHIFT, mainLayer, modifier::setShift,
         controllerConfig);
      modifier.addShiftValueObserver(shiftState -> shiftLayer.setIsActive(shiftState));
      modifier.addOptionValueObserver(optionState -> optionLayer.setIsActive(optionState));
      MainUnitButton.assignIsPressed(this, BasicNoteOnAssignment.OPTION, mainLayer, modifier::setOption,
         controllerConfig);
      MainUnitButton.assignIsPressed(this, BasicNoteOnAssignment.CONTROL, mainLayer, modifier::setControl,
         controllerConfig);
      MainUnitButton.assignIsPressed(this, BasicNoteOnAssignment.ALT, mainLayer, modifier::setAlt, controllerConfig);
   }

   private void initTransport() {
      transport.playStartPosition().markInterested();

      initKeyboardSection();
      initSeqSection();

      final MainUnitButton autoWriteButton = new MainUnitButton(this, BasicNoteOnAssignment.AUTO_READ_OFF,
         controllerConfig.getSimulationLayout());
      autoWriteButton.bindPressed(mainLayer, transport.isArrangerAutomationWriteEnabled());
      autoWriteButton.bindPressed(shiftLayer, transport.isClipLauncherAutomationWriteEnabled());

      final MainUnitButton touchButton = new MainUnitButton(this, BasicNoteOnAssignment.TOUCH,
         controllerConfig.getSimulationLayout());
      touchButton.bindPressed(mainLayer, () -> transport.automationWriteMode().set("touch"));
      final MainUnitButton latchButton = new MainUnitButton(this, BasicNoteOnAssignment.LATCH,
         controllerConfig.getSimulationLayout());
      latchButton.bindPressed(mainLayer, () -> transport.automationWriteMode().set("latch"));
      final MainUnitButton writeButton = new MainUnitButton(this, BasicNoteOnAssignment.AUTO_WRITE,
         controllerConfig.getSimulationLayout());
      writeButton.bindPressed(mainLayer, () -> transport.automationWriteMode().set("write"));

      transport.automationWriteMode().addValueObserver(v -> {
         switch (v) {
            case "latch":
               latchButton.setLed(true);
               touchButton.setLed(false);
               writeButton.setLed(false);
               break;
            case "touch":
               latchButton.setLed(false);
               touchButton.setLed(true);
               writeButton.setLed(false);
               break;
            case "write":
               latchButton.setLed(false);
               touchButton.setLed(false);
               writeButton.setLed(true);
               break;
         }
      });

      final MainUnitButton playButton = new MainUnitButton(this, BasicNoteOnAssignment.PLAY,
         controllerConfig.getSimulationLayout());
      playButton.bindPressed(mainLayer, transport.continuePlaybackAction());
      playButton.bindLight(mainLayer, transport.isPlaying());

      final MainUnitButton punchInButton = new MainUnitButton(this, BasicNoteOnAssignment.DROP,
         controllerConfig.getSimulationLayout());
      final MainUnitButton punchOutButton = new MainUnitButton(this, BasicNoteOnAssignment.REPLACE,
         controllerConfig.getSimulationLayout());
      punchInButton.bindToggle(mainLayer, transport.isPunchInEnabled());
      punchOutButton.bindToggle(mainLayer, transport.isPunchOutEnabled());
      punchInButton.bindPressed(shiftLayer, transport.jumpToPreviousCueMarkerAction());
      punchOutButton.bindPressed(shiftLayer, transport.jumpToNextCueMarkerAction());
      punchInButton.bindPressedState(shiftLayer);
      punchOutButton.bindPressedState(shiftLayer);

      final MainUnitButton stopButton = new MainUnitButton(this, BasicNoteOnAssignment.STOP,
         controllerConfig.getSimulationLayout(), true);
      stopButton.bindPressed(mainLayer, transport.stopAction());
      stopButton.bindLight(mainLayer, transport.isPlaying());
      stopButton.bindPressed(shiftLayer, project.getRootTrackGroup().stopAction());


      final MainUnitButton recordButton = new MainUnitButton(this, BasicNoteOnAssignment.RECORD,
         controllerConfig.getSimulationLayout());
      recordButton.bindToggle(mainLayer, transport.isArrangerRecordEnabled());
      recordButton.bindToggle(shiftLayer, transport.isClipLauncherOverdubEnabled());

      final MainUnitButton clipRecordButton = new MainUnitButton(this, BasicNoteOnAssignment.CLIP_OVERDUB,
         controllerConfig.getSimulationLayout());
      clipRecordButton.bindToggle(mainLayer, transport.isClipLauncherOverdubEnabled());

      final MainUnitButton rewindButton = new MainUnitButton(this, BasicNoteOnAssignment.REWIND,
         controllerConfig.getSimulationLayout()).activateHoldState();
      final MainUnitButton fastForwardButton = new MainUnitButton(this, BasicNoteOnAssignment.FFWD,
         controllerConfig.getSimulationLayout()).activateHoldState();
      fastForwardButton.bindIsPressed(mainLayer, pressed -> notifyHoldForwardReverse(pressed, 1));
      rewindButton.bindIsPressed(mainLayer, pressed -> notifyHoldForwardReverse(pressed, 1));

      initUndoRedo();

      transport.timeSignature().addValueObserver(sig -> ledDisplay.setDivision(sig));

      transport.playPosition().addValueObserver(v -> ledDisplay.updatePosition(v));
      transport.playPositionInSeconds().addValueObserver(ledDisplay::updateTime);

      final MainUnitButton flipButton = MainUnitButton.assignToggle(this, BasicNoteOnAssignment.FLIP, mainLayer,
         flipped, controllerConfig);

      initVuModeButton();

   }

   private void initVuModeButton() {
      final MainUnitButton displayNameButton = new MainUnitButton(this, BasicNoteOnAssignment.DISPLAY_NAME,
         controllerConfig.getSimulationLayout());
      if (getControllerConfig().isHasDedicateVu()) {
         displayNameButton.bindIsPressed(mainLayer, v -> sections.forEach(section -> section.handleNameDisplay(v)));
         displayNameButton.bindIsPressed(shiftLayer, v -> sections.forEach(section -> section.handleInfoDisplay(v)));
      } else {
         displayNameButton.bindIsPressed(mainLayer, v -> sections.forEach(section -> section.handleNameDisplay(v)));
         displayNameButton.bindIsPressed(shiftLayer, this::toggleVuMode);
         displayNameButton.bindIsPressed(optionLayer, v -> sections.forEach(section -> section.handleInfoDisplay(v)));
      }

      final MainUnitButton timeModeButton = new MainUnitButton(this, BasicNoteOnAssignment.DISPLAY_SMPTE,
         controllerConfig.getSimulationLayout());
      timeModeButton.bindLight(mainLayer, () -> ledDisplay.getMode() == TimeCodeLed.Mode.BEATS);
      timeModeButton.bindPressed(mainLayer, () -> ledDisplay.toggleMode());
   }

   private void initUndoRedo() {
      final MainUnitButton saveButton = new MainUnitButton(this, BasicNoteOnAssignment.SAVE,
         controllerConfig.getSimulationLayout());
      final Action saveAction = application.getAction("Save");
      saveButton.bindPressed(mainLayer, saveAction);

      application.canRedo().markInterested();
      application.canUndo().markInterested();

      final MainUnitButton undoButton = new MainUnitButton(this, BasicNoteOnAssignment.UNDO,
         controllerConfig.getSimulationLayout());
      undoButton.bindLight(mainLayer, application.canUndo());
      undoButton.bindPressed(mainLayer, () -> {
         application.undo();
         host.showPopupNotification("Undo");
      });
      undoButton.bindLight(shiftLayer, () -> application.canRedo().get() && blinkTicks % 8 < 3);
      undoButton.bindPressed(shiftLayer, () -> {
         application.redo();
         host.showPopupNotification("Redo");
      });
   }

   public void notifyHoldForwardReverse(final Boolean pressed, final int dir) {
      if (pressed) {
         holdAction.start(stage -> {
            if (modifier.isShiftSet()) {
               changePlayPosition(dir, MackieMcuProExtension.FFWD_SPEEDS_SHIFT[Math.min(stage,
                  MackieMcuProExtension.FFWD_SPEEDS_SHIFT.length - 1)], true, true);
            } else {
               changePlayPosition(dir,
                  MackieMcuProExtension.FFWD_SPEEDS[Math.min(stage, MackieMcuProExtension.FFWD_SPEEDS.length - 1)],
                  true, true);
            }
         }, MackieMcuProExtension.FFWD_TIMES);
      } else {
         holdAction.stop();
      }
   }

   private void toggleVuMode(final boolean pressed) {
      if (!pressed) {
         return;
      }
      if (vuMode == VuMode.LED) {
         vuMode = VuMode.LED_LCD_VERTICAL;
      } else if (vuMode == VuMode.LED_LCD_VERTICAL) {
         vuMode = VuMode.LED_LCD_HORIZONTAL;
      } else {
         vuMode = VuMode.LED;
      }
      sections.forEach(section -> section.applyVuMode(getVuMode()));
   }

   /**
    * Creates modes button
    *
    * @param modes first mode is the button, the second represents the light
    * @return the created button
    */
   private MainUnitButton createModeButton(final VPotMode... modes) {
      assert modes.length > 0;
      final VPotMode mode = modes[0];
      final MainUnitButton button = new MainUnitButton(this, mode.getButtonAssignment(),
         controllerConfig.getSimulationLayout());

      button.bindPressed(mainLayer, () -> setVPotMode(mode, true));
      button.bindReleased(mainLayer, () -> setVPotMode(mode, false));
      if (modes.length == 1) {
         button.bindLight(mainLayer, () -> lightState(mode));
      } else if (modes.length == 2) {
         button.bindLight(mainLayer, () -> lightState(mode, modes[1]));
      }
      return button;
   }

   private boolean lightState(final VPotMode mode) {
      return trackChannelMode.getMode() == mode;
   }

   private boolean lightState(final VPotMode mode, final VPotMode shiftMode) {
      if (trackChannelMode.getMode() == mode) {
         return true;
      } else if (trackChannelMode.getMode() == shiftMode) {
         return blinkTicks % 8 >= 3;
      }
      return false;
   }

   public void setVPotMode(final VPotMode mode, final boolean down) {
      final VPotMode trackMode = trackChannelMode.getMode();
      if (trackMode != mode && trackMode != null) {
         if (down) {
            trackChannelMode.setMode(mode);
         }
         sections.forEach(section -> section.notifyModeChange(mode, down));
      } else {
         sections.forEach(control -> control.notifyModeAdvance(mode, down));
      }
   }

   public TrackModeValue getVpotMode() {
      return trackChannelMode;
   }

   private void onMidi0(final ShortMidiMessage msg) {
//      RemoteConsole.out.println(" MIDI ch={} st={} d1={} d2={}", msg.getChannel(), msg.getStatusByte(), msg.getData1(),
//         msg.getData2());
   }

   private void setUpMidiSysExCommands() {
      midiIn.setSysexCallback(data -> {
         if (data.startsWith(MackieMcuProExtension.SYSEX_DEVICE_RELOAD)) {
            updateAll();
         }
         // Otherwise, track sysex here
      });
   }

   private void updateAll() {
      surface.updateHardware();
      sections.forEach(MixControl::fullHardwareUpdate);
      for (int i = 0; i < lightStatusMap.length; i++) {
         if (lightStatusMap[i] >= 0) {
            midiOut.sendMidi(Midi.NOTE_ON, i, lightStatusMap[i]);
         }
      }
   }

   protected void initTrackBank(final int nrOfScenes) {
      final int numberOfHwChannels = 8 * (nrOfExtenders + 1);

      cursorTrack.color().markInterested();

      cursorDeviceControl = new CursorDeviceControl(cursorTrack, 8, numberOfHwChannels);

      final MainUnitButton soloButton = new MainUnitButton(this, BasicNoteOnAssignment.SOLO,
         controllerConfig.getSimulationLayout());
      soloButton.bindToggle(mainLayer, cursorTrack.solo());

      final TrackBank mainTrackBank = getHost().createMainTrackBank(numberOfHwChannels, 1, nrOfScenes);

      mainTrackBank.followCursorTrack(cursorTrack);

      mixerTrackBank = getHost().createMainTrackBank(numberOfHwChannels, 1, nrOfScenes);
      mixerTrackBank.setSkipDisabledItems(false);
      mixerTrackBank.canScrollChannelsDown().markInterested();
      mixerTrackBank.canScrollChannelsUp().markInterested();
      mixerTrackBank.scrollPosition().addValueObserver(offset -> {
         if (mixerMode.get() == MixerMode.MAIN) {
            ledDisplay.setAssignment(StringUtil.toTwoCharVal(offset + 1), false);
         }
      });

      globalTrackBank = host.createTrackBank(numberOfHwChannels, 1, nrOfScenes);
      globalTrackBank.setSkipDisabledItems(false);
      globalTrackBank.canScrollChannelsDown().markInterested();
      globalTrackBank.canScrollChannelsUp().markInterested();
      globalTrackBank.scrollPosition().addValueObserver(offset -> {
         if (mixerMode.get() == MixerMode.GLOBAL) {
            ledDisplay.setAssignment(StringUtil.toTwoCharVal(offset + 1), false);
         }
      });

      final DeviceTypeBank deviceTypeBank = new DeviceTypeBank(host, cursorDeviceControl);

      final DrumPadBank drumPadBank = cursorDeviceControl.getDrumPadBank();
      drumPadBank.setIndication(true);
      drumPadBank.scrollPosition().addValueObserver(offset -> {
         if (mixerMode.get() == MixerMode.DRUM) {
            ledDisplay.setAssignment(StringUtil.toTwoCharVal(offset), true);
         }
      });

      mixerMode.addValueObserver((oldMode, newMode) -> {
         switch (newMode) {
            case DRUM:
               ledDisplay.setAssignment(StringUtil.toTwoCharVal(drumPadBank.scrollPosition().get()), true);
               break;
            case GLOBAL:
               ledDisplay.setAssignment(StringUtil.toTwoCharVal(globalTrackBank.scrollPosition().get() + 1), false);
               break;
            case MAIN:
               ledDisplay.setAssignment(StringUtil.toTwoCharVal(mixerTrackBank.scrollPosition().get() + 1), false);
               break;
            default:
               break;
         }
      });

      for (final MixControl channelSection : sections) {
         channelSection.initMainControl(mixerTrackBank, globalTrackBank, drumPadBank);
      }
      mainSection.initTrackControl(cursorTrack, cursorDeviceControl.getDrumCursor(), deviceTypeBank);
      initMenuButtons();
      cancelButton.bindPressed(optionLayer, () -> application.navigateToParentTrackGroup());
   }

   public BrowserConfiguration getBrowserConfiguration() {
      return browserConfiguration;
   }

   private void initMenuButtons() {
      final CueMarkerBank cueMarkerBank = arranger.createCueMarkerBank(8);

      createGlobalViewButton(BasicNoteOnAssignment.GLOBAL_VIEW);

      final MainUnitButton groupButton = new MainUnitButton(this, BasicNoteOnAssignment.GROUP,
         controllerConfig.getSimulationLayout());
      groupButton.bindLight(mainLayer, () -> buttonViewMode.get() == ButtonViewState.GROUP_LAUNCH);
      groupButton.bindPressed(mainLayer, () -> {
         final ButtonViewState current = buttonViewMode.get();
         if (current == ButtonViewState.GROUP_LAUNCH) {
            buttonViewMode.set(ButtonViewState.MIXER);
         } else {
            buttonViewMode.set(ButtonViewState.GROUP_LAUNCH);
         }
      });

      initCueMarkerSection(cueMarkerBank);
      transport.tempo().markInterested();

      initFButton(0, BasicNoteOnAssignment.F1, cueMarkerBank, () -> transport.resetAutomationOverrides(), null);
      initFButton(1, BasicNoteOnAssignment.F2, cueMarkerBank, () -> transport.returnToArrangement(), null);
      initFButton(2, BasicNoteOnAssignment.F3, cueMarkerBank, () -> transport.isFillModeActive().toggle(),
         transport.isFillModeActive());
      initFMenuButton(3, BasicNoteOnAssignment.F4, cueMarkerBank,
         () -> menuCreator.createQuantizeSection(transport, followClip));
      final Groove grove = host.createGroove();
      initFMenuButton(4, BasicNoteOnAssignment.F5, cueMarkerBank, () -> menuCreator.createGrooveMenu(grove)); //
      // GROOVE Menu
      initFMenuButton(5, BasicNoteOnAssignment.F6, cueMarkerBank,
         () -> menuCreator.createTempoMenu(transport, this::modifyTempo)); // Tempo Menu
      // Save
      initFButton(6, BasicNoteOnAssignment.F7, cueMarkerBank, () -> actionSet.execute(ActionSet.ActionType.SAVE), null);
      initFMenuButton(7, BasicNoteOnAssignment.F8, cueMarkerBank, () -> menuCreator.createClipMenuSection()); //
      // TOGGLE Layout

      if (controllerConfig.isFunctionSectionLayered()) {
         noteFxButton.bindPressed(cueMarkerModeLayer, () -> cueMarkerBank.getItemAt(0).launch(modifier.isShift()));
      }
      initActionButton(1, BasicNoteOnAssignment.GV_INPUTS_LF2, cueMarkerBank,
         () -> application.setPanelLayout(currentLayoutType.other().getName()));
      initActionButton(2, BasicNoteOnAssignment.GV_AUDIO_LF3, cueMarkerBank, () -> actionSet.focusDevice());
      initActionButton(3, BasicNoteOnAssignment.GV_INSTRUMENT_LF4, cueMarkerBank, () -> actionSet.focusEditor());
      initActionButton(4, BasicNoteOnAssignment.GV_AUX_LF5, cueMarkerBank,
         () -> arranger.isPlaybackFollowEnabled().toggle());
      initActionButton(5, BasicNoteOnAssignment.GV_BUSSES_LF6, cueMarkerBank, //
         () -> followClip.clipAction(4, modifier));
      initActionButton(6, BasicNoteOnAssignment.GV_OUTPUTS_LF7, cueMarkerBank, () -> followClip.clipCreate(4));
      // Icon Pro G2 this is the cancel Button
      if (controllerConfig.getSubType() == SubType.G2) {
         cancelButton.bindPressed(cueMarkerModeLayer, () -> cueMarkerBank.getItemAt(7).launch(modifier.isShift()));
      } else if (controllerConfig.getManufacturerType() != ManufacturerType.MACKIE) {
         initActionButton(7, BasicNoteOnAssignment.GV_USER_LF8, cueMarkerBank, () -> {
         });
      }

      final MainUnitButton clickButton = new MainUnitButton(this, BasicNoteOnAssignment.CLICK,
         controllerConfig.getSimulationLayout());
      final MenuModeLayerConfiguration metroMenu = menuCreator.createClickMenu(transport, value -> midiOut.sendSysex(
         MackieMcuProExtension.MAIN_UNIT_SYSEX_HEADER + (value ? "0A 01" : "0A 00") + " F7"));

      bindHoldToggleButton(clickButton, metroMenu, transport.isMetronomeEnabled());

      final MainUnitButton loopButton = new MainUnitButton(this, BasicNoteOnAssignment.CYCLE,
         controllerConfig.getSimulationLayout());

      bindHoldToggleButton(loopButton, menuCreator.createCyleMenu(host, transport), transport.isArrangerLoopEnabled());

      application.panelLayout().addValueObserver(v -> currentLayoutType = LayoutType.toType(v));
   }


   public void initFButton(final int index, final BasicNoteOnAssignment assign, final CueMarkerBank cueMarkerBank,
                           final Runnable nonMarkerFunction, final BooleanValue lightState) {
      final MainUnitButton fButton = new MainUnitButton(this, assign, controllerConfig.getSimulationLayout());
      fButton.bindPressed(cueMarkerModeLayer, () -> cueMarkerBank.getItemAt(index).launch(modifier.isShift()));
      fButton.bindPressed(mainLayer, nonMarkerFunction);
      fButton.bindLigthPressed(cueMarkerModeLayer);
      if (lightState == null) {
         fButton.bindLigthPressed(mainLayer);
      } else {
         fButton.bindLight(mainLayer, lightState);
      }
   }

   private MainUnitButton initActionButton(final int index, final BasicNoteOnAssignment assignment,
                                           final CueMarkerBank cueMarkerBank, final Runnable action) {
      final MainUnitButton fButton = new MainUnitButton(this, assignment, controllerConfig.getSimulationLayout());
      fButton.bindPressed(mainLayer, action);
      fButton.activateHoldState();
      if (controllerConfig.isFunctionSectionLayered()) {
         fButton.bindPressed(cueMarkerModeLayer, () -> cueMarkerBank.getItemAt(index).launch(modifier.isShift()));
      }
      return fButton;
   }

   private void modifyTempo(final int inc) {
      double increment = 1.0 * inc;
      if (modifier.isOptionSet() && modifier.isShiftSet()) {
         increment *= 10;
      } else if (modifier.isShiftSet()) {
         increment *= 0.1;
      } else if (modifier.isOptionSet()) {
         increment *= 0.01;
      }
      transport.tempo().incRaw(increment);
   }

   private void initClipSection() {
      final MainUnitButton clipButton = new MainUnitButton(this, BasicNoteOnAssignment.GV_AUDIO_LF3,
         controllerConfig.getSimulationLayout());
      final MenuModeLayerConfiguration menu = new MenuModeLayerConfiguration("CLIP_MENU", mainSection);

      final BasicStringValue currentClipName = followClip.getCurrentClipName();
      menu.addNameBinding(0, new BasicStringValue("Clip:"));
      menu.addNameBinding(1, 4, currentClipName);
      for (int i = 0; i < 8; i++) {
         menu.addRingFixedBinding(i);
      }
      bindHoldButton(clipButton, menu);
   }

   private void initKeyboardSection() {
      final MainUnitButton button = new MainUnitButton(this, BasicNoteOnAssignment.NUDGE,
         controllerConfig.getSimulationLayout());

      if (controllerConfig.getSubType() == SubType.G2) {
         button.bindToggle(shiftLayer, transport.isPunchInEnabled());
      }
      final MenuModeLayerConfiguration menu = menuCreator.createKeyboardMenu(notePlayingSetup);
      button.bindLight(mainLayer, () -> buttonViewMode.get() == ButtonViewState.NOTE_PLAY);
      button.bindPressed(mainLayer, () -> {
         holdState.enter(mainSection.getCurrentConfiguration(), button.getName());
         mainSection.setConfiguration(menu);
      });
      button.bindReleased(mainLayer, () -> {
         final LayerConfiguration layer = holdState.endHold();
         if (layer != null) {
            mainSection.setConfiguration(layer);
         }
         if (holdState.exit()) {
            final ButtonViewState current = buttonViewMode.get();
            if (current == ButtonViewState.NOTE_PLAY) {
               buttonViewMode.set(ButtonViewState.MIXER);
            } else {
               buttonViewMode.set(ButtonViewState.NOTE_PLAY);
            }
         }
      });
   }

   private BasicNoteOnAssignment getStepSequencerButton() {
      if (controllerConfig.getManufacturerType() == ManufacturerType.MACKIE) {
         return BasicNoteOnAssignment.GV_USER_LF8;
      }
      return BasicNoteOnAssignment.STEP_SEQ;
   }

   private void initSeqSection() {
      final BasicNoteOnAssignment seqButtonAssign = getStepSequencerButton();

      final MainUnitButton stepSequencerButton = new MainUnitButton(this, seqButtonAssign,
         controllerConfig.getSimulationLayout());
      if (controllerConfig.getSubType() == SubType.G2) {
         stepSequencerButton.bindToggle(shiftLayer, transport.isPunchOutEnabled());
      }
      stepSequencerButton.bindLight(mainLayer, () -> buttonViewMode.get() == ButtonViewState.STEP_SEQUENCER);

      stepSequencerButton.bindPressed(mainLayer, () -> {
         final ButtonViewState current = buttonViewMode.get();
         if (current == ButtonViewState.STEP_SEQUENCER) {
            buttonViewMode.set(ButtonViewState.MIXER);
            if (forcedIntoDrumMixMode && !modifier.isOptionSet()) {
               forcedIntoDrumMixMode = false;
               mixerMode.set(MixerMode.MAIN);
            }
         } else if (mixerMode.get() == MixerMode.DRUM) {
            buttonViewMode.set(ButtonViewState.STEP_SEQUENCER);
            forcedIntoDrumMixMode = false;
         } else if (cursorDeviceControl.cursorHasDrumPads()) {
            mixerMode.set(MixerMode.DRUM);
            buttonViewMode.set(ButtonViewState.STEP_SEQUENCER);
            forcedIntoDrumMixMode = true;
         } else {
            buttonViewMode.set(ButtonViewState.STEP_SEQUENCER);
         }
      });
      stepSequencerButton.bindPressed(shiftLayer, () -> mainSection.advanceMode(ButtonViewState.STEP_SEQUENCER));
      mixerMode.addValueObserver((oldMode, mode) -> {
         if (buttonViewMode.get() == ButtonViewState.STEP_SEQUENCER && mode != MixerMode.DRUM) {
            buttonViewMode.set(ButtonViewState.MIXER);
         }
      });
   }


   private void initCueMarkerSection(final CueMarkerBank cueMarkerBank) {
      final MainUnitButton markerButton = new MainUnitButton(this, BasicNoteOnAssignment.MARKER,
         controllerConfig.getSimulationLayout());
      final BooleanValueObject marker = new BooleanValueObject();
      final BeatTimeFormatter formatter = host.createBeatTimeFormatter(":", 2, 1, 1, 0);

      final MenuModeLayerConfiguration markerMenuConfig = new MenuModeLayerConfiguration("MARKER_MENU", mainSection);
      for (int i = 0; i < 8; i++) {
         final CueMarker cueMarker = cueMarkerBank.getItemAt(i);
         cueMarker.exists().markInterested();
         markerMenuConfig.addValueBinding(i, cueMarker.position(), cueMarker, "---",
            v -> cueMarker.position().getFormatted(formatter));
         markerMenuConfig.addNameBinding(i, cueMarker.name(), cueMarker, "<Cue" + (i + 1) + ">");
         markerMenuConfig.addRingExistsBinding(i, cueMarker);
         markerMenuConfig.addPressEncoderBinding(i, index -> {
            if (cueMarker.exists().get()) {
               if (modifier.isShift()) {
                  cueMarker.deleteObject();
               } else {
                  cueMarker.position().set(transport.getPosition().get());
               }
            } else {
               transport.addCueMarkerAtPlaybackPosition();
            }
         }, true);
         markerMenuConfig.addEncoderIncBinding(i, cueMarker.position(), 1, 0.25);
      }
      bindHoldToggleButton(markerButton, markerMenuConfig, marker);
      marker.addValueObserver(v -> cueMarkerModeLayer.setIsActive(v));
   }

   public void createGlobalViewButton(final BasicNoteOnAssignment assignConst) {
      final MainUnitButton button = new MainUnitButton(this, assignConst, controllerConfig.getSimulationLayout());
      mixerMode.addValueObserver((oldMode, newMode) -> {
         if (newMode != MixerMode.DRUM) {
            previousOverallMode = newMode;
         }
      });
      button.bindLight(mainLayer, () -> lightStateMixMode(mixerMode));
      button.bindPressed(mainLayer, () -> {
         if (mixerMode.get() == MixerMode.DRUM) {
            mixerMode.set(previousOverallMode);
         } else if (buttonViewMode.get() == ButtonViewState.GLOBAL_VIEW) {
            buttonViewMode.set(ButtonViewState.MIXER);
         } else {
            buttonViewMode.set(ButtonViewState.GLOBAL_VIEW);
         }
      });
   }

   private boolean lightStateMixMode(final ValueObject<MixerMode> valueState) {
      if (buttonViewMode.get() == ButtonViewState.GLOBAL_VIEW) {
         return true;
      } else if (valueState.get() == MixerMode.DRUM) {
         return blinkTicks % 8 >= 3;
      }
      return false;
   }

   public MixerMode getPreviousOverallMode() {
      return previousOverallMode;
   }

   public void initFMenuButton(final int index, final BasicNoteOnAssignment assign, final CueMarkerBank cueMarkerBank,
                               final Supplier<MenuModeLayerConfiguration> menuCreator) {
      final MainUnitButton fButton = new MainUnitButton(this, assign, controllerConfig.getSimulationLayout());
      fButton.bindPressed(cueMarkerModeLayer, () -> cueMarkerBank.getItemAt(index).launch(modifier.isShift()));
      bindHoldButton(fButton, menuCreator.get());
      fButton.activateHoldState();
   }

   private void bindHoldToggleButton(final MainUnitButton button, final MenuModeLayerConfiguration menu,
                                     final SettableBooleanValue value) {
      button.bindLight(mainLayer, value);
      button.bindPressed(mainLayer, () -> {
         holdState.enter(mainSection.getCurrentConfiguration(), button.getName());
         mainSection.setConfiguration(menu);
      });
      button.bindReleased(mainLayer, () -> {
         final LayerConfiguration layer = holdState.endHold();
         if (layer != null) {
            mainSection.setConfiguration(layer);
         }
         if (holdState.exit()) {
            value.toggle();
         }
      });
   }


   private void bindHoldButton(final MainUnitButton button, final MenuModeLayerConfiguration menu) {
      // mainLayer.bind(value, (OnOffHardwareLight) button.backgroundLight());
      // TODO consider light
      button.bindPressed(mainLayer, () -> {
         holdState.enter(mainSection.getCurrentConfiguration(), button.getName());
         mainSection.setConfiguration(menu);
      });
      button.bindReleased(mainLayer, () -> {
         final LayerConfiguration layer = holdState.endHold();
         if (layer != null) {
            mainSection.setConfiguration(layer);
         }
         holdState.exit();
      });
   }

   public ValueObject<MixerMode> getMixerMode() {
      return mixerMode;
   }

   public ValueObject<ButtonViewState> getButtonView() {
      return buttonViewMode;
   }

   public CursorDeviceControl getCursorDeviceControl() {
      return cursorDeviceControl;
   }

   public CursorTrack getCursorTrack() {
      return cursorTrack;
   }

   public void sendLedUpdate(final NoteAssignment assignment, final int value) {
      final int noteNr = assignment.getNoteNo();
      lightStatusMap[noteNr] = value;
      midiOut.sendMidi(assignment.getType(), assignment.getNoteNo(), value);
   }

   public void sendLedUpdate(final int noteNr, final int value) {
      lightStatusMap[noteNr] = value;
      midiOut.sendMidi(Midi.NOTE_ON, noteNr, value);
   }

   private void shutDownController(final CompletableFuture<Boolean> shutdown) {
      ledDisplay.clearAll();
      midiOut.sendSysex(MackieMcuProExtension.MAIN_UNIT_SYSEX_HEADER + "0A 00 F7"); // turn off click
      sections.forEach(MixControl::resetLeds);
      sections.forEach(MixControl::resetFaders);
      masterSlider.sendValue(0);
      sections.forEach(MixControl::exitMessage);
      try {
         Thread.sleep(300);
      } catch (final InterruptedException e) {
         e.printStackTrace();
      }
      shutdown.complete(true);
   }

   @Override
   public void exit() {
      final CompletableFuture<Boolean> shutdown = new CompletableFuture<>();
      Executors.newSingleThreadExecutor().execute(() -> shutDownController(shutdown));
      try {
         shutdown.get();
      } catch (final InterruptedException | ExecutionException e) {
         e.printStackTrace();
      }
      getHost().showPopupNotification(
         " Exit " + getExtensionDefinition().getName() + " " + getExtensionDefinition().getVersion());
   }

   public RelativeHardwarControlBindable createIncrementBinder(final IntConsumer consumer) {
      return host.createRelativeHardwareControlStepTarget(//
         host.createAction(() -> consumer.accept(1), () -> "+"),
         host.createAction(() -> consumer.accept(-1), () -> "-"));
   }

   public Layers getLayers() {
      return layers;
   }

   public MidiIn getMidiIn() {
      return midiIn;
   }

   public HardwareSurface getSurface() {
      return surface;
   }

   @Override
   public void flush() {
      surface.updateHardware();
   }

   public Project getProject() {
      return project;
   }

   public Application getApplication() {
      return application;
   }

   public ModifierValueObject getModifier() {
      return modifier;
   }

   public BooleanValueObject getFlipped() {
      return flipped;
   }

   public TrackModeValue getTrackChannelMode() {
      return trackChannelMode;
   }

   public NotePlayingSetup getNotePlayingSetup() {
      return notePlayingSetup;
   }

   public NoteAssignment get(final BasicNoteOnAssignment assignment) {
      return controllerConfig.get(assignment);
   }

   public ControllerConfig getControllerConfig() {
      return controllerConfig;
   }

   public ActionSet getActionSet() {
      return actionSet;
   }

   public SlotHandler createSlotHandler() {
      if (controllerConfig.getManufacturerType() == ManufacturerType.ICON) {
         return new SlotHandlerIcon();
      }
      return new SlotHandler();
   }

   public TrackSelectionHandler createTrackSelectionHandler() {
      if (controllerConfig.getManufacturerType() == ManufacturerType.ICON) {
         return new TrackSelectionHandlerIcon(this);
      }
      return new TrackSelectionHandler(this);
   }
}
