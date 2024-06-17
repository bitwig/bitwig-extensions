package com.bitwig.extensions.controllers.mcu.display;

import com.bitwig.extensions.controllers.mcu.McuExtension;
import com.bitwig.extensions.controllers.mcu.MidiProcessor;
import com.bitwig.extensions.controllers.mcu.config.McuAssignments;
import com.bitwig.extensions.framework.values.Midi;

public class TimeCodeLed {
    private static final int CODE_BLANK_OFFSET = -16;
    private final MidiProcessor midiProcessor;
    private double position;
    private boolean preCountBeats = false;
    private int bars = -1;
    private int beats = -1;
    private int subDivision = -1;
    private int ticks = -1;
    
    private boolean preCountTime = false;
    private int frames = -1;
    private int seconds = -1;
    private int minutes = -1;
    private int hours = -1;
    
    private int tsMain;
    private int tsDiv;
    private int tsTicks = 16;
    private Mode mode = Mode.BEATS;
    private final DisplayType displayType;
    
    public enum Mode {
        BEATS,
        TIME
    }
    
    public enum DisplayType {
        MCU(-3),
        ICON(11);
        private final int minusOffset;
        
        DisplayType(final int minusOffset) {
            this.minusOffset = minusOffset;
        }
        
        public int getMinusOffset() {
            return minusOffset;
        }
        
    }
    
    public TimeCodeLed(final MidiProcessor midiProcessor, final DisplayType displayType) {
        this.midiProcessor = midiProcessor;
        this.displayType = displayType;
    }
    
    public void toggleMode() {
        if (mode == Mode.BEATS) {
            setMode(Mode.TIME);
        } else {
            setMode(Mode.BEATS);
        }
        refreshMode();
    }
    
    public void refreshMode() {
        if (mode == Mode.BEATS) {
            midiProcessor.sendMidi(Midi.NOTE_ON, McuAssignments.BEATS_MODE.getNoteNo(), 127);
            midiProcessor.sendMidi(Midi.NOTE_ON, McuAssignments.SMPTE_MODE.getNoteNo(), 0);
        } else {
            midiProcessor.sendMidi(Midi.NOTE_ON, McuAssignments.BEATS_MODE.getNoteNo(), 0);
            midiProcessor.sendMidi(Midi.NOTE_ON, McuAssignments.SMPTE_MODE.getNoteNo(), 127);
        }
    }
    
    public void ensureMode() {
        if (mode == Mode.BEATS) {
            midiProcessor.sendMidi(Midi.NOTE_ON, McuAssignments.DISPLAY_SMPTE.getNoteNo(), 127);
        } else {
            midiProcessor.sendMidi(Midi.NOTE_ON, McuAssignments.DISPLAY_SMPTE.getNoteNo(), 0);
        }
    }
    
    private void refreshPosition() {
        displayTicksBeat(ticks);
        displaySubdivisionBeat(subDivision);
        displayBeatsBeat(beats);
        displayBarsBeat(bars, preCountBeats);
    }
    
    private void refreshTime() {
        displayTicks(frames);
        displaySubdivision(seconds);
        displayBeats(minutes);
        displayBars(hours, preCountTime);
    }
    
    private void displayTicksBeat(final int v) {
        final int value = this.preCountBeats ? 99 - v : v;
        final int seg2 = value / 10 % 10;
        final int seg3 = value / 100 % 10;
        final int v1 = value % 10;
        final int v2 = zeroToBlank(seg2, seg3);
        final int v3 = zeroToBlank(seg3, 0);
        midiProcessor.sendMidi(Midi.CC, 64, v1 + 48);
        midiProcessor.sendMidi(Midi.CC, 65, v2 + 48);
        midiProcessor.sendMidi(Midi.CC, 66, v3 + 48);
    }
    
    private int zeroToBlank(final int v, final int preDigit) {
        if (preDigit == 0 && v == 0) {
            return CODE_BLANK_OFFSET;
        }
        return v;
    }
    
