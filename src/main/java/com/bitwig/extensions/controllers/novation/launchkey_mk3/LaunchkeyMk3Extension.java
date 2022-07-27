package com.bitwig.extensions.controllers.novation.launchkey_mk3;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.control.Button;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.control.ModeType;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.control.RgbCcButton;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.definition.LaunchkeyMk3ExtensionDefinition;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.layer.*;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

import java.util.ArrayList;
import java.util.List;

public class LaunchkeyMk3Extension extends ControllerExtension {
   public static final int QUANTIZE_BUTTON_CC = 75;
   public static final int CLICK_BUTTON_CC = 76;
   public static final int UNDO_BUTTON_CC = 77;

   public static final int SHIFT_BUTTON_CC = 108;
   public static final int START_BUTTON_CC = 115;
   public static final int STOP_BUTTON_CC = 116;
   public static final int REC_BUTTON_CC = 117;
   public static final int LOOP_BUTTON_CC = 118;

   public static final int HOLD_INIT_TIME = 500;
   public static final int HOLD_REPEAT_INTERVAL = 100;
   public static final int TEXT_TRANSIENT_TIME = 1500;
   public static final int TEXT_RELEASE_TRANSIENT_TIME = 700;


   private final boolean hasFaders;
   private Layers layers;
   private MidiIn midiIn;
   private MidiIn playMidiIn;
   private MidiOut midiOut;
   private HardwareSurface surface;
   private ControllerHost host;

   private Layer mainLayer;
   private Layer shiftLayer;
   private SessionLayer sessionLayer;
   private HwControls hwControl;
   private LcdDisplay lcdDisplay;

   private TrackBank trackBank;
   private Transport transport;
   private Application application;
   private MasterTrack masterTrack;
   private CursorTrack cursorTrack;
   private PinnableCursorDevice cursorDevice;
   private CursorRemoteControlsPage remoteControlBank;
   private DeviceSelectionLayer deviceSelectionLayer;

   private RepeatTask repeatWhileHoldTask = null;
   private DelayedTask delayedTask = null;
   private DeviceBank deviceBank;
   private DrumPadLayer drumPadLayer;
   private PinnableCursorDevice primaryDevice;

   public LaunchkeyMk3Extension(final LaunchkeyMk3ExtensionDefinition definition, final ControllerHost host,
                                final boolean hasFaders) {
      super(definition, host);
      this.hasFaders = hasFaders;
   }

   @Override
   public void init() {
      host = getHost();
      surface = host.createHardwareSurface();
      layers = new Layers(this);
      application = host.createApplication();
      midiIn = host.getMidiInPort(0);
      playMidiIn = host.getMidiInPort(1);
      final NoteInput noteInput = playMidiIn.createNoteInput("MIDI", getMask());
      noteInput.setShouldConsumeEvents(true);
      //playMidiIn.setMidiCallback((ShortMidiMessageReceivedCallback) this::onMidi1);

      midiIn.setMidiCallback((ShortMidiMessageReceivedCallback) this::onMidi0);
      midiIn.setSysexCallback(this::onSysEx);
      midiOut = host.getMidiOutPort(0);

      lcdDisplay = new LcdDisplay(midiOut, host);
      mainLayer = new Layer(layers, "MAIN_LAYER");
      shiftLayer = new Layer(layers, "SHIFT_LAYER");
      trackBank = host.createMainTrackBank(8, 2, 2);
      cursorTrack = host.createCursorTrack(2, 2);
      cursorDevice = cursorTrack.createCursorDevice();
      deviceBank = cursorTrack.createDeviceBank(8);
      remoteControlBank = cursorDevice.createCursorRemoteControlsPage(8);
      trackBank.followCursorTrack(cursorTrack);
      primaryDevice = cursorTrack.createCursorDevice("DrumDetection", "Pad Device", 2,
         CursorDeviceFollowMode.FIRST_INSTRUMENT);

      hwControl = new HwControls(this);
      initTransport();
      initDeviceHandlingButton();

      sessionLayer = new SessionLayer(this);
      deviceSelectionLayer = new DeviceSelectionLayer(this);
      drumPadLayer = new DrumPadLayer(this);

      final ControlLayer knobLayer = new ControlLayer("KNOB", this, hwControl.getKnobs(),
         LaunchkeyConstants.BUTTON_CC_KNOB, LaunchkeyConstants.KNOB_PARAM_OFFSET, ControlMode.PAN);
      final ControlLayer faderLayer = new ControlLayer("FADER", this, hwControl.getSliders(),
         LaunchkeyConstants.BUTTON_CC_FADER, LaunchkeyConstants.FADER_PARAM_OFFSET, ControlMode.VOLUME);

      final TrackControlLayer trackSelectLayer = new TrackControlLayer(this);
      ModeType.bindButton(PadMode.values(), this, "PADS", LaunchkeyConstants.BUTTON_CC_PADS, mainLayer,
         this::changePadMode);

      mainLayer.activate();
      trackSelectLayer.activate();
      faderLayer.activate();
      knobLayer.activate();
      final LaunchkeyMk3ExtensionDefinition extensionDefinition = (LaunchkeyMk3ExtensionDefinition) getExtensionDefinition();
      midiOut.sendSysex(LaunchkeyConstants.DEVICE_INQUIRY);
      host.showPopupNotification(String.format("Launchkey %d Mk3 Initialized", extensionDefinition.numberOfKeys()));
      host.scheduleTask(this::handlePing, 50);
   }

