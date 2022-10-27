package com.bitwig.extensions.controllers.arturia.minilab3;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.values.BasicStringValue;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.bitwig.extensions.framework.values.ValueObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.IntConsumer;

public class MiniLab3Extension extends ControllerExtension {

   public static final int NUM_PADS_TRACK = 8;

   private static final int NUM_SLIDERS = 4;
   private static final int[] SLIDER_CC_MAPPING = new int[]{0x0E, 0x0F, 0x1E, 0x1F};
   private static final int[] ENCODER_CC_MAPPING = new int[]{0x56, 0x57, 0x59, 0x5A, 0x6E, 0x6F, 0x74, 0x75};

   private static final String ANALOG_LAB_V_DEVICE_ID = "4172747541564953416C617650726F63";

   private Layers layers;
   private MidiIn midiIn;
   private MidiOut midiOut;
   private Layer mainLayer;
   private Layer shiftLayer;
   private HardwareSurface surface;
   private ControllerHost host;
   private OledDisplay oled;

   private final AbsoluteHardwareKnob[] knobs = new AbsoluteHardwareKnob[8];
   private final HardwareSlider[] sliders = new HardwareSlider[NUM_SLIDERS];
   private final RgbButton[] padBankAButtons = new RgbButton[NUM_PADS_TRACK];
   private final RgbButton[] padBankBButtons = new RgbButton[NUM_PADS_TRACK];

   private HardwareButton shiftButton;
   private int blinkState = 0;
   private CursorTrack cursorTrack;

   boolean encoderPressTurned = true; // track if encoder was turned while being pressed.

   private PinnableCursorDevice cursorDevice;
   private CursorRemoteControlsPage parameterBank;

   private ClipLaunchingLayer clipLaunchingLayer;
   private DrumPadLayer drumPadLayer;
   private ArturiaModeLayer arturiaModeLayer;

   private Scene sceneTrackItem;

   private final ValueObject<PadBank> padBank = new ValueObject<>(PadBank.BANK_A);
   private final BooleanValueObject shiftDown = new BooleanValueObject();
   private SysExHandler sysExHandler;

   private final BooleanValueObject encoderDown = new BooleanValueObject();
   private TrackBank viewTrackBank;
   private Runnable nextPingAction = null;
   private BrowserLayer browserLayer;
   private String[] pageNames = new String[0];
   private RelativeHardwareKnob mainEncoder;
   private RelativeHardwareKnob shiftMainEncoder;
   private HardwareButton encoderPress;
   private HardwareButton shiftEncoderPress;
   private Transport transport;
   private PinnableCursorDevice primaryDevice;
   private FocusMode recordFocusMode = FocusMode.ARRANGER;

   protected MiniLab3Extension(final MiniLab3ExtensionDefinition definition, final ControllerHost host) {
      super(definition, host);
   }

