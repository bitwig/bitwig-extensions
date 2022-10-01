package com.bitwig.extensions.controllers.nativeinstruments.komplete;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.Layers;

public abstract class KompleteKontrolExtension extends ControllerExtension {
   static final int KOMPLETE_KONTROL_DEVICE_ID = 1315523403;
   static final String KOMPLETE_KONTROL_VST3_ID = "5653544E694B4B6B6F6D706C65746520";

   final NhiaSysexValueCommand trackAvailableCommand = new NhiaSysexValueCommand(0x40);
   final NhiaSysexTextCommand selectTrackCommand = new NhiaSysexTextCommand(0x41);
   final NhiaSysexValueCommand trackSelectedCommand = new NhiaSysexValueCommand(0x42);
   final NhiaSysexValueCommand trackMutedCommand = new NhiaSysexValueCommand(0x43);
   final NhiaSysexValueCommand trackSoloCommand = new NhiaSysexValueCommand(0x44);
   final NhiaSysexValueCommand trackArmedCommand = new NhiaSysexValueCommand(0x45);
   final NhiaSysexTextCommand trackVolumeTextCommand = new NhiaSysexTextCommand(0x46);
   final NhiaSysexTextCommand trackPanTextCommand = new NhiaSysexTextCommand(0x47);
   final NhiaSysexTextCommand trackNameCommand = new NhiaSysexTextCommand(0x48);
   final NhiaSyexLevelsCommand trackLevelMeterComand = new NhiaSyexLevelsCommand(0x49);
   final NhiaSysexValueCommand trackMutedBySoloCommand = new NhiaSysexValueCommand(0x4A);

   private SpecificPluginDevice kompleteKontrolPluginVst2;
   private SpecificPluginDevice kompleteKontrolVst3Id;

   protected HardwareSurface surface;
   protected MidiOut midiOutDaw;
   protected TrackBank mixerTrackBank;
   protected Transport mTransport;
   final RelativeHardwareKnob[] volumeKnobs = new RelativeHardwareKnob[8];
   final RelativeHardwareKnob[] panKnobs = new RelativeHardwareKnob[8];

   protected MidiIn midiIn;
   protected Layers layers;
   protected KompleteLayer mainLayer;
   protected Application application;
   // private Clip cursorClip;
   protected CursorTrack cursorTrack;

   protected boolean sceneNavMode = false;

   protected LayoutType currentLayoutType = LayoutType.LAUNCHER;
   protected Project project;

   boolean dawModeConfirmed = false;
   protected KompleteLayer arrangeFocusLayer;
   protected KompleteLayer sessionFocusLayer;
   private String lastReportedKKInstance = null;

   protected KompleteKontrolExtension(final ControllerExtensionDefinition definition, final ControllerHost host) {
      super(definition, host);
   }

   @Override
   public void init() {
      final ControllerHost host = getHost();
      application = host.createApplication();
      midiOutDaw = host.getMidiOutPort(0);
      midiIn = host.getMidiInPort(0);
      midiIn.setMidiCallback((ShortMidiMessageReceivedCallback) this::onMidi0);
   }

   protected void onMidi0(final ShortMidiMessage msg) {
      if (msg.getStatusByte() == 0xBF) {
         if (msg.getData1() == 1) {
            dawModeConfirmed = true;
         }
      }
   }

   protected void intoDawMode() {
      midiOutDaw.sendMidi(0xBF, 0x1, 0x3);
   }

   protected void sendLedUpdate(final CcAssignment assignement, final int value) {
      midiOutDaw.sendMidi(0xBF, assignement.getStateId(), value);
   }

   protected void sendLedUpdate(final int code, final int value) {
      midiOutDaw.sendMidi(0xBF, code, value);
   }

   protected abstract void initNavigation();

