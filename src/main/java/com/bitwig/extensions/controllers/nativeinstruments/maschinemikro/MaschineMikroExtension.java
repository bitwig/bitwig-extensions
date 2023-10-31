package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.buttons.TouchStrip;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers.EncoderLayer;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers.ModeHandler;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers.ModifierLayer;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers.StripMode;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Context;
import com.bitwig.extensions.framework.values.FocusMode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MaschineMikroExtension extends ControllerExtension {

   private final ControllerHost host;
   private HardwareSurface surface;
   private Layer mainLayer;
   private Layer shiftLayer;
   private FocusMode recordFocusMode = FocusMode.LAUNCHER;
   private StripMode stripMode = StripMode.NONE;
   private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("hh:mm:ss SSS");
   private static ControllerHost debugHost;

   protected MaschineMikroExtension(final ControllerExtensionDefinition definition, final ControllerHost host) {
      super(definition, host);
      this.host = host;
   }

   public static void println(final String format, final Object... args) {
      if (debugHost != null) {
         final LocalDateTime now = LocalDateTime.now();
         debugHost.println(now.format(DF) + " > " + String.format(format, args));
      }
   }

   @Override
   public void init() {
      debugHost = host;
      initPreferences(host);
      final Context diContext = new Context(this);
      Layer progressLayer = diContext.createLayer("Progress_layer");
      surface = diContext.getService(HardwareSurface.class);
      MidiIn midiIn = host.getMidiInPort(0);
      MidiOut midiOut = host.getMidiOutPort(0);
      diContext.registerService(MidiIn.class, midiIn);
      diContext.registerService(MidiOut.class, midiOut);
      MidiProcessor midiProcessor = new MidiProcessor(host, midiIn, midiOut);
      diContext.registerService(MidiProcessor.class, midiProcessor);
      mainLayer = diContext.createLayer("MAIN");
      diContext.getService(ModifierLayer.class).init(mainLayer, diContext.getService(HwElements.class));
      shiftLayer = diContext.createLayer("SHIFT");
      initTransport(diContext);
      intStripHandling(diContext, progressLayer);
      EncoderLayer encoderLayer = diContext.create(EncoderLayer.class);
      diContext.getService(ModeHandler.class).setEncoderLayer(encoderLayer);
      mainLayer.setIsActive(true);

      midiProcessor.start();
      diContext.activate();
   }

   private void initTransport(Context diContext) {
      Transport transport = diContext.getService(Transport.class);
      HwElements hwElements = diContext.getService(HwElements.class);
      ModifierLayer modifierLayer = diContext.getService(ModifierLayer.class);
      FocusClip focusClip = diContext.getService(FocusClip.class);
      ViewControl viewControl = diContext.getService(ViewControl.class);
      Track rootTrack = host.getProject().getRootTrackGroup();

      transport.isPlaying().markInterested();
      transport.isArrangerRecordEnabled().markInterested();
      transport.isClipLauncherOverdubEnabled().markInterested();
      transport.isMetronomeEnabled().markInterested();
      transport.isArrangerLoopEnabled().markInterested();
      transport.isClipLauncherAutomationWriteEnabled().markInterested();
      transport.isArrangerAutomationWriteEnabled().markInterested();

      modifierLayer.getShiftHeld().addValueObserver(shift -> shiftLayer.setIsActive(shift));

      hwElements.getButton(CcAssignment.PLAY).bindPressed(mainLayer, transport.playAction());
      hwElements.getButton(CcAssignment.PLAY).bindLight(mainLayer, transport.isPlaying());

      hwElements.getButton(CcAssignment.RECORD).bindPressed(mainLayer, () -> handleRecordButton(transport, focusClip));
      hwElements.getButton(CcAssignment.RECORD).bindLight(mainLayer, () -> recordActive(transport));
      hwElements.getButton(CcAssignment.RECORD).bindPressed(shiftLayer, () -> handleRecordButton(transport));
      hwElements.getButton(CcAssignment.RECORD).bindLight(shiftLayer, () -> recordActive(transport));

      hwElements.getButton(CcAssignment.AUTO)
         .bindPressed(mainLayer, () -> transport.isClipLauncherAutomationWriteEnabled().toggle());
      hwElements.getButton(CcAssignment.AUTO)
         .bindLight(mainLayer, () -> transport.isClipLauncherAutomationWriteEnabled().get());

      hwElements.getButton(CcAssignment.AUTO)
         .bindPressed(shiftLayer, () -> transport.isArrangerAutomationWriteEnabled().toggle());
      hwElements.getButton(CcAssignment.AUTO)
         .bindLight(shiftLayer, () -> transport.isArrangerAutomationWriteEnabled().get());

      hwElements.getButton(CcAssignment.STOP).bindPressed(mainLayer, transport.stopAction());
      hwElements.getButton(CcAssignment.STOP).bindLightHeld(mainLayer);
      hwElements.getButton(CcAssignment.STOP).bindPressed(shiftLayer, () -> rootTrack.stop());
      hwElements.getButton(CcAssignment.STOP).bindLightHeld(shiftLayer);

      hwElements.getButton(CcAssignment.TAP).bindPressed(mainLayer, transport.tapTempoAction());
      hwElements.getButton(CcAssignment.TAP).bindLightHeld(mainLayer);
      hwElements.getButton(CcAssignment.TAP).bindPressed(shiftLayer, transport.isMetronomeEnabled().toggleAction());
      hwElements.getButton(CcAssignment.TAP).bindLight(shiftLayer, transport.isMetronomeEnabled());
      hwElements.getButton(CcAssignment.RESTART)
         .bindPressed(shiftLayer, transport.isArrangerLoopEnabled().toggleAction());
      hwElements.getButton(CcAssignment.RESTART).bindLight(shiftLayer, transport.isArrangerLoopEnabled());
   }

   private void intStripHandling(Context diContext, Layer progressLayer) {
      HwElements hwElements = diContext.getService(HwElements.class);
      FocusClip focusClip = diContext.getService(FocusClip.class);
      TouchStrip touchStrip = hwElements.getTouchStrip();
      touchStrip.bindStripLight(progressLayer, () -> focusClip.getPlayPosition());
      progressLayer.setIsActive(true);
   }

   private void handleRecordButton(Transport transport, FocusClip focusClip) {
      focusClip.invokeRecord();
   }

   private void handleRecordButton(Transport transport) {
      if (recordFocusMode == FocusMode.LAUNCHER) {
         transport.isClipLauncherOverdubEnabled().toggle();
      } else {
         transport.isClipLauncherOverdubEnabled().toggle();
      }
   }

   private boolean recordActive(Transport transport) {
      if (recordFocusMode == FocusMode.LAUNCHER) {
         return transport.isClipLauncherOverdubEnabled().get();
      }
      return transport.isArrangerRecordEnabled().get();
   }

   void initPreferences(final ControllerHost host) {
      final Preferences preferences = host.getPreferences(); // THIS
      final SettableEnumValue recordButtonAssignment = preferences.getEnumSetting("Record Button assignment", //
         "Transport", new String[]{FocusMode.LAUNCHER.getDescriptor(), FocusMode.ARRANGER.getDescriptor()},
         recordFocusMode.getDescriptor());
      recordButtonAssignment.addValueObserver(value -> recordFocusMode = FocusMode.toMode(value));
   }

   @Override
   public void exit() {

   }

   @Override
   public void flush() {
      surface.updateHardware();
   }
}
