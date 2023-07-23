package com.bitwig.extensions.controllers.akai.mpkminiplus;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.time.TimedEvent;
import com.bitwig.extensions.framework.values.Midi;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MpkMiniPlusControllerExtension extends ControllerExtension {

   public static final String DEVICE_INQUIRY = "F07E7F0601F7";
   public static final String EXPECTED_DEVICE_RESPONSE = "f07e7f06020020";
   private static final double[] TRANSPORT_STAGES = {4.0, 8.0, 16.0, 32.0, 64.0};
   private static final int KNOB_CC = 70;
   private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("hh:mm:ss SSS");
   private static ControllerHost debugHost;
   private Clip cursorClip;

   public static void println(final String format, final Object... args) {
      if (debugHost != null) {
         final LocalDateTime now = LocalDateTime.now();
         debugHost.println(now.format(DF) + " > " + String.format(format, args));
      }
   }

   private MidiIn midiIn;
   private MidiOut midiOut;
   private CursorTrack cursorTrack;
   private PinnableCursorDevice cursorDevice;
   private CursorRemoteControlsPage remoteControlsPage;
   private HardwareSurface surface;
   private final AbsoluteHardwareKnob[] knobs = new AbsoluteHardwareKnob[8];
   private RecordFocusMode recordFocusMode = RecordFocusMode.LAUNCHER;
   private TransportButtonMode transportButtonMode = TransportButtonMode.TRANSPORT;
   private Transport transport;
   private ControllerHost host;
   private final Queue<TimedEvent> timedEvents = new ConcurrentLinkedQueue<>();
   private final Map<TransportButtonMode, Layer> transportButtonLayers = new EnumMap(TransportButtonMode.class);
   private Layer currenTransportLayer;

   public MpkMiniPlusControllerExtension(final MpkMiniPlusControllerExtensionDefinition definition,
                                         final ControllerHost host) {
      super(definition, host);
   }

   @Override
   public void init() {
      this.host = getHost();
      debugHost = host;
      midiIn = host.getMidiInPort(0);
      midiIn.setMidiCallback(this::onMidiCallback);
      midiIn.setSysexCallback(this::handleSysEx);

      final NoteInput keyboardInput = midiIn.createNoteInput("Keys", "80????", "90????", "b001??", "e0????", "b040??",
         "D0????", "b00A??", "b00B??");
      keyboardInput.setShouldConsumeEvents(true);
      final NoteInput padsInput = midiIn.createNoteInput("Pads", "89????", "99????", "D9????", "A9????");
      padsInput.setShouldConsumeEvents(true);
      midiOut = host.getMidiOutPort(0);
      cursorTrack = host.createCursorTrack(0, 0);
      cursorDevice = cursorTrack.createCursorDevice();

      remoteControlsPage = cursorDevice.createCursorRemoteControlsPage(8);
      surface = getHost().createHardwareSurface();
      transport = host.createTransport();

      final Layers layers = new Layers(this);
      final Layer mainLayer = new Layer(layers, "Main");
      Arrays.stream(TransportButtonMode.values())
         .forEach(mode -> transportButtonLayers.put(mode, new Layer(layers, mode.toString())));
      currenTransportLayer = transportButtonLayers.get(transportButtonMode);
      initTransport(mainLayer);
      initRemotes(mainLayer);
      initPreferences(host);
      mainLayer.activate();
      currenTransportLayer.setIsActive(true);
      midiOut.sendSysex(DEVICE_INQUIRY);
      host.requestFlush();
      handlePing();
   }

   private void handleSysEx(String sysEx) {
      //println("SYSEX = %s", sysEx);
   }

   public void queueEvent(final TimedEvent event) {
      timedEvents.add(event);
   }

   private void handlePing() {
      if (!timedEvents.isEmpty()) {
         for (final TimedEvent event : timedEvents) {
            event.process();
            if (event.isCompleted()) {
               timedEvents.remove(event);
            }
         }
      }
      host.scheduleTask(this::handlePing, 50);
   }

   private void initRemotes(Layer layer) {
      for (int i = 0; i < knobs.length; i++) {
         int index = i;
         knobs[i] = surface.createAbsoluteHardwareKnob("KNOB_" + i);
         knobs[i].setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, KNOB_CC + i));
         RemoteControl parameter = remoteControlsPage.getParameter(i);
         layer.bind(knobs[i], parameter);
//         parameter.value().addValueObserver(128, value -> {
//            midiOut.sendMidi(Midi.NOTE_ON, KNOB_CC + index, value);
//         });
      }
   }

   private void initTransport(Layer layer) {
      transport.isPlaying().markInterested();
      transport.playStartPosition().markInterested();
      transport.isClipLauncherOverdubEnabled().markInterested();
      transport.isArrangerRecordEnabled().markInterested();

      HardwareButton playButton = createButton("PLAY", 0x76);
      layer.bindPressed(playButton, transport.playAction());
      layer.bind(transport.isPlaying(), (OnOffHardwareLight) playButton.backgroundLight());
      HardwareButton stopButton = createButton("STOP", 0x75);
      layer.bindPressed(stopButton, transport.stopAction());
      layer.bind(transport.isPlaying(), (OnOffHardwareLight) stopButton.backgroundLight());

      HardwareButton recordButton = createButton("RECORD", 0x77);
      layer.bindPressed(recordButton, this::handleRecordButton);
      layer.bind(this::recordButtonState, (OnOffHardwareLight) recordButton.backgroundLight());

      HardwareButton rewButton = createButton("REWIND", 0x73);
      MpkButton rewButtonMpk = new MpkButton(this, rewButton);
      OnOffHardwareLight rewindLight = (OnOffHardwareLight) rewButton.backgroundLight();
      rewButton.isPressed().markInterested();
      HardwareButton ffwButton = createButton("FFORWARD", 0x74);
      MpkButton ffwButtonMpk = new MpkButton(this, ffwButton);
      OnOffHardwareLight forwardLight = (OnOffHardwareLight) ffwButton.backgroundLight();
      ffwButton.isPressed().markInterested();

      Layer transportLayer = transportButtonLayers.get(TransportButtonMode.TRANSPORT);
      rewButtonMpk.bindRepeatHold(transportLayer, repeat -> moveTransport(repeat, -1));
      transportLayer.bind(rewButton.isPressed(), rewindLight);
      ffwButtonMpk.bindRepeatHold(transportLayer, repeat -> moveTransport(repeat, 1));
      transportLayer.bind(ffwButton.isPressed(), forwardLight);

      Layer deviceLayer = transportButtonLayers.get(TransportButtonMode.DEVICE);
      rewButtonMpk.bindPressed(deviceLayer, () -> remoteControlsPage.selectPreviousPage(true));
      deviceLayer.bind(rewButton.isPressed(), rewindLight);
      ffwButtonMpk.bindPressed(deviceLayer, () -> remoteControlsPage.selectNextPage(true));
      deviceLayer.bind(ffwButton.isPressed(), forwardLight);

      Layer trackLayer = transportButtonLayers.get(TransportButtonMode.TRACK_SELECTION);
      rewButtonMpk.bindPressed(trackLayer, () -> cursorTrack.selectPrevious());
      trackLayer.bind(rewButton.isPressed(), rewindLight);
      ffwButtonMpk.bindPressed(trackLayer, () -> cursorTrack.selectNext());
      trackLayer.bind(ffwButton.isPressed(), forwardLight);
   }

   private boolean recordButtonState() {
      return switch (recordFocusMode) {
         case LAUNCHER -> transport.isClipLauncherOverdubEnabled().get();
         case ARRANGER -> transport.isArrangerRecordEnabled().get();
      };
   }

   private boolean clipLaunchingState() {
      return transport.isClipLauncherOverdubEnabled().get();
   }

   private void handleRecordButton() {
      switch (recordFocusMode) {
         case LAUNCHER -> transport.isClipLauncherOverdubEnabled().toggle();
         case ARRANGER -> transport.isArrangerRecordEnabled().toggle();
      }
   }

   private void moveTransport(int counter, int dir) {
      int stage = Math.min(counter / 10, TRANSPORT_STAGES.length - 1);
      changePlayPosition(dir, TRANSPORT_STAGES[stage], true, true);
   }

   private HardwareButton createButton(String name, int midiId) {
      HardwareButton hwButton = surface.createHardwareButton(name + "_" + midiId);
      hwButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0, midiId, 127));
      hwButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(0, midiId, 0));
      OnOffHardwareLight light = surface.createOnOffHardwareLight(name + "_LIGHT_" + midiId);
      hwButton.isPressed().markInterested();
      hwButton.setBackgroundLight(light);
      light.onUpdateHardware(() -> midiOut.sendMidi(Midi.NOTE_ON, midiId, light.isOn().currentValue() ? 127 : 0));
      return hwButton;
   }

   void initPreferences(final ControllerHost host) {
      DocumentState documentState = host.getDocumentState();
      final SettableEnumValue recordButtonAssignment = documentState.getEnumSetting("Record Button assignment", //
         "Transport", RecordFocusMode.toSelector(), recordFocusMode.getDescriptor());
      recordButtonAssignment.addValueObserver(value -> recordFocusMode = RecordFocusMode.toMode(value));

      final SettableEnumValue transportButtonAssignment = documentState.getEnumSetting("<< >> Button Function", //
         "Transport", TransportButtonMode.toSelector(), transportButtonMode.getDescriptor());
      transportButtonAssignment.addValueObserver(value -> {
         transportButtonMode = TransportButtonMode.toMode(value);
         currenTransportLayer.setIsActive(false);
         currenTransportLayer = transportButtonLayers.get(transportButtonMode);
         currenTransportLayer.setIsActive(true);
      });
   }

   private void onMidiCallback(int msg, int data1, int data2) {
      println("MIDI IN = > %02X %02X %02X", msg, data1, data2);
   }


   @Override
   public void exit() {
   }

   @Override
   public void flush() {
      surface.updateHardware();
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

}
