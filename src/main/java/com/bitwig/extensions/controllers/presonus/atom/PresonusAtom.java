package com.bitwig.extensions.controllers.presonus.atom;

import com.bitwig.extension.controller.api.Action;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.HardwareControlType;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.PlayingNote;
import com.bitwig.extension.controller.api.SettableColorValue;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.framework.targets.ButtonTarget;
import com.bitwig.extensions.framework.LayeredControllerExtension;
import com.bitwig.extensions.framework.targets.EncoderTarget;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.targets.RGBButtonTarget;
import com.bitwig.extensions.controllers.presonus.util.NoteInputUtils;

public class PresonusAtom extends LayeredControllerExtension
{
   final static int CC_ENCODER_1 = 0x0E;
   final static int CC_ENCODER_2 = 0x0F;
   final static int CC_ENCODER_3 = 0x10;
   final static int CC_ENCODER_4 = 0x11;

   final static int CC_SHIFT = 0x20;
   final static int CC_NOTE_REPEAT = 0x18;
   final static int CC_FULL_LEVEL = 0x19;
   final static int CC_BANK_TRANSPOSE = 0x1A;
   final static int CC_PRESET_PAD_SELECT= 0x1B;
   final static int CC_SHOW_HIDE= 0x1D;
   final static int CC_NUDGE_QUANTIZE= 0x1E;
   final static int CC_EDITOR = 0x1F;
   final static int CC_SET_LOOP = 0x55;
   final static int CC_SETUP = 0x56;
   final static int CC_UP = 0x57;
   final static int CC_DOWN = 0x59;
   final static int CC_LEFT = 0x5A;
   final static int CC_RIGHT = 0x66;
   final static int CC_SELECT = 0x67;
   final static int CC_ZOOM = 0x68;
   final static int CC_CLICK_COUNT_IN = 0x69;
   final static int CC_RECORD_SAVE= 0x6B;
   final static int CC_PLAY_LOOP_TOGGLE = 0x6D;
   final static int CC_STOP_UNDO = 0x6F;

   float[] WHITE = {1,1,1};
   float[] DIM_WHITE = {0.3f,0.3f,0.3f};


   public PresonusAtom(
      final PresonusAtomDefinition definition,
      final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();
      mApplication = host.createApplication();

      final MidiIn midiIn = host.getMidiInPort(0);

      midiIn.setMidiCallback(getMidiCallbackToUseForLayers());
      mNoteInput = midiIn.createNoteInput("Pads", "80????", "90????", "a0????");
      mNoteInput.setShouldConsumeEvents(true);

      mMidiOut = host.getMidiOutPort(0);

      mCursorTrack = host.createCursorTrack(0, 0);

      mCursorDevice =
         mCursorTrack.createCursorDevice("ATOM", "Atom", 0, CursorDeviceFollowMode.FIRST_INSTRUMENT);

      mRemoteControls = mCursorDevice.createCursorRemoteControlsPage(4);
      mRemoteControls.setHardwareLayout(HardwareControlType.ENCODER, 4);
      for (int i = 0; i < 4; ++i)
         mRemoteControls.getParameter(i).setIndication(true);

      mTransport = host.createTransport();

      mStepsLayer = createStepsMode();

      mCursorClip = host.createLauncherCursorClip(16, 1);
      mCursorClip.color().markInterested();

      initPads();
      initButtons();
      initEncoders();

      // Turn on Native Mode
      mMidiOut.sendMidi(0x8f, 0, 127);
   }

   @Override
   public void exit()
   {
      // Turn off Native Mode
      mMidiOut.sendMidi(0x8f, 0, 0);
   }

   private Layer createStepsMode()
   {
      return new Layer()
      {
         @Override
         public void setActivate(final boolean active)
         {
            if (active)
            {
               mNoteInput.setShouldConsumeEvents(false);
               mNoteInput.setKeyTranslationTable(NoteInputUtils.NO_NOTES);
               mCursorClip.scrollToKey(36 + mCurrentPadForSteps & 0x15);
               mCursorClip.scrollToStep(0);
            }
         }
      };
   }

   private Layer createBaseLayer()
   {
      return new Layer()
      {
         @Override
         public void setActivate(final boolean active)
         {
            if (active)
            {
               mNoteInput.setShouldConsumeEvents(true);
               mNoteInput.setKeyTranslationTable(NoteInputUtils.ALL_NOTES);
            }
         }
      };
   }

   @Override
   protected MidiOut getMidiOut()
   {
      return mMidiOut;
   }

