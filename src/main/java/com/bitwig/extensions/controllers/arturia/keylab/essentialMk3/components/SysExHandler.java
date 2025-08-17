package com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.components;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.CCAssignment;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.KeylabEssential3Extension;
import com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.RgbButton;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;
import com.bitwig.extensions.framework.time.TimedDelayEvent;
import com.bitwig.extensions.framework.time.TimedEvent;

@Component
public class SysExHandler {
    
    public static final String ARTURIA_SYSEX_HEADER = "f0 00 20 6B 7f 42 ";
    public static final String SYSEX_DEVICE_RECOGNITION = "f07e7f060200206b02";
    public static final String PAD_MODE_HEADER = "f000206b7f422111400000";
    public static final String MODE_CHANGE_HEADER = "f000206b7f422111400200";
    
    public enum SysexEventType {
        DAW_MODE,
        ARTURIA_MODE,
        USER_MODE,
        INIT
    }
    
    public enum PadMode {
        PAD_CLIPS,
        PAD_DRUMS;
        
        public PadMode inverted() {
            return this == PAD_CLIPS ? PAD_DRUMS : PAD_CLIPS;
        }
    }
    
    @Inject
    private MidiOut midiOut;
    @Inject
    private ControllerHost host;
    private final MidiIn midiIn;
    
    private final List<RgbButton> buttons = new ArrayList<>();
    private final List<Consumer<SysexEventType>> sysExEventListener = new ArrayList<>();
    private final List<Consumer<PadMode>> padModeEventListener = new ArrayList<>();
    private final Queue<Runnable> sysExTask = new LinkedList<>();
    private final Queue<TimedEvent> timedEvents = new LinkedList<>();
    private final List<Runnable> tickListeners = new ArrayList<>();
    
    private final long creationTime;
    private boolean processingReady = false;
    private boolean midiProcessingRunning;
    
    public SysExHandler(final MidiIn midiIn) {
        this.midiIn = midiIn;
        midiIn.setSysexCallback(this::handleSysExData);
        creationTime = System.currentTimeMillis();
    }
    
    @Activate
    public void activate() {
        host.scheduleTask(this::processMidi, 0);
        midiProcessingRunning = true;
    }
    
    public MidiIn getMidiIn() {
        return midiIn;
    }
    
    public void processMidi() {
        if (processingReady && !sysExTask.isEmpty()) {
            while (!sysExTask.isEmpty()) {
                sysExTask.poll().run();
            }
        }
        if (!timedEvents.isEmpty()) {
            final Iterator<TimedEvent> it = timedEvents.iterator();
            while (it.hasNext()) {
                final TimedEvent event = it.next();
                event.process();
                if (event.isCompleted()) {
                    it.remove();
                }
            }
        }
        if (!processingReady) {
            final long diff = System.currentTimeMillis() - creationTime;
            if (diff > 1000) {
                KeylabEssential3Extension.println(" Not Connected after %d ms", diff);
                disconnectState();
                midiProcessingRunning = false;
                pause(100);
                deviceInquiry();
                return;
            }
        }
        tickListeners.forEach(Runnable::run);
        host.scheduleTask(this::processMidi, 10);
    }
    
    public void registerTickTask(final Runnable task) {
        tickListeners.add(task);
    }
    
    public void requestPadBank() {
        midiOut.sendSysex(ARTURIA_SYSEX_HEADER + "01 11 40 00 F7");
    }
    
    public void sendDelayed(final byte[] sysExByteCommand, final int waitTime) {
        timedEvents.add(new TimedDelayEvent(() -> midiOut.sendSysex(sysExByteCommand), waitTime));
    }
    
    public void queueTimedEvent(final TimedEvent timedEvent) {
        timedEvents.add(timedEvent);
    }
    
    private void handleSysExData(final String sysEx) {
        if (sysEx.startsWith(SYSEX_DEVICE_RECOGNITION)) {
            final String value = extractSysexRest(sysEx, MODE_CHANGE_HEADER);
            final long diff = System.currentTimeMillis() - creationTime;
            KeylabEssential3Extension.println(
                "Device Inquiry Response = %s after %d ms MIDI Processing=%s", value,
                diff, midiProcessingRunning);
            requestInitState();
            pause(30);
            notify(SysexEventType.INIT);
            if (!midiProcessingRunning) {
                KeylabEssential3Extension.println(" REACTIVATING ");
                activate();
            }
        } else if (sysEx.startsWith(MODE_CHANGE_HEADER)) {
            final String mode = extractSysexRest(sysEx, MODE_CHANGE_HEADER);
            KeylabEssential3Extension.println(" MODE = %s", mode);
            switch (mode) {
                case "01" -> notify(SysexEventType.DAW_MODE);
                case "00" -> notify(SysexEventType.ARTURIA_MODE);
                case "02" -> notify(SysexEventType.USER_MODE);
            }
        } else if (sysEx.startsWith(PAD_MODE_HEADER)) {
            final String mode = extractSysexRest(sysEx, PAD_MODE_HEADER);
            if (mode.equals("01")) {
                padModeEventListener.forEach(e -> e.accept(PadMode.PAD_DRUMS));
            } else {
                padModeEventListener.forEach(e -> e.accept(PadMode.PAD_CLIPS));
            }
        } else {
            KeylabEssential3Extension.println("Unknown Received SysEx : %s", sysEx);
        }
    }
    
    private String extractSysexRest(final String sysEx, final String header) {
        return sysEx.substring(header.length(), sysEx.length() - 2);
    }
    
    public void registerButton(final RgbButton button) {
        buttons.add(button);
    }
    
    private void notify(final SysexEventType event) {
        sysExEventListener.forEach(e -> e.accept(event));
    }
    
    public void addSysExEventListener(final Consumer<SysexEventType> listener) {
        sysExEventListener.add(listener);
    }
    
    public void addPadModeEventListener(final Consumer<PadMode> listener) {
        padModeEventListener.add(listener);
    }
    
    public void deviceInquiry() {
        midiOut.sendSysex("f0 7e 7f 06 01 f7"); // Universal Request
    }
    
    public void requestInitState() {
        midiOut.sendSysex("f0 00 20 6B 7f 42 02 0F 40 5A 01 F7");
        processingReady = true;
    }
    
    public void disconnectState() {
        KeylabEssential3Extension.println(" Disconnect ");
        midiOut.sendSysex("f0 00 20 6B 7f 42 02 0F 40 5A 00 F7");
        processingReady = false;
        pause(20);
    }
    
    public void sendRgb(final CCAssignment hwElement, final int red, final int green, final int blue) {
        final String sysex = ARTURIA_SYSEX_HEADER + "04 01 " // Command ID + Patch ID
            + "16 " // Parameter Type
            + toHex(hwElement.getItemId()) + toHex(red) + toHex(green) + toHex(blue) + "F7";
        sysExTask.add(() -> midiOut.sendSysex(sysex));
    }
    
    public void sendSysex(final byte[] sysExExpression) {
        sysExTask.add(() -> midiOut.sendSysex(sysExExpression));
    }
    
    public void sendSysex(final String sysExExpression) {
        sysExTask.add(() -> midiOut.sendSysex(sysExExpression));
    }
    
    public void sendSysexText(final String sysExExpression) {
        sysExTask.add(() -> midiOut.sendSysex(sysExExpression));
    }
    
    public static String toHex(final int values) {
        final String hexValue = Integer.toHexString((byte) values);
        return (hexValue.length() < 2 ? "0" + hexValue : hexValue) + " ";
    }
    
    void pause(final int millis) {
        try {
            Thread.sleep(millis);
        }
        catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    
}