   @Override
   public void init() {
      host = getHost();
      layers = new Layers(this);
      midiIn = host.getMidiInPort(0);
      midiIn.setMidiCallback((ShortMidiMessageReceivedCallback) this::onMidi0);
      midiIn.setSysexCallback(this::handleSysExData);
      midiOut = host.getMidiOutPort(0);
      sysExHandler = new SysExHandler(midiOut, host);
      surface = host.createHardwareSurface();
      oled = new OledDisplay(sysExHandler, host);
      transport = host.createTransport();
      final String[] inputMasks = getInputMask(0x09,
         new int[]{0x01, 0x09, 0x10, 0x11, 0x12, 0x13, 0x40, 0x47, 0x4a, 0x4c, 0x4d, 0x52, 0x53, 0x55, 0x5d, 0x70, 0x71, 0x72, 0x73});
      final NoteInput noteInput = midiIn.createNoteInput("MIDI", inputMasks); //

      noteInput.setShouldConsumeEvents(true);
      initCursors();
      setUpHardware();

      mainLayer = new Layer(layers, "MAIN");
      shiftLayer = new Layer(layers, "SHIFT");

      browserLayer = new BrowserLayer(this);

      bindSliderValue(mainLayer, cursorTrack.volume(), sliders[0], cursorTrack.name(), new BasicStringValue("Vol"));
      bindKnobValue(4, mainLayer, cursorTrack.pan(), sliders[3], cursorTrack.name(), new BasicStringValue("Pan"),
         "Fader");
      bindKnobValue(2, mainLayer, cursorTrack.sendBank().getItemAt(0), sliders[1], cursorTrack.name(),
         cursorTrack.sendBank().getItemAt(0).name(), "Fader");
      bindKnobValue(3, mainLayer, cursorTrack.sendBank().getItemAt(1), sliders[2], cursorTrack.name(),
         cursorTrack.sendBank().getItemAt(0).name(), "Fader");

      shiftButton.isPressed().addValueObserver(this::handleShift);

      for (int i = 0; i < NUM_PADS_TRACK; i++) {
         final RemoteControl parameter = parameterBank.getParameter(i);
         mainLayer.bind(knobs[i], parameter);
         bindKnobValue(i + 1, mainLayer, parameter, knobs[i], cursorDevice.name(), parameter.name(), "Knob");
      }

      setUpTransportControl();

      initEncoders();

      clipLaunchingLayer = new ClipLaunchingLayer(this);
      drumPadLayer = new DrumPadLayer(this);
      arturiaModeLayer = new ArturiaModeLayer(this);

      mainLayer.activate();
      clipLaunchingLayer.activate();
      drumPadLayer.activate();

      sysExHandler.deviceInquiry();
      setUpPreferences();
      host.scheduleTask(this::handlePing, 100);
   }

   private void handleSysExData(final String sysEx) {

      switch (sysEx) {
         case "f000206b7f420200406301f7":
         case "f000206b7f420200400300f7":
            toBankMode(PadBank.BANK_A);
            break;
         case "f000206b7f420200406302f7":
         case "f000206b7f420200400301f7":
            toBankMode(PadBank.BANK_B);
            break;
         case "f000206b7f420200406201f7": // Arturia Mode
            drumPadLayer.deactivate();
            clipLaunchingLayer.deactivate();
            arturiaModeLayer.activate();
            break;
         case "f000206b7f420200406202f7": // In DAW Mode
            arturiaModeLayer.deactivate();
            drumPadLayer.activate();
            clipLaunchingLayer.activate();
            break;
         case "f000206b7f420200400100f7": // Confirm in Arturia Mode
            sysExHandler.enableProcessing();
            oled.notifyInit();
            drumPadLayer.deactivate();
            clipLaunchingLayer.deactivate();
            arturiaModeLayer.activate();
            host.showPopupNotification("MiniLab 3 Initialized");
            break;
         case "f000206b7f420200400101f7": // Confirm Connected to BW Studio
            sysExHandler.enableProcessing();
            oled.notifyInit();
            arturiaModeLayer.resetNotes();
            host.showPopupNotification("MiniLab 3 Initialized");
            break;
         default:
            if (sysEx.startsWith("f07e7f060200206b0200040")) {
               host.println(" DEVICE ID " + sysEx);
               sysExHandler.requestInitState();
            } else {
               host.println("Unknown Received SysEx : " + sysEx);
            }
            break;
      }
   }

   private void handlePing() {
      blinkState++;
      clipLaunchingLayer.notifyBlink(blinkState);
      oled.handleTransient();
      if (nextPingAction != null) {
         nextPingAction.run();
         nextPingAction = null;
      }
      host.scheduleTask(this::handlePing, 100);
   }

   private String[] getInputMask(final int excludeChannel, final int[] miniLabPassThroughCcs) {
      final List<String> masks = new ArrayList<>();
      for (int i = 0; i < 16; i++) {
         if (i != excludeChannel) {
            masks.add(String.format("8%01x????", i));
            masks.add(String.format("9%01x????", i));
         }
      }
      masks.add("A?????"); // Poly Aftertouch
      masks.add("D?????"); // Channel Aftertouch
      masks.add("E?????"); // Pitchbend
      masks.add("B1????"); // CCs Channel 2
      //masks.add("B0????");
      for (final int miniLabPassThroughCc : miniLabPassThroughCcs) {
         masks.add(String.format("B0%02x??", miniLabPassThroughCc));
      }
      return masks.toArray(String[]::new);
   }


