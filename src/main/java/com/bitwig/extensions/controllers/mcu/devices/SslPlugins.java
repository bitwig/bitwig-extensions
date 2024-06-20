package com.bitwig.extensions.controllers.mcu.devices;

public enum SslPlugins {
    
    S4K_B("56535453344B4273736C20346B206200"),
    S4K_E("56535453344B4573736C20346B206500"),
    METER("5653544D54524273736C206D65746572"),
    BUS_COMPRESSOR("5653544E42433273736C206E61746976"),
    CHANNEL_STRIP("5653544E43533273736C206E61746976"),
    LINK_360("5653543336304C73736C20333630206C");
    
    private final String id;
    
    SslPlugins(final String id) {
        this.id = id;
    }
    
    public String getId() {
        return id;
    }
    
}
