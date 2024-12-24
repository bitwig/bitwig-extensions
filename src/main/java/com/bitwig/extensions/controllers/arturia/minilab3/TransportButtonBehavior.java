package com.bitwig.extensions.controllers.arturia.minilab3;

public enum TransportButtonBehavior
{
    LOOP, TAP, METRONOME, UNDO;

    public String displayName()
    {
        return name().substring(0, 1).toUpperCase() + name().substring(1).toLowerCase();
    }
}
