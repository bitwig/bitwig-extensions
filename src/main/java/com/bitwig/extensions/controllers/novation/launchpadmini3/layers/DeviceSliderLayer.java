package com.bitwig.extensions.controllers.novation.launchpadmini3.layers;

import com.bitwig.extension.controller.api.CursorDeviceLayer;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extensions.controllers.novation.commonsmk3.LabeledButton;
import com.bitwig.extensions.controllers.novation.commonsmk3.MidiProcessor;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.commonsmk3.SliderBinding;
import com.bitwig.extensions.controllers.novation.commonsmk3.ViewCursorControl;
import com.bitwig.extensions.controllers.novation.launchpadmini3.LpMiniHwElements;
import com.bitwig.extensions.controllers.novation.launchpadmini3.LabelCcAssignmentsMini;
import com.bitwig.extensions.framework.Layers;

public class DeviceSliderLayer extends AbstractSliderLayer {
    
    private static final int[] PARAM_COLORS = {5, 9, 13, 25, 29, 41, 49, 57};
    
    private final CursorRemoteControlsPage parameterBank;
    private final DeviceBank drumDeviceBank;
    final PinnableCursorDevice device;
    private int parameterPages;
    private int pageIndex;
    private boolean isNested;
    private boolean hasDrumPads;
    private boolean hasLayers;
    private boolean hasSlots;
    private String[] deviceSlotNames = new String[0];
    
    public DeviceSliderLayer(final ViewCursorControl viewCursorControl, final HardwareSurface controlSurface,
        final Layers layers, final MidiProcessor midiProcessor, final LpMiniHwElements hwElements) {
        super("DEVICE", controlSurface, layers, midiProcessor, 40, 31);
        device = viewCursorControl.getCursorDevice();
        parameterBank = device.createCursorRemoteControlsPage(8);
        parameterBank.pageCount().addValueObserver(pages -> parameterPages = pages);
        parameterBank.selectedPageIndex().addValueObserver(pageIndex -> this.pageIndex = pageIndex);
        final PinnableCursorDevice primary = viewCursorControl.getPrimaryDevice();
        final CursorDeviceLayer drumCursor = primary.createCursorLayer();
        drumDeviceBank = drumCursor.createDeviceBank(8);
        
        //initSceneButtons(hwElements);
        for (int i = 0; i < 8; i++) {
            final RemoteControl parameter = parameterBank.getParameter(i);
            final SliderBinding binding = new SliderBinding(baseCcNr, parameter, sliders[i], i, midiProcessor);
            addBinding(binding);
            valueBindings.add(binding);
        }
        //initNavigation(hwElements);
    }
    
    private void initNavigation(final LpMiniHwElements hwElements) {
        final LabeledButton upButton = hwElements.getLabeledButton(LabelCcAssignmentsMini.UP);
        final LabeledButton downButton = hwElements.getLabeledButton(LabelCcAssignmentsMini.DOWN);
        final LabeledButton leftButton = hwElements.getLabeledButton(LabelCcAssignmentsMini.LEFT);
        final LabeledButton rightButton = hwElements.getLabeledButton(LabelCcAssignmentsMini.RIGHT);
        parameterBank.hasNext().markInterested();
        parameterBank.hasPrevious().markInterested();
        
        device.hasNext().markInterested();
        device.hasPrevious().markInterested();
        
        device.isNested().addValueObserver(nested -> isNested = nested);
        device.slotNames().addValueObserver(slotNames -> deviceSlotNames = slotNames);
        device.hasSlots().addValueObserver(hasSlots -> this.hasSlots = hasSlots);
        device.hasLayers().addValueObserver(hasLayers -> this.hasLayers = hasLayers);
        device.hasDrumPads().addValueObserver(hasPads -> hasDrumPads = hasPads);
        
        
        final RgbState scrollColor = RgbState.of(23);
        upButton.bindRepeatHold(this, () -> handleNavigateDevice(-1));
        upButton.bindLight(this, () -> isNested ? scrollColor : RgbState.OFF);
        
        downButton.bindRepeatHold(this, () -> handleNavigateDevice(1));
        downButton.bindLight(this, () -> canNavigateDown() ? scrollColor : RgbState.OFF);
        
        leftButton.bindRepeatHold(this, () -> handleNavigateChain(-1));
        leftButton.bindLight(this, () -> device.hasPrevious().get() ? scrollColor : RgbState.OFF);
        
        rightButton.bindRepeatHold(this, () -> handleNavigateChain(1));
        rightButton.bindLight(this, () -> device.hasNext().get() ? scrollColor : RgbState.OFF);
    }
    
    private boolean canNavigateDown() {
        return hasDrumPads || hasLayers || hasSlots;
    }
    
    private void handleNavigateChain(final int amount) {
        if (amount > 0) {
            device.selectNext();
        } else {
            device.selectPrevious();
        }
    }
    
    private void handleNavigateDevice(final int amount) {
        if (amount > 0) {
            if (device.hasDrumPads().get()) {
                device.selectDevice(drumDeviceBank.getItemAt(0));
            } else if (hasSlots) {
                device.selectFirstInSlot(deviceSlotNames[0]);
            } else if (hasLayers) {
                device.selectFirstInLayer(0);
            }
        } else {
            device.selectParent();
        }
    }
    
    private void handleNavigateParameters(final int amount, final CursorRemoteControlsPage parameters) {
        if (amount > 0) {
            parameters.selectNextPage(false);
        } else {
            parameters.selectPreviousPage(false);
        }
    }
    
    
    private void initSceneButtons(final LpMiniHwElements hwElements) {
        for (int i = 0; i < 8; i++) {
            final int index = i;
            final LabeledButton sceneButton = hwElements.getSceneLaunchButtons().get(index);
            sceneButton.bindPressed(this, () -> handleSendSelect(index));
            sceneButton.bindLight(this, () -> getColor(index));
        }
    }
    
    private void handleSendSelect(final int index) {
        parameterBank.selectedPageIndex().set(index);
    }
    
    private RgbState getColor(final int index) {
        if (index < parameterPages) {
            if (pageIndex == index) {
                return RgbState.WHITE;
            }
            return RgbState.DIM_WHITE;
        }
        return RgbState.OFF;
    }
    
    @Override
    protected void refreshTrackColors() {
        System.arraycopy(PARAM_COLORS, 0, tracksExistsColors, 0, PARAM_COLORS.length);
        midiProcessor.setFaderBank(0, tracksExistsColors, true, baseCcNr);
    }
    
    @Override
    protected void onActivate() {
        super.onActivate();
        refreshTrackColors();
    }
    
    @Override
    protected void onDeactivate() {
        super.onDeactivate();
    }
}
