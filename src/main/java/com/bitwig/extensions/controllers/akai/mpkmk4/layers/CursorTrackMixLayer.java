package com.bitwig.extensions.controllers.akai.mpkmk4.layers;

import java.util.List;

import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Send;
import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkHwElements;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkMidiProcessor;
import com.bitwig.extensions.controllers.akai.mpkmk4.MpkViewControl;
import com.bitwig.extensions.controllers.akai.mpkmk4.bindings.ParameterDisplayBinding;
import com.bitwig.extensions.controllers.akai.mpkmk4.controls.Encoder;
import com.bitwig.extensions.controllers.akai.mpkmk4.display.LineDisplay;
import com.bitwig.extensions.framework.Layers;

public class CursorTrackMixLayer extends EncoderLayer {
    
    private final SendBank sendBank;
    private int sendsScrollPosition = 0;
    private final LineDisplay display;
    private boolean sendsChangedAction = false;
    
    public CursorTrackMixLayer(final Layers layers, final MpkHwElements hwElements,
        final MpkMidiProcessor midiProcessor, final MpkViewControl viewControl) {
        super("CURSOR_MIXER", layers, midiProcessor);
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        final List<Encoder> encoders = hwElements.getEncoders();
        display = hwElements.getMainLineDisplay();
        final Encoder panEncoder = encoders.get(0);
        panEncoder.bindValue(this, cursorTrack.pan());
        this.addBinding(new ParameterDisplayBinding(cursorTrack.pan(), panEncoder, parameterValues, 0));
        final Encoder volumenEncoder = encoders.get(4);
        volumenEncoder.bindValue(this, cursorTrack.volume());
        this.addBinding(new ParameterDisplayBinding(cursorTrack.volume(), volumenEncoder, parameterValues, 4));
        sendBank = cursorTrack.sendBank();
        sendBank.canScrollForwards().markInterested();
        sendBank.canScrollBackwards().markInterested();
        sendBank.scrollPosition().addValueObserver(this::handleScrollPosition);
        sendBank.itemCount().addValueObserver(this::sendCountChanged);
        for (int i = 0; i < 6; i++) {
            final Send send = sendBank.getItemAt(i);
            final int index = i + (i / 3 + 1);
            final Encoder encoder = encoders.get(index);
            encoder.bindValue(this, send);
            this.addBinding(new ParameterDisplayBinding(send, encoder, parameterValues, index));
        }
    }
    
    private void sendCountChanged(final int sends) {
        if (isActive()) {
            updateDisplay(sends);
        }
    }
    
    private void handleScrollPosition(final int pos) {
        this.sendsScrollPosition = pos;
        if (sendsChangedAction) {
            display.temporaryInfo(1, "Sends", "%d-%d".formatted(sendsScrollPosition + 1, sendsScrollPosition + 6));
            sendsChangedAction = false;
        }
        updateDisplay(sendBank.itemCount().get());
    }
    
    @Override
    public void navigateLeft() {
        sendsChangedAction = true;
        sendBank.scrollBackwards();
    }
    
    @Override
    public void navigateRight() {
        sendsChangedAction = true;
        sendBank.scrollForwards();
    }
    
    @Override
    public boolean canScrollRight() {
        return sendBank.canScrollForwards().get();
    }
    
    @Override
    public boolean canScrollLeft() {
        return sendBank.canScrollBackwards().get();
    }
    
    @Override
    protected void onActivate() {
        updateDisplay(sendBank.itemCount().get());
    }
    
    private void updateDisplay(final int sendsCount) {
        display.setText(1, "Track");
        if (sendsCount == 0) {
            display.setText(2, "No Sends");
        } else if (sendsCount == 1) {
            display.setText(2, "Send %d".formatted(sendsScrollPosition + 1));
        } else if (sendsCount > 1) {
            display.setText(
                2, "Sends %d-%d".formatted(sendsScrollPosition + 1, Math.min(sendsScrollPosition + 6, sendsCount)));
        }
    }
    
}
