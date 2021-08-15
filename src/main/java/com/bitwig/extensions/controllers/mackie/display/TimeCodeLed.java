package com.bitwig.extensions.controllers.mackie.display;

import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.controllers.mackie.Midi;
import com.bitwig.extensions.controllers.mackie.NoteOnAssignment;

public class TimeCodeLed {
	private final MidiOut midiOut;
	private double position;
	private boolean precountBeats = false;
	private int bars = -1;
	private int beats = -1;
	private int subDevision = -1;
	private int ticks = -1;

	private final boolean precountTime = false;
	private int frames = -1;
	private int seconds = -1;
	private int minutes = -1;
	private int hours = -1;

	private int tsMain;
	private int tsDiv;
	private int tsTicks = 16;
	private Mode mode = Mode.BEATS;

	public enum Mode {
		BEATS, TIME;
	}

	public TimeCodeLed(final MidiOut midiOut) {
		this.midiOut = midiOut;
	}

	public void toggleMode() {
		if (this.mode == Mode.BEATS) {
			setMode(Mode.TIME);
		} else {
			setMode(Mode.BEATS);
		}
		refreschMode();
	}

	public void refreschMode() {
		if (mode == Mode.BEATS) {
			NoteOnAssignment.BEATS_MODE.send(midiOut, 127);
			NoteOnAssignment.SMPTE_MODE.send(midiOut, 0);
		} else {
			NoteOnAssignment.BEATS_MODE.send(midiOut, 0);
			NoteOnAssignment.SMPTE_MODE.send(midiOut, 127);
		}
	}

	public Mode getMode() {
		return mode;
	}

	public void setMode(final Mode mode) {
		if (this.mode != mode) {
			this.mode = mode;
			if (mode == Mode.BEATS) {
				refreshPositon();
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
				final String[] denom = v[1].split(",");
				if (denom.length == 2) {
					tsDiv = Integer.parseInt(denom[0]);
					tsTicks = Integer.parseInt(denom[1]);
				}
			} else {
				tsDiv = Integer.parseInt(v[1]);
				tsTicks = 16;
			}
			updatePosition(position, "");
		}
	}

	private void refreshPositon() {
		displayTicks(ticks);
		displaySubdivision(subDevision);
		displayBeats(beats);
		displayBars(bars, precountBeats);
	}

	private void refreshTime() {
		displayTicks(frames);
		displaySubdivision(seconds);
		displayBeats(minutes);
		displayBars(hours, precountTime);
	}

	public void updatePosition(final double pos, final String formatted) {
		this.position = pos;
		final boolean precount = pos < 0;
		final double posabs = Math.abs(pos);
		final int totalBeats = (int) (posabs * tsDiv / 4);
		final double rest = posabs - (int) posabs;

		final int bars = totalBeats / tsMain + 1;
		final int beats = totalBeats % tsMain + 1;
		final int sub = (int) (rest * 4 * tsTicks / 16) % (int) (16.0 / tsDiv) + 1;
		final int ticks = (int) (rest * 400 * tsTicks / 16) % 100;

		if (ticks != this.ticks) {
			this.ticks = ticks;
			if (mode == Mode.BEATS) {
				displayTicks(ticks);
			}
		}
		if (sub != this.subDevision) {
			this.subDevision = sub;
			if (mode == Mode.BEATS) {
				displaySubdivision(sub);
			}
		}
		if (beats != this.beats) {
			this.beats = beats;
			if (mode == Mode.BEATS) {
				displayBeats(beats);
			}
		}
		if (bars != this.bars || precount != this.precountBeats) {
			this.bars = bars;
			this.precountBeats = precount;
			if (mode == Mode.BEATS) {
				displayBars(bars, precount);
			}
		}
	}

	public void updateTime(final double seconds) {
		final boolean precount = seconds < 0;
		final int secondstotal = (int) Math.abs(seconds);
		final double rest = Math.abs(seconds) - secondstotal;
		final int secs = secondstotal % 60;
		final int minutes = secondstotal / 60 % 60;
		final int hours = secondstotal / 60 / 60;
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
		if (hours != this.hours || precount != precountTime) {
			this.hours = hours;
			if (mode == Mode.TIME) {
				displayBars(hours, precount);
			}
		}
	}

	private void displayTicks(final int value) {
		final int v1 = value % 10;
		final int v2 = value / 10 % 10;
		final int v3 = value / 100 % 10;
		midiOut.sendMidi(Midi.CC, 64, v1 + 48);
		midiOut.sendMidi(Midi.CC, 65, v2 + 48);
		midiOut.sendMidi(Midi.CC, 66, v3 + 48);
	}

	private void displaySubdivision(final int value) {
		final int v1 = value % 10;
		final int v2 = value / 10 % 10;
		midiOut.sendMidi(Midi.CC, 67, v1 + 48 + 64);
		midiOut.sendMidi(Midi.CC, 68, v2 + 48);
	}

	private void displayBeats(final int value) {
		final int v1 = value % 10;
		final int v2 = value / 10 % 10;
		midiOut.sendMidi(Midi.CC, 69, v1 + 48 + 64);
		midiOut.sendMidi(Midi.CC, 70, v2 + 48);
	}

	private void displayBars(final int value, final boolean precount) {
		final int v1 = value % 10;
		final int v2 = value / 10 % 10;
		final int v3 = value / 100 % 10;
		midiOut.sendMidi(Midi.CC, 71, v1 + 48 + 64);
		midiOut.sendMidi(Midi.CC, 72, v2 + 48);
		if (precount) {
			midiOut.sendMidi(Midi.CC, 73, 45);
		} else {
			midiOut.sendMidi(Midi.CC, 73, v3 + 48);
		}
	}

	public void setAssignment(final String ch, final boolean dotted) {
		if (ch.length() != 2) {
			return;
		}
		final char c1 = ch.charAt(0);
		final char c2 = ch.charAt(1);
		midiOut.sendMidi(Midi.CC, 75, toCharValue(c1));
		midiOut.sendMidi(Midi.CC, 74, toCharValue(c2) | (dotted ? 0x20 : 0x0));
	}

	public void setAssignment(final String ch) {
		if (ch.length() != 2) {
			return;
		}
		final char c1 = ch.charAt(0);
		final char c2 = ch.charAt(1);
		midiOut.sendMidi(Midi.CC, 75, toCharValue(c1));
		midiOut.sendMidi(Midi.CC, 74, toCharValue(c2));
	}

	public void clearAll() {
		for (int cc = 64; cc < 76; cc++) {
			midiOut.sendMidi(Midi.CC, cc, 0);
		}
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

}
