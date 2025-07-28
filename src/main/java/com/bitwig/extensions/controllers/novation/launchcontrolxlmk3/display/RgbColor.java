package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display;

public record RgbColor(int red, int green, int blue) {
    public static final RgbColor OFF = new RgbColor(0, 0, 0);
    public static final RgbColor BLUE = new RgbColor(0, 0, 0x7f);
    public static final RgbColor BLUE_LOW = new RgbColor(0, 0, 0x1f);
    public static final RgbColor BLUE_DIM = new RgbColor(0, 0, 0x2);
    public static final RgbColor LOW_WHITE = new RgbColor(0x2f, 0x2f, 0x2f);
    public static final RgbColor TURQUOISE = new RgbColor(0x20, 0x6F, 067);
    public static final RgbColor YELLOW = new RgbColor(0x4f, 0x4f, 0);
    public static final RgbColor ORANGE = new RgbColor(0x7F, 0x52, 0x00);
    public static final RgbColor RED = new RgbColor(0x7F, 0x00, 0x00);
    public static final RgbColor RED_DIM = new RgbColor(0x05, 0x00, 0x00);
    public static final RgbColor GREEN = new RgbColor(0x00, 0x7F, 0x00);
    public static final RgbColor GREEN_DIM = new RgbColor(0x0, 0x05, 0x00);
    
    public static final RgbColor RED_ORANGE = new RgbColor(0x7f, 0x20, 0x00);
    public static final RgbColor RED_ORANGE_DIM = new RgbColor(0x5, 0x2, 0x0);
    
    
}
