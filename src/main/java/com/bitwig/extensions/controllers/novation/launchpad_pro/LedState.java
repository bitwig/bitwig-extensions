package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.ColorValue;
import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

public class LedState extends InternalHardwareLightState
{
   public static final LedState OFF = new LedState(Color.OFF, 0);

   public static final LedState SESSION_MODE_ON = new LedState(Color.SESSION_MODE_ON);
   public static final LedState SESSION_MODE_OFF = new LedState(Color.SESSION_MODE_OFF);

   public static final LedState PAN_MODE = new LedState(Color.PAN_MODE);
   public static final LedState PAN_MODE_LOW = new LedState(Color.PAN_MODE_LOW);

   public static final LedState SENDS_MODE = new LedState(Color.SENDS_MODE);
   public static final LedState SENDS_MODE_LOW = new LedState(Color.SENDS_MODE_LOW);

   public static final LedState VOLUME_MODE = new LedState(Color.VOLUME_MODE);
   public static final LedState VOLUME_MODE_LOW = new LedState(Color.VOLUME_MODE_LOW);

   public static final LedState PLAY_MODE = new LedState(Color.PLAY_MODE);
   public static final LedState PLAY_MODE_OFF = new LedState(Color.PLAY_MODE_OFF);

   public static final LedState DRUM_SEQ_MODE = new LedState(Color.DRUM_SEQ_MODE);
   public static final LedState DRUM_SEQ_MODE_OFF = new LedState(Color.DRUM_SEQ_MODE_OFF);

   public static final LedState STEP_SEQ_MODE = new LedState(Color.STEP_SEQ_MODE);
   public static final LedState STEP_SEQ_MODE_OFF = new LedState(Color.STEP_SEQ_MODE_OFF);

   public static final LedState TRACK = new LedState(Color.TRACK);
   public static final LedState TRACK_LOW = new LedState(Color.TRACK_LOW);

   public static final LedState SCENE = new LedState(Color.SCENE);
   public static final LedState SCENE_LOW = new LedState(Color.SCENE_LOW);

   public static final LedState SHIFT_ON = new LedState(Color.SHIFT_ON);
   public static final LedState SHIFT_OFF = new LedState(Color.SHIFT_OFF);

   public static final LedState CLICK_ON = new LedState(Color.CLICK_ON);
   public static final LedState CLICK_OFF = new LedState(Color.CLICK_OFF);

   public static final LedState UNDO_ON = new LedState(Color.UNDO_ON);
   public static final LedState UNDO_OFF = new LedState(Color.UNDO_OFF);

   public static final LedState REC_ON = new LedState(Color.REC_ON);
   public static final LedState REC_OFF = new LedState(Color.REC_OFF);

   public static final LedState PLAY_ON = new LedState(Color.PLAY_ON);
   public static final LedState PLAY_OFF = new LedState(Color.PLAY_OFF);

   public static final LedState DELETE_ON = new LedState(Color.DELETE_ON);
   public static final LedState DELETE_OFF = new LedState(Color.DELETE_OFF);

   public static final LedState QUANTIZE_ON = new LedState(Color.QUANTIZE_ON);
   public static final LedState QUANTIZE_OFF = new LedState(Color.QUANTIZE_OFF);

   public static final LedState DUPLICATE_ON = new LedState(Color.DUPLICATE_ON);
   public static final LedState DUPLICATE_OFF = new LedState(Color.DUPLICATE_OFF);

   public static final LedState MUTE = new LedState(Color.MUTE);
   public static final LedState MUTE_LOW = new LedState(Color.MUTE_LOW);

   public static final LedState SOLO = new LedState(Color.SOLO);
   public static final LedState SOLO_LOW = new LedState(Color.SOLO_LOW);

   public static final LedState STOP_CLIP_ON = new LedState(Color.STOP_CLIP_ON);
   public static final LedState STOP_CLIP_OFF = new LedState(Color.STOP_CLIP_OFF);

   public static final LedState STEP_HOLD = new LedState(Color.STEP_HOLD);
   public static final LedState STEP_PLAY_HEAD = new LedState(Color.STEP_PLAY_HEAD);
   public static final LedState STEP_PLAY = new LedState(Color.STEP_PLAY);
   public static final LedState STEP_ON = new LedState(Color.STEP_ON);
   public static final LedState STEP_SUSTAIN = new LedState(Color.STEP_SUSTAIN);
   public static final LedState STEP_OFF = new LedState(Color.STEP_OFF);

   public static final LedState PITCH = new LedState(Color.PITCH);
   public static final LedState PITCH_LOW = new LedState(Color.PITCH_LOW);

   public LedState(final ColorValue color)
   {
      this(new Color(color));
   }

   LedState(final Color color)
   {
      this(color, 0);
   }

   LedState(final Color color, final int pulse)
   {
      mColor = color;
      mPulse = pulse;
   }

   @Override
   public HardwareLightVisualState getVisualState()
   {
      return HardwareLightVisualState.createForColor(mColor.toApiColor());
   }

   public Color getColor()
   {
      return mColor;
   }

   public int getPulse()
   {
      return mPulse;
   }

   @Override
   public boolean equals(final Object obj)
   {
      return obj instanceof LedState && equals((LedState) obj);
   }
   public boolean equals(final LedState obj)
   {
      if (obj == this)
         return true;

      return mColor.equals(obj.mColor) && mPulse == obj.mPulse;
   }

   private final Color mColor;

   private final int mPulse;
}