   private int velocityForPlayingNote(int padIndex)
   {
      if (mPlayingNotes != null)
      {
         for (PlayingNote playingNote : mPlayingNotes)
         {
            if (playingNote.pitch() == 36 + padIndex)
            {
               return playingNote.velocity();
            }
         }
      }

      return 0;
   }

   private void initPads()
   {
      mDrumPadBank = mCursorDevice.createDrumPadBank(16);
      mDrumPadBank.exists().markInterested();
      mCursorTrack.color().markInterested();

      mDrumPadColors = new float[16][3];

      Pad[] pads = new Pad[16];
      final float darken = 0.7f;

      for(int i=0; i<16; i++)
      {
         final int padIndex = i;
         Pad pad = addElement(new Pad(padIndex));
         pads[padIndex] = pad;
         DrumPad drumPad = mDrumPadBank.getItemAt(padIndex);
         drumPad.exists().markInterested();
         SettableColorValue color = drumPad.color();
         color.addValueObserver((r,g,b) ->
            {
               mDrumPadColors[padIndex][0] = r * darken;
               mDrumPadColors[padIndex][1] = g * darken;
               mDrumPadColors[padIndex][2] = b * darken;
            });

         mBaseLayer.bind(pad, new RGBButtonTarget()
         {
            @Override
            public float[] getRGB()
            {
               float[] drumPadColor = mDrumPadColors[padIndex];

               if (!mDrumPadBank.exists().get())
               {
                  SettableColorValue c = mCursorTrack.color();
                  drumPadColor = new float[] { c.red(), c.green(), c.blue() };
               }

               int playing = velocityForPlayingNote(padIndex);
               if (playing > 0)
               {
                  return mixColorWithWhite(drumPadColor, playing);
               }
               return drumPadColor;
            }

            @Override
            public boolean get()
            {
               return mDrumPadBank.exists().get() ? drumPad.exists().get() : true;
            }

            @Override
            public void set(final boolean pressed)
            {
            }
         });
         mDrumPadColors[padIndex][0] = color.red() * darken;
         mDrumPadColors[padIndex][1] = color.green() * darken;
         mDrumPadColors[padIndex][2] = color.blue() * darken;

         mStepsLayer.bind(pad, new RGBButtonTarget()
         {
            @Override
            public float[] getRGB()
            {

               if (mShift)
               {
                  if (mCurrentPadForSteps == padIndex)
                  {
                     return WHITE;
                  }

                  int playingNote = velocityForPlayingNote(padIndex);

                  if (playingNote > 0)
                  {
                     return mixColorWithWhite(clipColor(0.3f), playingNote);
                  }


                  return clipColor(0.3f);
               }

               if (mPlayingStep == padIndex)
               {
                  return WHITE;
               }

               boolean hasData = mStepData[padIndex] > 0;

               return clipColor(hasData ? 1.f : 0.3f);
            }

            private float[] clipColor(float scale)
            {
               float[] color = new float[3];
               SettableColorValue c = mCursorClip.color();
               color[0] = c.red() * scale;
               color[1] = c.green() * scale;
               color[2] = c.blue() * scale;
               return color;
            }

            @Override
            public boolean get()
            {
               return true;
            }

            @Override
            public void set(final boolean pressed)
            {
               if (pressed)
               {
                  if (mShift)
                  {
                     mCursorClip.scrollToKey(36 + padIndex);
                     mCurrentPadForSteps = padIndex;
                     mCursorTrack.playNote(36 + padIndex, 100);
                  }
                  else mCursorClip.toggleStep(padIndex, 0, 100);
               }
            }
         });

      }
      mCursorClip.playingStep().addValueObserver(s -> mPlayingStep = s, -1);
      mCursorClip.scrollToKey(36);
      mCursorClip.addStepDataObserver((x, y, state) -> mStepData[x] = state);
      mCursorTrack.playingNotes().addValueObserver(notes -> mPlayingNotes = notes);
   }

   private float[] mixColorWithWhite(final float[] color, final int velocity)
   {
      float x = velocity / 127.f;
      float[] mixed = new float[3];
      for (int i=0; i<3; i++)
         mixed[i] = color[i] * (1-x) + x;

      return mixed;
   }

