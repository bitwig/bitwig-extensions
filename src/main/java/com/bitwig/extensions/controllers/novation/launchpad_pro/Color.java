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

   static final Color PAN_MODE = fromRgb255(255, 85, 127);
   static final Color PAN_MODE_LOW = scale(PAN_MODE, 0.1f);

   static final Color SENDS_MODE = new Color(1.f, 1.f, 0.f);
   static final Color SENDS_MODE_LOW = new Color(SENDS_MODE, 0.1f);

   static final Color TRACK = CYAN;
   static final Color TRACK_LOW = new Color(TRACK, 0.1f);

   static final Color SCENE = WHITE;
   static final Color SCENE_LOW = new Color(SCENE, 0.1f);

   static final Color VOLUME_MODE = Color.fromRgb255(126, 87, 20);
   static final Color VOLUME_MODE_LOW = Color.scale(VOLUME_MODE, 0.1f);

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

   static final Color SESSION_MODE_ON = new Color(1.f, 1.f, 0.f);
   static final Color SESSION_MODE_OFF = new Color(SESSION_MODE_ON, 0.1f);

   final static Color PLAY_MODE = Color.fromRgb255(11, 100, 63);
   final static Color PLAY_MODE_OFF = Color.scale(PLAY_MODE, 0.2f);

   final static Color DRUM_SEQ_MODE = Color.fromRgb255(255, 183, 0);
   final static Color DRUM_SEQ_MODE_OFF = new Color(DRUM_SEQ_MODE, .1f);

   static final Color STEP_SEQ_MODE = Color.fromRgb255(255, 183, 0);
   static final Color STEP_SEQ_MODE_OFF = new Color(STEP_SEQ_MODE, .1f);

   static final Color SHIFT_ON = WHITE;
   static final Color SHIFT_OFF = WHITE_LOW;

   static final Color CLICK_ON = YELLOW;
   static final Color CLICK_OFF = YELLOW_LOW;

   static final Color UNDO_ON = ORANGE;
   static final Color UNDO_OFF = ORANGE_LOW;

   static final Color REC_ON = RED;
   static final Color REC_OFF = RED_LOW;

   static final Color PLAY_ON = GREEN;
   static final Color PLAY_OFF = GREEN_LOW;

   static final Color DELETE_ON = ORANGE;
   static final Color DELETE_OFF = ORANGE_LOW;

   static final Color QUANTIZE_ON = ORANGE;
   static final Color QUANTIZE_OFF = ORANGE_LOW;

   static final Color DUPLICATE_ON = ORANGE;
   static final Color DUPLICATE_OFF = ORANGE_LOW;

   static final Color STOP_CLIP_ON = Color.fromRgb255(180, 180, 180);
   static final Color STOP_CLIP_OFF = Color.scale(STOP_CLIP_ON, 0.3f);

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
      return this == color || (mRed == color.mRed && mGreen == color.mGreen && mBlue == color.mBlue);
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
