package com.bitwig.extensions.controllers.akai.mpkmk4.layers;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkHwElements;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkMidiProcessor;
import com.bitwig.extensions.controllers.akai.mpkmk4.bindings.ParameterDisplayBinding;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.Encoder;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.LineDisplay;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.ParameterValues;
import com.bitwig.extensions.controllers.nativeinstruments.komplete.binding.KnobParameterBinding;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.values.BasicStringValue;

public class RemotesControlLayer extends Layer {
    private int pageCount;
    private int pageIndex;
    private String[] pageNames = new String[0];
    private final ParameterValues display;
    private final LineDisplay mainDisplay;
    final CursorRemoteControlsPage deviceRemotes;
    private final List<KnobParameterBinding> bindings = new ArrayList<>();
    final MpkMidiProcessor midiProcessor;
    private final BasicStringValue pageName = new BasicStringValue();
    
    public RemotesControlLayer(String name, Layers layers,
        StringValue deviceNameValue,
        final CursorRemoteControlsPage deviceRemotes,
        MpkHwElements hwElements, final MpkMidiProcessor midiProcessor) {
        super(layers, name);
        this.deviceRemotes = deviceRemotes;
        this.midiProcessor = midiProcessor;
        this.display = new ParameterValues(midiProcessor);
        this.mainDisplay = hwElements.getMainLineDisplay();
        deviceNameValue.addValueObserver(this::handleDeviceNameChanged);
        deviceRemotes.selectedPageIndex().addValueObserver(this::handlePageIndex);
        deviceRemotes.pageCount().addValueObserver(this::handlePageCount);
        final List<Encoder> encoders = hwElements.getEncoders();
        for (int i = 0; i < 8; i++) {
            final RemoteControl remote = deviceRemotes.getParameter(i);
            final Encoder encoder = encoders.get(i);
            encoder.bindValue(this, remote.value());
            this.addBinding(new ParameterDisplayBinding(remote, encoder, display, i));
         }
        deviceRemotes.pageNames().addValueObserver(this::handlePageNames);
        pageName.addValueObserver(this::handlePageNameChanged);
    }
    
    private void handleDeviceNameChanged(String name) {
        if(isActive()) {
            mainDisplay.setText(1, name);
        }
    }
    
    private void handlePageNameChanged(final String page) {
        if (isActive()) {
            mainDisplay.setText(2, page);
        }
    }
    
    private void handlePageNames(final String[] pageNames) {
        this.pageNames = pageNames;
        if(pageIndex < this.pageNames.length) {
            pageName.set(this.pageNames[pageIndex]);
        }
    }
    
    private void handlePageCount(final int pageCount) {
        if (pageCount == -1) {
            return;
        }
        this.pageCount = pageCount;
        //updatePageCount();
    }
    
    private void handlePageIndex(final int pageIndex) {
        if (pageIndex == -1) {
            pageName.set("");
            return;
        }
        this.pageIndex = pageIndex;
        if(pageIndex < this.pageNames.length) {
            pageName.set(this.pageNames[pageIndex]);
        }
    }
    
    public void navigateLeft() {
        deviceRemotes.selectPreviousPage(false);
    }
    
    public void navigateRight() {
        deviceRemotes.selectNextPage(false);
    }
    
    public boolean canScrollRight() {
        return pageIndex < pageCount - 1;
    }
    
    public boolean canScrollLeft() {
        return pageIndex > 0;
    }
    
}
