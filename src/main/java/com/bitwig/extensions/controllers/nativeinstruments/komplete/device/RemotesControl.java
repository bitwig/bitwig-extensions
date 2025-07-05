package com.bitwig.extensions.controllers.nativeinstruments.komplete.device;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.RelativeHardwareControlBinding;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.midi.MidiProcessor;
import com.bitwig.extensions.framework.Layer;

public class RemotesControl {
    
    private int pageCount;
    private int pageIndex;
    final CursorRemoteControlsPage deviceRemotes;
    private final List<ParameterSlot> deviceParameters = new ArrayList<>();
    final MidiProcessor midiProcessor;
    
    public RemotesControl(final Layer layer, final CursorRemoteControlsPage deviceRemotes,
        final RelativeHardwareKnob[] knobs, final MidiProcessor midiProcessor) {
        this.deviceRemotes = deviceRemotes;
        this.midiProcessor = midiProcessor;
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
    
}