   protected void setUpChannelControl(final int index, final Track channel) {
      final IndexButton selectButton = new IndexButton(this, index, "SELECT_BUTTON", 0x42);
      mainLayer.bindPressed(selectButton.getHwButton(), () -> {
         if (!channel.exists().get()) {
            application.createInstrumentTrack(-1);
         } else {
            channel.selectInMixer();
         }
      });
      final IndexButton muteButton = new IndexButton(this, index, "MUTE_BUTTON", 0x43);
      mainLayer.bindPressed(muteButton.getHwButton(), () -> channel.mute().toggle());
      final IndexButton soloButton = new IndexButton(this, index, "SOLO_BUTTON", 0x44);
      mainLayer.bindPressed(soloButton.getHwButton(), () -> channel.solo().toggle());
      final IndexButton armButton = new IndexButton(this, index, "ARM_BUTTON", 0x45);
      mainLayer.bindPressed(armButton.getHwButton(), () -> channel.arm().toggle());

      channel.exists().markInterested();
      channel.addIsSelectedInMixerObserver(v -> trackSelectedCommand.send(midiOutDaw, index, v));
      channel.mute().addValueObserver(v -> trackMutedCommand.send(midiOutDaw, index, v));
      channel.solo().addValueObserver(v -> trackSoloCommand.send(midiOutDaw, index, v));
      channel.arm().addValueObserver(v -> trackArmedCommand.send(midiOutDaw, index, v));
      channel.isMutedBySolo().addValueObserver(v -> trackMutedBySoloCommand.send(midiOutDaw, index, v));

      channel.name().addValueObserver(name -> trackNameCommand.send(midiOutDaw, index, name));

      channel.volume()
         .displayedValue()
         .addValueObserver(valueText -> trackVolumeTextCommand.send(midiOutDaw, index, valueText));

      setUpChannelDisplayFeedback(index, channel);
      channel.trackType().addValueObserver(v -> {
         final TrackType type = TrackType.toType(v);
         trackAvailableCommand.send(midiOutDaw, index, type.getId());
      });
      volumeKnobs[index].addBindingWithSensitivity(channel.volume(), 0.025);
      panKnobs[index].addBindingWithSensitivity(channel.pan(), 0.025);

      channel.isActivated().markInterested();
      channel.canHoldAudioData().markInterested();
      channel.canHoldNoteData().markInterested();
   }

   protected abstract void setUpChannelDisplayFeedback(final int index, final Track channel);

   @Override
   public void exit() {
   }

   @Override
   public void flush() {
      surface.updateHardware();
   }

   public Layers getLayers() {
      return layers;
   }

   public HardwareSurface getSurface() {
      return surface;
   }

   public MidiIn getMidiIn() {
      return midiIn;
   }

   protected void initJogWheel() {
      final RelativeHardwareKnob fourDKnob = surface.createRelativeHardwareKnob("4D_WHEEL_PLUGIN_MODE");
      fourDKnob.setAdjustValueMatcher(midiIn.createRelative2sComplementCCValueMatcher(0xF, 0x34, 128));
      fourDKnob.setStepSize(1 / 128.0);

      final HardwareActionBindable incAction = getHost().createAction(() -> mTransport.fastForward(), () -> "+");
      final HardwareActionBindable decAction = getHost().createAction(() -> mTransport.rewind(), () -> "-");
      fourDKnob.addBinding(getHost().createRelativeHardwareControlStepTarget(incAction, decAction));

      final RelativeHardwareKnob fourDKnobMixer = surface.createRelativeHardwareKnob("4D_WHEEL_MIX_MODE");
      fourDKnobMixer.setAdjustValueMatcher(midiIn.createRelative2sComplementCCValueMatcher(0xF, 0x64, 4096));
      fourDKnobMixer.setStepSize(1 / 128.0);

      final HardwareActionBindable incMixAction = getHost().createAction(() -> {
      }, () -> "+");
      final HardwareActionBindable decMixAction = getHost().createAction(() -> {
      }, () -> "-");
      fourDKnobMixer.addBinding(getHost().createRelativeHardwareControlStepTarget(incMixAction, decMixAction));
   }

   protected void setUpSliders(final MidiIn midiIn) {
      for (int i = 0; i < 8; i++) {
         final RelativeHardwareKnob knob = surface.createRelativeHardwareKnob("VOLUME_KNOB" + i);
         volumeKnobs[i] = knob;
         knob.setAdjustValueMatcher(midiIn.createRelative2sComplementCCValueMatcher(0xF, 0x50 + i, 128));
         knob.setStepSize(1 / 128.0);

         final RelativeHardwareKnob panKnob = surface.createRelativeHardwareKnob("PAN_KNOB" + i);
         panKnobs[i] = panKnob;
         panKnob.setAdjustValueMatcher(midiIn.createRelative2sComplementCCValueMatcher(0xF, 0x58 + i, 128));
         panKnob.setStepSize(1 / 128.0);
      }
   }

