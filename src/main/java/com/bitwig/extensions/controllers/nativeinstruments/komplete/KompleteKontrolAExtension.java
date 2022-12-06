package com.bitwig.extensions.controllers.nativeinstruments.komplete;

import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.Layers;

public class KompleteKontrolAExtension extends KompleteKontrolExtension {

   protected KompleteKontrolAExtension(final ControllerExtensionDefinition definition, final ControllerHost host) {
      super(definition, host);
   }

   @Override
   public void init() {
      super.init();
      intoDawMode();
      final ControllerHost host = getHost();

      surface = host.createHardwareSurface();
      surface.setPhysicalSize(200, 100);

      layers = new Layers(this);
      mainLayer = new KompleteLayer(this, "Main");
      arrangeFocusLayer = new KompleteLayer(this, "ArrangeFocus");
      sessionFocusLayer = new KompleteLayer(this, "SessionFocus");

      project = host.getProject();
      mTransport = host.createTransport();

      setUpSliders(midiIn);
      final MidiIn midiIn2 = host.getMidiInPort(1);
      final NoteInput noteInput = midiIn2.createNoteInput("MIDI", "80????", "90????", "D0????", "E0????", "B001??",
         "B040??", "B1????");
      noteInput.setShouldConsumeEvents(true);

      initTrackBank();
      setUpTransport();
      initJogWheel();

      final PinnableCursorDevice cursorDevice = cursorTrack.createCursorDevice();
      bindMacroControl(cursorDevice, midiIn2);

      doHardwareLayout();

      for (final CcAssignment cc : CcAssignment.values()) {
         sendLedUpdate(cc, 0);
      }
      mainLayer.activate();
      host.showPopupNotification("Komplete Kontrol A Initialized");
   }

   @Override
   protected void setUpSliders(final MidiIn midiIn) {
      for (int i = 0; i < 8; i++) {
         final RelativeHardwareKnob knob = surface.createRelativeHardwareKnob("VOLUME_KNOB" + i);
         volumeKnobs[i] = knob;
         knob.setAdjustValueMatcher(midiIn.createRelative2sComplementCCValueMatcher(0xF, 0x50 + i, 128));
         knob.setStepSize(1 / 1024.0);

         final RelativeHardwareKnob panKnob = surface.createRelativeHardwareKnob("PAN_KNOB" + i);
         panKnobs[i] = panKnob;
         panKnob.setAdjustValueMatcher(midiIn.createRelative2sComplementCCValueMatcher(0xF, 0x58 + i, 128));
         panKnob.setStepSize(1 / 1024.0);
      }
   }

