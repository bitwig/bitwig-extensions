package com.bitwig.extensions.controllers.mackie.devices;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DeviceMatcher;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.SpecificBitwigDevice;
import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.controllers.mackie.layer.DisplayLayer;
import com.bitwig.extensions.controllers.mackie.layer.InfoSource;
import com.bitwig.extensions.controllers.mackie.value.ModifierValueObject;

/**
 * Fully Customized control of the Bitwig EQ+ Device Missing Parameters:
 *
 * OUTPUT_GAIN GLOBAL_SHIFT BAND SOLO
 *
 * ADAPTIVE_Q DECIBEL_RANGE
 *
 */
public class EqDevice implements ControlDevice, DeviceManager {
	private final static String[] PNAMES = { "TYPE", "FREQ", "GAIN", "Q" };
	private final static RingDisplayType[] RING_TYPES = { RingDisplayType.FILL_LR, RingDisplayType.SINGLE,
			RingDisplayType.FILL_LR, RingDisplayType.FILL_LR };
	private final static double[] SENSITIVITIES = { 2, 0.25, 0.25, 0.25 };
	private static final String[] TYPES = { "*Off*", "LowC-1", "LowC-2", "LowC-4", "LowC-6", "LowC-8", "LoShlv", "Bell", //
			"HiC-1", "HiC-2", "HiC-4", "HiC-6", "HiC-8", "HiShlv", "Notch" };
	private static final String[] PAGE_NAMES = { "Bands 1 & 2", "Bands 3 & 4", "Bands 5 & 6", "Bands 7 & 8",
			"General" };

	private static final ParameterSetting[] PAGE_5 = { //
			new ParameterSetting("OUTPUT_GAIN", 0.125, RingDisplayType.FILL_LR), //
			new ParameterSetting("GLOBAL_SHIFT", 0.125, RingDisplayType.FILL_LR), //
			new ParameterSetting("BAND", 2, RingDisplayType.FILL_LR), //
			new ParameterSetting("SOLO", 2, RingDisplayType.FILL_LR), //
			new ParameterSetting("ADAPTIVE_Q", 2, RingDisplayType.FILL_LR), //
			new ParameterSetting("DECIBEL_RANGE", 1, RingDisplayType.FILL_LR), //
			new ParameterSetting("----", 1, RingDisplayType.FILL_LR), //
			new ParameterSetting("----", 1, RingDisplayType.SINGLE), };

	private final SpecificBitwigDevice bitwigDevice;
	private final Device device;
	private final List<ParameterPage> parameterSlots = new ArrayList<>();
	private int pageIndex = 0;
	private final int[] enableValues = new int[8];
	private final List<Parameter> enableParams = new ArrayList<>();
	private final BooleanValue cursorOnDevice;
	private DeviceBank overallBank;
	private int bankIndex = 0;
	private DisplayLayer infoLayer;
	private InfoSource infoSource;

	public EqDevice(final MackieMcuProExtension driver, final DeviceMatcher matcher) {
		bitwigDevice = driver.getCursorDevice().createSpecificBitwigDevice(Devices.EQ_PLUS.getUuid());

		final DeviceBank eqPlusDeviceBank = driver.getCursorTrack().createDeviceBank(1);
		eqPlusDeviceBank.setDeviceMatcher(matcher);
		device = eqPlusDeviceBank.getItemAt(0);

		cursorOnDevice = device.createEqualsValue(driver.getCursorDevice());

		for (int i = 0; i < 8; i++) {
			parameterSlots.add(new ParameterPage(i, this));
		}

		for (int i = 0; i < 8; i++) {
			final Parameter enableParam = createEnableParam(i);
			enableParams.add(enableParam);
			final int page = i / 2;
			final int index = i % 2 * 4;
			final Parameter typeParam = parameterSlots.get(index).getParameter(page);
			typeParam.value().addValueObserver(v -> {
				notifyEnablementFromType(page, index, enableParam, v);
			});
			enableParam.value().addValueObserver(2, v -> {
				notifyEnablementFromEnable(page, index, typeParam, v);
			});
		}

		initBankTracking(driver);
// final Just code to final list all parameters final of device
//		device.addDirectParameterIdObserver(allp -> {
//			for (final String pname : allp) {
//				RemoteConsole.out.println("[{}]", pname);
//			}
//		});
	}