    private void displayTicks(final int value) {
        final int v1 = value % 10;
        final int v2 = value / 10 % 10;
        final int v3 = value / 100 % 10;
        midiProcessor.sendMidi(Midi.CC, 64, v1 + 48);
        midiProcessor.sendMidi(Midi.CC, 65, v2 + 48);
        midiProcessor.sendMidi(Midi.CC, 66, v3 + 48);
    }
    
    private void displaySubdivision(final int value) {
        final int v1 = value % 10;
        final int v2 = value / 10 % 10;
        midiProcessor.sendMidi(Midi.CC, 67, v1 + 48 + 64);
        midiProcessor.sendMidi(Midi.CC, 68, v2 + 48);
    }
    
    private void displaySubdivisionBeat(final int v) {
        final int value = this.preCountBeats ? 5 - v : v;
        final int v1 = value % 10;
        final int v2 = value / 10 % 10;
        midiProcessor.sendMidi(Midi.CC, 67, v1 + 48 + 64);
        midiProcessor.sendMidi(Midi.CC, 68, zeroToBlank(v2, 0) + 48);
    }
    
    private void displayBeats(final int value) {
        final int v1 = value % 10;
        final int v2 = value / 10 % 10;
        midiProcessor.sendMidi(Midi.CC, 69, v1 + 48 + 64);
        midiProcessor.sendMidi(Midi.CC, 70, v2 + 48);
    }
    
    private void displayBeatsBeat(final int v) {
        final int value = this.preCountBeats ? 5 - v : v;
        final int v1 = value % 10;
        final int v2 = zeroToBlank(value / 10 % 10, 0);
        midiProcessor.sendMidi(Midi.CC, 69, v1 + 48 + 64);
        midiProcessor.sendMidi(Midi.CC, 70, v2 + 48);
    }
    
    private void displayBars(final int value, final boolean preCount) {
        final int v1 = value % 10;
        final int v2 = value / 10 % 10;
        midiProcessor.sendMidi(Midi.CC, 71, v1 + 48 + 64);
        midiProcessor.sendMidi(Midi.CC, 72, v2 + 48);
        if (preCount) {
            midiProcessor.sendMidi(Midi.CC, 73, 48 + displayType.getMinusOffset());
        } else {
            final int v3 = value / 100 % 10;
            midiProcessor.sendMidi(Midi.CC, 73, v3 + 48);
        }
    }
    
    private void displayBarsBeat(final int value, final boolean preCount) {
        final int v1 = value % 10;
        final int v2 = value / 10 % 10;
        final int v3 = value / 100 % 10;
        midiProcessor.sendMidi(Midi.CC, 71, v1 + 48 + 64);
        midiProcessor.sendMidi(Midi.CC, 72, zeroToBlank(v2, v3) + 48);
        if (preCount) {
            midiProcessor.sendMidi(Midi.CC, 73, 48 + displayType.getMinusOffset());
        } else {
            midiProcessor.sendMidi(Midi.CC, 73, zeroToBlank(v3, 0) + 48);
        }
    }
    
    public Mode getMode() {
        return mode;
    }
    
    public void setMode(final Mode mode) {
        if (this.mode != mode) {
            this.mode = mode;
            if (mode == Mode.BEATS) {
                refreshPosition();
            } else {
                refreshTime();
            }
        }
    }
    
    public void setDivision(final String division) {
        final String[] v = division.split("/");
        if (v.length == 2) {
            tsMain = Integer.parseInt(v[0]);
            if (v[1].indexOf(',') > 0) {
                final String[] denominator = v[1].split(",");
                if (denominator.length == 2) {
                    tsDiv = Integer.parseInt(denominator[0]);
                    tsTicks = Integer.parseInt(denominator[1]);
                }
            } else {
                tsDiv = Integer.parseInt(v[1]);
                tsTicks = 16;
            }
            updatePosition(position);
        }
    }
    
