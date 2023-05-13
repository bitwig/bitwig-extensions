package com.bitwig.extensions.controllers.novation.launchpadpromk3.sliderlayers;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.novation.commonsmk3.LabeledButton;
import com.bitwig.extensions.controllers.novation.commonsmk3.MidiProcessor;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.commonsmk3.SliderBinding;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.*;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.layers.ControlMode;
import com.bitwig.extensions.framework.Layers;

public class DeviceSliderLayer extends SliderLayer {

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
                             final Layers layers, final MidiProcessor midiProcessor, final SysExHandler sysExHandler,
                             final HwElements hwElements) {
        super("DEVICE", ControlMode.DEVICE, controlSurface, layers, midiProcessor, sysExHandler);
        device = viewCursorControl.getCursorDevice();
        parameterBank = device.createCursorRemoteControlsPage(8);
        parameterBank.pageCount().addValueObserver(pages -> parameterPages = pages);
        parameterBank.selectedPageIndex().addValueObserver(pageIndex -> this.pageIndex = pageIndex);
        final PinnableCursorDevice primary = viewCursorControl.getPrimaryDevice();
        final CursorDeviceLayer drumCursor = primary.createCursorLayer();
        drumDeviceBank = drumCursor.createDeviceBank(8);

        initSceneButtons(hwElements);
        for (int i = 0; i < 8; i++) {
            final RemoteControl parameter = parameterBank.getParameter(i);

            final SliderBinding binding = new SliderBinding(mode.getCcNr(), parameter, sliders[i], i, midiProcessor);
            addBinding(binding);
            valueBindings.add(binding);
        }
        initNavigation(hwElements);
    }

    private void initNavigation(final HwElements hwElements) {
        final LabeledButton upButton = hwElements.getLabeledButton(LabelCcAssignments.UP);
        final LabeledButton downButton = hwElements.getLabeledButton(LabelCcAssignments.DOWN);
        final LabeledButton leftButton = hwElements.getLabeledButton(LabelCcAssignments.LEFT);
        final LabeledButton rightButton = hwElements.getLabeledButton(LabelCcAssignments.RIGHT);
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


    private void initSceneButtons(final HwElements hwElements) {
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
    }

    @Override
    protected void onActivate() {
        super.onActivate();
        refreshTrackColors();
        modeHandler.setFaderBank(0, mode, tracksExistsColors);
        modeHandler.changeMode(LpBaseMode.FADER, mode.getBankId());
    }

}
