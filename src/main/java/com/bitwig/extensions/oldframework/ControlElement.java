package com.bitwig.extensions.oldframework;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extensions.oldframework.targets.Target;

public interface ControlElement<T extends Target>
{
   void onMidi(final T target, final ShortMidiMessage data);

   void flush(final T target, final LayeredControllerExtension extension);
}