	@Override
	public void setInfoLayer(final DisplayLayer infoLayer) {
		this.infoLayer = infoLayer;
	}

	@Override
	public void enableInfo(final InfoSource type) {
		infoSource = type;
		if (infoSource == InfoSource.NAV_VERTICAL) {
			infoLayer.setMainText(getDeviceInfo(), "", false);
		} else if (infoSource == InfoSource.NAV_HORIZONTAL) {
			infoLayer.setMainText(getPageInfo(), "", false);
		}
	}

	@Override
	public void disableInfo() {
		infoSource = null;
	}

	@Override
	public InfoSource getInfoSource() {
		return infoSource;
	}

	public String getDeviceInfo() {
		return "EQ+ Device";
	}

	public String getPageInfo() {
		return "Parameter Page : " + PAGE_NAMES[pageIndex];
	}

	private void initBankTracking(final MackieMcuProExtension driver) {
		overallBank = driver.getCursorTrack().createDeviceBank(8);
		overallBank.canScrollBackwards().markInterested();
		overallBank.canScrollForwards().markInterested();
		for (int i = 0; i < overallBank.getSizeOfBank(); i++) {
			final int which = i;
			final BooleanValue evo = overallBank.getItemAt(which).createEqualsValue(device);
			evo.addValueObserver(v -> {
				if (v) {
					if (which == 0 && overallBank.canScrollBackwards().get()) {
						overallBank.scrollBackwards();
					} else if (which == overallBank.getSizeOfBank() - 1 && overallBank.canScrollForwards().get()) {
						overallBank.scrollForwards();
					}
					bankIndex = which;
				}
			});
		}
	}

	private void notifyEnablementFromEnable(final int page, final int bandIndexOffset, final Parameter typeParam,
			final int value) {
		final int enabledValue = value * (typeParam.value().get() > 0 ? 1 : 0);
		enableValues[bandIndexOffset / 4 + page * 2] = enabledValue;
		if (page == pageIndex) {
			for (int i = 0; i < 4; i++) {
				final ParameterPage paramSlot = parameterSlots.get(i + bandIndexOffset);
				paramSlot.notifyEnablement(enabledValue);
			}
		}
	}

	public void handleResetInvoked(final int index, final ModifierValueObject modifier) {
		if (modifier.isShiftSet()) {
			toggleEnable(index);
		} else if (modifier.isControlSet()) {
			resetAll();
		} else if (modifier.isOptionSet()) {
			device.isEnabled().toggle();
		} else {
			parameterSlots.get(index).doReset();
		}
	}

	@Override
	public BooleanValue getCursorOnDevice() {
		return cursorOnDevice;
	}

	private void resetAll() {
		for (final ParameterPage slot : parameterSlots) {
			slot.resetAll();
		}
		for (final Parameter param : enableParams) {
			param.reset();
		}
	}

	private void notifyEnablementFromType(final int page, final int bandIndexOffset, final Parameter enableParam,
			final double value) {
		final int enabledValue = (value > 0 ? 1 : 0) * (enableParam.value().get() > 0 ? 1 : 0);
		enableValues[bandIndexOffset / 4 + page * 2] = enabledValue;
		if (page == pageIndex && pageIndex < 4) {
			for (int i = 0; i < 4; i++) {
				final ParameterPage paramSlot = parameterSlots.get(i + bandIndexOffset);
				paramSlot.notifyEnablement(enabledValue);
			}
		}
	}

	private Parameter createEnableParam(final int index) {
		final String enablePname = "ENABLE" + (index + 1);
		final Parameter enableParam = bitwigDevice.createParameter(enablePname);
		return enableParam;
	}

	public void navigateParameterBanks(final int direction) {
		if (direction < 0) {
			navigatePrevious();
		} else {
			navigateNext();
		}
	}

	@Override
	public void navigateNext() {
		pageIndex = (pageIndex + 1) % getPages();
		updateSlots();
	}