   private void initCursors() {
      cursorTrack = host.createCursorTrack(2, NUM_PADS_TRACK);
      viewTrackBank = host.createTrackBank(NUM_PADS_TRACK, 2, 1);
      viewTrackBank.followCursorTrack(cursorTrack);
      sceneTrackItem = viewTrackBank.sceneBank().getScene(0);
      sceneTrackItem.name()
         .addValueObserver(
            sceneName -> oled.sendTextInfo(DisplayMode.SCENE, cursorTrack.name().get(), sceneName, true));

      cursorDevice = cursorTrack.createCursorDevice();

      primaryDevice = cursorTrack.createCursorDevice("DrumDetection", "Pad Device", NUM_PADS_TRACK,
         CursorDeviceFollowMode.FIRST_INSTRUMENT);

      setUpFollowArturiaDevice();


      cursorDevice.presetName().markInterested();
      cursorTrack.name()
         .addValueObserver(
            name -> updateTrackInfo(transport.isPlaying().get(), transport.isArrangerRecordEnabled().get(), name,
               cursorDevice.name().get(), cursorDevice.exists().get()));
      cursorDevice.name()
         .addValueObserver(
            deviceName -> updateTrackInfo(transport.isPlaying().get(), transport.isArrangerRecordEnabled().get(),
               cursorTrack.name().get(), deviceName, cursorDevice.exists().get()));
      cursorDevice.exists()
         .addValueObserver(
            deviceExists -> updateTrackInfo(transport.isPlaying().get(), transport.isArrangerRecordEnabled().get(),
               cursorTrack.name().get(), cursorDevice.name().get(), deviceExists));
      parameterBank = cursorDevice.createCursorRemoteControlsPage(NUM_PADS_TRACK);
   }

   private void setUpFollowArturiaDevice() {
      final DeviceMatcher arturiaMatcher = host.createVST3DeviceMatcher(ANALOG_LAB_V_DEVICE_ID);
      final DeviceBank matcherBank = cursorTrack.createDeviceBank(1);
      matcherBank.setDeviceMatcher(arturiaMatcher);
      final Device matcherDevice = matcherBank.getItemAt(0);
      matcherDevice.exists().markInterested();

      final BooleanValueObject controlsAnalogLab = new BooleanValueObject();

      controlsAnalogLab.addValueObserver(controlsLab -> sysExHandler.fireArturiaMode(
         controlsLab ? SysExHandler.GeneralMode.ANALOG_LAB : SysExHandler.GeneralMode.DAW_MODE,
         arturiaModeLayer.isActive()));

      final BooleanValue onArturiaDevice = cursorDevice.createEqualsValue(matcherDevice);
      cursorTrack.arm()
         .addValueObserver(
            armed -> controlsAnalogLab.set(armed && cursorDevice.exists().get() && onArturiaDevice.get()));
      onArturiaDevice.addValueObserver(
         onArturia -> controlsAnalogLab.set(cursorTrack.arm().get() && cursorDevice.exists().get() && onArturia));
      cursorDevice.exists()
         .addValueObserver(cursorDeviceExists -> controlsAnalogLab.set(
            cursorTrack.arm().get() && cursorDeviceExists && onArturiaDevice.get()));
   }


   private void setUpPreferences() {
      final Preferences preferences = getHost().getPreferences(); // THIS
      final SettableEnumValue recordButtonAssignment = preferences.getEnumSetting("Recording Button assignment", //
         "Transport", new String[]{FocusMode.LAUNCHER.getDescriptor(), FocusMode.ARRANGER.getDescriptor()},
         recordFocusMode.getDescriptor());
      recordButtonAssignment.addValueObserver(value -> {
         recordFocusMode = FocusMode.toMode(value);
         updateTrackInfo();
      });
      final SettableEnumValue clipStopTiming = preferences.getEnumSetting("Long press to stop clip", //
         "Clip", new String[]{"Fast", "Medium", "Standard"}, "Medium");
      clipStopTiming.addValueObserver(clipLaunchingLayer::setClipStopTiming);
   }

   private void handleShift(final boolean pressed) {
      shiftDown.set(pressed);
      if (!pressed) {
         shiftLayer.deactivate();
         // When holding Shift and releasing the Encoder, no Event is sent! Thus we preemptively release it.
         encoderDown.set(false);
      } else {
         shiftLayer.activate();
      }
   }


