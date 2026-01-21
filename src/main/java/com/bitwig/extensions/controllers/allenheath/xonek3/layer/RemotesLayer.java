package com.bitwig.extensions.controllers.allenheath.xonek3.layer;

import java.util.List;

import com.bitwig.extension.controller.api.AbsoluteHardwareControl;
import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extensions.controllers.allenheath.xonek3.DeviceHwElements;
import com.bitwig.extensions.controllers.allenheath.xonek3.TrackSpecControl;
import com.bitwig.extensions.controllers.allenheath.xonek3.ViewControl;
import com.bitwig.extensions.controllers.allenheath.xonek3.XoneHwElements;
import com.bitwig.extensions.controllers.allenheath.xonek3.XoneK3ControllerExtension;
import com.bitwig.extensions.controllers.allenheath.xonek3.XoneK3GlobalStates;
import com.bitwig.extensions.controllers.allenheath.xonek3.color.XoneRgbColor;
import com.bitwig.extensions.controllers.allenheath.xonek3.control.XoneRgbButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class RemotesLayer extends Layer {
    
    private final Layer shiftLayer;
    public static final int[] BRIGHT = {1, 0xF, 0x30};
    public static final XoneRgbColor.BrightnessScale[] PARAM_COLORS = {
        XoneRgbColor.RED.scaleOf(BRIGHT), XoneRgbColor.ORANGE.scaleOf(BRIGHT), XoneRgbColor.YELLOW.scaleOf(BRIGHT),
        XoneRgbColor.GREEN.scaleOf(BRIGHT), XoneRgbColor.CYAN.scaleOf(BRIGHT), XoneRgbColor.BLUE.scaleOf(BRIGHT),
        XoneRgbColor.PURPLE.scaleOf(BRIGHT), XoneRgbColor.MAGENTA.scaleOf(BRIGHT)
    };
    private static final XoneRgbColor.BrightnessScale WHITE = XoneRgbColor.WHITE.scaleOf(BRIGHT);
    private static final XoneRgbColor.BrightnessScale AQUA = XoneRgbColor.AQUA.scaleOf(BRIGHT);
    private static final XoneRgbColor.BrightnessScale DEVICE = XoneRgbColor.YELLOW.scaleOf(BRIGHT);
    
    private final XoneK3GlobalStates globalStates;
    
    private static class DeviceState {
        private boolean exists;
        private XoneRgbColor stdColor;
        private XoneRgbColor selectColor;
        private XoneRgbColor disabledColor;
        private boolean enabled;
        private boolean selected;
        private final Device device;
        
        public DeviceState(final Device device, final CursorDevice cursorDevice) {
            this.device = device;
            device.exists().addValueObserver(exists -> this.exists = exists);
            device.deviceType().addValueObserver(this::handleDeviceType);
            device.isEnabled().addValueObserver(enabled -> this.enabled = enabled);
            final BooleanValue onCursor = cursorDevice.createEqualsValue(device);
            onCursor.addValueObserver(select -> this.selected = select);
            applyColor(XoneRgbColor.WHITE);
        }
        
        private void applyColor(final XoneRgbColor color) {
            this.stdColor = color;
            this.disabledColor = color.bright(XoneRgbColor.DIM);
            this.selectColor = color.bright(XoneRgbColor.FULL_BRIGHT);
        }
        
        private void handleDeviceType(final String type) {
            if ("instrument".equals(type)) {
                applyColor(XoneRgbColor.YELLOW.bright(XoneRgbColor.HALF_BRIGHT));
            } else if ("audio_to_audio".equals(type)) {
                applyColor(XoneRgbColor.ORANGE.bright(XoneRgbColor.HALF_BRIGHT));
            } else {
                applyColor(XoneRgbColor.BLUE.bright(XoneRgbColor.HALF_BRIGHT));
            }
        }
        
        public InternalHardwareLightState getColor() {
            if (exists) {
                if (selected) {
                    return selectColor;
                }
                if (!enabled) {
                    return disabledColor;
                }
                return stdColor;
            }
            return XoneRgbColor.OFF;
        }
        
        public void toggleEnable() {
            device.isEnabled().toggle();
        }
    }
    
    public RemotesLayer(final Layers layers, final ViewControl viewControl, final XoneHwElements hwElements,
        final XoneK3GlobalStates globalStates) {
        super(layers, "REMOTES");
        this.globalStates = globalStates;
        shiftLayer = new Layer(layers, "REMOTES_SHIFT");
        final CursorRemoteControlsPage remotes = viewControl.getDeviceRemotePages();
        final DeviceHwElements deviceElements = hwElements.getDeviceElements(0);
        hwElements.disableKnobButtonSectionRightSide(this);
        
        bindStandardRemoteControl(this, deviceElements, remotes, viewControl.getCursorDevice());
        globalStates.getShiftHeld().addValueObserver(shift -> {
            if (isActive()) {
                shiftLayer.setIsActive(shift);
            }
        });
        final PinnableCursorDevice cursorDevice = viewControl.getCursorDevice();
        
        final List<XoneRgbButton> buttons = deviceElements.getKnobButtons();
        final XoneRgbButton leftBottomButton = buttons.get(8);
        final XoneRgbButton rightBottomButton = buttons.get(9);
        cursorDevice.isEnabled().markInterested();
        cursorDevice.exists().markInterested();
        
        leftBottomButton.bindLight(shiftLayer, () -> deviceEnabledState(cursorDevice));
        leftBottomButton.bindPressed(shiftLayer, () -> cursorDevice.isEnabled().toggle());
        rightBottomButton.bindLight(shiftLayer, () -> XoneRgbColor.OFF);
        rightBottomButton.bindPressed(shiftLayer, () -> {});
    }
    
    private static XoneRgbColor deviceEnabledState(final PinnableCursorDevice cursorDevice) {
        if (cursorDevice.exists().get()) {
            return cursorDevice.isEnabled().get() ? DEVICE.getColor(2) : DEVICE.getColor(0);
        }
        return XoneRgbColor.GRAY.bright(01);
    }
    
    public static void bindStandardRemoteControl(final Layer layer, final DeviceHwElements hwElements,
        final CursorRemoteControlsPage remotes, final PinnableCursorDevice cursorDevice) {
        final List<AbsoluteHardwareControl> knobs = hwElements.getKnobs();
        final List<XoneRgbButton> buttons = hwElements.getKnobButtons();
        remotes.pageCount().markInterested();
        remotes.selectedPageIndex().markInterested();
        for (int i = 0; i < 8; i++) {
            final int index = i;
            final XoneRgbButton button = buttons.get(i);
            final AbsoluteHardwareControl knob = knobs.get(i);
            final RemoteControl param = remotes.getParameter(i);
            
            param.exists().markInterested();
            param.value().markInterested();
            param.discreteValueCount().markInterested();
            button.bindLight(layer, () -> getParamState(index, param));
            button.bindPressed(layer, () -> toggleParameter(param));
            layer.bind(knob, param.value());
        }
        for (int i = 0; i < 4; i++) {
            final int index = i + 8;
            final AbsoluteHardwareControl knob = knobs.get(index);
            layer.bind(knob, v -> {});
        }
        final XoneRgbButton devicePreviousButton = buttons.get(8);
        final XoneRgbButton deviceNextButton = buttons.get(9);
        if (cursorDevice != null) {
            cursorDevice.hasPrevious().markInterested();
            cursorDevice.hasNext().markInterested();
            cursorDevice.exists().markInterested();
            devicePreviousButton.bindLight(layer, () -> deviceNavigateLeftColor(cursorDevice));
            devicePreviousButton.bindPressed(layer, () -> cursorDevice.selectPrevious());
            deviceNextButton.bindLight(layer, () -> deviceNavigateRightColor(cursorDevice));
            deviceNextButton.bindPressed(layer, () -> cursorDevice.selectNext());
        } else {
            devicePreviousButton.bindLight(layer, () -> XoneRgbColor.OFF);
            devicePreviousButton.bindPressed(layer, () -> {});
            deviceNextButton.bindLight(layer, () -> XoneRgbColor.OFF);
            deviceNextButton.bindPressed(layer, () -> {});
        }
        final XoneRgbButton remotesPreviousButton = buttons.get(10);
        final XoneRgbButton remotesNextButton = buttons.get(11);
        remotes.selectedPageIndex().markInterested();
        remotes.pageCount().markInterested();
        remotesPreviousButton.bindLight(layer, () -> remotesNavigateLeftColor(remotes));
        remotesPreviousButton.bindPressed(layer, () -> remotes.selectedPageIndex().inc(-1));
        remotesNextButton.bindLight(layer, () -> remotesNavigateRightColor(remotes));
        remotesNextButton.bindPressed(layer, () -> remotes.selectedPageIndex().inc(1));
    }
    
    private static XoneRgbColor deviceNavigateLeftColor(final PinnableCursorDevice device) {
        if (device.exists().get()) {
            return device.hasPrevious().get() ? WHITE.getColor(2) : WHITE.getColor(0);
        }
        return XoneRgbColor.GRAY.bright(1);
    }
    
    
    private static XoneRgbColor deviceNavigateRightColor(final PinnableCursorDevice device) {
        if (device.exists().get()) {
            return device.hasNext().get() ? WHITE.getColor(2) : WHITE.getColor(0);
        }
        return XoneRgbColor.GRAY.bright(1);
    }
    
    private static XoneRgbColor remotesNavigateLeftColor(final CursorRemoteControlsPage remotes) {
        if (remotes.pageCount().get() == 0) {
            return AQUA.getColor(0);
        }
        return remotes.selectedPageIndex().get() > 0 ? AQUA.getColor(2) : AQUA.getColor(1);
    }
    
    private static XoneRgbColor remotesNavigateRightColor(final CursorRemoteControlsPage remotes) {
        if (remotes.pageCount().get() == 0) {
            return AQUA.getColor(0);
        }
        return remotes.selectedPageIndex().get() + 1 < remotes.pageCount().get() ? AQUA.getColor(2) : AQUA.getColor(1);
    }
    
    public static void toggleParameter(final RemoteControl param) {
        final SettableRangedValue paramValue = param.value();
        final double value = paramValue.get();
        if (param.discreteValueCount().get() == -1) {
            XoneK3ControllerExtension.println(" > %f", value);
            if (value > 0.5) {
                paramValue.setImmediately(0);
            } else {
                paramValue.setImmediately(1.0);
            }
        } else {
            final int intVal = (int) Math.round(paramValue.get() * param.discreteValueCount().get()) + 1;
            
            if (intVal <= param.discreteValueCount().get()) {
                paramValue.set(intVal, param.discreteValueCount().get());
            } else {
                paramValue.set(0);
            }
        }
    }
    
    public static XoneRgbColor getParamState(final int index, final RemoteControl param) {
        if (param.exists().get()) {
            if (param.value().get() > 0) {
                return PARAM_COLORS[index].getColor(2);
            } else {
                return PARAM_COLORS[index].getColor(1);
            }
        }
        return PARAM_COLORS[index].getColor(0);
    }
    
    public static XoneRgbColor getParamState(final int index, final RemoteControl param,
        final TrackSpecControl trackSpecControl) {
        if (param.exists().get()) {
            if (param.value().get() > 0) {
                return PARAM_COLORS[index].getColor(2);
            } else {
                return PARAM_COLORS[index].getColor(1);
            }
        }
        if (trackSpecControl.isTrackExists()) {
            return PARAM_COLORS[index].getColor(0);
        }
        return XoneRgbColor.OFF;
    }
    
    
    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        this.shiftLayer.setIsActive(false);
    }
    
    @Override
    protected void onActivate() {
        super.onActivate();
    }
}
