package com.bitwig.extensions.controllers.akai.mpkmk4.layers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkHwElements;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkMidiProcessor;
import com.bitwig.extensions.controllers.akai.mpkmk4.bindings.ParameterDisplayBinding;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.Encoder;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.RemotesDisplayControl;
import com.bitwig.extensions.framework.Layers;

public class RemotesControlLayer extends EncoderLayer {
    
    private final CursorRemoteControlsPage deviceRemotes;
    private final RemotesDisplayControl displayControl;
    private final List<ParameterDisplayBinding> bindings = new ArrayList<>();
    
    public RemotesControlLayer(final String name, final Layers layers, final StringValue deviceNameValue,
        final CursorRemoteControlsPage deviceRemotes, final MpkHwElements hwElements,
        final MpkMidiProcessor midiProcessor) {
        super(name, layers, midiProcessor);
        this.deviceRemotes = deviceRemotes;
        this.displayControl =
            new RemotesDisplayControl(deviceNameValue, hwElements.getMainLineDisplay(), deviceRemotes);
        final List<Encoder> encoders = hwElements.getEncoders();
        for (int i = 0; i < 8; i++) {
            final RemoteControl remote = deviceRemotes.getParameter(i);
            final Encoder encoder = encoders.get(i);
            encoder.bindValue(this, remote.value());
            final ParameterDisplayBinding binding = new ParameterDisplayBinding(remote, encoder, parameterValues, i);
            this.bindings.add(binding);
            this.addBinding(binding);
        }
    }
    
    public void updateTemporary() {
        displayControl.updateTemporary();
    }
    
    public Optional<RemotesDisplayControl> getDisplayControl() {
        return Optional.of(displayControl);
    }
    
    public void navigateLeft() {
        deviceRemotes.selectPreviousPage(false);
    }
    
    public void navigateRight() {
        deviceRemotes.selectNextPage(false);
    }
    
    public boolean canScrollRight() {
        return this.displayControl.canScrollRight();
    }
    
    public boolean canScrollLeft() {
        return this.displayControl.canScrollLeft();
    }
    
    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        this.displayControl.setActive(false);
    }
    
    @Override
    protected void onActivate() {
        super.onActivate();
        displayControl.setActive(true);
        this.displayControl.updateDisplay();
    }
}
