package com.bitwig.extensions.controllers.akai.mpkmk4;

public enum MpkVariant {
    MINI((byte) 0x5D, false, LayoutStyle.STANDARD),
    MINI_PLUS((byte) 0x03, true, LayoutStyle.NEW_STYLE);
    
    public enum LayoutStyle {
        STANDARD,
        NEW_STYLE
    }
    
    private final byte productId;
    private final boolean padReverse;
    private final LayoutStyle layoutStyle;
    
    MpkVariant(byte productId, boolean padReverse, LayoutStyle layoutStyle) {
        this.productId = productId;
        this.padReverse = padReverse;
        this.layoutStyle = layoutStyle;
    }
    
    public boolean isPadReverse() {
        return padReverse;
    }
    
    public LayoutStyle getLayoutStyle() {
        return layoutStyle;
    }
    
    public MpkSysExCommands buildCommands() {
        return new MpkSysExCommands(productId);
    }
}