   @Override
   protected void initNavigation() {
      final Clip cursorClip = getHost().createLauncherCursorClip(8, 128);
      final Clip arrangerClip = getHost().createArrangerCursorClip(8, 128);

      arrangerClip.exists().markInterested();
      final Track rootTrack = project.getRootTrackGroup();

      final TrackBank singleTrackBank = getHost().createTrackBank(1, 0, 1);
      singleTrackBank.scrollPosition().markInterested();

      cursorTrack = getHost().createCursorTrack(1, 1);
      singleTrackBank.followCursorTrack(cursorTrack);

      final Track theTrack = singleTrackBank.getItemAt(0);
      final ClipLauncherSlotBank slotBank = theTrack.clipLauncherSlotBank();
      slotBank.setIndication(true);
      final ClipLauncherSlot theClip = slotBank.getItemAt(0);

      final PinnableCursorDevice cursorDevice = cursorTrack.createCursorDevice();
      createKompleteKontrolDeviceKompleteKontrol(cursorDevice);
      final SceneBank sceneBank = singleTrackBank.sceneBank();

      sceneBank.cursorIndex().markInterested();
      sceneBank.canScrollBackwards().markInterested();
      sceneBank.canScrollForwards().markInterested();

      sceneBank.getScene(0).setIndication(false);

      application.panelLayout().addValueObserver(v -> currentLayoutType = LayoutType.toType(v));

      final HardwareButton leftNavButton = surface.createHardwareButton("LEFT_NAV_BUTTON");
      leftNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x32, 1));
      final HardwareButton rightNavButton = surface.createHardwareButton("RIGHT_NAV_BUTTON");
      rightNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x32, 127));
      final HardwareButton upNavButton = surface.createHardwareButton("UP_NAV_BUTTON");
      upNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x30, 127));
      final HardwareButton downNavButton = surface.createHardwareButton("DOWN_NAV_BUTTON");
      downNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x30, 1));
      mainLayer.bindPressed(leftNavButton, () -> {
         switch (currentLayoutType) {
            case LAUNCHER:
               singleTrackBank.scrollBy(1);
               theClip.select();
               theTrack.selectInMixer();
               if (sceneNavMode) {
                  sceneNavMode = false;
                  sceneBank.getScene(0).setIndication(false);
                  theClip.setIndication(true);
               }
               break;
            case ARRANGER:
               sceneBank.scrollForwards();
               theClip.select();
               break;
            default:
               break;
         }
      });
      mainLayer.bindPressed(rightNavButton, () -> {
         switch (currentLayoutType) {
            case LAUNCHER:
               if (singleTrackBank.scrollPosition().get() == 0) {
                  sceneNavMode = true;
                  sceneBank.getScene(0).setIndication(true);
                  theClip.setIndication(false);
               } else {
                  singleTrackBank.scrollBy(-1);
                  theClip.select();
                  theTrack.selectInMixer();
               }
               break;
            case ARRANGER:
               sceneBank.scrollBackwards();
               theClip.select();
               break;
            default:
               break;
         }
      });
      mainLayer.bindPressed(upNavButton, () -> {
         switch (currentLayoutType) {
            case LAUNCHER:
               sceneBank.scrollBackwards();
               theClip.select();
               break;
            case ARRANGER:
               if (singleTrackBank.scrollPosition().get() == 0) {
                  sceneNavMode = true;
                  sceneBank.getScene(0).setIndication(true);
                  theClip.setIndication(false);
               } else {
                  singleTrackBank.scrollBy(-1);
                  theClip.select();
                  theTrack.selectInMixer();
               }
               break;
            default:
               break;
         }
      });
      mainLayer.bindPressed(downNavButton, () -> {
         switch (currentLayoutType) {
            case LAUNCHER:
               sceneBank.scrollForwards();
               theClip.select();
               break;
            case ARRANGER:
               singleTrackBank.scrollBy(1);
               theClip.select();
               theTrack.selectInMixer();
               if (sceneNavMode) {
                  sceneNavMode = false;
                  sceneBank.getScene(0).setIndication(false);
                  theClip.setIndication(true);
               }
               break;
            default:
               break;
         }
      });

      cursorClip.exists().markInterested();
      final ModeButton quantizeButton = new ModeButton(this, "QUANTIZE_BUTTON", CcAssignment.QUANTIZE);
      sessionFocusLayer.bindPressed(quantizeButton, () -> cursorClip.quantize(1.0));
      sessionFocusLayer.bind(() -> cursorTrack.canHoldNoteData().get() && cursorClip.exists().get(),
         quantizeButton.getLed());

      arrangeFocusLayer.bindPressed(quantizeButton, () -> arrangerClip.quantize(1.0));
      arrangeFocusLayer.bind(() -> cursorTrack.canHoldNoteData().get() && arrangerClip.exists().get(),
         quantizeButton.getLed());

      cursorTrack.canHoldNoteData().markInterested();
      cursorClip.exists().markInterested();

      final ModeButton clearButton = new ModeButton(this, "CLEAR_BUTTON", CcAssignment.CLEAR);
      sessionFocusLayer.bindPressed(clearButton, cursorClip::clearSteps);
      sessionFocusLayer.bind(() -> cursorTrack.canHoldNoteData().get() && cursorClip.exists().get(),
         clearButton.getLed());

      arrangeFocusLayer.bindPressed(clearButton, arrangerClip::clearSteps);
      arrangeFocusLayer.bind(() -> cursorTrack.canHoldNoteData().get() && arrangerClip.exists().get(),
         clearButton.getLed());

      clearButton.getLed()
         .isOn()
         .setValueSupplier(() -> cursorTrack.canHoldNoteData().get() && cursorClip.exists().get());

      final ModeButton knobPressed = new ModeButton(this, "KNOB4D_PRESSED", CcAssignment.PRESS_4D_KNOB);
      mainLayer.bindPressed(knobPressed, () -> {
         if (sceneNavMode) {
            sceneBank.getScene(0).launch();
         } else {
            theClip.launch();
         }
      });
      final ModeButton knobShiftPressed = new ModeButton(this, "KNOB4D_PRESSED_SHIFT",
         CcAssignment.PRESS_4D_KNOB_SHIFT);
      mainLayer.bindPressed(knobShiftPressed, () -> {
         if (sceneNavMode) {
            rootTrack.stop();
         } else {
            cursorTrack.stop();
         }
      });
   }

   @Override
   public void exit() {
      midiOutDaw.sendMidi(Midi.KK_DAW, Midi.GOODBYE, 0);
      getHost().showPopupNotification("Komplete Kontrol A Series Exited");
   }

   @Override
   public void flush() {
      if (dawModeConfirmed) {
         surface.updateHardware();
      }
   }

   @Override
   protected void setUpChannelControl(final int index, final Track channel) {
      final IndexButton selectButton = new IndexButton(this, index, "SELECT_BUTTON", 0x42);
      mainLayer.bindPressed(selectButton.getHwButton(), () -> {
         if (!channel.exists().get()) {
            application.createInstrumentTrack(-1);
         } else {
            channel.selectInMixer();
         }
      });

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

      channel.pan().displayedValue().addValueObserver(value -> trackPanTextCommand.send(midiOutDaw, index, value));

      channel.pan().value().addValueObserver(value -> {
         final int v = (int) (value * 127);
         midiOutDaw.sendMidi(0xBF, 0x58 + index, v);
      });

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

   @Override
   protected void setUpChannelDisplayFeedback(final int index, final Track channel) {
   }

   private void doHardwareLayout() {
      surface.hardwareElementWithId("VOLUME_KNOB0").setBounds(62.75, 7.5, 10.0, 10.0);
      surface.hardwareElementWithId("PAN_KNOB0").setBounds(62.25, 19.0, 10.0, 10.0);
      surface.hardwareElementWithId("VOLUME_KNOB1").setBounds(75.75, 7.5, 10.0, 10.0);
      surface.hardwareElementWithId("PAN_KNOB1").setBounds(76.25, 19.0, 10.0, 10.0);
      surface.hardwareElementWithId("VOLUME_KNOB2").setBounds(88.25, 7.5, 10.0, 10.0);
      surface.hardwareElementWithId("PAN_KNOB2").setBounds(89.25, 19.0, 10.0, 10.0);
      surface.hardwareElementWithId("VOLUME_KNOB3").setBounds(101.5, 7.5, 10.0, 10.0);
      surface.hardwareElementWithId("PAN_KNOB3").setBounds(104.5, 19.0, 10.0, 10.0);
      surface.hardwareElementWithId("VOLUME_KNOB4").setBounds(115.25, 7.5, 10.0, 10.0);
      surface.hardwareElementWithId("PAN_KNOB4").setBounds(115.5, 19.0, 10.0, 10.0);
      surface.hardwareElementWithId("VOLUME_KNOB5").setBounds(127.25, 7.5, 10.0, 10.0);
      surface.hardwareElementWithId("PAN_KNOB5").setBounds(127.5, 19.0, 10.0, 10.0);
      surface.hardwareElementWithId("VOLUME_KNOB6").setBounds(138.0, 7.5, 10.0, 10.0);
      surface.hardwareElementWithId("PAN_KNOB6").setBounds(138.75, 19.0, 10.0, 10.0);
      surface.hardwareElementWithId("VOLUME_KNOB7").setBounds(148.0, 7.5, 10.0, 10.0);
      surface.hardwareElementWithId("PAN_KNOB7").setBounds(149.25, 19.0, 10.0, 10.0);

      surface.hardwareElementWithId("LEFT_NAV_BUTTON").setBounds(160.75, 20.0, 6.0, 6.0);
      surface.hardwareElementWithId("RIGHT_NAV_BUTTON").setBounds(174.25, 20.0, 5.5, 6.0);
      surface.hardwareElementWithId("UP_NAV_BUTTON").setBounds(167.25, 14.25, 6.0, 5.0);
      surface.hardwareElementWithId("DOWN_NAV_BUTTON").setBounds(167.25, 26.5, 6.5, 6.0);
      surface.hardwareElementWithId("KNOB4D_PRESSED").setBounds(167.25, 20.0, 6.5, 3.0);
      surface.hardwareElementWithId("KNOB4D_PRESSED_SHIFT").setBounds(168.0, 23.75, 4.75, 2.0);

      surface.hardwareElementWithId("QUANTIZE_BUTTON").setBounds(26.25, 13.5, 7.25, 2.0);
      surface.hardwareElementWithId("CLEAR_BUTTON").setBounds(35.25, 27.75, 7.5, 2.0);
      surface.hardwareElementWithId("TRACK_LEFT_NAV_BUTTON").setBounds(48.75, 25.75, 2.25, 3.0);
      surface.hardwareElementWithId("TRACK_RIGHT_NAV_BUTTON").setBounds(52.75, 25.5, 3.25, 3.5);
      surface.hardwareElementWithId("MUTE_SELECTED_BUTTON").setBounds(48.75, 29.0, 2.5, 2.75);
      surface.hardwareElementWithId("SOLO_SELECTED_BUTTON").setBounds(52.75, 29.25, 3.25, 2.5);
      surface.hardwareElementWithId("SELECT_BUTTON_0").setBounds(60.75, 17.25, 9.5, 4.25);
      surface.hardwareElementWithId("SELECT_BUTTON_1").setBounds(72.25, 17.25, 10.0, 4.0);
      surface.hardwareElementWithId("SELECT_BUTTON_2").setBounds(84.25, 17.25, 10.0, 4.0);
      surface.hardwareElementWithId("SELECT_BUTTON_3").setBounds(96.25, 17.25, 10.0, 4.0);
      surface.hardwareElementWithId("SELECT_BUTTON_4").setBounds(108.25, 17.25, 10.0, 4.0);
      surface.hardwareElementWithId("SELECT_BUTTON_5").setBounds(120.25, 17.25, 10.0, 4.0);
      surface.hardwareElementWithId("SELECT_BUTTON_6").setBounds(132.25, 17.25, 10.0, 4.0);
      surface.hardwareElementWithId("SELECT_BUTTON_7").setBounds(144.25, 17.25, 9.25, 4.0);
      surface.hardwareElementWithId("REC_BUTTON").setBounds(26.75, 25.0, 6.75, 4.0);
      surface.hardwareElementWithId("AUTO_BUTTON").setBounds(26.0, 15.75, 7.75, 2.0);
      surface.hardwareElementWithId("COUNTIN_BUTTON").setBounds(7.75, 57.75, 25.5, 10.0);
      surface.hardwareElementWithId("PLAY_BUTTON").setBounds(18.0, 25.0, 7.0, 2.0);
      surface.hardwareElementWithId("RESTART_BUTTON").setBounds(18.0, 27.0, 7.0, 2.25);
      surface.hardwareElementWithId("STOP_BUTTON").setBounds(35.25, 24.75, 7.0, 2.5);
      surface.hardwareElementWithId("LOOP_BUTTON").setBounds(18.0, 20.75, 7.0, 2.75);
      surface.hardwareElementWithId("METRO_BUTTON").setBounds(26.75, 20.75, 7.0, 3.0);
      surface.hardwareElementWithId("TAP_BUTTON").setBounds(35.25, 20.75, 7.0, 3.25);
      surface.hardwareElementWithId("UNDO_BUTTON").setBounds(18.0, 13.5, 7.0, 2.0);
      surface.hardwareElementWithId("REDO_BUTTON").setBounds(18.0, 15.75, 7.0, 1.75);
      surface.hardwareElementWithId("4D_WHEEL_PLUGIN_MODE").setBounds(180.0, 13.5, 10.0, 10.0);
      surface.hardwareElementWithId("4D_WHEEL_MIX_MODE").setBounds(180.25, 24.5, 10.0, 10.0);
      surface.hardwareElementWithId("MACRO_0").setBounds(61.75, 31.5, 10.0, 10.0);
      surface.hardwareElementWithId("MACRO_1").setBounds(73.75, 31.5, 10.0, 10.0);
      surface.hardwareElementWithId("MACRO_2").setBounds(85.75, 31.5, 10.0, 10.0);
      surface.hardwareElementWithId("MACRO_3").setBounds(97.75, 31.5, 10.0, 10.0);
      surface.hardwareElementWithId("MACRO_4").setBounds(112.25, 30.75, 10.0, 10.0);
      surface.hardwareElementWithId("MACRO_5").setBounds(124.25, 30.75, 10.0, 10.0);
      surface.hardwareElementWithId("MACRO_6").setBounds(136.25, 30.75, 10.0, 10.0);
      surface.hardwareElementWithId("MACRO_7").setBounds(148.25, 30.75, 10.0, 10.0);

   }
}