   void bindEncoder(final Layer layer, final RelativeHardwareKnob encoder, final IntConsumer action) {
      final HardwareActionBindable incAction = host.createAction(() -> action.accept(1), () -> "+");
      final HardwareActionBindable decAction = host.createAction(() -> action.accept(-1), () -> "-");
      layer.bind(encoder, host.createRelativeHardwareControlStepTarget(incAction, decAction));
   }

   private void initEncoders() {
      bindEncoder(mainLayer, mainEncoder, this::mainEncoderAction);
      bindEncoder(mainLayer, shiftMainEncoder, this::mainEncoderShiftAction);

      parameterBank.pageNames().addValueObserver(pages -> {
         pageNames = pages;
         showParameterPage(parameterBank.selectedPageIndex().get());
      });
      parameterBank.pageCount().markInterested();
      parameterBank.selectedPageIndex().addValueObserver(this::showParameterPage);
      shiftEncoderPress.isPressed().addValueObserver(this::handleShiftEncoderPressed);
      encoderPress.isPressed().addValueObserver(this::handleEncoderPressed);
   }

   private void handleShiftEncoderPressed(final boolean down) {
      encoderDown.set(down);
      if (down) {
         encoderPressTurned = false;
         browserLayer.shiftPressAction();
      }
   }

   private void handleEncoderPressed(final boolean down) {
      encoderDown.set(down);
      if (down) {
         encoderPressTurned = false;
      }
      if (browserLayer.isActive()) {
         browserLayer.pressAction(down);
      } else {
         if (down) {
            oled.enableValues(DisplayMode.PARAM_PAGE);
         } else {
            if (!encoderPressTurned && padBank.get() != PadBank.BANK_B) {
               clipLaunchingLayer.launchScene();
            }
            updateTrackInfo();
         }
      }
   }

   public RelativeHardwareKnob getMainEncoder() {
      return mainEncoder;
   }

   public RelativeHardwareKnob getShiftMainEncoder() {
      return shiftMainEncoder;
   }

   private void showParameterPage(final int index) {
      if (parameterBank.pageCount().get() == 0) {
         oled.sendTextInfo(DisplayMode.PARAM_PAGE, cursorDevice.name().get(), "<NO PARAM PAGES>", true);
      } else if (index >= 0 && index < pageNames.length) {
         oled.sendTextInfo(DisplayMode.PARAM_PAGE, cursorDevice.name().get(), pageNames[index], true);
      }
   }

   public OledDisplay getOled() {
      return oled;
   }

   public SysExHandler getSysExHandler() {
      return sysExHandler;
   }


   public Layers getLayers() {
      return layers;
   }

   public PinnableCursorDevice getCursorDevice() {
      return cursorDevice;
   }

   public PinnableCursorDevice getPrimaryDevice() {
      return primaryDevice;
   }

   private void bindSliderValue(final Layer layer, final Parameter parameter, final AbsoluteHardwareControl slider,
                                final StringValue nameSource, final StringValue label) {
      layer.bind(slider, parameter.value());
      layer.bind(slider, v -> oled.enableValues(DisplayMode.PARAM));
      label.addValueObserver(
         v -> oled.sendSliderInfo(DisplayMode.PARAM, parameter.value().get(), v + " : " + nameSource.get(),
            parameter.value().displayedValue().get()));
      parameter.value()
         .displayedValue()
         .addValueObserver(displayedValue -> oled.sendSliderInfo(DisplayMode.PARAM, parameter.value().get(),
            label.get() + " : " + nameSource.get(), displayedValue));
      parameter.value()
         .addValueObserver(v -> oled.sendSliderInfo(DisplayMode.PARAM, v, label.get() + " : " + nameSource.get(),
            parameter.value().displayedValue().get()));
   }

