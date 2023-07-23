package com.bitwig.extensions.controllers.nativeinstruments.komplete;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.definition.KompleteKontrolSMk2ExtensionDefinition;
import com.bitwig.extensions.framework.Layers;

public class KompleteKontrolSMk2Extension extends KompleteKontrolExtension {

   private static final byte[] levelDbLookup = new byte[201]; // maps level values to align with KK display

   public KompleteKontrolSMk2Extension(final KompleteKontrolSMk2ExtensionDefinition definition,
                                       final ControllerHost host) {
      super(definition, host);
   }

   @Override
   public void init() {
      initSliderLookup();
      super.init();
      final ControllerHost host = getHost();

      midiProcessor.intoDawMode();
      layers = new Layers(this);
      mainLayer = new KompleteLayer(this, "Main");
      arrangeFocusLayer = new KompleteLayer(this, "ArrangeFocus");
      sessionFocusLayer = new KompleteLayer(this, "SessionFocus");

      project = host.getProject();
      mTransport = host.createTransport();

      setUpSliders();
      final MidiIn midiIn2 = host.getMidiInPort(1);

      final NoteInput noteInput = midiIn2.createNoteInput("MIDI", "80????", "90????", "D0????", "E0????", "B001??",
         "B040??", "B1????");
      noteInput.setShouldConsumeEvents(true);

      initTrackBank();
      setUpTransport();
      initJogWheel();

      final PinnableCursorDevice cursorDevice = cursorTrack.createCursorDevice();
      bindMacroControl(cursorDevice, midiIn2);

      mainLayer.activate();
   }

   private static void initSliderLookup() {
      final int[] intervals = {0, 25, 63, 100, 158, 200};
      final int[] pollValues = {0, 14, 38, 67, 108, 127};
      int currentIv = 1;
      int ivLb = 0;
      int plLb = 0;
      int ivUb = intervals[currentIv];
      int plUb = pollValues[currentIv];
      double ratio = (double) (plUb - plLb) / (double) (ivUb - ivLb);
      for (int i = 0; i < levelDbLookup.length; i++) {
         if (i > intervals[currentIv]) {
            currentIv++;
            ivLb = ivUb;
            plLb = plUb;
            ivUb = intervals[currentIv];
            plUb = pollValues[currentIv];
            ratio = (double) (plUb - plLb) / (double) (ivUb - ivLb);
         }
         levelDbLookup[i] = (byte) Math.round(plLb + (i - ivLb) * ratio);
      }
   }

   @Override
   public void exit() {
      midiProcessor.exit();
   }

   @Override
   public void flush() {
      midiProcessor.doFlush();
   }

