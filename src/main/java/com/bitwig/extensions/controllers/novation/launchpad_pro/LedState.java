package com.bitwig.extensions.controllers.novation.launchpad_pro;

import com.bitwig.extension.controller.api.ColorValue;
import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

public class LedState implements InternalHardwareLightState
{
   public static final LedState OFF = new LedState();

   public static final LedState SESSION_MODE_ON = new LedState(Color.SESSION_MODE_ON);
   public static final LedState SESSION_MODE_OFF = new LedState(Color.SESSION_MODE_OFF);

   public static final LedState TRACK = new LedState(Color.TRACK);
   public static final LedState TRACK_LOW = new LedState(Color.TRACK_LOW);

   public static final LedState SCENE = new LedState(Color.SCENE);
   public static final LedState SCENE_LOW = new LedState(Color.SCENE_LOW);

   public static final LedState PAN = new LedState(Color.PAN);
   public static final LedState PAN_LOW = new LedState(Color.PAN_LOW);

   public static final LedState SENDS = new LedState(Color.SENDS);
   public static final LedState SENDS_LOW = new LedState(Color.SENDS_LOW);

   public static final LedState VOLUME = new LedState(Color.VOLUME);
   public static final LedState VOLUME_LOW = new LedState(Color.VOLUME_LOW);

   public static final LedState PLAY_MODE = new LedState(Color.PLAY_MODE);
   public static final LedState PLAY_MODE_OFF = new LedState(Color.PLAY_MODE_OFF);

   public static final LedState DRUM_SEQ_MODE = new LedState(Color.DRUM_SEQ_MODE);
   public static final LedState DRUM_SEQ_MODE_OFF = new LedState(Color.DRUM_SEQ_MODE_OFF);

   public static final LedState STEP_SEQ_MODE = new LedState(Color.STEP_SEQ_MODE);
   public static final LedState STEP_SEQ_MODE_OFF = new LedState(Color.STEP_SEQ_MODE_OFF);

   LedState()
   {
      this(Color.OFF, 0);
   }

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
      return obj instanceof LedState ? equals((LedState)obj) : false;
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
