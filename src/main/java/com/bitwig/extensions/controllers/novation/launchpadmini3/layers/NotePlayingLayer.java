package com.bitwig.extensions.controllers.novation.launchpadmini3.layers;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.PlayingNote;
import com.bitwig.extensions.controllers.novation.commonsmk3.MidiProcessor;
import com.bitwig.extensions.controllers.novation.commonsmk3.ViewCursorControl;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Inject;
import com.bitwig.extensions.framework.di.PostConstruct;

public class NotePlayingLayer extends Layer {
    @Inject
    private ViewCursorControl viewCursorControl;
    @Inject
    private MidiProcessor midiProcessor;
    
    private final List<Integer> lastNotes = new ArrayList<>();
    
    public NotePlayingLayer(final Layers layers) {
        super(layers, "NOTE_PLAYING_LAYER");
    }
    
    @PostConstruct
    public void init() {
        final CursorTrack cursorTrack = viewCursorControl.getCursorTrack();
        cursorTrack.playingNotes().addValueObserver(this::handleNotes);
    }
    
    private void handleNotes(final PlayingNote[] playingNotes) {
        for (final Integer playing : lastNotes) {
            midiProcessor.sendMidi(0x8f, playing, 21);
        }
        lastNotes.clear();
        for (final PlayingNote note : playingNotes) {
            midiProcessor.sendMidi(0x9f, note.pitch(), 21);
            lastNotes.add(note.pitch());
        }
    }
}
