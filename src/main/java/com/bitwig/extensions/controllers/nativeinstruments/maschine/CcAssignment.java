package com.bitwig.extensions.controllers.nativeinstruments.maschine;

import com.bitwig.extension.controller.api.HardwareActionMatcher;
import com.bitwig.extension.controller.api.MidiIn;

public enum CcAssignment {
	PLAY(57), //
	STOP(59), //
	RECORD(58), //
	RESTART(53), //
	MACRO(43), //
	ERASE(54), //
	TAPMETRO(60), //
	FOLLOWGRID(56), //
	DKNOB_TURN(7), //
	DKNOB_PUSH(8), //
	DKNOB_TOUCH(9), //
	DKNOB_LEFT(33), //
	DKNOB_RIGHT(31), //
	DKNOB_UP(30), //
	DKNOB_DOWN(32), //
	SCENE(85), //
	PATTERN(86), //
	EVENTS(87), //
	VARIATION(88), //
	DUPLICATE(89), //
	SELECT(90), //
	SOLO(91), //
	MUTE(92), //
	PADMODE(80), //
	KEYBOARD(82), //
	STEP(83), //
	NOTE_REPEAT(46), //
	VOLUME(44), //
	SWING(45), //
	TEMPO(47), //
	AUTO(42), //
	NAV_LEFT(110), //
	NAV_RIGHT(111), //
	MIXER(37), //
	ARRANGER(36), //
	BROWSER(38), //
	SAVE(40), //
	SETTINGS(41), //
	CHANNEL(34), //
	PLUGIN(35), //
	SAMPLING(39), //
	FIXEDVEL(81), //
	CHORD(84), //
	MODE_BUTTON_1(22), MODE_BUTTON_2(23), //
	MODE_BUTTON_3(24), MODE_BUTTON_4(25), //
	MODE_BUTTON_5(26), MODE_BUTTON_6(27), //
	MODE_BUTTON_7(28), MODE_BUTTON_8(29);
	//

	private int ccNr;
	private int channel;

	private CcAssignment(final int ccNr, final int channel) {
		this.ccNr = ccNr;
		this.channel = channel;
	}

	private CcAssignment(final int ccNr) {
		this.ccNr = ccNr;
		this.channel = 0;
	}

	public int getCcNr() {
		return ccNr;
	}

	public void setCcNr(final int ccNr) {
		this.ccNr = ccNr;
	}

	public int getChannel() {
		return channel;
	}

	public void setChannel(final int channel) {
		this.channel = channel;
	}

	public HardwareActionMatcher createActionMatcher(final MidiIn midiIn, final int matchvalue) {
		return midiIn.createCCActionMatcher(channel, ccNr, matchvalue);
	}

	public int getType() {
		return Midi.CC | channel;
	}

}
