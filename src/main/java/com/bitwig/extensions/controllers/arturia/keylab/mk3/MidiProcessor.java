package com.bitwig.extensions.controllers.arturia.keylab.mk3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.IntConsumer;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.color.RgbColor;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.color.RgbLightState;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.display.ButtonDisplayType;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.display.CenterIcons;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.display.ScreenTarget;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.display.SmallIcon;
import com.bitwig.extensions.controllers.arturia.keylab.mk3.display.SysExBuilder;
import com.bitwig.extensions.framework.time.TimedEvent;

public class MidiProcessor {
    
    private static final String DEVICE_INQUIRY = "F0 7E 7F 06 01 F7";
    private final ControllerHost host;
    private final MidiIn midiIn;
    private final MidiOut midiOut;
    private final NoteInput noteInput;
    private final Queue<TimedEvent> timedEvents = new LinkedList<>();
    private int blinkCounter = 0;
    public static final String ARTURIA_HEADER = "F0 00 20 6B 7F 42 ";
    private static final String DAW_CONNECTION = ARTURIA_HEADER + "00 02 05 01 F7";
    private static final String DAW_DISCONNECTION = ARTURIA_HEADER + "00 02 05 00 F7";
    private static final String SET_DAW_PAD_BANK = ARTURIA_HEADER + "00 02 06 01 F7";
    private static final String SCREEN_HEADER = ARTURIA_HEADER + "00 02 04 ";
    private final List<Runnable> tickActions = new ArrayList<>();
    
    public MidiProcessor(final ControllerHost host) {
        this.host = host;
        this.midiIn = host.getMidiInPort(0);
        this.midiOut = host.getMidiOutPort(0);
        midiIn.createNoteInput("MIDI", "B00B??");
        final MidiIn midiIn2 = host.getMidiInPort(1);
        noteInput = midiIn2.createNoteInput("MIDI", "8?????", "9?????", "A?????", "B?????", "D?????", "E?????");
        midiIn.setMidiCallback(this::handleMidiIn);
        midiIn.setSysexCallback(this::handleSysEx);
        midiIn2.setMidiCallback(this::handleMidiIn2);
    }
    
    public void init() {
        midiOut.sendSysex(DEVICE_INQUIRY);
        host.scheduleTask(this::processMidi, 50);
    }
    
    public void addTickAction(final Runnable action) {
        tickActions.add(action);
    }
    
    private void processMidi() {
        blinkCounter = (blinkCounter + 1) % 8;
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
        for (final Runnable action : tickActions) {
            action.run();
        }
        host.scheduleTask(this::processMidi, 50);
    }
    
    public void delayAction(final Runnable action, final int time) {
        host.scheduleTask(action, time);
    }
    
    public void queueTimedEvent(final TimedEvent timedEvent) {
        timedEvents.add(timedEvent);
    }
    
    public void exit() {
        midiOut.sendSysex(DAW_DISCONNECTION);
    }
    
    public RgbLightState blinkSlow(final RgbLightState color) {
        return blinkCounter % 8 < 4 ? color : RgbLightState.OFF;
    }
    
    public RgbLightState blinkMid(final RgbLightState color) {
        return blinkCounter % 4 < 2 ? color : RgbLightState.OFF;
    }
    
    public RgbLightState blinkFast(final RgbLightState color) {
        return blinkCounter % 2 == 0 ? color : RgbLightState.OFF;
    }
    
    public RgbLightState blinkPeriodic(final RgbLightState color) {
        return (blinkCounter == 0 || blinkCounter == 3) ? color : RgbLightState.OFF;
    }
    
    public int getBlinkCounter() {
        return blinkCounter;
    }
    
    
    public RelativeHardwarControlBindable createIncrementBindable(final IntConsumer consumer) {
        final HardwareActionBindable incAction = host.createAction(() -> consumer.accept(1), () -> "+");
        final HardwareActionBindable decAction = host.createAction(() -> consumer.accept(-1), () -> "-");
        return host.createRelativeHardwareControlStepTarget(incAction, decAction);
    }
    
    public MidiIn getMidiIn() {
        return midiIn;
    }
    
    private void handleSysEx(final String sysEx) {
        if (sysEx.startsWith("f07e7f06020020")) {
            KeylabMk3ControllerExtension.println("Connected ");
            midiOut.sendSysex(DAW_CONNECTION);
            host.scheduleTask(this::afterConnect, 1000);
        } else {
            KeylabMk3ControllerExtension.println("SysEx = %s", sysEx);
        }
    }
    
    private void afterConnect() {
        screenLine2(ScreenTarget.POP_UP_TOP_ICON_2LINES, "Bitwig", RgbColor.WHITE, "Connected", RgbColor.WHITE,
            CenterIcons.BITWIGSTUDIO);
    }
    
