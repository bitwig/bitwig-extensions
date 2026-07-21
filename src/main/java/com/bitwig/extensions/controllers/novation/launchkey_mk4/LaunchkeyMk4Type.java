package com.bitwig.extensions.controllers.novation.launchkey_mk4;

public enum LaunchkeyMk4Type {
    STANDARD((byte) 0x14),
    MINI((byte) 0x13);
    
    private final byte sysExCode;
    
    LaunchkeyMk4Type(final byte sysExCode) {
        this.sysExCode = sysExCode;
    }
    
    public byte getSysExCode() {
        return sysExCode;
    }
}
