package com.bitwig.extensions.controllers.presonus;

import com.bitwig.extension.controller.api.MidiOut;

public interface Flushable
{
   void flush(final MidiOut midiOut);
}
