package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.ColorValue;

final class Color
{
   static final Color OFF = Color.fromRgb255(0, 0, 0);

   static final Color BLUE = new Color(0.f, 0.f, 1.f);
   static final Color BLUE_LOW = new Color(BLUE, 0.1f);

   static final Color GREEN = new Color(0.f, 1.f, 0.f);
   static final Color GREEN_LOW = new Color(GREEN, 0.1f);

   static final Color RED = new Color(1.f, 0.f, 0.f);
   static final Color RED_LOW = new Color(RED, 0.1f);

   static final Color WHITE = new Color(1.f, 1.f, 1.f);
   static final Color WHITE_LOW = new Color(WHITE, 0.1f);

   static final Color YELLOW = new Color(1.f, 1.f, 0.f);
   static final Color YELLOW_LOW = new Color(YELLOW, 0.1f);

   static final Color ORANGE = new Color(1.f, .3f, .0f);
   static final Color ORANGE_LOW = new Color(ORANGE, 0.1f);

   static final Color CYAN = new Color(.0f, 1.f, 1.f);
   static final Color CYAN_LOW = new Color(CYAN, .1f);

   static final Color PURPLE = Color.fromRgb255(219, 3, 252);
   static final Color PURPLE_LOW = Color.scale(PURPLE, 0.1f);

   static final Color PAN = fromRgb255(255, 85, 127);
   static final Color PAN_LOW = scale(PAN, 0.1f);

   static final Color SEND = new Color(1.f, 1.f, 0.f);
   static final Color SEND_LOW = new Color(SEND, 0.1f);

   static final Color TRACK = CYAN;
   static final Color TRACK_LOW = new Color(TRACK, 0.1f);

   static final Color SCENE = WHITE;
   static final Color SCENE_LOW = new Color(SCENE, 0.1f);

   static final Color VOLUME = Color.fromRgb255(126, 87, 20);
   static final Color VOLUME_LOW = Color.scale(VOLUME, 0.1f);

   static final Color SOLO = YELLOW;
   static final Color SOLO_LOW = Color.scale(SOLO, 0.1f);

   static final Color PITCH = ORANGE;
   static final Color PITCH_LOW = new Color(PITCH, 0.1f);

   static final Color MUTE = Color.fromRgb255(255, 85, 0);
   static final Color MUTE_LOW = Color.scale(MUTE, 0.1f);

   static final Color STEP_HOLD = YELLOW;
   static final Color STEP_PLAY_HEAD = WHITE;
   static final Color STEP_PLAY = GREEN;
   static final Color STEP_ON = Color.fromRgb255(255, 255, 127);
   static final Color STEP_SUSTAIN = Color.scale(STEP_ON, 0.1f);
   static final Color STEP_OFF = OFF;

   Color(final float red, final float green, final float blue)
   {
      assert red >= 0;
      assert red <= 1.0f;
      assert green >= 0;
      assert green <= 1.0f;
      assert blue >= 0;
      assert blue <= 1.0f;

      mRed = (byte)(63 * red);
      mGreen = (byte)(63 * green);
      mBlue = (byte)(63 * blue);
   }

   public Color(final byte red, final byte green, final byte blue)
   {
      mRed = red;
      mGreen = green;
      mBlue = blue;
   }

   public Color(final ColorValue value)
   {
      this(value.red(), value.green(), value.blue());
   }

   public Color(final Color color, final float scale)
   {
      mRed = (byte) (color.mRed * scale);
      mGreen = (byte) (color.mGreen * scale);
      mBlue = (byte) (color.mBlue * scale);
   }

   static Color fromRgb255(final int r, final int g, final int b)
   {
      return new Color(r / 255.0f, g / 255.0f, b / 255.0f);
   }

   public byte getRed()
   {
      return mRed;
   }

   public byte getGreen()
   {
      return mGreen;
   }

   public byte getBlue()
   {
      return mBlue;
   }

   public boolean equals(final Color color)
   {
      return mRed == color.mRed && mGreen == color.mGreen && mBlue == color.mBlue;
   }

   public static Color scale(final Color color, final float scale)
   {
      return new Color((byte) (color.mRed * scale),
         (byte) (color.mGreen * scale),
         (byte) (color.mBlue * scale));
   }

   public boolean isBlack()
   {
      return mRed == 0 && mGreen == 0 && mBlue == 0;
   }

   public com.bitwig.extension.api.Color toApiColor()
   {
      return com.bitwig.extension.api.Color.fromRGB255(mRed, mGreen, mBlue);
   }

   public int toInt24()
   {
      return (mRed << 16) | (mGreen << 8) | (mBlue);
   }

   static Color fromInt24(final int value)
   {
      int red = (value >> 16) & 0xff;
      int green = (value >> 8) & 0xff;
      int blue = value & 0xff;

      return Color.fromRgb255(4 * red, 4 * green, 4 * blue);
   }

   private final byte mRed;
   private final byte mGreen;
   private final byte mBlue;
}