   private void bindKnobValue(final int index, final Layer layer, final Parameter parameter,
                              final AbsoluteHardwareControl slider, final StringValue nameSource,
                              final StringValue label, final String type) {
      final SettableRangedValue value = parameter.value();
      value.markInterested();
      parameter.exists().markInterested();
      nameSource.markInterested();
      layer.bind(slider, value);
      layer.bind(slider, v -> oled.enableValues(DisplayMode.PARAM));
      value.displayedValue().markInterested();
      label.addValueObserver(
         v -> oled.sendSliderInfo(DisplayMode.PARAM, value.get(), String.format("%s : %s", v, nameSource.get()),
            value.displayedValue().get()));
      value.displayedValue()
         .addValueObserver(displayedValue -> oled.sendEncoderInfo(DisplayMode.PARAM, value.get(),
            String.format("%s : %s", label.get(), nameSource.get()), displayedValue));
      value.addValueObserver(
         v -> oled.sendEncoderInfo(DisplayMode.PARAM, v, String.format("%s : %s", label.get(), nameSource.get()),
            value.displayedValue().get()));
      if (type.equals("Fader")) {
         slider.value().addValueObserver(v -> {
            if (!parameter.exists().get()) {
               oled.sendSliderInfo(DisplayMode.PARAM, v, String.format("%s : %d", type, index),
                  String.format("%d", (int) Math.round(v * 127)));
            }
         });
      } else {
         slider.value().addValueObserver(v -> {
            if (!parameter.exists().get()) {
               oled.sendEncoderInfo(DisplayMode.PARAM, v, String.format("%s : %d", type, index),
                  String.format("%d", (int) Math.round(v * 127)));
            }
         });
      }
   }


   private void setUpTransportControl() {
      final RgbButton loopButton = new RgbButton(0x57, PadBank.TRANSPORT, RgbButton.Type.CC, 105, 0, true, this);
      loopButton.bindToggle(shiftLayer, transport.isArrangerLoopEnabled(), RgbLightState.ORANGE,
         RgbLightState.ORANGE_DIMMED);
      transport.isArrangerLoopEnabled()
         .addValueObserver(
            loopEnabled -> oled.sendTextCond(DisplayMode.LOOP_VALUE, "Loop Mode", loopEnabled ? "ON" : "OFF"));
      loopButton.bind(shiftLayer, () -> {
         final boolean loopEnabled = transport.isArrangerLoopEnabled().get();
         oled.sendText(DisplayMode.LOOP_VALUE, "Loop Mode", loopEnabled ? "ON" : "OFF");
      }, () -> transport.isArrangerLoopEnabled().get() ? RgbLightState.ORANGE : RgbLightState.ORANGE_DIMMED);

      final RgbButton recordButton = new RgbButton(0x5A, PadBank.TRANSPORT, RgbButton.Type.CC, 108, 0, true, this);
      transport.isArrangerRecordEnabled().markInterested();
      transport.isClipLauncherOverdubEnabled().markInterested();

      recordButton.bind(shiftLayer, this::handleRecordPressed, this::getRecordingLightState);

      final RgbButton playButton = new RgbButton(0x59, PadBank.TRANSPORT, RgbButton.Type.CC, 107, 0, true, this);
      playButton.bindToggle(shiftLayer, transport.isPlaying(), RgbLightState.GREEN, RgbLightState.GREEN_DIMMED);

      final RgbButton stopButton = new RgbButton(0x58, PadBank.TRANSPORT, RgbButton.Type.CC, 106, 0, true, this);
      stopButton.bindPressed(shiftLayer, pressed -> {
         if (pressed) {
            transport.stop();
         }
      }, () -> transport.isPlaying().get() ? RgbLightState.WHITE : RgbLightState.WHITE_DIMMED);

      final RgbButton tapButton = new RgbButton(0x5B, PadBank.TRANSPORT, RgbButton.Type.CC, 109, 0, true, this);
      transport.tempo().value().addRawValueObserver(v -> {
         final int tempo = (int) Math.round(v);
         oled.sendTextCond(DisplayMode.TEMPO, "Tap Tempo", String.format("%d BPM", tempo));
      });
      tapButton.bind(shiftLayer, () -> {
         transport.tapTempo();
         final int tempo = (int) Math.round(transport.tempo().value().getRaw());
         oled.sendText(DisplayMode.TEMPO, "Tap Tempo", String.format("%d BPM", tempo));
      }, RgbLightState.WHITE, RgbLightState.WHITE_DIMMED);

      transport.isArrangerRecordEnabled()
         .addValueObserver(isRecording -> updateTrackInfo(transport.isPlaying().get(),
            recordFocusMode == FocusMode.ARRANGER ? isRecording : recordFocusMode.getState(transport)));
      transport.isClipLauncherOverdubEnabled()
         .addValueObserver(isRecording -> updateTrackInfo(transport.isPlaying().get(),
            recordFocusMode == FocusMode.LAUNCHER ? isRecording : recordFocusMode.getState(transport)));

      transport.isPlaying()
         .addValueObserver(isPlaying -> updateTrackInfo(isPlaying, recordFocusMode.getState(transport)));
   }

