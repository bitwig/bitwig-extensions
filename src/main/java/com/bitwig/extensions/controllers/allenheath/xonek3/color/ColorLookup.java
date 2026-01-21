package com.bitwig.extensions.controllers.allenheath.xonek3.color;

import java.util.List;

import com.bitwig.extension.api.Color;

public class ColorLookup {
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


    private static final List<ColorMatch> COLOR_SEQ = List.of( //
        new ColorMatch(0, 0, 0, 0), //
        new ColorMatch(0, 0, 0, 0), // 0x1 Black
        new ColorMatch(1, 255, 0, 0), // 0x2 RED
        new ColorMatch(2, 255, 165, 0), // 0x3 ORANGE
        new ColorMatch(2, 165, 42, 42), // 0x4 BROWN
        new ColorMatch(3, 55, 255, 0), // 0x5 YELLOW
        new ColorMatch(4, 44, 238, 144), // 0x6 Light GREEN
        new ColorMatch(6, 0, 255, 0), // 0x7 GREEN
        new ColorMatch(7, 244, 255, 255), // 0x8 Light Cyan
        new ColorMatch(7, 0, 255, 255), // 0x9 Cyan
        new ColorMatch(9, 173, 216, 230), // 0xA Light blue
        new ColorMatch(8, 0, 0, 255), // 0xB Blue
        new ColorMatch(11, 255, 0, 255), // 0xC Magenta
        new ColorMatch(12, 255, 182, 193), // 0xD Light Pink
        new ColorMatch(12, 255, 192, 203), // 0xE  Pink
        new ColorMatch(15, 255, 255, 255), // 0xF  WHITE
        //
        new ColorMatch(3, 255, 255, 38), // 0x5 YELLOW
        new ColorMatch(8, 134, 137, 172), // 0xA LIGHT BLUE
        new ColorMatch(7, 87, 97, 198), // 0xB LIGHT BLUE
        new ColorMatch(11, 149, 73, 203), // 0xC MAGENTA
        new ColorMatch(3, 246, 246, 156), // 0x5 YELLOW
        new ColorMatch(3, 219, 188, 28), // 0x5 YELLOW
        new ColorMatch(2, 244, 168, 147), // 0x4 BROWN
        new ColorMatch(2, 163, 121, 67), // 0x4 BROWN
        new ColorMatch(6, 0, 157, 71), // 0x7 GREEN
        new ColorMatch(6, 0, 166, 148), // 0x7 GREEN
        new ColorMatch(6, 67, 210, 185), // 0x7 GREEN
        new ColorMatch(1, 217, 46, 36), // 0x2 RED
        new ColorMatch(1, 172, 35, 59), // 0x2 RED
        new ColorMatch(1, 236, 97, 87), // 0x2 RED
        new ColorMatch(2, 255, 87, 6), // 0x3 ORANGE
        new ColorMatch(2, 236, 97, 87), // 0x3 ORANGE
        new ColorMatch(2, 255, 131, 62), // 0x3 ORANGE
        new ColorMatch(12, 134, 137, 172), // 0xE  Pink
        new ColorMatch(4, 115, 152, 20), // 0x6 Light GREEN
        new ColorMatch(7, 68, 200, 255), // 0x9 Cyan
        new ColorMatch(7, 0, 153, 217), // 0x9 Cyan
        new ColorMatch(4, 160, 192, 76), // 0x6 Light GREEN
        new ColorMatch(4, 139, 232, 184), // 0x6 Light GREEN
        new ColorMatch(7, 88, 180, 186), // 0xA Light blue
        new ColorMatch(11, 78, 0, 137), // 0xC Magenta
        new ColorMatch(12, 225, 102, 145), // PINK
        new ColorMatch(8, 90, 177, 245), // Light BLUE
        new ColorMatch(8, 9, 156, 183), // Light BLUE
        new ColorMatch(9, 90, 177, 245), // BLUE
        new ColorMatch(9, 11, 115, 194), // BLUE
        new ColorMatch(9, 14, 142, 240), // BLUE
        //
        new ColorMatch(12, 209, 185, 216) // BW PINK
    );

    private static final XoneIndexColor[] colorTable = new XoneIndexColor[16];

    static {
        for (int i = 0; i < 16; i++) {
            colorTable[i] = new XoneIndexColor(i);
        }
    }

    public static XoneIndexColor toColor(final float r, final float g, final float b) {
        return colorTable[rgbToIndex(r, g, b)];
    }

    public static XoneIndexColor toColor(
        final float r, final float g, final float b, final int defaultIndex, final XoneIndexColor defaultColor) {
        final int index = rgbToIndex(r, g, b);
        if (index == defaultIndex) {
            return defaultColor;
        }
        return colorTable[index];
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