   protected void bindMacroControl(final PinnableCursorDevice device, final MidiIn midiIn) {
      final CursorRemoteControlsPage remote = device.createCursorRemoteControlsPage(8);
      for (int i = 0; i < 8; i++) {
         final AbsoluteHardwareKnob knob = surface.createAbsoluteHardwareKnob("MACRO_" + i);
         knob.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, 14 + i));
         final RemoteControl parameter = remote.getParameter(i);
         parameter.setIndication(true);
         mainLayer.bind(knob, parameter);
      }
   }

   protected void createKompleteKontrolDeviceKompleteKontrol(final PinnableCursorDevice cursorDevice) {
      kompleteKontrolVst3Id = cursorDevice.createSpecificVst3Device(KOMPLETE_KONTROL_VST3_ID);
      final Parameter kompleteKontrolVst3InstId = kompleteKontrolVst3Id.createParameter(0);
      kompleteKontrolVst3InstId.name().addValueObserver(this::updateKompleteKontroInstance);

      kompleteKontrolPluginVst2 = cursorDevice.createSpecificVst2Device(KOMPLETE_KONTROL_DEVICE_ID);
      final Parameter kompleteKontrolVst2InstId = kompleteKontrolPluginVst2.createParameter(0);
      kompleteKontrolVst2InstId.markInterested();
      kompleteKontrolVst2InstId.name().markInterested();
      kompleteKontrolVst2InstId.exists().markInterested();
      kompleteKontrolVst2InstId.name().addValueObserver(this::updateKompleteKontroInstance);
   }

   private void updateKompleteKontroInstance(final String instanceParamName) {
      if (lastReportedKKInstance == null || !lastReportedKKInstance.equals(instanceParamName)) {
         selectTrackCommand.send(midiOutDaw, instanceParamName);
         lastReportedKKInstance = instanceParamName;
      }
   }

   protected void initTrackBank() {
      initNavigation();

      final PinnableCursorDevice cursorDevice = cursorTrack.createCursorDevice();

      createKompleteKontrolDeviceKompleteKontrol(cursorDevice);
      mixerTrackBank = getHost().createTrackBank(8, 0, 1);
      mixerTrackBank.setSkipDisabledItems(true);
      mixerTrackBank.canScrollChannelsDown().markInterested();
      mixerTrackBank.canScrollChannelsUp().markInterested();

      final HardwareButton trackNavLeftButton = surface.createHardwareButton("TRACK_LEFT_NAV_BUTTON");
      trackNavLeftButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x31, 127));
      final HardwareButton trackRightNavButton = surface.createHardwareButton("TRACK_RIGHT_NAV_BUTTON");
      trackRightNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x31, 1));
      mixerTrackBank.setChannelScrollStepSize(8);

      final HardwareButton muteSelectedButton = surface.createHardwareButton("MUTE_SELECTED_BUTTON");
      muteSelectedButton.pressedAction().setActionMatcher(CcAssignment.MUTE_CURRENT.createActionMatcher(midiIn, 1));

      final HardwareButton soloSelectedButton = surface.createHardwareButton("SOLO_SELECTED_BUTTON");
      soloSelectedButton.pressedAction().setActionMatcher(CcAssignment.SOLO_CURRENT.createActionMatcher(midiIn, 1));

      mainLayer.bindPressed(muteSelectedButton, cursorTrack.mute().toggleAction());
      mainLayer.bindPressed(soloSelectedButton, cursorTrack.solo().toggleAction());

      mainLayer.bindPressed(trackNavLeftButton, () -> mixerTrackBank.scrollBy(-8));

      mainLayer.bindPressed(trackRightNavButton, () -> mixerTrackBank.scrollBy(8));

      mixerTrackBank.canScrollChannelsUp().addValueObserver(v -> {
         final int bw = mixerTrackBank.canScrollChannelsDown().get() ? 0x2 : 0x0;
         sendLedUpdate(0x31, (v ? 0x1 : 0x0) | bw);
      });
      mixerTrackBank.canScrollChannelsDown().addValueObserver(v -> {
         final int bw = mixerTrackBank.canScrollChannelsUp().get() ? 0x1 : 0x0;
         sendLedUpdate(0x31, (v ? 0x2 : 0x0) | bw);
      });

      for (int i = 0; i < 8; i++) {
         setUpChannelControl(i, mixerTrackBank.getItemAt(i));
      }
   }

   public void setUpTransport() {
      final Preferences preferences = getHost().getPreferences(); // THIS
      final SettableEnumValue focusMode = preferences.getEnumSetting("Focus", //
         "Recording/Automation", new String[]{FocusMode.LAUNCHER.getDescriptor(), FocusMode.ARRANGER.getDescriptor()},
         FocusMode.ARRANGER.getDescriptor());
      final ModeButton recButton = new ModeButton(this, "REC_BUTTON", CcAssignment.REC);
      final ModeButton autoButton = new ModeButton(this, "AUTO_BUTTON", CcAssignment.AUTO);
      final ModeButton countInButton = new ModeButton(this, "COUNTIN_BUTTON", CcAssignment.COUNTIN);
      focusMode.markInterested();

      arrangeFocusLayer.bindToggle(recButton.getHwButton(), mTransport.isArrangerRecordEnabled());
      arrangeFocusLayer.bindToggle(autoButton.getHwButton(), mTransport.isArrangerAutomationWriteEnabled());
      arrangeFocusLayer.bindToggle(countInButton.getHwButton(), mTransport.isArrangerOverdubEnabled());

      sessionFocusLayer.bindToggle(recButton.getHwButton(), mTransport.isClipLauncherOverdubEnabled());
      sessionFocusLayer.bindToggle(autoButton.getHwButton(), mTransport.isClipLauncherAutomationWriteEnabled());
      sessionFocusLayer.bindToggle(countInButton.getHwButton(), mTransport.isClipLauncherOverdubEnabled());

      focusMode.addValueObserver(newValue -> {
         final FocusMode newMode = FocusMode.toMode(newValue);
         switch (newMode) {
            case ARRANGER:
               sessionFocusLayer.deactivate();
               arrangeFocusLayer.activate();
               break;
            case LAUNCHER:
               arrangeFocusLayer.deactivate();
               sessionFocusLayer.activate();
               break;
            default:
               break;
         }
      });

      final ModeButton playButton = new ModeButton(this, "PLAY_BUTTON", CcAssignment.PLAY);
      mainLayer.bindToggle(playButton.getHwButton(), mTransport.isPlaying());
      mainLayer.bindLightState(playButton, mTransport.isPlaying());
      final ModeButton restartButton = new ModeButton(this, "RESTART_BUTTON", CcAssignment.RESTART);
      mainLayer.bindPressed(restartButton.getHwButton(), () -> mTransport.launchFromPlayStartPosition());
      final ModeButton stopButton = new ModeButton(this, "STOP_BUTTON", CcAssignment.STOP);
      mainLayer.bindPressed(stopButton.getHwButton(), mTransport.stopAction());

      final ModeButton loopButton = new ModeButton(this, "LOOP_BUTTON", CcAssignment.LOOP);
      mainLayer.bindToggle(loopButton.getHwButton(), mTransport.isArrangerLoopEnabled());
      final ModeButton metroButton = new ModeButton(this, "METRO_BUTTON", CcAssignment.METRO);
      mainLayer.bindToggle(metroButton.getHwButton(), mTransport.isMetronomeEnabled());
      final ModeButton tapTempoButton = new ModeButton(this, "TAP_BUTTON", CcAssignment.TAPTEMPO);
      mainLayer.bindPressed(tapTempoButton.getHwButton(), mTransport::tapTempo);
      tapTempoButton.bindLightToPressed();

      final ModeButton undoButton = new ModeButton(this, "UNDO_BUTTON", CcAssignment.UNDO);
      mainLayer.bindPressed(undoButton.getHwButton(), () -> application.undo());
      undoButton.getLed().isOn().setValue(true); // As long as there is no canUndo

      final ModeButton redoButton = new ModeButton(this, "REDO_BUTTON", CcAssignment.REDO);
      mainLayer.bindPressed(redoButton.getHwButton(), () -> application.redo());
      redoButton.getLed().isOn().setValue(true);

   }

}
