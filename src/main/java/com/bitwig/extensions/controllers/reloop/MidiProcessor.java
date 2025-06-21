package com.bitwig.extensions.controllers.reloop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.IntConsumer;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extensions.controllers.reloop.display.ScreenBuffer;
import com.bitwig.extensions.framework.time.TimedEvent;

public class MidiProcessor {
    private static final String ENTER_DAW_MODE = "F0 26 4A 16 01 01 F7";
    private static final String EXIT_DAW_MODE = "F0 26 4A 16 01 00 F7";
    private final MidiIn midiIn;
    private final MidiOut midiOut;
    private final ControllerHost host;
    private final IntConsumer initDoneCallback;
    private int blinkState = 0;
    private boolean initComplete = false;
    private final String[] displayLines = new String[4];
    private final ScreenBuffer screenBuffer;
    private final GlobalStates globalStates;
    protected final Queue<TimedEvent> timedEvents = new ConcurrentLinkedQueue<>();
    int lastTempoValue = -1;
    private boolean ccStateNeedsRefresh = false;
    
    
    public MidiProcessor(final ControllerHost host, final IntConsumer initDoneCallback,
        final GlobalStates globalStates) {
        this.host = host;
        this.initDoneCallback = initDoneCallback;
        Arrays.fill(displayLines, "");
        this.midiIn = host.getMidiInPort(0);
        this.midiOut = host.getMidiOutPort(0);
        this.globalStates = globalStates;
        midiIn.setMidiCallback((ShortMidiMessageReceivedCallback) this::onMidi0);
        midiIn.setSysexCallback(this::onSysex0);
        this.screenBuffer = new ScreenBuffer(midiOut);
        final NoteInput noteInput = midiIn.createNoteInput("MIDI", getMasks(9));
        noteInput.setShouldConsumeEvents(true);
    }
    
    public ScreenBuffer getScreenBuffer() {
        return screenBuffer;
    }
    
    private void onMidi0(final ShortMidiMessage msg) {
        if (msg.getData1() == 0x67) {
            globalStates.getCcState().set(msg.getData2() > 0);
            if (!initComplete && msg.getData2() > 0) {
                ccStateNeedsRefresh = true;
            }
        }
        if (!initComplete && msg.getStatusByte() == 0XB1 && msg.getData1() == 0x7A && msg.getData2() == 0x7F) {
            KeypadProControllerExtension.println("INIT Complete ");
            host.scheduleTask(() -> {
                midiOut.sendSysex(ENTER_DAW_MODE);
            }, 200);
            host.scheduleTask(() -> {
                screenBuffer.showConnected();
                initDoneCallback.accept(0);
            }, 300);
            host.scheduleTask(() -> {
                initComplete = true;
                initDoneCallback.accept(1);
                if (ccStateNeedsRefresh) {
                    KeypadProControllerExtension.println(" ############# ");
                    globalStates.getCcState().set(false);
                    globalStates.getCcState().set(true);
                    ccStateNeedsRefresh = false;
                }
            }, 1000);
            
        }
        //        if ((msg.getStatusByte() & 0xF0) != 0xA0) {
        //            KeypadProControllerExtension.println("      => %02X %02X %02X", msg.getStatusByte(), msg
        //            .getData1(),
        //                msg.getData2());
        //        }
    }
    
    private void onSysex0(final String sysExString) {
        KeypadProControllerExtension.println(" SYSEX=%s", sysExString);
    }
    
    private String[] getMasks(final int exclusionChannel) {
        final List<String> maskList = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            if (i != exclusionChannel) {
                maskList.addAll(getThroughMask(i));
            }
        }
        
        return maskList.toArray(String[]::new);
    }
    
    private List<String> getThroughMask(final int channel) {
        return List.of("8%X????".formatted(channel), "9%X????".formatted(channel), "A%X????".formatted(channel),
            "D%X????".formatted(channel), "B%X01??".formatted(channel), "B%X40??".formatted(channel),
            "E%X????".formatted(channel));
    }
    
    public void init(final int delay) {
        host.scheduleTask(() -> {
            midiOut.sendSysex("F0 00 20 7F 00 F7");
        }, delay);
        host.scheduleTask(this::handlePing, 100);
    }
    
    public void updateTempo(final int bpmValue) {
        if (bpmValue < 20 || bpmValue > 480 || lastTempoValue == bpmValue) {
            return;
        }
        lastTempoValue = bpmValue;
        final int sendValue = bpmValue;
        final String sysEx = "F0 26 4A 16 03 %02X %02X 00 F7".formatted((sendValue >> 7) & 0x7F, sendValue & 0x7f);
        KeypadProControllerExtension.println(" Set Tempo=%d  SysEx=%s", bpmValue, sysEx);
        midiOut.sendSysex(sysEx);
    }
    
    public void exit() {
        //midiOut.sendSysex(EXIT_DAW_MODE);
    }
    
    public void sendMidi(final int status, final int data1, final int data2) {
        midiOut.sendMidi(status, data1, data2);
    }
    
    public MidiIn getMidiIn() {
        return midiIn;
    }
    
    private void handlePing() {
        blinkState = (blinkState + 1) % 16;
        if (!timedEvents.isEmpty()) {
            for (final TimedEvent event : timedEvents) {
                event.process();
                if (event.isCompleted()) {
                    timedEvents.remove(event);
                }
            }
        }
        host.scheduleTask(this::handlePing, 35);
    }
    
    public void queueEvent(final TimedEvent event) {
        timedEvents.add(event);
    }
    
    public ReloopRgb blinkBrightSlow(final int color) {
        return ReloopRgb.of(blinkState / 8 == 0 ? color : color | 0x40);
    }
    
    public ReloopRgb blinkSlow(final int color, final ReloopRgb altColor) {
        return blinkState / 8 == 0 ? ReloopRgb.of(color) : altColor;
    }
    
    public ReloopRgb blinkMid(final int onColor, final int offColor) {
        return ReloopRgb.of(blinkState % 8 < 4 ? onColor : offColor);
    }
    
    public ReloopRgb blinkFast(final int onColor, final int offColor) {
        return ReloopRgb.of(blinkState % 2 == 0 ? onColor : offColor);
    }
    
    public ReloopRgb blinkBrightFast(final int color) {
        return ReloopRgb.of(blinkState % 2 == 0 ? color : color | 0x40);
    }
    
    public ReloopRgb blinkBrightMid(final int color) {
        return ReloopRgb.of(blinkState % 4 < 2 ? color : color | 0x40);
    }
    
    public boolean blinkMid() {
        return blinkState % 4 < 2;
    }
    
    public boolean blinkSlow() {
        return blinkState / 8 == 0;
    }
    
    
}
