package com.bitwig.extensions.controllers.nativeinstruments.komplete;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.Layers;

public class KompleteKontrolSMk2Extension extends KompleteKontrolExtension {

   private static final byte[] levelDbLookup = new byte[201]; // maps level values to align with KK display

   final ModeButton[] selectButtons = new ModeButton[8];

   protected KompleteKontrolSMk2Extension(final KompleteKontrolSMk2ExtensionDefinition definition,
                                          final ControllerHost host) {
      super(definition, host);
   }

   @Override
   public void init() {
      initSliderLookup();
      super.init();
      final ControllerHost host = getHost();

      intoDawMode();
      surface = host.createHardwareSurface();
      layers = new Layers(this);
      mainLayer = new KompleteLayer(this, "Main");
      arrangeFocusLayer = new KompleteLayer(this, "ArrangeFocus");
      sessionFocusLayer = new KompleteLayer(this, "SeesionFocus");

      project = host.getProject();
      mTransport = host.createTransport();

      setUpSliders(midiIn);
      final MidiIn midiIn2 = host.getMidiInPort(1);
//		final AutoDetectionMidiPortNamesList defs = getExtensionDefinition()
//				.getAutoDetectionMidiPortNamesList(host.getPlatformType());
//		final AutoDetectionMidiPortNames inport = defs.getPortNames().get(0);

      final NoteInput noteInput = midiIn2.createNoteInput("MIDI", "80????", "90????", "D0????", "E0????", "B001??",
         "B040??", "B1????");
      noteInput.setShouldConsumeEvents(true);

      initTrackBank();
      setUpTransport();
      initJogWheel();

      final PinnableCursorDevice cursorDevice = cursorTrack.createCursorDevice();
      bindMacroControl(cursorDevice, midiIn2);

      mainLayer.activate();
      host.showPopupNotification("Komplete Kontrol S Mk2 Initialized");
   }

   private static void initSliderLookup() {
      final int[] intervalls = {0, 25, 63, 100, 158, 200};
      final int[] pollValues = {0, 14, 38, 67, 108, 127};
      int curentIv = 1;
      int ivLb = 0;
      int plLb = 0;
      int ivUb = intervalls[curentIv];
      int plUb = pollValues[curentIv];
      double ratio = (double) (plUb - plLb) / (double) (ivUb - ivLb);
      for (int i = 0; i < levelDbLookup.length; i++) {
         if (i > intervalls[curentIv]) {
            curentIv++;
            ivLb = ivUb;
            plLb = plUb;
            ivUb = intervalls[curentIv];
            plUb = pollValues[curentIv];
            ratio = (double) (plUb - plLb) / (double) (ivUb - ivLb);
         }
         levelDbLookup[i] = (byte) Math.round(plLb + (i - ivLb) * ratio);
      }
   }

   @Override
   public void exit() {
      midiOutDaw.sendMidi(Midi.KK_DAW, Midi.GOODBYE, 0);
      getHost().showPopupNotification("Komplete Kontrol S Mk2 Exited");
   }

   @Override
   public void flush() {
      if (dawModeConfirmed) {
         surface.updateHardware();
      }
   }

   @Override
   public void setUpChannelDisplayFeedback(final int index, final Track channel) {
      channel.volume().value().addValueObserver(value -> {
         final byte v = toSliderVal(value);
         midiOutDaw.sendMidi(0xBF, 0x50 + index, v);
      });
      channel.pan().value().addValueObserver(value -> {
         final int v = (int) (value * 127);
         midiOutDaw.sendMidi(0xBF, 0x58 + index, v);
      });
      channel.addVuMeterObserver(201, 0, true,
         leftValue -> trackLevelMeterComand.updateLeft(index, levelDbLookup[leftValue]));
      channel.addVuMeterObserver(201, 1, true, rightValue -> {
         trackLevelMeterComand.updateRight(index, levelDbLookup[rightValue]);
         trackLevelMeterComand.update(midiOutDaw);
      });
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

      application.panelLayout().addValueObserver(v -> {
         currentLayoutType = LayoutType.toType(v);
         updateLedChannelDown(singleTrackBank, singleTrackBank.canScrollChannelsDown().get());
         udpateLedChannelUp(singleTrackBank, singleTrackBank.canScrollChannelsUp().get());
         updateLedSceneForwards(sceneBank, sceneBank.canScrollForwards().get());
         updateLedSceneBackwards(sceneBank, sceneBank.canScrollBackwards().get());
      });

      singleTrackBank.canScrollChannelsUp().addValueObserver(v -> udpateLedChannelUp(singleTrackBank, v));
      singleTrackBank.canScrollChannelsDown().addValueObserver(v -> updateLedChannelDown(singleTrackBank, v));

      sceneBank.canScrollBackwards().addValueObserver(v -> updateLedSceneBackwards(sceneBank, v));
      sceneBank.canScrollForwards().addValueObserver(v -> updateLedSceneForwards(sceneBank, v));

      final HardwareButton leftNavButton = surface.createHardwareButton("LEFT_NAV_BUTTON");
      leftNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x30, 1));
      final HardwareButton rightNavButton = surface.createHardwareButton("RIGHT_NAV_BUTTON");
      rightNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x30, 127));
      final HardwareButton upNavButton = surface.createHardwareButton("UP_NAV_BUTTON");
      upNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x32, 127));
      final HardwareButton downNavButton = surface.createHardwareButton("DOWN_NAV_BUTTON");
      downNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x32, 1));
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

   private void updateLedSceneForwards(final SceneBank sceneBank, final boolean v) {
      final int sv = (v ? 0x2 : 0x0) | (sceneBank.canScrollBackwards().get() ? 0x1 : 0x0);
      switch (currentLayoutType) {
         case LAUNCHER:
            sendLedUpdate(0x32, sv);
            break;
         case ARRANGER:
            sendLedUpdate(0x30, sv);
            break;
         default:
            break;
      }
   }

   private void updateLedSceneBackwards(final SceneBank sceneBank, final boolean v) {
      final int sv = sceneBank.canScrollForwards().get() ? 0x2 : 0x0;
      switch (currentLayoutType) {
         case LAUNCHER:
            sendLedUpdate(0x32, (v ? 0x1 : 0x0) | sv);
            break;
         case ARRANGER:
            sendLedUpdate(0x30, (v ? 0x1 : 0x0) | sv);
            break;
         default:
            break;
      }
   }

   private void updateLedChannelDown(final TrackBank singleTrackBank, final boolean v) {
      final int sv = (v ? 0x2 : 0x0) | (singleTrackBank.canScrollChannelsUp().get() ? 0x1 : 0x0);
      switch (currentLayoutType) {
         case LAUNCHER:
            sendLedUpdate(0x30, sv);
            break;
         case ARRANGER:
            sendLedUpdate(0x32, sv);
            break;
         default:
            break;
      }
   }

   private void udpateLedChannelUp(final TrackBank singleTrackBank, final boolean v) {
      final int sv = (v ? 0x1 : 0x0) | (singleTrackBank.canScrollChannelsDown().get() ? 0x2 : 0x0);
      switch (currentLayoutType) {
         case LAUNCHER:
            sendLedUpdate(0x30, sv);
            break;
         case ARRANGER:
            sendLedUpdate(0x32, sv);
            break;
         default:
            break;
      }
   }

   byte toSliderVal(final double value) {
      return levelDbLookup[(int) (value * 200)];
   }

}
