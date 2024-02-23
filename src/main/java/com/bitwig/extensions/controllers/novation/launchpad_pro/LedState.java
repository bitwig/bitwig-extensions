package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.ColorValue;
import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

final class LedState extends InternalHardwareLightState
{
   static final LedState OFF = new LedState(Color.OFF, 0);

   static final LedState SESSION_MODE_ON = new LedState(Color.SESSION_MODE_ON);
   static final LedState SESSION_MODE_OFF = new LedState(Color.SESSION_MODE_OFF);

   static final LedState PAN_MODE = new LedState(Color.PAN_MODE);
   static final LedState PAN_MODE_LOW = new LedState(Color.PAN_MODE_LOW);

   static final LedState SENDS_MODE = new LedState(Color.SENDS_MODE);
   static final LedState SENDS_MODE_LOW = new LedState(Color.SENDS_MODE_LOW);

   static final LedState VOLUME_MODE = new LedState(Color.VOLUME_MODE);
   static final LedState VOLUME_MODE_LOW = new LedState(Color.VOLUME_MODE_LOW);

   static final LedState PLAY_MODE = new LedState(Color.PLAY_MODE);
   static final LedState PLAY_MODE_OFF = new LedState(Color.PLAY_MODE_OFF);

   static final LedState DRUM_SEQ_MODE = new LedState(Color.DRUM_SEQ_MODE);
   static final LedState DRUM_SEQ_MODE_OFF = new LedState(Color.DRUM_SEQ_MODE_OFF);

   static final LedState STEP_SEQ_MODE = new LedState(Color.STEP_SEQ_MODE);
   static final LedState STEP_SEQ_MODE_OFF = new LedState(Color.STEP_SEQ_MODE_OFF);

   static final LedState TRACK = new LedState(Color.TRACK);
   static final LedState TRACK_LOW = new LedState(Color.TRACK_LOW);

   static final LedState SCENE = new LedState(Color.SCENE);
   static final LedState SCENE_LOW = new LedState(Color.SCENE_LOW);

   static final LedState SHIFT_ON = new LedState(Color.SHIFT_ON);
   static final LedState SHIFT_OFF = new LedState(Color.SHIFT_OFF);

   static final LedState CLICK_ON = new LedState(Color.CLICK_ON);
   static final LedState CLICK_OFF = new LedState(Color.CLICK_OFF);

   static final LedState UNDO_ON = new LedState(Color.UNDO_ON);
   static final LedState UNDO_OFF = new LedState(Color.UNDO_OFF);

   static final LedState REC_ON = new LedState(Color.REC_ON);
   static final LedState REC_OFF = new LedState(Color.REC_OFF);

   static final LedState PLAY_ON = new LedState(Color.PLAY_ON);
   static final LedState PLAY_OFF = new LedState(Color.PLAY_OFF);

   static final LedState DELETE_ON = new LedState(Color.DELETE_ON);
   static final LedState DELETE_OFF = new LedState(Color.DELETE_OFF);

   static final LedState QUANTIZE_ON = new LedState(Color.QUANTIZE_ON);
   static final LedState QUANTIZE_OFF = new LedState(Color.QUANTIZE_OFF);

   static final LedState DUPLICATE_ON = new LedState(Color.DUPLICATE_ON);
   static final LedState DUPLICATE_OFF = new LedState(Color.DUPLICATE_OFF);

   static final LedState MUTE = new LedState(Color.MUTE);
   static final LedState MUTE_LOW = new LedState(Color.MUTE_LOW);

   static final LedState SOLO = new LedState(Color.SOLO);
   static final LedState SOLO_LOW = new LedState(Color.SOLO_LOW);

   static final LedState STOP_CLIP_ON = new LedState(Color.STOP_CLIP_ON);
   static final LedState STOP_CLIP_OFF = new LedState(Color.STOP_CLIP_OFF);

   static final LedState STEP_HOLD = new LedState(Color.STEP_HOLD);
   static final LedState STEP_PLAY_HEAD = new LedState(Color.STEP_PLAY_HEAD);
   static final LedState STEP_PLAY = new LedState(Color.STEP_PLAY);
   static final LedState STEP_ON = new LedState(Color.STEP_ON);
   static final LedState STEP_SUSTAIN = new LedState(Color.STEP_SUSTAIN);
   static final LedState STEP_OFF = new LedState(Color.STEP_OFF);

   static final LedState PITCH = new LedState(Color.PITCH);
   static final LedState PITCH_LOW = new LedState(Color.PITCH_LOW);

   final static LedState ROOT_KEY_COLOR = new LedState(Color.fromRgb255(11, 100, 63));
   final static LedState USED_KEY_COLOR = new LedState(Color.fromRgb255(255, 240, 240));
   final static LedState UNUSED_KEY_COLOR = new LedState(Color.fromRgb255(40, 40, 40));
   final static LedState SCALE_ON_COLOR = new LedState(Color.fromRgb255(50, 167, 202));
   final static LedState SCALE_OFF_COLOR = new LedState(Color.scale(SCALE_ON_COLOR.mColor, 0.2f));

   static final int NO_PULSE = 0;
   static final int PULSE_PLAYING = 88;
   static final int PULSE_RECORDING = 72;
   static final int PULSE_PLAYBACK_QUEUED = 89;
   static final int PULSE_RECORDING_QUEUED = 56;
   static final int PULSE_STOP_QUEUED = 118;

   LedState(final ColorValue color)
   {
      this(new Color(color));
   }

   LedState(final Color color)
   {
      this(color, NO_PULSE);
   }

   LedState(final com.bitwig.extension.api.Color color)
   {
      this(new Color(color));
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

   @Override
   public boolean equals(final Object obj)
   {
      return obj instanceof LedState && equals((LedState) obj);
   }

   boolean equals(final LedState obj)
   {
      if (obj == this)
         return true;

      return mColor.equals(obj.mColor) && mPulse == obj.mPulse;
   }

   final Color mColor;

   final int mPulse;
}