   private void changePadMode(final PadMode padMode) {
      switch (padMode) {
         case SESSION:
            drumPadLayer.setIsActive(false);
            sessionLayer.setIsActive(true);
            break;
         case DRUM:
            sessionLayer.setIsActive(false);
            drumPadLayer.setIsActive(true);
            break;
         default:
            sessionLayer.setIsActive(false);
            drumPadLayer.setIsActive(false);
            break;
      }
   }

   public void initDeviceHandlingButton() {
      final Button deviceButton = hwControl.getDeviceSelectButton();
      deviceButton.bindIsPressed(mainLayer, pressed -> {
         if (pressed) {
            deviceSelectionLayer.setIsActive(true);
            lcdDisplay.sendText(cursorDevice.name().get(), 0);
            lcdDisplay.sendText(deviceSelectionLayer.getCurrentBankName(), 1);
         } else {
            deviceSelectionLayer.setIsActive(false);
            lcdDisplay.clearDisplay();
         }
      });
   }

   public void initTransport() {
      transport = host.createTransport();
      masterTrack = host.createMasterTrack(2);

      final Clip cursorClip = getHost().createLauncherCursorClip(8, 128);
      final Clip arrangerClip = getHost().createArrangerCursorClip(8, 128);
      cursorClip.exists().markInterested();
      arrangerClip.exists().markInterested();

      mainLayer.bind(hwControl.getMasterSlider(), masterTrack.volume());
      masterTrack.volume().displayedValue().addValueObserver(v -> lcdDisplay.setValue(v, 88));
      masterTrack.name().addValueObserver(name -> lcdDisplay.setParameter("Volume - " + name, 88));

      final Button shiftButton = new Button(this, "SHIFT", SHIFT_BUTTON_CC, 0);
      shiftButton.bindIsPressed(mainLayer, pressed -> shiftLayer.setIsActive(pressed));

      final Button startButton = new Button(this, "START_BUTTON", START_BUTTON_CC, 15);
      startButton.bind(mainLayer, transport.playAction());
      startButton.bind(shiftLayer, transport.restartAction());

      final Button stopButton = new Button(this, "STOP_BUTTON", STOP_BUTTON_CC, 15);
      stopButton.bind(mainLayer, transport.stopAction());

      final Button recButton = new Button(this, "REC_BUTTON", REC_BUTTON_CC, 15);
      recButton.bind(mainLayer, transport.recordAction());
      recButton.bindToggle(shiftLayer, transport.isClipLauncherOverdubEnabled());

      final Button loopButton = new Button(this, "LOOP_BUTTON", LOOP_BUTTON_CC, 15);
      loopButton.bindToggle(mainLayer, transport.isArrangerLoopEnabled());

      final Button quantizeButton = new Button(this, "QUANTIZE_BUTTON", QUANTIZE_BUTTON_CC, 15);
      quantizeButton.bindPressed(mainLayer, () -> {
         cursorClip.quantize(1.0);
         arrangerClip.quantize(1.0);
         if (cursorClip.exists().get() || arrangerClip.exists().get()) {
            host.showPopupNotification("Quantize 100%");
         }
      });
      quantizeButton.bindPressed(shiftLayer, () -> {
         cursorClip.quantize(0.5);
         arrangerClip.quantize(0.5);
         if (cursorClip.exists().get() || arrangerClip.exists().get()) {
            host.showPopupNotification("Quantize 50%");
         }
      });

      final RgbCcButton pinButton = hwControl.getDeviceLockButton();
      cursorDevice.isPinned().markInterested();
      pinButton.bindPressed(mainLayer, () -> cursorDevice.isPinned().toggle(),
         () -> cursorDevice.isPinned().get() ? RgbState.WHITE : RgbState.OFF);

      final Button clickButton = new Button(this, "CLICK_BUTTON", CLICK_BUTTON_CC, 15);
      clickButton.bindToggle(mainLayer, transport.isMetronomeEnabled());
      clickButton.bind(shiftLayer, transport.tapTempoAction());

      final Button undoButton = new Button(this, "UNDO_BUTTON", UNDO_BUTTON_CC, 15);
      undoButton.bind(mainLayer, application.undoAction());
      undoButton.bind(shiftLayer, application.redoAction());

      final Button trackLeftButton = hwControl.getTrackLeftButton();
      trackLeftButton.bindIsPressed(mainLayer, pressed -> {
         if (pressed) {
            startHold(() -> cursorTrack.selectPrevious());
         } else {
            stopHold();
         }
      });
      final Button trackRightButton = hwControl.getTrackRightButton();
      trackRightButton.bindIsPressed(mainLayer, pressed -> {
         if (pressed) {
            startHold(() -> cursorTrack.selectNext());
         } else {
            stopHold();
         }
      });
   }