   @Override
   public void setUpChannelDisplayFeedback(final int index, final Track channel) {
      channel.volume().value().addValueObserver(value -> {
         final byte v = toSliderVal(value);
         midiProcessor.sendVolumeValue(index, v);
      });
      channel.pan().value().addValueObserver(value -> {
         final int v = (int) (value * 127);
         midiProcessor.sendPanValue(index, v);
      });
      channel.addVuMeterObserver(201, 0, true,
         leftValue -> midiProcessor.updateVuLeft(index, levelDbLookup[leftValue]));
      channel.addVuMeterObserver(201, 1, true,
         rightValue -> midiProcessor.updateVuRight(index, levelDbLookup[rightValue]));
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
      singleTrackBank.setShouldShowClipLauncherFeedback(true);
      final ClipLauncherSlot theClip = slotBank.getItemAt(0);


      final PinnableCursorDevice cursorDevice = cursorTrack.createCursorDevice();
      createKompleteKontrolDeviceKompleteKontrol(cursorDevice);
      final SceneBank sceneBank = singleTrackBank.sceneBank();

      sceneBank.cursorIndex().markInterested();
      sceneBank.canScrollBackwards().markInterested();
      sceneBank.canScrollForwards().markInterested();

      sceneBank.setIndication(false);

      application.panelLayout().addValueObserver(v -> {
         currentLayoutType = LayoutType.toType(v);
         updateLedChannelDown(singleTrackBank, singleTrackBank.canScrollChannelsDown().get());
         updateLedChannelUp(singleTrackBank, singleTrackBank.canScrollChannelsUp().get());
         updateLedSceneForwards(sceneBank, sceneBank.canScrollForwards().get());
         updateLedSceneBackwards(sceneBank, sceneBank.canScrollBackwards().get());
      });

      singleTrackBank.canScrollChannelsUp().addValueObserver(v -> updateLedChannelUp(singleTrackBank, v));
      singleTrackBank.canScrollChannelsDown().addValueObserver(v -> updateLedChannelDown(singleTrackBank, v));

      sceneBank.canScrollBackwards().addValueObserver(v -> updateLedSceneBackwards(sceneBank, v));
      sceneBank.canScrollForwards().addValueObserver(v -> updateLedSceneForwards(sceneBank, v));

      final MidiIn midiIn = midiProcessor.getMidiIn();
      final HardwareButton leftNavButton = surface.createHardwareButton("LEFT_NAV_BUTTON");
      leftNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x30, 1));

      final HardwareButton rightNavButton = surface.createHardwareButton("RIGHT_NAV_BUTTON");
      rightNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x30, 127));

      final HardwareButton upNavButton = surface.createHardwareButton("UP_NAV_BUTTON");
      upNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x32, 127));

      final HardwareButton downNavButton = surface.createHardwareButton("DOWN_NAV_BUTTON");
      downNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x32, 1));

      mainLayer.bindPressed(leftNavButton, () -> doNavigateLeft(singleTrackBank, theTrack, theClip, sceneBank));
      mainLayer.bindPressed(rightNavButton, () -> doNavigateRight(singleTrackBank, theTrack, theClip, sceneBank));
      mainLayer.bindPressed(upNavButton, () -> doNavigateUp(singleTrackBank, theTrack, theClip, sceneBank));
      mainLayer.bindPressed(downNavButton, () -> doNavigateDown(singleTrackBank, theTrack, theClip, sceneBank));

      cursorClip.exists().markInterested();
      final ModeButton quantizeButton = new ModeButton(midiProcessor, "QUANTIZE_BUTTON", CcAssignment.QUANTIZE);
      sessionFocusLayer.bindPressed(quantizeButton, () -> cursorClip.quantize(1.0));
      sessionFocusLayer.bind(() -> cursorTrack.canHoldNoteData().get() && cursorClip.exists().get(),
         quantizeButton.getLed());

      arrangeFocusLayer.bindPressed(quantizeButton, () -> arrangerClip.quantize(1.0));
      arrangeFocusLayer.bind(() -> cursorTrack.canHoldNoteData().get() && arrangerClip.exists().get(),
         quantizeButton.getLed());

      cursorTrack.canHoldNoteData().markInterested();
      cursorClip.exists().markInterested();

      final ModeButton clearButton = new ModeButton(midiProcessor, "CLEAR_BUTTON", CcAssignment.CLEAR);
      sessionFocusLayer.bindPressed(clearButton, cursorClip::clearSteps);
      sessionFocusLayer.bind(() -> cursorTrack.canHoldNoteData().get() && cursorClip.exists().get(),
         clearButton.getLed());

      arrangeFocusLayer.bindPressed(clearButton, arrangerClip::clearSteps);
      arrangeFocusLayer.bind(() -> cursorTrack.canHoldNoteData().get() && arrangerClip.exists().get(),
         clearButton.getLed());

      clearButton.getLed()
         .isOn()
         .setValueSupplier(() -> cursorTrack.canHoldNoteData().get() && cursorClip.exists().get());

      final ModeButton knobPressed = new ModeButton(midiProcessor, "KNOB4D_PRESSED", CcAssignment.PRESS_4D_KNOB);
      mainLayer.bindPressed(knobPressed, () -> {
         if (sceneNavMode) {
            sceneBank.getScene(0).launch();
         } else {
            theClip.launch();
         }
      });
      final ModeButton knobShiftPressed = new ModeButton(midiProcessor, "KNOB4D_PRESSED_SHIFT",
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
         case LAUNCHER -> midiProcessor.sendLedUpdate(0x32, sv);
         case ARRANGER -> midiProcessor.sendLedUpdate(0x30, sv);
         default -> {
         }
      }
   }

   private void updateLedSceneBackwards(final SceneBank sceneBank, final boolean v) {
      final int sv = sceneBank.canScrollForwards().get() ? 0x2 : 0x0;
      switch (currentLayoutType) {
         case LAUNCHER -> midiProcessor.sendLedUpdate(0x32, (v ? 0x1 : 0x0) | sv);
         case ARRANGER -> midiProcessor.sendLedUpdate(0x30, (v ? 0x1 : 0x0) | sv);
         default -> {
         }
      }
   }

   private void updateLedChannelDown(final TrackBank singleTrackBank, final boolean v) {
      final int sv = (v ? 0x2 : 0x0) | (singleTrackBank.canScrollChannelsUp().get() ? 0x1 : 0x0);
      switch (currentLayoutType) {
         case LAUNCHER -> midiProcessor.sendLedUpdate(0x30, sv);
         case ARRANGER -> midiProcessor.sendLedUpdate(0x32, sv);
         default -> {
         }
      }
   }

   private void updateLedChannelUp(final TrackBank singleTrackBank, final boolean v) {
      final int sv = (v ? 0x1 : 0x0) | (singleTrackBank.canScrollChannelsDown().get() ? 0x2 : 0x0);
      switch (currentLayoutType) {
         case LAUNCHER -> midiProcessor.sendLedUpdate(0x30, sv);
         case ARRANGER -> midiProcessor.sendLedUpdate(0x32, sv);
         default -> {
         }
      }
   }

   byte toSliderVal(final double value) {
      return levelDbLookup[(int) (value * 200)];
   }

}
