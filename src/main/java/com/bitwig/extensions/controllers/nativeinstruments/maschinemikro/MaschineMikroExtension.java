package com.bitwig.extensions.controllers.nativeinstruments.maschinemikro;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.nativeinstruments.maschinemikro.layers.ModifierLayer;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Context;
import com.bitwig.extensions.framework.values.FocusMode;

public class MaschineMikroExtension extends ControllerExtension {

   private final ControllerHost host;
   private HardwareSurface surface;
   private Layer mainLayer;
   private FocusMode recordFocusMode = FocusMode.LAUNCHER;

   protected MaschineMikroExtension(final ControllerExtensionDefinition definition, final ControllerHost host) {
      super(definition, host);
      this.host = host;
   }

   @Override
   public void init() {
      DebugOutMk.registerHost(host);
      initPreferences(host);
      final Context diContext = new Context(this);
      surface = diContext.getService(HardwareSurface.class);
      MidiIn midiIn = host.getMidiInPort(0);
      MidiOut midiOut = host.getMidiOutPort(0);
      diContext.registerService(MidiIn.class, midiIn);
      diContext.registerService(MidiOut.class, midiOut);
      MidiProcessor midiProcessor = new MidiProcessor(host, midiIn, midiOut);
      diContext.registerService(MidiProcessor.class, midiProcessor);
      mainLayer = diContext.createLayer("MAIN");
      diContext.getService(ModifierLayer.class).init(mainLayer, diContext.getService(HwElements.class));
      initTransport(diContext);
      mainLayer.setIsActive(true);
      DebugOutMk.println(" INIT MIKRO MK3");

      midiProcessor.start();
      diContext.activate();
   }

   private void initTransport(Context diContext) {
      Transport transport = diContext.getService(Transport.class);
      HwElements hwElements = diContext.getService(HwElements.class);
      transport.isPlaying().markInterested();
      transport.isArrangerRecordEnabled().markInterested();
      transport.isClipLauncherOverdubEnabled().markInterested();

      hwElements.getButton(CcAssignment.PLAY).bindPressed(mainLayer, transport.playAction());
      hwElements.getButton(CcAssignment.PLAY).bindLight(mainLayer, transport.isPlaying());
      hwElements.getButton(CcAssignment.RECORD).bindPressed(mainLayer, () -> handleRecordButton(transport));
      hwElements.getButton(CcAssignment.RECORD).bindLight(mainLayer, () -> recordActive(transport));
      hwElements.getButton(CcAssignment.STOP).bindPressed(mainLayer, transport.stopAction());
      hwElements.getButton(CcAssignment.STOP).bindLightHeld(mainLayer);
   }

   private void handleRecordButton(Transport transport) {
      if (recordFocusMode == FocusMode.LAUNCHER) {
         transport.isClipLauncherOverdubEnabled().toggle();
      } else {
         transport.isArrangerRecordEnabled().toggle();
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
