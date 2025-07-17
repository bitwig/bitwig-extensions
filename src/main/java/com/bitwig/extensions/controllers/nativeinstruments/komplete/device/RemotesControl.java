package com.bitwig.extensions.controllers.nativeinstruments.komplete.device;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.ControlElements;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.binding.KnobParameterBinding;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.midi.MidiProcessor;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.BasicStringValue;

public class RemotesControl extends AbstractParameterControl {
    
    private int pageCount;
    private int pageIndex;
    private String[] pageNames = new String[0];
    private final String name;
    final CursorRemoteControlsPage deviceRemotes;
    private final List<KnobParameterBinding> bindings = new ArrayList<>();
    final MidiProcessor midiProcessor;
    private final BasicStringValue pageName = new BasicStringValue();
    
    public RemotesControl(final Layer layer, final CursorRemoteControlsPage remotes,
        final ControlElements controlElements, final MidiProcessor midiProcessor) {
        super(layer);
        this.deviceRemotes = remotes;
        this.midiProcessor = midiProcessor;
        this.name = layer.getName();
        remotes.selectedPageIndex().addValueObserver(this::handlePageIndex);
        remotes.pageCount().addValueObserver(this::handlePageCount);
        for (int i = 0; i < 8; i++) {
            final RemoteControl remote = remotes.getParameter(i);
            final RelativeHardwareKnob knob = controlElements.getDeviceKnobs().get(i);
            final KnobParameterBinding binding = new KnobParameterBinding(i, knob, remote, midiProcessor);
            layer.addBinding(binding);
            this.bindings.add(binding);
        }
        remotes.pageNames().addValueObserver(this::handlePageNames);
        pageName.addValueObserver(this::handlePageNameChanged);
    }
    
    private void handlePageNameChanged(final String page) {
        if (isActive()) {
            midiProcessor.sendSection(0, pageName.get());
        }
    }
    
    private void handlePageNames(final String[] pageNames) {
        this.pageNames = pageNames;
        updatePageDisplay();
    }
    
    @Override
    public void setActive(final boolean active) {
        super.setActive(active);
        updatePageCount();
    }
    
    private void updatePageDisplay() {
        if (pageIndex < this.pageNames.length) {
            pageName.set(this.pageNames[pageIndex]);
        }
    }
    
    public void navigateLeft() {
        deviceRemotes.selectPreviousPage(false);
    }
    
    public void navigateRight() {
        deviceRemotes.selectNextPage(false);
    }
    
    public void setFineTune(final boolean fineTune) {
        bindings.forEach(binding -> binding.setFineTune(fineTune));
    }
    
    public void setOnPlugin(final boolean onPlugin) {
        bindings.forEach(binding -> binding.setOnPlugin(onPlugin));
    }
    
    private void handlePageCount(final int pageCount) {
        if (pageCount == -1) {
            return;
        }
        this.pageCount = pageCount;
        updatePageCount();
    }
    
    private void handlePageIndex(final int pageIndex) {
        if (pageIndex == -1) {
            return;
        }
        this.pageIndex = pageIndex;
        updatePageDisplay();
        updatePageCount();
    }
    
    private void updatePageCount() {
        if (isActive()) {
            midiProcessor.sendPageCount(this.pageCount, this.pageIndex);
            midiProcessor.sendSection(0, pageName.get());
        }
    }
    
    
    public boolean canScrollRight() {
        return pageIndex < pageCount - 1;
    }
    
    public boolean canScrollLeft() {
        return pageIndex > 0;
    }
    
}
