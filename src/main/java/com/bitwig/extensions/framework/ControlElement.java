package com.bitwig.extensions.framework;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extensions.framework.targets.Target;

public interface ControlElement<T extends Target>
{
   void onMidi(final T target, final ShortMidiMessage data);

   void flush(final T target, final LayeredControllerExtension extension);
}