   private RgbLightState getRecordingLightState() {
      if (recordFocusMode == FocusMode.ARRANGER) {
         return transport.isArrangerRecordEnabled().get() ? RgbLightState.RED : RgbLightState.RED_DIMMED;
      } else {
         return transport.isClipLauncherOverdubEnabled().get() ? RgbLightState.RED : RgbLightState.RED_DIMMED;
      }
   }

   private void handleRecordPressed() {
      if (recordFocusMode == FocusMode.ARRANGER) {
         transport.isArrangerRecordEnabled().toggle();
      } else {
         transport.isClipLauncherOverdubEnabled().toggle();
      }
   }

   public BooleanValueObject getShiftDown() {
      return shiftDown;
   }

   public void browserDisplayMode(final boolean browserModeActive) {
      oled.setMainMode(browserModeActive ? DisplayMode.BROWSER : DisplayMode.TRACK,
         browserModeActive ? browserLayer::updateInfo : this::updateTrackInfo);
      if (!browserModeActive) {
         updateTrackInfo();
      }
   }


   private void updateTrackInfo(final boolean isPlaying, final boolean isRecording, final String trackName,
                                final String deviceName, final boolean deviceExists) {
      oled.sendPictogramInfo(DisplayMode.TRACK, isRecording ? OledDisplay.Pict.REC : OledDisplay.Pict.NONE,
         isPlaying ? OledDisplay.Pict.PLAY : OledDisplay.Pict.NONE, trackName,
         deviceExists ? deviceName : "<NO DEVICE>");
   }

   private void updateTrackInfo(final boolean playing, final boolean recording) {
      updateTrackInfo(playing, recording, cursorTrack.name().get(), cursorDevice.name().get(),
         cursorDevice.exists().get());
   }

   private void updateTrackInfo() {
      updateTrackInfo(transport.isPlaying().get(), recordFocusMode.getState(transport), cursorTrack.name().get(),
         cursorDevice.name().get(), cursorDevice.exists().get());
   }

   public ValueObject<PadBank> getPadBank() {
      return padBank;
   }

   private void toBankMode(final PadBank bankMode) {
      padBank.set(bankMode);
   }

   private RelativeHardwareKnob createMainEncoder(final int ccNr) {
      final RelativeHardwareKnob mainEncoder = surface.createRelativeHardwareKnob("MAIN_ENCODER+_" + ccNr);
      final RelativeHardwareValueMatcher stepUpMatcher = midiIn.createRelativeValueMatcher(
         "(status == 176 && data1 == " + ccNr + " && data2 > 64)", 1);
      final RelativeHardwareValueMatcher stepDownMatcher = midiIn.createRelativeValueMatcher(
         "(status == 176 && data1 == " + ccNr + " && data2 < 63)", -1);
      final RelativeHardwareValueMatcher matcher = host.createOrRelativeHardwareValueMatcher(stepDownMatcher,
         stepUpMatcher);
      mainEncoder.setAdjustValueMatcher(matcher);
      mainEncoder.setStepSize(1);
      return mainEncoder;
   }

