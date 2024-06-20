package com.bitwig.extensions.controllers.mcu.devices;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.mcu.CursorDeviceControl;
import com.bitwig.extensions.controllers.mcu.GlobalStates;
import com.bitwig.extensions.controllers.mcu.control.RingDisplayType;

public class EqDevice extends SpecificDevice {
    
    private static final String[] TYPES = {
        "*Off*", "LowC1", "LowC2", "LowC4", "LowC6", "LowC8", "LoShlv", "Bell", //
        "HiC-1", "HiC-2", "HiC-4", "HiC-6", "HiC-8", "HiShlv", "Notch"
    };
    private static final String[] PAGE_NAMES = {"Band 1&2", "Band 3&4", "Bands 5&6", "Bands 7&8", "General"};
    private static final int[] DEFAULT_BAND_TYPES = {
        3, 6, 7, 7, 7, 7, 9, 12
    };
    private static final ParameterSetting[] PAGE_5 = { //
        new ParameterSetting("OUTPUT_GAIN", 0.125, RingDisplayType.FILL_LR), //
        new ParameterSetting("GLOBAL_SHIFT", 0.125, RingDisplayType.FILL_LR), //
        new ParameterSetting("BAND", 0.25, RingDisplayType.FILL_LR), //
        new ParameterSetting("SOLO", 0.25, RingDisplayType.FILL_LR), //
        new ParameterSetting("ADAPTIVE_Q", 0.25, RingDisplayType.FILL_LR), //
        new ParameterSetting("DECIBEL_RANGE", 0.25, RingDisplayType.FILL_LR), //
        new ParameterSetting("----", 1, RingDisplayType.FILL_LR), //
        new ParameterSetting("----", 1, RingDisplayType.SINGLE),
    };
    private final List<ParamSetup> PARAMS = List.of(new ParamSetup("TYPE%d", RingDisplayType.FILL_LR, 0.25),
        new ParamSetup("FREQ%d", RingDisplayType.SINGLE, 1), new ParamSetup("GAIN%d", RingDisplayType.FILL_LR, 1),
        new ParamSetup("Q%d", RingDisplayType.FILL_LR, 1));
    private final boolean[] enableValues = new boolean[8];
    private final boolean[] typeOn = new boolean[8];
    private final List<Parameter> enableParams = new ArrayList<>();
    private final GlobalStates states;
    
    private record ParamSetup(String nameFormat, RingDisplayType ringDisplayType, double sensitivites) {
    
    }
    
    public EqDevice(final CursorDeviceControl cursorDeviceControl, final DeviceTypeFollower deviceFollower,
        final GlobalStates states) {
        super(SpecialDevices.EQ_PLUS, cursorDeviceControl, deviceFollower);
        this.states = states;
        for (int slotIndex = 0; slotIndex < 8; slotIndex++) {
            final ParamPageSlot parameterSlot = pageSlots.get(slotIndex);
            for (int bandPage = 0; bandPage < 4; bandPage++) {
                final DeviceParameter parameter = createDeviceParameter(bandPage, slotIndex);
                parameterSlot.addParameter(parameter);
            }
            parameterSlot.addParameter(createDeviceParameter(4, slotIndex));
        }
        
        for (int i = 0; i < 8; i++) {
            final int bandIndex = i;
            final Parameter enableParam = bitwigDevice.createParameter("ENABLE%d".formatted(i + 1));
            enableParams.add(enableParam);
            enableParam.value().addValueObserver(2, enabled -> updateEnablement(bandIndex, enabled));
        }
        applyPageValues(0);
    }
    
    private DeviceParameter createDeviceParameter(final int page, final int index) {
        if (page < 4) {
            return createBandParameter(page, index);
        } else {
            final ParameterSetting pageSetting = PAGE_5[index];
            final Parameter param = bitwigDevice.createParameter(pageSetting.getParameterName());
            final DeviceParameter parameter =
                new DeviceParameter(pageSetting.getParameterName(), param, pageSetting.getRingType(),
                    pageSetting.getSensitivity());
            
            return parameter;
        }
    }
    
    private void updateEnablement(final int bandIndex, final int enableValue) {
        enableValues[bandIndex] = enableValue == 1;
        final int pageIndex = bandIndex / 2;
        if (this.pageIndex == pageIndex) {
            notifyEnablement(bandIndex);
        }
    }
    
    @Override
    public void applyPageValues(final int page) {
        if (page < 4) {
            for (int i = 0; i < 8; i++) {
                final ParamPageSlot slot = pageSlots.get(i);
                final int bandIndex = page * 2 + i / 4;
                slot.notifyEnablement(enableValues[bandIndex] & typeOn[bandIndex]);
            }
        }
    }
    
    private DeviceParameter createBandParameter(final int page, final int index) {
        final String paramName = getParamName(page, index);
        final Parameter param = bitwigDevice.createParameter(paramName);
        final ParamSetup paramSetup = PARAMS.get(index % 4);
        final DeviceParameter parameter =
            new DeviceParameter(paramName, param, paramSetup.ringDisplayType(), paramSetup.sensitivites());
        if (index == 0 || index == 4) {
            final int bandIndex = page * 2 + index / 4;
            parameter.setCustomResetAction(p -> handleReset(bandIndex, p));
            parameter.setCustomValueConverter(value -> TYPES[(int) (value * 14)]);
            parameter.getParameter().value().addValueObserver(14, v -> {
                typeOn[bandIndex] = v > 0;
                if (this.pageIndex == pageIndex) {
                    notifyEnablement(bandIndex);
                }
            });
        }
        return parameter;
    }
    
    private void notifyEnablement(final int bandIndex) {
        final int offset = (bandIndex % 2) * 4;
        for (int i = 0; i < 4; i++) {
            final ParamPageSlot slot = pageSlots.get(i + offset);
            slot.notifyEnablement(enableValues[bandIndex] & typeOn[bandIndex]);
        }
    }
    
    private String getParamName(final int page, final int index) {
        return PARAMS.get(index % 4).nameFormat().formatted(1 + index / 4 + page * 2);
    }
    
    private void handleReset(final int bandIndex, final Parameter p) {
        if (states.isShiftSet()) {
            if (enableValues[bandIndex]) {
                enableParams.get(bandIndex).value().set(0);
            } else {
                enableParams.get(bandIndex).value().set(1);
            }
        } else {
            p.value().set(DEFAULT_BAND_TYPES[bandIndex], 15);
        }
    }
    
    @Override
    public Parameter getParameter(final int index) {
        return null;
    }
    
    @Override
    public int getPageCount() {
        return 5;
    }
    
    @Override
    public String getDeviceInfo() {
        return "EQ+ Device";
    }
    
    @Override
    public String getPageInfo() {
        return PAGE_NAMES[pageIndex];
    }
    
}