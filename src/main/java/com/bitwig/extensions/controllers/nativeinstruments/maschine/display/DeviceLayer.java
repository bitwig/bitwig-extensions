package com.bitwig.extensions.controllers.nativeinstruments.maschine.display;

import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineExtension;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.buttons.ModeButton;

public class DeviceLayer extends DisplayLayer implements NameContainer {
	private final KnobParamLayer currentParamLayer;
	private final String[] names = new String[8];
	private final String[] fullNames = new String[8];
	private final boolean[] parameterExsists = new boolean[8];

	private String deviceName = "";

	public DeviceLayer(final MaschineExtension driver, final String name) {
		super(driver, name);
		final PinnableCursorDevice device = driver.getCursorDevice();
		final CursorRemoteControlsPage remote = device.createCursorRemoteControlsPage(8);
		final RelativeHardwareKnob[] knobs = driver.getDisplayKnobs();
		final KnobParamLayer macroLayer = new KnobParamLayer(driver, "DEVICE_MACRO_LAYER", "Macro", this);
		final ModeButton leftNav = driver.getNavLeftButton();
		final ModeButton rightNav = driver.getNavRightButton();

		device.name().addValueObserver(this::deviceNameChanged);

		currentParamLayer = macroLayer;
		for (int i = 0; i < 8; i++) {
			final int index = i;
			final RemoteControl parameter = remote.getParameter(index);
			parameter.setIndication(true);
			parameter.name().markInterested();
			parameter.exists().markInterested();
			parameter.value().markInterested();

			names[i] = DisplayUtil.padString(parameter.name().get(), 6);
			fullNames[i] = DisplayUtil.padString(parameter.name().get(), 6);
			parameter.name().addValueObserver(v -> updateParmName(v, index));
			parameter.exists().addValueObserver(v -> handleExists(index, v));
			macroLayer.bindDiplayValue(index, parameter);
			macroLayer.bind(knobs[i], parameter);
		}

		final ModeButton[] buttons = driver.getDisplayButtons();
		macroLayer.bindPressed(buttons[0], device.isEnabled(), () -> device.isEnabled().toggle());
		macroLayer.bindPressed(buttons[2], device.hasPrevious(), () -> device.selectPrevious());
		macroLayer.bindPressed(buttons[3], device.hasNext(), () -> device.selectNext());
		macroLayer.bindPressed(leftNav, remote.hasPrevious(), () -> remote.selectPrevious());
		macroLayer.bindPressed(rightNav, remote.hasNext(), () -> remote.selectNext());
	}

	public void deviceNameChanged(final String deviceName) {
		this.deviceName = deviceName;
	}

	private void handleExists(final int index, final boolean exists) {
		parameterExsists[index] = exists;
	}

	@Override
	public String getValueString(final int index, final String[] value) {
		if (!parameterExsists[index]) {
			return " ---- ";
		}
		if (getDriver().getTouchHandler().isTouched(index) || isInfoModeActive()) {
			return value[index];
		}
		return names[index];
	}

	@Override
	public void updateDetail() {
		final StringBuilder b = new StringBuilder();
		final int currentParamIndex = getFocusTouchIndex();
		String rightInfo = "";
		if (isInfoModeActive()) {
			b.append("<BYPAS>| ---- |<DEV  |  DEV>");
			rightInfo = " ---- | ---- | --- | ----";
		} else if (getDriver().getTouchHandler().isTouched() //
				&& currentParamIndex != -1 //
				&& parameterExsists[currentParamIndex]) {
			b.append(fullNames[currentParamIndex] + " = " + currentParamLayer.getValue(currentParamIndex));
		} else {
			b.append("P: " + deviceName);
		}
		sendToDisplay(0, b.toString());

		sendToDisplay(1, rightInfo);
	}

	@Override
	protected void doNotifyMainTouched(final boolean touched) {
		updateDetail();
		currentParamLayer.refreshValue(0);
		currentParamLayer.refreshValue(1);
	}

	@Override
	protected void notifyEncoderTouched(final int index, final boolean v) {
		updateDetail();
		currentParamLayer.refreshValue(index / 4);
	}

	private void refreshToLineNames(final int section) {
		if (!isActive()) {
			return;
		}
		updateDetail();
	}

	private void updateParmName(final String name, final int index) {
		names[index] = DisplayUtil.padString(name, 6);
		fullNames[index] = DisplayUtil.padString(name, 12);
		refreshToLineNames(index / 4);
	}

	@Override
	protected void doDeactivate() {
		super.doDeactivate();
		clearDisplay();
		currentParamLayer.deactivate();
	}

	@Override
	protected void doNotifyMacroDown(final boolean active) {
		setKnobSensitivity(isMacroDown ? 1.0 : 4.0);
	}

	@Override
	protected void doActivate() {
		super.doActivate();

		final MaschineExtension driver = getDriver();
		setKnobSensitivity(isMacroDown ? 1.0 : 4.0);
		driver.getApplication().toggleNoteEditor();
		driver.getApplication().toggleDevices();
		refreshToLineNames(0);
		refreshToLineNames(1);
		currentParamLayer.activate();
		currentParamLayer.refreshValue(0);
		currentParamLayer.refreshValue(1);
	}

}
