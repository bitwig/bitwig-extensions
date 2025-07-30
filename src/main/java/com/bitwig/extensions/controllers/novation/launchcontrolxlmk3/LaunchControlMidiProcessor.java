package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.RgbColor;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.layer.BaseMode;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Deactivate;
import com.bitwig.extensions.framework.time.TimedEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

@Component
public class LaunchControlMidiProcessor {
   private static final String DEVICE_INQUIRY = "F0 7E 7F 06 01 F7";
   private static final String DEVICE_RESPONSE_HEADER = "f07e000602002029";
   private static final String NOVATION_HEADER = "F0 00 20 29 02 15 ";
   private static final byte[] TEXT_CONFIG_COMMAND =
      {(byte) 0xF0, 0x00, 0x20, 0x29, 0x02, 0x15, 0x04, 0x00, 0x00, (byte) 0xF7};
   private static final int[] ROW_CC_IDS = {0x45, 0x48, 0x49};
   private static final String COLOR_RGB = "F0 00 20 29 02 15 01 53 %02X %02X %02X %02X F7";

   private final ControllerHost host;
   private final MidiIn midiIn;
   private final MidiOut midiOut;
   private Runnable hwUpdater;
   protected final Queue<TimedEvent> timedEvents = new ConcurrentLinkedQueue<>();
   private final List<Consumer<BaseMode>> modeListeners = new ArrayList<>();
   private final List<Runnable> startListeners = new ArrayList<>();
   private boolean init = false;

   public LaunchControlMidiProcessor(final ControllerHost host) {
      this.host = host;
      this.midiIn = host.getMidiInPort(0);
      this.midiOut = host.getMidiOutPort(0);
      midiIn.setMidiCallback(this::handleMidiIn);
      midiIn.setSysexCallback(this::handleSysEx);
   }

   @Activate
   public void init() {
      midiOut.sendSysex(DEVICE_INQUIRY);
   }

   @Deactivate
   public void exit() {
      midiOut.sendMidi(0x9F, 0x0C, 0x00);
   }

   public String getSysexHeader() {
      return NOVATION_HEADER;
   }

   private void setMixLayout() {
      midiOut.sendMidi(0xB6, 0x1E, 0x01);
   }

   private void setControlLayout() {
      midiOut.sendMidi(0xB6, 0x1E, 0x02);
   }

   private void setToRelative(final int row, final boolean on) {
      midiOut.sendMidi(0xB6, ROW_CC_IDS[row], on ? 0x7F : 0x00);
   }

   public void queueEvent(final TimedEvent event) {
      timedEvents.add(event);
   }

   public void setCcMatcher(final HardwareButton hwButton, final int ccNr, final int channel) {
      hwButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, ccNr, 127));
      hwButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, ccNr, 0));
   }

   public RelativeHardwareValueMatcher createNonAcceleratedMatcher(final int ccNr) {
      final RelativeHardwareValueMatcher stepDownMatcher =
         midiIn.createRelativeValueMatcher("(status == 191 && data1 == %d && data2 > 64)".formatted(ccNr), -1);
      final RelativeHardwareValueMatcher stepUpMatcher =
         midiIn.createRelativeValueMatcher("(status == 191 && data1 == %d && data2 < 65)".formatted(ccNr), 1);
      return host.createOrRelativeHardwareValueMatcher(stepDownMatcher, stepUpMatcher);
   }

   public RelativeHardwareValueMatcher createAcceleratedMatcher(final int ccNr) {
      return midiIn.createRelativeBinOffsetCCValueMatcher(0xF, ccNr, 200);
   }

   public RelativeHardwarControlBindable createIncAction(final IntConsumer changeAction) {
      final HardwareActionBindable incAction = host.createAction(() -> changeAction.accept(1), () -> "+");
      final HardwareActionBindable decAction = host.createAction(() -> changeAction.accept(-1), () -> "-");
      return host.createRelativeHardwareControlStepTarget(incAction, decAction);
   }

   public void setAbsoluteCcMatcher(final AbsoluteHardwareControl control, final int ccNr, final int channel) {
      control.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(channel, ccNr));
   }

   private void handleMidiIn(final int status, final int data1, final int data2) {
      //LaunchControlXlMk3Extension.println("MIDI => %02X %02X %02X", status, data1, data2);
      if (status == 0xB6 && data1 == 0x1E) {
         final BaseMode mode = BaseMode.toMode(data2);
         if (mode != null) {
            this.modeListeners.forEach(listener -> listener.accept(mode));
            if (mode == BaseMode.MIXER) {
               setToRelative(0, false);
               setToRelative(1, false);
               setToRelative(2, true);
            } else {
               setToRelative(0, true);
               setToRelative(1, true);
               setToRelative(2, true);
            }
         }
      }
   }

   public void addModeListener(final Consumer<BaseMode> listener) {
      this.modeListeners.add(listener);
   }

   public void addStartListener(final Runnable startAction) {
      this.startListeners.add(startAction);
   }

   private void handleSysEx(final String data) {
      if (!data.endsWith("f7")) {
         LaunchControlXlMk3Extension.println("Illegal Sysex Received : %s", data);
         return;
      }
      if (data.startsWith(DEVICE_RESPONSE_HEADER)) {
         final String[] values = extractValues(data, DEVICE_RESPONSE_HEADER.length(), 8);
         LaunchControlXlMk3Extension.println("Device response : %s", Arrays.toString(values));
         startMidi();
      } else if (data.startsWith("f00020290215027ff7")) {
         hwUpdater.run();
      } else {
         LaunchControlXlMk3Extension.println(" SYSEX %s", data);
      }
   }

   private void startMidi() {
      init = true;
      startDawMode();
      setControlLayout();
      setMixLayout();
      startListeners.forEach(Runnable::run);
      host.scheduleTask(this::handlePing, 50);
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
      host.scheduleTask(this::handlePing, 100);
   }

   private void startDawMode() {
      final String msg = NOVATION_HEADER + "02 7F F7";
      midiOut.sendSysex(msg);
   }

   public void sendMidi(final int status, final int data1, final int data2) {
      midiOut.sendMidi(status, data1, data2);
   }

   public void sendSysExBytes(final byte[] data) {
      if (!init) {
         return;
      }
      midiOut.sendSysex(data);
   }

   public void sendSysExString(final String data) {
      if (!init) {
         return;
      }
      //LaunchControlXlMk3Extension.println(" SEND %s", data);
      midiOut.sendSysex(data);
   }

   private static String[] extractValues(final String data, final int offset, final int count) {
      final String[] result = new String[count];
      int location = offset;
      for (int i = 0; i < count && location + 1 < data.length(); i++) {
         result[i] = data.substring(location, location + 2).toUpperCase();
         location += 2;
      }
      return result;
   }


   public void registerUpdater(final Runnable hwUpdater) {
      this.hwUpdater = hwUpdater;
   }

   public void sendRgb(final int index, final RgbColor color) {
      final String msg = COLOR_RGB.formatted(index, color.red(), color.green(), color.blue());
      midiOut.sendSysex(msg);
   }
}