   private void initButtons()
   {
      mTransport.isPlaying().markInterested();

      Button shiftButton = addElement(new Button(CC_SHIFT));
      mBaseLayer.bind(shiftButton, new ButtonTarget()
      {
         @Override
         public boolean get()
         {
            return mShift;
         }

         @Override
         public void set(final boolean pressed)
         {
            mShift = pressed;
         }
      });

      Button clickToggle = addElement(new Button(CC_CLICK_COUNT_IN));
      mBaseLayer.bindToggle(clickToggle, mTransport.isMetronomeEnabled());

      Button playButton = addElement(new Button(CC_PLAY_LOOP_TOGGLE));
      mBaseLayer.bindPressedRunnable(playButton, mTransport.isPlaying(), () ->
      {
         if (mShift) mTransport.isArrangerLoopEnabled().toggle();
         else mTransport.togglePlay();
      });

      Button stopButton = addElement(new Button(CC_STOP_UNDO));
      mBaseLayer.bind(stopButton, new ButtonTarget()
         {
            @Override
            public boolean get()
            {
               return !mTransport.isPlaying().get();
            }

            @Override
            public void set(final boolean pressed)
            {
               if (pressed)
               {
                  if (mShift)
                     mApplication.undo();
                  else
                     mTransport.stop();
               }
            }
         });

      Button recordButton = addElement(new Button(CC_RECORD_SAVE));
      mBaseLayer.bindPressedRunnable(recordButton, mTransport.isArrangerRecordEnabled(), () ->
      {
         if (mShift) save();
         else mTransport.isArrangerRecordEnabled().toggle();
      });

      Button upButton = addElement(new Button(CC_UP));
      mBaseLayer.bindPressedRunnable(upButton, mCursorTrack.hasPrevious(), mCursorTrack::selectPrevious);
      mStepsLayer.bindPressedRunnable(upButton, mCursorClip.canScrollKeysUp(), mCursorClip::scrollKeysStepUp);
      Button downButton = addElement(new Button(CC_DOWN));
      mBaseLayer.bindPressedRunnable(downButton, mCursorTrack.hasNext(), mCursorTrack::selectNext);
      mStepsLayer.bindPressedRunnable(downButton, mCursorClip.canScrollKeysDown(), mCursorClip::scrollKeysStepDown);
      Button leftButton = addElement(new Button(CC_LEFT));
      mBaseLayer.bindPressedRunnable(leftButton, mCursorDevice.hasPrevious(), mCursorDevice::selectPrevious);
      mStepsLayer.bindPressedRunnable(leftButton, mCursorClip.canScrollStepsBackwards(), mCursorClip::scrollStepsPageBackwards);
      Button rightButton = addElement(new Button(CC_RIGHT));
      mBaseLayer.bindPressedRunnable(rightButton, mCursorDevice.hasNext(), mCursorDevice::selectNext);
      mStepsLayer.bindPressedRunnable(rightButton, mCursorClip.canScrollStepsForwards(), mCursorClip::scrollStepsPageForward);

      Button selectButton = addElement(new Button(CC_SELECT));
      mBaseLayer.bindToggle(selectButton, mCursorTrack.isPinned());
      Button zoomButton = addElement(new Button(CC_ZOOM));
      mBaseLayer.bindPressedRunnable(zoomButton, null, () ->
      {
         if (mShift) mApplication.zoomOut();
         else mApplication.zoomIn();
      });

      Button editorToggle = addElement(new Button(CC_EDITOR));
      mBaseLayer.bind(editorToggle, new ButtonTarget()
      {
         @Override
         public boolean get()
         {
            return isLayerActive(mStepsLayer);
         }

         @Override
         public void set(final boolean pressed)
         {
            if (pressed)
            {
               toggleLayer(mStepsLayer);
            }
         }
      });

      activateLayer(mBaseLayer);
   }

   private void initEncoders()
   {
      for(int i=0; i<4; i++)
      {
         SettableRangedValue parameterValue = mRemoteControls.getParameter(i).value();
         Encoder encoder = addElement(new Encoder(CC_ENCODER_1 + i));
         mBaseLayer.bind(encoder, new EncoderTarget()
         {
            @Override
            public void inc(final int steps)
            {
               parameterValue.inc(steps, mShift ? 1010 : 101);
            }
         });
      }
   }

   private void save()
   {
      Action saveAction = mApplication.getAction("Save");
      if (saveAction != null)
      {
         saveAction.invoke();
      }
   }

   /* API Objects */
   private CursorTrack mCursorTrack;
   private PinnableCursorDevice mCursorDevice;
   private CursorRemoteControlsPage mRemoteControls;
   private Transport mTransport;
   private MidiOut mMidiOut;
   private Application mApplication;
   private DrumPadBank mDrumPadBank;
   private boolean mShift;
   private NoteInput mNoteInput;
   private PlayingNote[] mPlayingNotes;
   private float[][] mDrumPadColors;

   /* Steps mode */
   private Layer mStepsLayer;
   private Clip mCursorClip;
   private int mPlayingStep;
   private int[] mStepData = new int[16];
   private int mCurrentPadForSteps;
   private Layer mBaseLayer = createBaseLayer();
}
