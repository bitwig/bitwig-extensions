package com.bitwig.extensions.controllers.reloop;

import java.util.List;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;

@Component
public class DeviceSelectionLayer extends Layer {
    
    private final boolean[] deviceExists = new boolean[8];
    private final String[] deviceType = new String[8];
    private int selectedDeviceIndex;
    private int pageIndex = 0;
    private int pageCount = 0;
    
    @Inject
    GlobalStates globalStates;
    
    public DeviceSelectionLayer(final Layers layers, final MidiProcessor midiProcessor, final BitwigControl viewControl,
        final HwElements hwElements) {
        super(layers, "Device Selection Layer");
        final DeviceBank deviceBank = viewControl.getDeviceBank();
        final List<RgbButton> buttons = hwElements.getNoteButtons();
        final PinnableCursorDevice cursorDevice = viewControl.getCursorDevice();
        for (int i = 0; i < deviceBank.getSizeOfBank(); i++) {
            final int index = i;
            final Device device = deviceBank.getDevice(i);
            device.exists().addValueObserver(exist -> {
                this.deviceExists[index] = exist;
            });
            final BooleanValue onCursor = device.createEqualsValue(cursorDevice);
            onCursor.addValueObserver(isOnCursor -> handleDeviceSelection(index, isOnCursor));
            device.deviceType().addValueObserver(type -> {
                this.deviceType[index] = type;
            });
        }
        final CursorRemoteControlsPage remotes = viewControl.getRemotes();
        remotes.pageCount().addValueObserver(pages -> {
            this.pageCount = pages;
        });
        remotes.selectedPageIndex().addValueObserver(pageIndex -> {
            this.pageIndex = pageIndex;
        });
        
        for (int i = 0; i < 32; i++) {
            final int row = (i % 16) / 8;
            final int deviceIndex = i % 8;
            final RgbButton button = buttons.get(i);
            final Device bankDevice = deviceBank.getDevice(deviceIndex);
            if (row == 0) {
                button.bindPressed(this, () -> {
                    selectDevice(cursorDevice, bankDevice);
                });
                button.bindLight(this, () -> this.toDeviceStateColor(deviceIndex));
            } else {
                button.bindPressed(this, () -> {
                    remotes.selectedPageIndex().set(deviceIndex);
                });
                button.bindLight(this, () -> this.toRemoteStateColor(deviceIndex));
            }
        }
    }
    
    private void selectDevice(final PinnableCursorDevice cursorDevice, final Device bankDevice) {
        if (this.globalStates.getShiftState().get()) {
            cursorDevice.deleteObject();
        } else {
            cursorDevice.selectDevice(bankDevice);
            bankDevice.selectInEditor();
        }
    }
    
    private ReloopRgb toDeviceStateColor(final int deviceIndex) {
        if (deviceExists[deviceIndex]) {
            return getDeviceColor(deviceType[deviceIndex], deviceIndex == selectedDeviceIndex);
        }
        return ReloopRgb.OFF;
    }
    
    private ReloopRgb toRemoteStateColor(final int pageIndex) {
        if (pageIndex < pageCount && selectedDeviceIndex != -1 && selectedDeviceIndex < deviceType.length) {
            return getDeviceColor(deviceType[selectedDeviceIndex], this.pageIndex == pageIndex);
        }
        return ReloopRgb.OFF;
    }
    
    private void handleDeviceSelection(final int index, final boolean isOnCursor) {
        if (isOnCursor) {
            this.selectedDeviceIndex = index;
        }
    }
    
    private ReloopRgb getDeviceColor(final String type, final boolean selected) {
        return switch (type) {
            case "audio_to_audio" -> selected ? ReloopRgb.BRIGHT_ORANGE : ReloopRgb.DIMMED_ORANGE;
            case "instrument" -> selected ? ReloopRgb.BRIGHT_YELLOW : ReloopRgb.DIMMED_YELLOW;
            case "note-effect" -> selected ? ReloopRgb.BRIGHT_BLUE : ReloopRgb.DIMMED_BLUE;
            default -> selected ? ReloopRgb.WHITE_BRIGHT : ReloopRgb.WHITE_DIM;
        };
        
    }
    
}