    public void updatePosition(final double pos) {
        position = pos;
        final boolean preCount = pos < 0;
        McuExtension.println(" POS = %f %s %s", pos, preCount, this.preCountBeats);
        final double positionAbs = Math.abs(pos);
        final int totalBeats = (int) (positionAbs * tsDiv / 4);
        final double rest = positionAbs - (int) positionAbs;
        
        final int bars = totalBeats / tsMain + 1;
        final int beats = totalBeats % tsMain + 1;
        final int sub = (int) (rest * 4 * tsTicks / 16) % (int) (16.0 / tsDiv) + 1;
        final int ticks = (int) (rest * 400 * tsTicks / 16) % 100;
        
        if (this.preCountBeats != preCount && mode == Mode.BEATS) {
            this.ticks = ticks;
            subDivision = sub;
            this.beats = beats;
            this.bars = bars;
            this.preCountBeats = preCount;
            refreshPosition();
        } else {
            if (ticks != this.ticks) {
                this.ticks = ticks;
                if (mode == Mode.BEATS) {
                    displayTicksBeat(ticks);
                }
            }
            if (sub != subDivision) {
                subDivision = sub;
                if (mode == Mode.BEATS) {
                    displaySubdivisionBeat(sub);
                }
            }
            if (beats != this.beats) {
                this.beats = beats;
                if (mode == Mode.BEATS) {
                    displayBeatsBeat(beats);
                }
            }
            if (bars != this.bars) {
                this.bars = bars;
                if (mode == Mode.BEATS) {
                    displayBarsBeat(bars, preCount);
                }
            }
        }
    }
    
    public void updateTime(final double seconds) {
        final boolean preCount = seconds < 0;
        final int secondsTotal = (int) Math.abs(seconds);
        final double rest = Math.abs(seconds) - secondsTotal;
        final int secs = secondsTotal % 60;
        final int minutes = secondsTotal / 60 % 60;
        final int hours = secondsTotal / 60 / 60;
        final int frames = (int) Math.round(rest * 24);
        
        if (frames != this.frames) {
            this.frames = frames;
            if (mode == Mode.TIME) {
                displayTicks(frames);
            }
        }
        if (secs != this.seconds) {
            this.seconds = secs;
            if (mode == Mode.TIME) {
                displaySubdivision(secs);
            }
        }
        if (minutes != this.minutes) {
            this.minutes = minutes;
            if (mode == Mode.TIME) {
                displayBeats(minutes);
            }
        }
        if (hours != this.hours || preCount != preCountTime) {
            this.hours = hours;
            this.preCountTime = preCount;
            if (mode == Mode.TIME) {
                displayBars(hours, preCount);
            }
        }
    }
    
    public void setAssignment(final String ch, final boolean dotted) {
        if (ch.length() != 2) {
            return;
        }
        final char c1 = ch.charAt(0);
        final char c2 = ch.charAt(1);
        midiProcessor.sendMidi(Midi.CC, 75, toCharValue(c1));
        midiProcessor.sendMidi(Midi.CC, 74, toCharValue(c2) | (dotted ? 0x20 : 0x0));
    }
    
    private int toCharValue(final char c) {
        if (c >= 97) {
            return c - 96;
        }
        if (c >= 65) {
            return c - 64;
        }
        if (c >= 48) {
            return c;
        }
        return 0;
    }
    
    public void setAssignment(final String ch) {
        if (ch.length() != 2) {
            return;
        }
        final char c1 = ch.charAt(0);
        final char c2 = ch.charAt(1);
        midiProcessor.sendMidi(Midi.CC, 75, toCharValue(c1));
        midiProcessor.sendMidi(Midi.CC, 74, toCharValue(c2));
    }
    
    public void clearAll() {
        for (int cc = 64; cc < 76; cc++) {
            midiProcessor.sendMidi(Midi.CC, cc, 0);
        }
    }
    
    
}