   private HardwareButton createEncoderPress(final int ccNr, final String name) {
      final HardwareButton encoderButton = surface.createHardwareButton(name);
      encoderButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0, ccNr, 127));
      encoderButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(0, ccNr, 0));
      return encoderButton;
   }

   private void mainEncoderAction(final int dir) {
      oled.disableValues();
      if (encoderDown.get()) {
         oled.enableValues(DisplayMode.PARAM_PAGE);
         if (!encoderPressTurned) {
            showParameterPage(parameterBank.selectedPageIndex().get());
         } else {
            navigateParametersBanks(dir);
         }
      } else {
         if (padBank.get() == PadBank.BANK_A) {
            oled.enableValues(DisplayMode.SCENE);
            oled.sendTextInfo(DisplayMode.SCENE, cursorTrack.name().get(), sceneTrackItem.name().get(), true);
            clipLaunchingLayer.navigateScenes(dir);
         } else {
            drumPadLayer.navigate(dir);
         }
      }
      encoderPressTurned = true;
   }

   private void mainEncoderShiftAction(final int dir) {
      encoderPressTurned = true;
      if (encoderDown.get()) {
         if (dir > 0) {
            cursorDevice.selectNext();
         } else {
            cursorDevice.selectPrevious();
         }
      } else {
         navigateTracks(dir);
      }
   }

   private void navigateTracks(final int dir) {
      if (dir > 0) {
         cursorTrack.selectNext();
      } else {
         cursorTrack.selectPrevious();
      }
   }

   private void navigateParametersBanks(final int dir) {
      if (dir > 0) {
         parameterBank.selectNext();
      } else {
         parameterBank.selectPrevious();
      }
   }

   private void setUpHardware() {
      mainEncoder = createMainEncoder(0x1C);
      shiftMainEncoder = createMainEncoder(0x1D);
      encoderPress = createEncoderPress(0x76, "ENCODER_PRESSED");
      shiftEncoderPress = createEncoderPress(0x77, "SHIFT_ENCODER_PRESSED");

      for (int i = 0; i < ENCODER_CC_MAPPING.length; i++) {
         knobs[i] = surface.createAbsoluteHardwareKnob("KNOB_" + (i + 1));
         knobs[i].setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, ENCODER_CC_MAPPING[i]));

         padBankAButtons[i] = new RgbButton(i + 0x34, PadBank.BANK_A, RgbButton.Type.NOTE, 0x24 + i, 9, false, this);
         padBankBButtons[i] = new RgbButton(i + 0x44, PadBank.BANK_B, RgbButton.Type.NOTE, 0x2C + i, 9, false, this);
      }

      for (int i = 0; i < SLIDER_CC_MAPPING.length; i++) {
         sliders[i] = surface.createHardwareSlider("FADER_" + (i + 1));
         sliders[i].setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, SLIDER_CC_MAPPING[i]));
      }
      shiftButton = surface.createHardwareButton("SHIFT");
      shiftButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0, 27, 127));
      shiftButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(0, 27, 0));
   }

   public RgbButton[] getPadBankAButtons() {
      return padBankAButtons;
   }

   public RgbButton[] getPadBankBButtons() {
      return padBankBButtons;
   }

   public CursorTrack getCursorTrack() {
      return cursorTrack;
   }

   public TrackBank getViewTrackBank() {
      return viewTrackBank;
   }

   public void updateBankState(final InternalHardwareLightState state) {
      if (state instanceof RgbBankLightState) {
         sysExHandler.sendBankState((RgbBankLightState) state);
      }
   }

   private void onMidi0(final ShortMidiMessage msg) {
      final int channel = msg.getChannel();
      final int sb = msg.getStatusByte() & (byte) 0xF0;
      if (channel == 9) {
         drumPadLayer.notifyNote(sb, msg.getData1());
      }
   }

   public HardwareSurface getSurface() {
      return surface;
   }

   public MidiIn getMidiIn() {
      return midiIn;
   }

   public MidiOut getMidiOut() {
      return midiOut;
   }

   @Override
   public void exit() {
      final CompletableFuture<Boolean> shutdown = new CompletableFuture<>();
      Executors.newSingleThreadExecutor().execute(() -> {
         oled.clearText();
         sysExHandler.disconnectState();
         try {
            Thread.sleep(100);
         } catch (final InterruptedException e) {
            e.printStackTrace();
         }
         shutdown.complete(true);
      });
      try {
         shutdown.get();
      } catch (final InterruptedException | ExecutionException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void flush() {
      surface.updateHardware();
   }

   /**
    * Make sure no scene is launched upon release.
    */
   public void notifyTurn() {
      encoderPressTurned = true;
   }


}
