package com.bitwig.extensions.controllers.akai.mpkmk4;

import java.util.List;

import com.bitwig.extension.api.Color;

public class MpkColorLookup {
    
    private static final List<ColorMatch> COLORS = List.of(
        new ColorMatch(0, 0x00, 0x00, 0x00), //
        new ColorMatch(2, 0x7F, 0x7F, 0x7F), //
        new ColorMatch(3, 0xFF, 0xFF, 0xFF), //
        new ColorMatch(4, 0xFF, 0x4C, 0x4C), //
        new ColorMatch(5, 0xFF, 0x00, 0x00), //
        new ColorMatch(8, 0xFF, 0xBD, 0x6C), //
        new ColorMatch(9, 0xFF, 0x54, 0x00), //
        new ColorMatch(12, 0xFF, 0xFF, 0x4C), //
        new ColorMatch(13, 0xFF, 0xFF, 0x00), //
        new ColorMatch(16, 0x88, 0xFF, 0x4C), //
        new ColorMatch(17, 0x54, 0xFF, 0x00), //
        new ColorMatch(18, 0x1D, 0x59, 0x00), //
        new ColorMatch(20, 0x4C, 0xFF, 0x4C), //
        new ColorMatch(21, 0x00, 0xFF, 0x00), //
        new ColorMatch(22, 0x00, 0x59, 0x00), //
        new ColorMatch(24, 0x4C, 0xFF, 0x5E), //
        new ColorMatch(25, 0x00, 0xFF, 0x19), //
        new ColorMatch(26, 0x00, 0x59, 0x0D), //
        new ColorMatch(28, 0x4C, 0xFF, 0x88), //
        new ColorMatch(29, 0x00, 0xFF, 0x55), //
        new ColorMatch(30, 0x00, 0x59, 0x1D), //
        
        new ColorMatch(32, 0x4C, 0xFF, 0xB7), //
        new ColorMatch(33, 0x00, 0xFF, 0x99), //
        new ColorMatch(34, 0x00, 0x59, 0x35), //
        new ColorMatch(36, 0x4C, 0xC3, 0xFF), //
        new ColorMatch(37, 0x00, 0xA9, 0xFF), //
        new ColorMatch(38, 0x00, 0x41, 0x52), //
        new ColorMatch(40, 0x4C, 0x88, 0xFF), //
        new ColorMatch(41, 0x00, 0x55, 0xFF), //
        new ColorMatch(44, 0x4C, 0x4C, 0xFF), //
        new ColorMatch(45, 0x00, 0x00, 0xFF), //
        new ColorMatch(48, 0x87, 0x4C, 0xFF), //
        new ColorMatch(49, 0x54, 0x00, 0xFF), //
        new ColorMatch(52, 0xFF, 0x4C, 0xFF), //
        new ColorMatch(53, 0xFF, 0x00, 0xFF), //
        new ColorMatch(56, 0xFF, 0x4C, 0x87), //
        new ColorMatch(57, 0xFF, 0x00, 0x54), //
        
        new ColorMatch(60, 0xFF, 0x15, 0x00), //
        new ColorMatch(61, 0x99, 0x35, 0x00), //
        new ColorMatch(62, 0x79, 0x51, 0x00), //
        new ColorMatch(63, 0x43, 0x64, 0x00), //
        new ColorMatch(64, 0x03, 0x39, 0x00), //
        new ColorMatch(65, 0x00, 0x57, 0x35), //
        new ColorMatch(66, 0x00, 0x54, 0x7F), //
        new ColorMatch(67, 0x00, 0x00, 0xFF), //
        new ColorMatch(68, 0x00, 0x45, 0x4F), //
        new ColorMatch(69, 0x25, 0x00, 0xCC), //
        
        new ColorMatch(73, 0xBD, 0xFF, 0x2D), //
        new ColorMatch(74, 0xAF, 0xED, 0x06), //
        new ColorMatch(75, 0x64, 0xFF, 0x09), //
        new ColorMatch(76, 0x10, 0x8B, 0x00), //
        new ColorMatch(77, 0x00, 0xFF, 0x87), //
        new ColorMatch(79, 0x00, 0x2A, 0xFF), //
        new ColorMatch(80, 0x3F, 0x00, 0xFF), //
        new ColorMatch(81, 0x7A, 0x00, 0xFF), //
        new ColorMatch(82, 0xB2, 0x1A, 0x7D), //
        new ColorMatch(84, 0xFF, 0x4A, 0x00), //
        new ColorMatch(85, 0x88, 0xE1, 0x06), //
        new ColorMatch(86, 0x72, 0xFF, 0x15), //
        new ColorMatch(88, 0x3B, 0xFF, 0x26), //
        new ColorMatch(90, 0x38, 0xFF, 0xCC), //
        new ColorMatch(91, 0x5B, 0x8A, 0xFF), //
        new ColorMatch(92, 0x31, 0x51, 0xC6), //
        new ColorMatch(93, 0x87, 0x7F, 0xE9), //
        new ColorMatch(94, 0xD3, 0x1D, 0xFF), //
        new ColorMatch(95, 0xFF, 0x00, 0x5D), //
        
        new ColorMatch(96, 0xFF, 0x7F, 0x00), //
        new ColorMatch(97, 0xB9, 0xB0, 0x00), //
        new ColorMatch(98, 0x90, 0xFF, 0x00), //
        new ColorMatch(101, 0x14, 0x4C, 0x10), //
        new ColorMatch(102, 0x0D, 0x50, 0x38), //
        new ColorMatch(106, 0xA8, 0x00, 0x0A), //
        new ColorMatch(107, 0xDE, 0x51, 0x3D), //
        new ColorMatch(108, 0xD8, 0x6A, 0x1C), //
        new ColorMatch(109, 0xFF, 0xE1, 0x26), //
        new ColorMatch(110, 0x9E, 0xE1, 0x2F), //
        new ColorMatch(111, 0x67, 0xB5, 0x0F), //
        new ColorMatch(113, 0xDC, 0xFF, 0x6B), //
        new ColorMatch(114, 0x80, 0xFF, 0xBD), //
        new ColorMatch(116, 0x8E, 0x66, 0xFF), //
        new ColorMatch(118, 0x75, 0x75, 0x75), //
        new ColorMatch(119, 0xE0, 0xFF, 0xFF), //
        new ColorMatch(120, 0xA0, 0x00, 0x00), //
        new ColorMatch(122, 0x1A, 0xD0, 0x00), //
        new ColorMatch(126, 0xB3, 0x5F, 0x00) //
    );
    
    private record ColorMatch(int index, int red, int green, int blue, Color color) {
        public ColorMatch(final int index, final int red, final int green, final int blue) {
            this(index, red, green, blue, Color.fromRGB255(red, green, blue));
        }
        
        public boolean isGrayTone() {
            return red == green && green == blue;
        }
        
        private double colorDistance(final float r1, final float g1, final float b1) {
            final double dr = r1 - (this.red / 255.0f);
            final double dg = g1 - (this.green / 255.0f);
            final double db = b1 - (this.blue / 255.0f);
            return 0.15f * dr * dr + 0.35f * dg * dg + 0.10f * db * db;
            //return dr * dr + dg * dg + db * db; // Euclidean distance
        }
    }
    
    public static int rgbToIndex(final float red, final float green, final float blue) {
        int closestIndex = -1;
        double minDistance = Double.MAX_VALUE;
        
        for (int i = 0; i < COLORS.size(); i++) {
            final ColorMatch c = COLORS.get(i);
            final double distance = c.colorDistance(red, green, blue);
            
            if (distance < minDistance) {
                minDistance = distance;
                closestIndex = i;
            }
        }
        final ColorMatch colorMatch = COLORS.get(closestIndex);
        return colorMatch.index();
    }
}