	@Override
	public void navigatePrevious() {
		final int nextIndex = pageIndex - 1;
		pageIndex = nextIndex < 0 ? getPages() - 1 : nextIndex < getPages() ? nextIndex : 0;
		updateSlots();
	}

	@Override
	public int getPages() {
		return 5;
	}

	private void updateSlots() {
		parameterSlots.forEach(p -> p.updatePage(pageIndex));
		if (pageIndex < 4) {
			for (int i = 0; i < 4; i++) {
				final ParameterPage paramSlot1 = parameterSlots.get(i);
				paramSlot1.notifyEnablement(enableValues[pageIndex * 2]);
				final ParameterPage paramSlot2 = parameterSlots.get(i + 4);
				paramSlot2.notifyEnablement(enableValues[pageIndex * 2 + 1]);
			}
		} else {
			parameterSlots.forEach(ps -> ps.notifyEnablement(1));
		}
		parameterSlots.forEach(ParameterPage::triggerUpdate);
	}

	public String getParamName(final int page, final int index) {
		return PNAMES[index % 4] + Integer.toString(1 + index / 4 + page * 2);
	}

	@Override
	public DeviceParameter createDeviceParameter(final int page, final int index) {
		if (page < 4) {
			return createBandParameter(page, index);
		} else {
			final ParameterSetting pageSetting = PAGE_5[index];
			final Parameter param = bitwigDevice.createParameter(pageSetting.getPname());
			final DeviceParameter parameter = new DeviceParameter(pageSetting.getPname(), param,
					pageSetting.getRingType(), pageSetting.getSensitivity());

			return parameter;
		}
	}

	private DeviceParameter createBandParameter(final int page, final int index) {
		final String pname = getParamName(page, index);
		final Parameter param = bitwigDevice.createParameter(pname);
		final DeviceParameter parameter = new DeviceParameter(pname, param, RING_TYPES[index % 4],
				SENSITIVITIES[index % 4]);
		if (index == 0 || index == 4) {
			parameter.setCustomResetAction(p -> {
				p.value().setImmediately(0.0);
			});
			parameter.setCustomValueConverter(new CustomValueConverter() {

				@Override
				public String convert(final int value) {
					return TYPES[Math.min(14, value)];
				}

				@Override
				public int getIntRange() {
					return 15;
				}

			});
		}
		return parameter;
	}

	@Override
	public Device getDevice() {
		return device;
	}

	public List<ParameterPage> getEqBands() {
		return parameterSlots;
	}

//	public boolean exists() {
//		return device.exists().get();
//	}

	public void triggerUpdate() {
		parameterSlots.forEach(ParameterPage::triggerUpdate);
	}

	@Override
	public int getCurrentPage() {
		return pageIndex;
	}

	@Override
	public BooleanValue exists() {
		return device.exists();
	}

	public void toggleEnable(final Integer pindex) {
		if (pageIndex < 4) {
			final int bandIndex = pageIndex * 2 + pindex / 4;
			final Parameter enable = enableParams.get(bandIndex);
			final double v = enable.get();
			if (v == 0) {
				enable.value().setImmediately(1.0);
			} else {
				enable.value().setImmediately(0);
			}
		}
	}

	@Override
	public CursorRemoteControlsPage getRemote() {
		return null;
	}

	@Override
	public boolean isCanTrackMultiple() {
		return true;
	}

	@Override
	public void moveDeviceToLeft() {
		if (overallBank == null) {
			return;
		}
		if (bankIndex > 0) {
			final Device nextDevice = overallBank.getItemAt(bankIndex - 1);
			nextDevice.beforeDeviceInsertionPoint().moveDevices(device);
		}
	}

	@Override
	public void moveDeviceToRight() {
		if (overallBank == null) {
			return;
		}
		if (bankIndex < overallBank.getSizeOfBank() - 1) {
			final Device nextDevice = overallBank.getItemAt(bankIndex + 1);
			nextDevice.afterDeviceInsertionPoint().moveDevices(device);
		}
	}

	@Override
	public void removeDevice() {
		device.deleteObject();
	}
}
