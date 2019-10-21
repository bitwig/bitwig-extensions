package com.bitwig.extensions.framework;

import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extensions.framework.targets.Target;

public interface ControlElement<T extends Target>
{
   void onMidi(final T target, final int status, final int data1, final int data2);

   void flush(final T target, final MidiOut midiOut);
}