   public void startHold(final int initTime, final int repeatTime, final Runnable action) {
      action.run();
      repeatWhileHoldTask = new RepeatTask(initTime, repeatTime, action);
   }

   public void startHold(final Runnable action) {
      startHold(HOLD_INIT_TIME, HOLD_REPEAT_INTERVAL, action);
   }

   public void stopHold() {
      repeatWhileHoldTask = null;
   }

   public void setTransientText(final String row1, final String row2) {
      lcdDisplay.sendText(row1, 0);
      lcdDisplay.sendText(row2, 1);
      delayedTask = new DelayedTask(TEXT_TRANSIENT_TIME, () -> lcdDisplay.clearDisplay());
   }

   public void releaseText() {
      delayedTask = new DelayedTask(TEXT_RELEASE_TRANSIENT_TIME, () -> lcdDisplay.clearDisplay());
   }

   private void handlePing() {
      if (repeatWhileHoldTask != null) {
         repeatWhileHoldTask.ping();
      }
      if (delayedTask != null && delayedTask.expired()) {
         delayedTask.execute();
         delayedTask = null;
      }
      host.scheduleTask(this::handlePing, 50);
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

   public Layers getLayers() {
      return layers;
   }

   public TrackBank getTrackBank() {
      return trackBank;
   }

   public PinnableCursorDevice getCursorDevice() {
      return cursorDevice;
   }

   public CursorRemoteControlsPage getRemoteControlBank() {
      return remoteControlBank;
   }

   public CursorTrack getCursorTrack() {
      return cursorTrack;
   }

   public HwControls getHwControl() {
      return hwControl;
   }

   public LcdDisplay getLcdDisplay() {
      return lcdDisplay;
   }

   public DeviceBank getDeviceBank() {
      return deviceBank;
   }

   public PinnableCursorDevice getPrimaryDevice() {
      return primaryDevice;
   }

   public void println(final String format, final Object... args) {
      host.println(String.format(format, args));
   }

   private String[] getMask() {
      final List<String> masks = new ArrayList<>();
      masks.add("8?????"); // Note On
      masks.add("9?????"); // Note Off
      masks.add("A?????"); // Poly Aftertouch
      masks.add("D?????"); // Channel Aftertouch
      masks.add("B?????"); // CCss
      masks.add("E?????"); // Pitchbend
      return masks.toArray(String[]::new);
   }

   private void onMidi0(final ShortMidiMessage msg) {
      final int channel = msg.getChannel();
      final int sb = msg.getStatusByte() & (byte) 0xF0;
      if (channel == LaunchkeyConstants.DRUM_CHANNEL) {
         drumPadLayer.notifyNote(sb, msg.getData1(), msg.getData2());
      }
      // if (sb != 0xA0) {
      // getHost().println("MIDI " + sb + " <" + channel + "> " + msg.getData1() + " " + msg.getData2());
      // }
   }

   private void onSysEx(final String sysEx) {
      if (sysEx.startsWith(LaunchkeyConstants.SYS_EX_DEVICE_INQUIRY_HEAD)) {
         final String deviceId = LaunchkeyConstants.getDeviceId(sysEx);
         final String appId = LaunchkeyConstants.getAppId(sysEx);
         println("Connect device=%s app=%s", deviceId, appId);
         setDawMode(true);
         changePadMode(PadMode.SESSION);
      } else {
         host.println("Launchkey SysEx= : " + sysEx);
      }
   }

   private void setDawMode(final boolean enable) {
      midiOut.sendMidi(0x9F, 0xC, enable ? 0x7f : 0x00);
      //midiOut.sendMidi(0xBF, 0xC, enable ? 0x7f : 0x00);
   }

   @Override
   public void exit() {
      setDawMode(false);
      host.println(" *************** EXIT Launchkey");
   }

   @Override
   public void flush() {
      surface.updateHardware();
   }


}
