package com.bitwig.extensions.controllers.neuzeitinstruments;

import java.util.List;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

public class DropColor extends InternalHardwareLightState {
    
    private final int colorIndex;
    
    private final static int MASK_PLAYING = 0x40;
    private final static int MASK_RECORDING = 0x20;
    private final static int MASK_TRIGGERED = 0x10;
    
    private static final List<ColorMatch> COLOR_SEQ = List.of( //
        new ColorMatch(0, 0, 0, 0), //
        new ColorMatch(1, 0, 0, 0), // 0x1 Black
        new ColorMatch(2, 255, 0, 0), // 0x2 RED
        new ColorMatch(3, 255, 165, 0), // 0x3 ORANGE
        new ColorMatch(4, 165, 42, 42), // 0x4 BROWN
        new ColorMatch(5, 55, 255, 0), // 0x5 YELLOW
        new ColorMatch(6, 44, 238, 144), // 0x6 Light GREEN
        new ColorMatch(7, 0, 255, 0), // 0x7 GREEN
        new ColorMatch(8, 244, 255, 255), // 0x8 Light Cyan
        new ColorMatch(9, 0, 255, 255), // 0x9 Cyan
        new ColorMatch(10, 173, 216, 230), // 0xA Light blue
        new ColorMatch(11, 0, 0, 255), // 0xB Blue
        new ColorMatch(12, 255, 0, 255), // 0xC Magenta
        new ColorMatch(13, 255, 182, 193), // 0xD Light Pink
        new ColorMatch(14, 255, 192, 203), // 0xE  Pink
        new ColorMatch(15, 255, 255, 255), // 0xF  WHITE
        //
        new ColorMatch(5, 255, 255, 38), // 0x5 YELLOW
        new ColorMatch(10, 134, 137, 172), // 0xA LIGHT BLUE
        new ColorMatch(11, 87, 97, 198), // 0xB LIGHT BLUE
        new ColorMatch(12, 149, 73, 203), // 0xC MAGENTA
        new ColorMatch(5, 246, 246, 156), // 0x5 YELLOW
        new ColorMatch(5, 219, 188, 28), // 0x5 YELLOW
        new ColorMatch(4, 244, 168, 147), // 0x4 BROWN
        new ColorMatch(4, 163, 121, 67), // 0x4 BROWN
        new ColorMatch(7, 0, 157, 71), // 0x7 GREEN
        new ColorMatch(7, 0, 166, 148), // 0x7 GREEN
        new ColorMatch(7, 67, 210, 185), // 0x7 GREEN
        new ColorMatch(2, 217, 46, 36), // 0x2 RED
        new ColorMatch(2, 172, 35, 59), // 0x2 RED
        new ColorMatch(2, 236, 97, 87), // 0x2 RED
        new ColorMatch(3, 255, 87, 6), // 0x3 ORANGE
        new ColorMatch(3, 236, 97, 87), // 0x3 ORANGE
        new ColorMatch(3, 255, 131, 62), // 0x3 ORANGE
        new ColorMatch(11, 134, 137, 172), // 0xE  Pink
        new ColorMatch(6, 115, 152, 20), // 0x6 Light GREEN
        new ColorMatch(9, 68, 200, 255), // 0x9 Cyan
        new ColorMatch(9, 0, 153, 217), // 0x9 Cyan
        new ColorMatch(6, 160, 192, 76), // 0x6 Light GREEN
        new ColorMatch(6, 139, 232, 184), // 0x6 Light GREEN
        new ColorMatch(10, 88, 180, 186), // 0xA Light blue
        new ColorMatch(12, 78, 0, 137), // 0xC Magenta
        new ColorMatch(14, 225, 102, 145), // PINK
        new ColorMatch(10, 90, 177, 245), // Light BLUE
        new ColorMatch(10, 9, 156, 183), // Light BLUE
        new ColorMatch(11, 90, 177, 245), // BLUE
        new ColorMatch(11, 11, 115, 194), // BLUE
        new ColorMatch(11, 14, 142, 240), // BLUE
        //
        new ColorMatch(13, 209, 185, 216) // BW PINK
    );
    
    public static final DropColor OFF = new DropColor(1);
    public static final DropColor RED = new DropColor(2);
    public static final DropColor GREEN = new DropColor(7);
    public static final DropColor BLUE = new DropColor(11);
    public static final DropColor WHITE = new DropColor(0xF);
    
    
    private record ColorMatch(int index, int red, int green, int blue, Color color) {
        
        public ColorMatch(final int index, final int red, final int green, final int blue) {
            this(index, red, green, blue, Color.fromRGB255(red, green, blue));
        }
        
        private double colorDistance(final double r1, final double g1, final double b1) {
            final double dr = r1 - (this.red / 255.0);
            final double dg = g1 - (this.green / 255.0);
            final double db = b1 - (this.blue / 255.0);
            
            return Math.sqrt(dr * dr + dg * dg + db * db); // Euclidean distance
        }
    }
    
    private final static DropColor[] colors = new DropColor[128];
    
    static {
        for (int i = 0; i < 128; i++) {
            colors[i] = new DropColor(i);
        }
    }
    
    private DropColor(final int colorIndex) {
        this.colorIndex = colorIndex;
    }
    
    public static DropColor fromRgb(final double r, final double g, final double b) {
        final int index = rgbToIndex(r, g, b);
        return colors[index];
    }
    
    public static DropColor fromRgb(final double r, final double g, final double b, final DropColor defaultColor) {
        if (r == g && g == b) {
            if ((int) Math.round(r * 255) == 80) {
                return defaultColor;
            }
        }
        return colors[rgbToIndex(r, g, b)];
    }
    
    public DropColor playing() {
        return colors[this.colorIndex & 0xF | MASK_PLAYING];
    }
    
    public DropColor recording() {
        return colors[this.colorIndex & 0xF | MASK_RECORDING];
    }
    
    public DropColor triggered() {
        return colors[this.colorIndex & 0xF | MASK_TRIGGERED];
    }
    
    @Override
    public HardwareLightVisualState getVisualState() {
        if (colorIndex == 0) {
            return null;
        }
        return HardwareLightVisualState.createForColor(COLOR_SEQ.get(colorIndex & 0xF).color());
    }
    
    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DropColor dropColor = (DropColor) o;
        return colorIndex == dropColor.colorIndex;
    }
    
    public int getColorIndex() {
        return colorIndex;
    }
    
    @Override
    public int hashCode() {
        return colorIndex;
    }
    
    public static int rgbToIndex(final double red, final double green, final double blue) {
        int closestIndex = -1;
        double minDistance = Double.MAX_VALUE;
        
        if (red == green && green == blue) {
            return 15;
        }
        
        for (int i = 1; i < COLOR_SEQ.size(); i++) {
            final ColorMatch c = COLOR_SEQ.get(i);
            final double distance = c.colorDistance(red, green, blue);
            
            if (distance < minDistance) {
                minDistance = distance;
                closestIndex = i;
            }
        }
        
        return COLOR_SEQ.get(closestIndex).index();
    }
    
    
}
