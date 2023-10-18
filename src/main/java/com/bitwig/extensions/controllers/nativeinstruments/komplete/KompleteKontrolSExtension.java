package com.bitwig.extensions.controllers.nativeinstruments.komplete;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.definition.AbstractKompleteKontrolExtensionDefinition;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class KompleteKontrolSExtension extends KompleteKontrolExtension {

   private static final byte[] levelDbLookup = new byte[201]; // maps level values to align with KK display

   public KompleteKontrolSExtension(final AbstractKompleteKontrolExtensionDefinition definition,
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
      mainLayer = new Layer(layers, "Main");
      arrangeFocusLayer = new Layer(layers, "ArrangeFocus");
      sessionFocusLayer = new Layer(layers, "SessionFocus");

      project = host.getProject();
      mTransport = host.createTransport();

      setUpSliders();
      final MidiIn midiIn2 = host.getMidiInPort(1);

      final NoteInput noteInput = midiIn2.createNoteInput("MIDI", "80????", "90????", "A0????", "D0????", "E0????",
         "B001??", "B00B??", "B040??", "B042??", "B1????");
      noteInput.setShouldConsumeEvents(true);
      midiIn2.setMidiCallback((status, data1, data2) -> {
         host.println("Midi IN => %02X %02X %02X".formatted(status, data1, data2));
      });

      initTrackBank();
      setUpTransport();
      initJogWheel();

      final PinnableCursorDevice cursorDevice = clipSceneCursor.getCursorTrack().createCursorDevice();
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
      CursorTrack cursorTrack = clipSceneCursor.getCursorTrack();

      application.panelLayout().addValueObserver(v -> {
         currentLayoutType = LayoutType.toType(v);
         updateChanelLed();
         updateSceneLed();
      });

      navigationState.setStateChangeListener(() -> {
         this.updateSceneLed();
         this.updateChanelLed();
      });

      final MidiIn midiIn = midiProcessor.getMidiIn();
      final HardwareButton leftNavButton = surface.createHardwareButton("LEFT_NAV_BUTTON");
      leftNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x30, 1));

      final HardwareButton rightNavButton = surface.createHardwareButton("RIGHT_NAV_BUTTON");
      rightNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x30, 127));

      final HardwareButton upNavButton = surface.createHardwareButton("UP_NAV_BUTTON");
      upNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x32, 127));

      final HardwareButton downNavButton = surface.createHardwareButton("DOWN_NAV_BUTTON");
      downNavButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0xF, 0x32, 1));

      mainLayer.bindPressed(leftNavButton, () -> clipSceneCursor.doNavigateRight(currentLayoutType));
      mainLayer.bindPressed(rightNavButton, () -> clipSceneCursor.doNavigateLeft(currentLayoutType));
      mainLayer.bindPressed(upNavButton, () -> clipSceneCursor.doNavigateUp(currentLayoutType));
      mainLayer.bindPressed(downNavButton, () -> clipSceneCursor.doNavigateDown(currentLayoutType));

      cursorClip.exists().markInterested();
      final ModeButton quantizeButton = new ModeButton(midiProcessor, "QUANTIZE_BUTTON", CcAssignment.QUANTIZE);
      sessionFocusLayer.bindPressed(quantizeButton.getHwButton(), () -> cursorClip.quantize(1.0));

      sessionFocusLayer.bind(() -> cursorTrack.canHoldNoteData().get() && cursorClip.exists().get(),
         quantizeButton.getLed());

      arrangeFocusLayer.bindPressed(quantizeButton.getHwButton(), () -> arrangerClip.quantize(1.0));
      arrangeFocusLayer.bind(() -> cursorTrack.canHoldNoteData().get() && arrangerClip.exists().get(),
         quantizeButton.getLed());

      cursorTrack.canHoldNoteData().markInterested();
      cursorClip.exists().markInterested();

      final ModeButton clearButton = new ModeButton(midiProcessor, "CLEAR_BUTTON", CcAssignment.CLEAR);
      sessionFocusLayer.bindPressed(clearButton.getHwButton(), () -> cursorClip.clearSteps());
      sessionFocusLayer.bind(() -> cursorTrack.canHoldNoteData().get() && cursorClip.exists().get(),
         clearButton.getLed());

      arrangeFocusLayer.bindPressed(clearButton.getHwButton(), () -> arrangerClip.clearSteps());
      arrangeFocusLayer.bind(() -> cursorTrack.canHoldNoteData().get() && arrangerClip.exists().get(),
         clearButton.getLed());

      clearButton.getLed()
         .isOn()
         .setValueSupplier(() -> cursorTrack.canHoldNoteData().get() && cursorClip.exists().get());

      final ModeButton knobPressed = new ModeButton(midiProcessor, "KNOB4D_PRESSED", CcAssignment.PRESS_4D_KNOB);
      mainLayer.bindPressed(knobPressed.getHwButton(), () -> clipSceneCursor.launch());
      final ModeButton knobShiftPressed = new ModeButton(midiProcessor, "KNOB4D_PRESSED_SHIFT",
         CcAssignment.PRESS_4D_KNOB_SHIFT);
      mainLayer.bindPressed(knobShiftPressed.getHwButton(), () -> {
         if (navigationState.isSceneNavMode()) {
            rootTrack.stop();
         } else {
            cursorTrack.stop();
         }
      });
   }


   private void updateSceneLed() {
      final int sceneValue = navigationState.getSceneValue();
      switch (currentLayoutType) {
         case LAUNCHER -> midiProcessor.sendLedUpdate(0x32, sceneValue);
         case ARRANGER -> midiProcessor.sendLedUpdate(0x30, sceneValue);
         default -> {
         }
      }
   }


   private void updateChanelLed() {
      final int trackValue = navigationState.getTrackValue();
      switch (currentLayoutType) {
         case LAUNCHER -> midiProcessor.sendLedUpdate(0x30, trackValue);
         case ARRANGER -> midiProcessor.sendLedUpdate(0x32, trackValue);
         default -> {
         }
      }
   }

   byte toSliderVal(final double value) {
      return levelDbLookup[(int) (value * 200)];
   }

}
