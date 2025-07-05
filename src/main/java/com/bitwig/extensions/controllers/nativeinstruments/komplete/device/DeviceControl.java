package com.bitwig.extensions.controllers.nativeinstruments.komplete.device;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RelativeHardwareControlBinding;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.ClipSceneCursor;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.ControlElements;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.midi.DeviceMidiListener;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.midi.MidiProcessor;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class DeviceControl implements DeviceMidiListener {
    
    private final MidiProcessor midiProcessor;
    private final RelativeHardwareKnob[] knobs = new RelativeHardwareKnob[8];
    private final BankControl mainBank;
    private final ControllerHost host;
    private Runnable bankUpdateTask = null;
    private final Layer navigationLayer;
    
    private int pageCount = 0;
    private int pageIndex = 0;
    private final List<ParameterSlot> deviceParameters = new ArrayList<>();
    private final PinnableCursorDevice cursorDevice;
    private final ControlElements controlElements;
    private final Layer deviceRemoteLayer;
    private final Layer trackRemoteLayer;
    private final Layer projectRemoteLayer;
    
    public DeviceControl(final ControllerHost host, final HardwareSurface surface, final MidiProcessor midiProcessor,
        final ClipSceneCursor clipSceneCursor, final Layers layers, final ControlElements controlElements) {
        this.midiProcessor = midiProcessor;
        this.midiProcessor.addDeviceMidiListener(this);
        this.midiProcessor.addModeListener(this::changeMode);
        navigationLayer = new Layer(layers, "PARAM_NAVIGATION_LAYER");
        deviceRemoteLayer = new Layer(layers, "DEVICE");
        trackRemoteLayer = new Layer(layers, "TRACK");
        projectRemoteLayer = new Layer(layers, "PROJECT");
        final CursorTrack cursorTrack = clipSceneCursor.getCursorTrack();
        this.host = host;
        this.controlElements = controlElements;
        cursorDevice = cursorTrack.createCursorDevice();
        setUpKnobs(surface);
        
        this.mainBank = new BankControl(cursorDevice, this.midiProcessor, this);
        //        devices.add(new DeviceSlot(0, "Proj-Remotes"));
        //        devices.add(new DeviceSlot(1, "Track Remotes"));
        
        //layerBank.itemCount().addValueObserver(items -> KompleteKontrolExtension.println(" LAYER = %d", items));
        final CursorRemoteControlsPage deviceRemotes = cursorDevice.createCursorRemoteControlsPage(8);
        deviceRemotes.selectedPageIndex().addValueObserver(this::handlePageIndex);
        deviceRemotes.pageCount().addValueObserver(this::handlePageCount);
        for (int i = 0; i < 8; i++) {
            final int index = i;
            final RemoteControl remote = deviceRemotes.getParameter(i);
            final RelativeHardwareControlBinding binding =
                knobs[index].addBindingWithSensitivity(remote.value(), 0.125);
            
            final ParameterSlot slot = new ParameterSlot(i, remote, binding);
            deviceParameters.add(slot);
            remote.name().addValueObserver(name -> updateRemoteName(slot, name));
            remote.exists().addValueObserver(exists -> updateRemoteExists(slot, exists));
            remote.displayedValue().addValueObserver(valueName -> updateValueName(index, valueName));
            remote.discreteValueCount().addValueObserver(values -> updateRemotesValueCount(slot, values));
            remote.getOrigin().addValueObserver(origin -> updateRemoteOrigin(slot, origin));
            remote.value().addValueObserver(128, value -> midiProcessor.updateParameterValue(index, value));
        }
        cursorDevice.isPlugin()
            .addValueObserver(onPlugin -> deviceParameters.forEach(param -> param.setOnPlugin(onPlugin)));
        controlElements.getShiftHeld().addValueObserver(this::applyShift);
        
        navigationLayer.bindPressed(
            controlElements.getTrackNavLeftButton(), () -> deviceRemotes.selectPreviousPage(false));
        navigationLayer.bindPressed(
            controlElements.getTrackRightNavButton(), () -> deviceRemotes.selectNextPage(false));
        
        navigationLayer.bind(() -> pageIndex < pageCount - 1, controlElements.getTrackNavRightButtonLight());
        navigationLayer.bind(() -> pageIndex > 0, controlElements.getTrackNavLeftButtonLight());
    }
    
    private void applyShift(final boolean shift) {
        deviceParameters.forEach(slot -> slot.applyFineAdjust(shift));
    }
    
    private void changeMode(final int mode) {
        if (mode == 1) {
            navigationLayer.activate();
            controlElements.updateLights();
        } else {
            navigationLayer.deactivate();
        }
    }
    
    private void handlePageCount(final int pageCount) {
        if (pageCount == -1) {
            return;
        }
        this.pageCount = pageCount;
        midiProcessor.sendPageCount(pageCount, pageIndex);
    }
    
    private void handlePageIndex(final int pageIndex) {
        if (pageIndex == -1) {
            return;
        }
        this.pageIndex = pageIndex;
        midiProcessor.sendPageCount(pageCount, pageIndex);
    }
    
    private void updateValueName(final int index, final String value) {
        midiProcessor.sendParamValue(index, value);
    }
    
    private void updateRemoteOrigin(final ParameterSlot slot, final double origin) {
        slot.setOrigin(origin);
        midiProcessor.sendRemoteState(slot);
    }
    
    private void updateRemotesValueCount(final ParameterSlot slot, final int values) {
        slot.setValueCount(values);
        midiProcessor.sendRemoteState(slot);
    }
    
    private void updateRemoteName(final ParameterSlot slot, final String name) {
        slot.setName(name);
        midiProcessor.sendRemoteState(slot);
    }
    
    private void updateRemoteExists(final ParameterSlot slot, final boolean exists) {
        slot.setExists(exists);
        midiProcessor.sendRemoteState(slot);
    }
    
    //handleIsNested
    public void triggerUpdateAction() {
        if (bankUpdateTask == null) {
            bankUpdateTask = this::updateBanks;
            host.scheduleTask(bankUpdateTask, 200);
        }
    }
    
    private void updateBanks() {
        midiProcessor.sendBankUpdate(mainBank.getBankConfig());
        midiProcessor.sendSelectionIndex(mainBank.getSelectionIndex(), new int[0]);
        bankUpdateTask = null;
    }
    
    protected void setUpKnobs(final HardwareSurface surface) {
        final MidiIn midiIn = midiProcessor.getMidiIn();
        for (int i = 0; i < 8; i++) {
            final RelativeHardwareKnob knob = surface.createRelativeHardwareKnob("DEVICE_KNOB" + i);
            knobs[i] = knob;
            knob.setAdjustValueMatcher(midiIn.createRelative2sComplementCCValueMatcher(0xF, 0x70 + i, 128));
            knob.setStepSize(1 / 128.0);
        }
    }
    
    @Override
    public void handleDeviceSelect(final int[] selectionPath) {
        //KompleteKontrolExtension.println(" SELECT INDEX = %s", Arrays.toString(selectionPath));
        this.mainBank.select(selectionPath);
    }
}
