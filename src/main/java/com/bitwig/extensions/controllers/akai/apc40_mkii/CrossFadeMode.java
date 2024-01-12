package com.bitwig.extensions.controllers.akai.apc40_mkii;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.Track;

class CrossFadeMode extends InternalHardwareLightState
{
    public static final CrossFadeMode A = new CrossFadeMode("A", 0, 1);

    public static final CrossFadeMode B = new CrossFadeMode("B", 1, 2);

    public static final CrossFadeMode AB = new CrossFadeMode("AB", 2, 0);

    public static CrossFadeMode getBestModeForColor(final Color color)
    {
        if (color == null || color.getAlpha() == 0
            || color.getRed() == 0 && color.getGreen() == 0 && color.getBlue() == 0)
            return AB;

        if (B_COLOR.equals(color))
            return B;

        return A;
    }

    public static CrossFadeMode forEnumName(final String name)
    {
        if (name.equals(A.mEnumName))
            return A;
        if (name.equals(B.mEnumName))
            return B;
        return AB;
    }

    public static CrossFadeMode forTrack(final Track track)
    {
        return forEnumName(track.crossFadeMode().get());
    }

    private static CrossFadeMode forIndex(final int index)
    {
        final CrossFadeMode mode = switch (index)
        {
        case 0 -> A;
        case 1 -> B;
        default -> AB;
        };

        assert mode.getIndex() == index;

        return mode;
    }

    private CrossFadeMode(final String enumName, final int index, final int colorIndex)
    {
        mEnumName = enumName;
        mIndex = index;
        mColorIndex = colorIndex;
    }

    public int getIndex()
    {
        return mIndex;
    }

    /** The color value we need to send to the hardware */
    public int getColorIndex()
    {
        return mColorIndex;
    }

    public CrossFadeMode getNext()
    {
        final int index = (mColorIndex + 1) % 3;

        return forIndex(index);
    }

    public String getEnumName()
    {
        return mEnumName;
    }

    @Override
    public HardwareLightVisualState getVisualState()
    {
        if (this == AB)
            return null;

        if (this == A)
            return A_VISUAL_STATE;

        return B_VISUAL_STATE;
    }

    @Override
    public boolean equals(final Object obj)
    {
        return this == obj;
    }

    private final String mEnumName;

    private final int mColorIndex, mIndex;

    private static final Color A_COLOR = Color.fromRGB(1, 0.64, 0);

    private static final Color B_COLOR = Color.fromRGB(0, 0, 1);

    private static final HardwareLightVisualState A_VISUAL_STATE = HardwareLightVisualState
        .createForColor(A_COLOR);

    private static final HardwareLightVisualState B_VISUAL_STATE = HardwareLightVisualState
        .createForColor(B_COLOR);
}
