package com.bitwig.extensions.controllers.maudio.oxygenpro;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.maudio.oxygenpro.control.CcButton;
import com.bitwig.extensions.controllers.maudio.oxygenpro.definition.OxygenProExtensionDefinition;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Context;
import com.bitwig.extensions.framework.values.FocusMode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class OxygenProExtension extends ControllerExtension {

   private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("hh:mm:ss SSS");
   private static ControllerHost debugHost;

   public static void println(final String format, final Object... args) {
      if (debugHost != null) {
         final LocalDateTime now = LocalDateTime.now();
         debugHost.println(now.format(DF) + " > " + String.format(format, args));
      }
   }

   private FocusMode recordFocusMode = FocusMode.LAUNCHER;
   private Layer mainLayer;
   private ControllerHost host;
   private HardwareSurface surface;
   private MidiOut midiOut;
   private MidiIn midiInKey;
   private final OxyConfig config;

   public OxygenProExtension(final OxygenProExtensionDefinition definition, final ControllerHost host,
                             OxyConfig config) {
      super(definition, host);
      this.config = config;
   }

   @Override
   public void init() {
      host = getHost();
      debugHost = host;
      initPreferences(host);
      final Context diContext = new Context(this);
      diContext.registerService(OxyConfig.class, config);
      surface = diContext.getService(HardwareSurface.class);
      MidiIn midiIn = host.getMidiInPort(0);
      midiInKey = host.getMidiInPort(1);
      midiOut = host.getMidiOutPort(0);
      diContext.registerService(MidiIn.class, midiIn);
      diContext.registerService(MidiOut.class, midiOut);
      MidiProcessor midiProcessor = new MidiProcessor(host, midiIn, midiOut);
      diContext.registerService(MidiProcessor.class, midiProcessor);

      midiInKey.createNoteInput("MIDI", "8?????", "9?????", "A?????", "D?????", "B?????", "E?????");

      mainLayer = diContext.createLayer("MAIN");
      midiProcessor.initSysexMessages();
      //host.showPopupNotification("Oxygen Pro started");
      initTransport(diContext);
      mainLayer.setIsActive(true);
      midiProcessor.start();
      diContext.activate();
   }

   void initTransport(Context diContext) {
      Transport transport = diContext.getService(Transport.class);
      HwElements sessionLayer = diContext.getService(HwElements.class);
      transport.isArrangerRecordEnabled().markInterested();
      transport.isClipLauncherOverdubEnabled().markInterested();
      HwElements hwElements = diContext.getService(HwElements.class);
      CcButton rewindButton = hwElements.getButton(OxygenCcAssignments.FAST_RWD);
      rewindButton.bindRepeatHold(mainLayer, () -> transport.rewind());
      CcButton forwardButton = hwElements.getButton(OxygenCcAssignments.FAST_FWD);
      forwardButton.bindRepeatHold(mainLayer, () -> transport.fastForward());

      CcButton playButton = hwElements.getButton(OxygenCcAssignments.PLAY);
      playButton.bindPressed(mainLayer, transport.playAction());

      CcButton stopButton = hwElements.getButton(OxygenCcAssignments.STOP);
      stopButton.bindPressed(mainLayer, transport.stopAction());

      CcButton loopButton = hwElements.getButton(OxygenCcAssignments.LOOP);
      loopButton.bindPressed(mainLayer, transport.isArrangerLoopEnabled().toggleAction());

      hwElements.getButton(OxygenCcAssignments.METRO)
         .bindPressed(mainLayer, transport.isMetronomeEnabled().toggleAction());

      CcButton recButton = hwElements.getButton(OxygenCcAssignments.RECORD);
      recButton.bindPressed(mainLayer, () -> {
         if (recordFocusMode == FocusMode.LAUNCHER) {
            transport.isClipLauncherOverdubEnabled().toggle();
         } else {
            transport.isArrangerRecordEnabled().toggle();
         }
      });

      CcButton shiftButton = hwElements.getButton(OxygenCcAssignments.SHIFT);
      shiftButton.bindPressed(mainLayer, () -> hwElements.getShiftActive().set(true));
      shiftButton.bindRelease(mainLayer, () -> hwElements.getShiftActive().set(false));

//      CcButton backButton = hwElements.getButton(OxygenCcAssignments.BACK);
//      ModeLayer modeLayer = diContext.getService(ModeLayer.class);
//      backButton.bindPressed(mainLayer, () -> modeLayer.setIsActive(true));
//      backButton.bindRelease(mainLayer, () -> modeLayer.setIsActive(false));
   }

   void initPreferences(final ControllerHost host) {
      DocumentState documentState = host.getDocumentState();
      final SettableEnumValue recordButtonAssignment = documentState.getEnumSetting("Record Button assignment", //
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