    private void handleMidiIn(final int status, final int data1, final int data2) {
        if ((status & 0xF0) != 0xA0) {
            KeylabMk3ControllerExtension.println("*MIDI => %02X %02X %02X", status, data1, data2);
        }
    }
    
    private void handleMidiIn2(final int status, final int data1, final int data2) {
        KeylabMk3ControllerExtension.println("MIDI2 => %02X %02X %02X", status, data1, data2);
    }
    
    public void sendSysex(final byte[] sysExData) {
        midiOut.sendSysex(sysExData);
    }
    
    public void screenLine1(final ScreenTarget target, final String text, final RgbColor color) {
        screenLine1(target, text, color, null);
    }
    
    public void screenLine1(final ScreenTarget target, final String text, final RgbColor color, final int iconId) {
        final SysExBuilder sysEx = new SysExBuilder(target);
        sysEx.appendText(text);
        sysEx.appendValue(0); // color type  effects not clear
        sysEx.appendColor(color);
        sysEx.appendValue(iconId);
        sysEx.complete();
        midiOut.sendSysex(sysEx.getData());
    }
    
    public void screenLine1(final ScreenTarget target, final String text, final RgbColor color,
        final CenterIcons icon) {
        final SysExBuilder sysEx = new SysExBuilder(target);
        sysEx.appendText(text);
        sysEx.appendValue(0); // color type  effects not clear
        sysEx.appendColor(color);
        sysEx.appendIcon(icon);
        sysEx.complete();
        midiOut.sendSysex(sysEx.getData());
    }
    
    public void popup(final int type, final String line1, final String line2, final int widgetPos,
        final RgbColor color) {
        final SysExBuilder sysEx = new SysExBuilder(0x1C + type);
        sysEx.appendText(line1);
        sysEx.appendText(line2);
        sysEx.appendValue(widgetPos);
        sysEx.appendValue(1); // color type  effects not clear
        sysEx.appendColor(color);
        sysEx.complete();
        midiOut.sendSysex(sysEx.getData());
    }
    
    public void screenLine2(final ScreenTarget target, final String text, final RgbColor color, final String text2,
        final RgbColor color2, final CenterIcons icon) {
        final SysExBuilder sysEx = new SysExBuilder(target);
        sysEx.appendText(text);
        sysEx.appendValue(1);
        sysEx.appendColor(color);
        sysEx.appendText(text2);
        sysEx.appendValue(1);
        sysEx.appendColor(color2);
        sysEx.appendIcon(icon);
        sysEx.complete();
        midiOut.sendSysex(sysEx.getData());
    }
    
    public void screenLine3(final ScreenTarget target, final String text, final RgbColor color, final String text2,
        final RgbColor color2, final String text3, final RgbColor color3, final CenterIcons icon) {
        final SysExBuilder sysEx = new SysExBuilder(target);
        sysEx.appendText(text);
        sysEx.appendValue(1);
        sysEx.appendColor(color);
        sysEx.appendText(text2);
        sysEx.appendValue(1);
        sysEx.appendColor(color2);
        sysEx.appendText(text3);
        sysEx.appendValue(1);
        sysEx.appendColor(color3);
        sysEx.appendIcon(icon);
        sysEx.complete();
        midiOut.sendSysex(sysEx.getData());
    }
    
    
    public void screenContextButton(final int contextId, final ButtonDisplayType type, final SmallIcon icon,
        final RgbColor color, final String text) {
        final SysExBuilder sysEx = new SysExBuilder(contextId);
        sysEx.appendValue(type.getId());
        sysEx.appendValue(0); // line b  effects not clear
        sysEx.appendValue(1); // color type  effects not clear
        sysEx.appendIcon(icon); // color type  effects not clear
        sysEx.appendColor(color);
        sysEx.appendText(text);
        sysEx.complete();
        midiOut.sendSysex(sysEx.getData());
    }
    
    public void screenContextButton(final int contextId, final int type, final int iconId, final RgbColor color,
        final String text) {
        final SysExBuilder sysEx = new SysExBuilder(contextId);
        sysEx.appendValue(type);
        sysEx.appendValue(0); // line b  effects not clear
        sysEx.appendValue(1); // color type  effects not clear
        sysEx.appendValue(iconId); // color type  effects not clear
        sysEx.appendColor(color);
        sysEx.appendText(text);
        sysEx.complete();
        midiOut.sendSysex(sysEx.getData());
    }
    
    public void submitControl(final int id, final boolean exists) {
        final String sysEx =
            "F0 00 20 6B 7F 42 00 %02X 00 %02X 00 %02X 00 01 %02X 00 02 %02X 00 F7".formatted(2, id, 1, 0,
                exists ? 1 : 0);
        midiOut.sendSysex(sysEx);
    }
}
