package com.bitwig.extensions.controllers.mcu;

public enum VPotMode {
    // TRACK(NoteOnAssignment.V_TRACK, Assign.MIXER), //
    ALL_SENDS(BitwigType.CHANNEL), //
    SEND(BitwigType.CHANNEL), //
    PAN(BitwigType.CHANNEL), //
    DEVICE(BitwigType.DEVICE), // Standard Device MODE
    PLUGIN(BitwigType.DEVICE, "audio-effect", "DEVICE", "", "AudioFX"),
    EQ(BitwigType.SPEC_DEVICE, "EQ+ device", "EQ", "EQ+", "EQ+"), //
    INSTRUMENT(BitwigType.DEVICE, "instrument", "INSTRUMENT", "INSTRUMENT", "Synth"),
    MIDI_EFFECT(BitwigType.DEVICE, "note-effect", "NOTEFX", "", "NoteFX"),
    TRACK_REMOTE(BitwigType.REMOTE, "track-remotes", "TRACK_REMOTE", "", "TrackRemote"),
    PROJECT_REMOTE(BitwigType.REMOTE, "project-remotes", "PROJECT_REMOTE", "", "ProjectRemote"),
    ARPEGGIATOR(BitwigType.SPEC_DEVICE, "note-effect", "ALT+INSTRUMENT");
    
    
    private final BitwigType assign;
    private final String typeName;
    private final String deviceName;
    private final String typeDisplayName;
    private final String buttonDescription;
    private final String description;
    
    public enum BitwigType {
        CHANNEL,
        DEVICE,
        SPEC_DEVICE,
        REMOTE
    }
    
    VPotMode(final BitwigType assign) {
        this(assign, null, null);
    }
    
    VPotMode(final BitwigType assign, final String typeName, final String buttonDescription) {
        this(assign, typeName, buttonDescription, null, null);
    }
    
    VPotMode(final BitwigType assign, final String typeName, final String buttonDescription, final String deviceName,
        final String description) {
        this.assign = assign;
        this.typeName = typeName;
        this.deviceName = deviceName;
        if (this.typeName != null && this.typeName.length() > 1) {
            typeDisplayName = this.typeName.substring(0, 1).toUpperCase() + this.typeName.substring(1);
        } else {
            typeDisplayName = null;
        }
        this.buttonDescription = buttonDescription;
        this.description = description;
    }
    
    public BitwigType getAssign() {
        return assign;
    }
    
    public String getTypeName() {
        return typeName;
    }
    
    public String getDescription() {
        return description;
    }
    
}
