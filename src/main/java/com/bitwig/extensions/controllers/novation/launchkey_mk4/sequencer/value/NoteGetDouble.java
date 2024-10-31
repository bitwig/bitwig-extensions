package com.bitwig.extensions.controllers.novation.launchkey_mk4.sequencer.value;

import com.bitwig.extension.controller.api.NoteStep;

@FunctionalInterface
public interface NoteGetDouble {
    double get(NoteStep note);
}
