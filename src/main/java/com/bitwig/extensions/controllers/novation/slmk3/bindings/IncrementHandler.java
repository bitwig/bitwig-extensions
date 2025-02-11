package com.bitwig.extensions.controllers.novation.slmk3.bindings;

import java.util.function.IntConsumer;

import com.bitwig.extensions.controllers.novation.slmk3.sequencer.value.IncBuffer;

public class IncrementHandler implements IntConsumer {
    private final IncBuffer incBuffer;
    private final IntConsumer incReceiver;
    
    public IncrementHandler(final IntConsumer receiver, final int incBuffer) {
        this.incBuffer = incBuffer == 0 ? null : new IncBuffer(incBuffer);
        this.incReceiver = receiver;
    }
    
    
    @Override
    public void accept(final int value) {
        final int increment = incBuffer == null ? value : incBuffer.inc(value);
        if (increment == 0) {
            return;
        }
        incReceiver.accept(value);
    }
}
