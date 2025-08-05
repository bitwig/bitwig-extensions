package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.layer;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchControlXlHwElements;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.DisableBinding;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.LightValueBindings;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.RelativeEncoderBinding;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchRelativeEncoder;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.DisplayControl;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.GradientColor;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.BasicStringValue;

class Remotes {
    public static final List<GradientColor> DEVICE_COLORS =
        List.of(
            GradientColor.RED, GradientColor.ORANGE, GradientColor.YELLOW, GradientColor.GREEN,
            GradientColor.DARK_GREEN, GradientColor.BLUE, GradientColor.PURPLE, GradientColor.PINK);
    
    private String[] devicePageNames;
    private int devicePageIndex = 0;
    private int devicePage2Index = 0;
    private int devicePageCount = 0;
    private boolean active;
    private final BasicStringValue devicePageName = new BasicStringValue();
    private final CursorRemoteControlsPage remotes;
    private final CursorRemoteControlsPage remotes2;
    private final BasicStringValue deviceName = new BasicStringValue("");
    private final List<DisableBinding> disableBindings = new ArrayList<>();
    
    public Remotes(final CursorDevice cursorDevice) {
        this.remotes = cursorDevice.createCursorRemoteControlsPage(8);
        this.remotes2 = cursorDevice.createCursorRemoteControlsPage("PAGE2", 8, null);
        remotes.pageNames().addValueObserver(this::handleDevicePages);
        remotes.selectedPageIndex().addValueObserver(this::handleDevicePageIndex);
        remotes.pageCount().addValueObserver(this::updatePageCount);
        remotes2.selectedPageIndex().addValueObserver(this::handleDevicePage2Index);
        cursorDevice.name().addValueObserver(deviceName::set);
    }
    
    private void handleDevicePage2Index(final int index) {
        devicePage2Index = index;
        updateDevicePageName(true);
    }
    
    public void bind(final Layer layer, final LaunchControlXlHwElements hwElements,
        final DisplayControl displayControl) {
        for (int i = 0; i < 8; i++) {
            final LaunchRelativeEncoder row1Encoder = hwElements.getRelativeEncoder(0, i);
            final LaunchRelativeEncoder row2Encoder = hwElements.getRelativeEncoder(1, i);
            final Parameter parameter = this.getParameter(i);
            layer.addBinding(
                new RelativeEncoderBinding(parameter, row1Encoder, displayControl, deviceName, parameter.name()));
            layer.addBinding(new LightValueBindings(parameter, row1Encoder.getLight(), DEVICE_COLORS.get(i)));
            final Parameter parameter2 = getParameter2(i);
            
            final RelativeEncoderBinding row2Binding =
                new RelativeEncoderBinding(parameter2, row2Encoder, displayControl, deviceName, parameter2.name());
            layer.addBinding(row2Binding);
            final LightValueBindings row2LightBinding =
                new LightValueBindings(parameter2, row2Encoder.getLight(), DEVICE_COLORS.get(i));
            layer.addBinding(row2LightBinding);
            disableBindings.add(row2Binding);
            disableBindings.add(row2LightBinding);
        }
    }
    
    private void updatePageCount(final int count) {
        this.devicePageCount = count;
        updateRow2Layer();
        if (devicePageIndex == devicePage2Index && devicePageCount > 1) {
            if (devicePageIndex + 1 < this.devicePageCount) {
                this.devicePage2Index = devicePageIndex + 1;
            } else {
                this.devicePage2Index = 0;
            }
            this.remotes2.selectedPageIndex().set(devicePage2Index);
        }
    }
    
    private void updateRow2Layer() {
        final boolean isRow2Active = active && devicePageCount > 1 && devicePageIndex + 1 < devicePageCount;
        disableBindings.forEach(binding -> binding.setDisabled(!isRow2Active));
    }
    
    private void handleDevicePages(final String[] pages) {
        this.devicePageNames = pages;
        updateDevicePageName(true);
    }
    
    private void handleDevicePageIndex(final int selectedPageIndex) {
        this.devicePageIndex = selectedPageIndex;
        final int page2Index = selectedPageIndex + 1 < devicePageCount ? selectedPageIndex + 1 : 0;
        this.remotes2.selectedPageIndex().set(page2Index);
        devicePage2Index = page2Index;
        updateRow2Layer();
        updateDevicePageName(false);
    }
    
    private void updateDevicePageName(final boolean handleEmpty) {
        if (this.devicePageNames != null && devicePageIndex < this.devicePageNames.length && devicePageIndex != -1) {
            if (devicePageIndex == devicePage2Index || devicePageIndex + 1 >= devicePageNames.length
                || devicePage2Index == -1) {
                this.devicePageName.set(this.devicePageNames[devicePageIndex]);
            } else {
                this.devicePageName.set(
                    "%s/%s".formatted(
                        devicePageNames[Math.min(devicePageIndex, devicePageNames.length - 1)],
                        devicePageNames[Math.min(devicePage2Index, devicePageNames.length - 1)]));
            }
        } else if (handleEmpty) {
            this.devicePageName.set("No Page");
        }
    }
    
    public void setActive(final boolean active) {
        this.active = active;
    }
    
    public Parameter getParameter(final int index) {
        return remotes.getParameter(index);
    }
    
    public Parameter getParameter2(final int index) {
        return remotes2.getParameter(index);
    }
    
    public BasicStringValue getDevicePageName() {
        return devicePageName;
    }
    
    public boolean canGoBack() {
        return devicePageIndex > 0;
    }
    
    public boolean canGoForward() {
        return devicePageNames != null && devicePageNames.length > 0 && devicePageIndex != devicePageCount - 1;
    }
    
    public void selectPreviousPage() {
        this.remotes.selectPreviousPage(false);
    }
    
    public void selectNextPage() {
        this.remotes.selectNextPage(false);
    }
}
