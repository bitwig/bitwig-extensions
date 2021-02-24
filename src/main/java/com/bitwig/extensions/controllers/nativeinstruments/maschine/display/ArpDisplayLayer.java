package com.bitwig.extensions.controllers.nativeinstruments.maschine.display;

import com.bitwig.extension.controller.api.Arpeggiator;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.EnumDefinition;
import com.bitwig.extension.controller.api.EnumValueDefinition;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.SettableDoubleValue;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineExtension;

public class ArpDisplayLayer extends DisplayLayer {

	private final double[] rateTable = { 0.0625, 0.125, 0.25, 0.5, 1.0, 2.0, 4.0 };

	private final String[] rateDisplayValues = { "1/64", "1/32", "1/16", "1/8", "1/4", "1/2", "1/1" };

	private final String[] values = new String[8];
	private final int[] lengths = { 6, 12, 6, 6 };

	public ArpDisplayLayer(final MaschineExtension driver, final String name) {
		super(driver, name);
		final RelativeHardwareKnob[] knobs = driver.getDisplayKnobs();
		// final ModeButton[] buttons = driver.getDisplayButtons();

		final ControllerHost host = driver.getHost();

		for (int i = 0; i < values.length; i++) {
			values[i] = "";
		}

		final Arpeggiator arp = driver.getNoteInput().arpeggiator();
		arp.usePressureToVelocity().markInterested();
		arp.usePressureToVelocity().set(true);
		arp.octaves().markInterested();
		bind(knobs[0], arp.octaves());
		arp.octaves().addValueObserver(v -> {
			updateDisplayValues(Integer.toString(v), 0);
		});

		arp.octaves().set(0);
		arp.mode().markInterested();
		bind(knobs[1],
				host.createRelativeHardwareControlStepTarget(host.createAction(() -> incMode(arp.mode(), 1), () -> "+"),
						host.createAction(() -> incMode(arp.mode(), -1), () -> "-")));
		arp.mode().addValueObserver(v -> {
			updateDisplayValues(v, 1);
		});

		arp.rate().markInterested();
		arp.rate().addValueObserver(v -> {
			updateDisplayValues(rateToString(v), 2);
		});
		bind(knobs[3],
				host.createRelativeHardwareControlStepTarget(host.createAction(() -> incRate(arp.rate(), 1), () -> "+"),
						host.createAction(() -> incRate(arp.rate(), -1), () -> "-")));
		// TODO more value to come
		arp.gateLength().markInterested();
		arp.gateLength().addValueObserver(v -> {
			updateDisplayValues(toPercent(v), 3);
		});
		arp.exists().addValueObserver(exists -> { // Set Initial Value to 0 please
			if (exists) {
				arp.octaves().set(0);
			}
		});
		bind(knobs[4],
				host.createRelativeHardwareControlStepTarget(
						host.createAction(() -> incGateLength(arp.gateLength(), 1), () -> "+"),
						host.createAction(() -> incGateLength(arp.gateLength(), -1), () -> "-")));
//		final ModeButton[] buttons = driver.getDisplayButtons();
//		bindPressed(buttons[0], () -> setGate());
//		bindPressed(buttons[1], () -> updateScale(1));
//		bindPressed(buttons[2], () -> updateOctave(-1));
//		bindPressed(buttons[3], () -> updateOctave(1));
//		bindPressed(buttons[4], () -> updateSemi(-1));
//		bindPressed(buttons[5], () -> updateSemi(1));
	}

	private void incGateLength(final SettableDoubleValue value, final int incValue) {
		double cv = value.get();
		if (cv < 1.0) {
			cv += 0.01 * incValue;
		} else if (cv >= 1.0) {
			cv += 0.5 * incValue;
		}
		if (cv >= 0.125 && cv <= 8.0) {
			value.set(cv);
		}
	}

	@Override
	public void notifyEncoderTouched(final int index, final boolean v) {
	}

	private static String toPercent(final double v) {
		final int percent = (int) (v * 100);
		return Integer.toString(percent) + "%";
	}

	private void incRate(final SettableDoubleValue value, final int incValue) {
		int currentRateIndex = currentRateIndex(value.get());
		currentRateIndex += incValue;
		if (currentRateIndex >= 0 && currentRateIndex < rateTable.length) {
			value.set(rateTable[currentRateIndex]);
		}
	}

	private void incMode(final SettableEnumValue mode, final int inc) {
		final EnumDefinition modes = mode.enumDefinition();
		final String current = mode.get();
		int indexMode = -1;
		for (int i = 0; i < modes.getValueCount(); i++) {
			final EnumValueDefinition valDef = modes.valueDefinitionAt(i);
			if (valDef.getId().equals(current)) {
				indexMode = i;
				break;
			}
		}
		if (indexMode != -1) {
			final int newIndex = indexMode + inc;
			if (newIndex > 0 && newIndex < modes.getValueCount()) {
				mode.set(modes.valueDefinitionAt(newIndex).getId());
			}
		}
	}

	@Override
	protected void doNotifyMainTouched(final boolean touched) {
		// TODO Auto-generated method stub
	}

	public String rateToString(final double v) {
		return rateDisplayValues[currentRateIndex(v)];
	}

	private int currentRateIndex(final double v) {
		for (int i = 0; i < rateTable.length; i++) {
			if (v == rateTable[i]) {
				return i;
			}
		}
		return 2;
	}

	public void updateDisplayValues(final String value, final int index) {
		values[index] = DisplayUtil.padString(value, lengths[index]);
		refreshValue(index < 3 ? 0 : 1);
	}

	public void refreshValue(final int section) {
		if (!isActive()) {
			return;
		}
		if (section == 0) {
			final StringBuilder b = new StringBuilder();
			for (int i = 0; i < 3; i++) {
				b.append(values[i]);
				if (i < 2) {
					b.append('|');
				}
			}
			sendToDisplay(2, b.toString());
		} else if (section == 1) {
			final StringBuilder b = new StringBuilder();
			for (int i = 3; i < lengths.length; i++) {
				b.append(values[i]);
				if (i < lengths.length - 1) {
					b.append('|');
				}
			}
			sendToDisplay(3, b.toString());
		}
	}

	@Override
	protected void doActivate() {
		super.doActivate();
		final RelativeHardwareKnob[] knobs = getDriver().getDisplayKnobs();
		for (int i = 0; i < knobs.length; i++) {
			knobs[i].setStepSize(1 / 128.0);
			knobs[i].setSensitivity(1.0);
		}
		updateScaleValue();
		sendToDisplay(0, "<Oct> |<MODE>      |<RATE>");
		sendToDisplay(1, "<Gate>");
		refreshValue(0);
		refreshValue(1);
	}

	@Override
	protected void doDeactivate() {
		super.doDeactivate();
		clearDisplay();
	}

	private void updateScaleValue() {
	}

	@Override
	protected void doNotifyMacroDown(final boolean active) {
		// Do Nothing
	}

}
