package com.bitwig.extensions.controllers.novation.launchkey_mk3;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.control.Button;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.control.RgbCcButton;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.layer.*;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

import java.util.ArrayList;
import java.util.List;

public class LaunchkeyMk3Extension extends ControllerExtension {
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

   private HoldTask holdTask = null;

   protected LaunchkeyMk3Extension(final LaunchkeyMk3ExtensionDefinition definition, final ControllerHost host) {
      super(definition, host);
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
      remoteControlBank = cursorDevice.createCursorRemoteControlsPage(8);
      trackBank.followCursorTrack(cursorTrack);

      hwControl = new HwControls(this);
      initTransport();
      initDeviceHandlingButton();

      sessionLayer = new SessionLayer(this);
      deviceSelectionLayer = new DeviceSelectionLayer(this);

      final ControlLayer knobLayer = new ControlLayer("KNOB", this, hwControl.getKnobs(), 9, 56, ControlMode.PAN);
      final ControlLayer faderLayer = new ControlLayer("FADER", this, hwControl.getSliders(), 10, 80,
         ControlMode.VOLUME);
      final TrackControlLayer trackSelectLayer = new TrackControlLayer(this);

      mainLayer.activate();
      sessionLayer.activate();
      trackSelectLayer.activate();
      faderLayer.activate();
      knobLayer.activate();
      host.println("########### Init Launchkey Mk3 ############ ");
      midiOut.sendSysex("f07e7f0601f7");
      host.showPopupNotification("Launchkey Initialized");
      host.scheduleTask(this::handlePing, 100);
   }

   public void initDeviceHandlingButton() {
      final Button deviceButton = hwControl.getDeviceSelectButton();
      deviceButton.bindIsPressed(mainLayer, pressed -> {
         if (pressed) {
            deviceSelectionLayer.setIsActive(true);
         } else {
            deviceSelectionLayer.setIsActive(false);
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

      final Button shiftButton = new Button(this, "SHIFT", 108, 0);
      shiftButton.bindIsPressed(mainLayer, pressed -> shiftLayer.setIsActive(pressed));

      final Button startButton = new Button(this, "START_BUTTON", 115, 15);
      startButton.bind(mainLayer, transport.playAction());
      startButton.bind(shiftLayer, transport.restartAction());

      final Button stopButton = new Button(this, "STOP_BUTTON", 116, 15);
      stopButton.bind(mainLayer, transport.stopAction());

      final Button recButton = new Button(this, "REC_BUTTON", 117, 15);
      recButton.bind(mainLayer, transport.recordAction());
      recButton.bindToggle(shiftLayer, transport.isClipLauncherOverdubEnabled());

      final Button loopButton = new Button(this, "LOOP_BUTTON", 118, 15);
      loopButton.bindToggle(mainLayer, transport.isArrangerLoopEnabled());

      final Button quantizeButton = new Button(this, "QUANTIZE_BUTTON", 75, 15);
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
         () -> cursorDevice.isPinned().get() ? RgbState.OFF : RgbState.WHITE);

      final Button clickButton = new Button(this, "CLICK_BUTTON", 76, 15);
      clickButton.bindToggle(mainLayer, transport.isMetronomeEnabled());
      clickButton.bind(shiftLayer, transport.tapTempoAction());

      final Button undoButton = new Button(this, "UNDO_BUTTON", 77, 15);
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
      holdTask = new HoldTask(initTime, repeatTime, action);
   }

   public void startHold(final Runnable action) {
      startHold(500, 90, action);
   }

   public void stopHold() {
      holdTask = null;
   }

   private void handlePing() {
      if (holdTask != null) {
         holdTask.ping();
      }
      host.scheduleTask(this::handlePing, 100);
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
      if (sb != 0xA0) {
         getHost().println("MIDI " + sb + " <" + channel + "> " + msg.getData1() + " " + msg.getData2());
      }
   }

//   private void onMidi1(final ShortMidiMessage msg) {
//      final int channel = msg.getChannel();
//      final int sb = msg.getStatusByte() & (byte) 0xF0;
//      getHost().println("Play " + sb + " <" + channel + "> " + msg.getData1() + " " + msg.getData2());
//   }

   private void onSysEx(final String sysEx) {
      if (sysEx.equals("f07e0006020020293601000000010907f7")) {
         setDawMode(true);
      }
      host.println("Launchkey SysEx= : " + sysEx);
   }

   private void setDawMode(final boolean enable) {
      host.println("to daw mode > " + enable);
      midiOut.sendMidi(0x9F, 0xC, enable ? 0x7f : 0x00);
      //midiOut.sendMidi(0xBF, 0xC, enable ? 0x7f : 0x00);
   }

   @Override
   public void exit() {
      setDawMode(false);
      host.println("EXIT Launchkey");
   }

   @Override
   public void flush() {
      surface.updateHardware();
   }


}
