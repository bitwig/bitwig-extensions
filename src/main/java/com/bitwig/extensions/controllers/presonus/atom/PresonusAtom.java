package com.bitwig.extensions.controllers.presonus.atom;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.Action;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.Arpeggiator;
import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareControlType;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiExpressions;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.PlayingNote;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.oldframework.targets.RGBButtonTarget;
import com.bitwig.extensions.util.NoteInputUtils;

public class PresonusAtom extends ControllerExtension
{
   final static int CC_ENCODER_1 = 0x0E;

   final static int CC_ENCODER_2 = 0x0F;

   final static int CC_ENCODER_3 = 0x10;

   final static int CC_ENCODER_4 = 0x11;

   final static int CC_SHIFT = 0x20;

   final static int CC_NOTE_REPEAT = 0x18;

   final static int CC_FULL_LEVEL = 0x19;

   final static int CC_BANK_TRANSPOSE = 0x1A;

   final static int CC_PRESET_PAD_SELECT = 0x1B;

   final static int CC_SHOW_HIDE = 0x1D;

   final static int CC_NUDGE_QUANTIZE = 0x1E;

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

   final static int CC_RECORD_SAVE = 0x6B;

   final static int CC_PLAY_LOOP_TOGGLE = 0x6D;

   final static int CC_STOP_UNDO = 0x6F;

   final static int LAUNCHER_SCENES = 16;

   float[] WHITE = { 1, 1, 1 };

   float[] DIM_WHITE = { 0.3f, 0.3f, 0.3f };

   float[] BLACK = { 0, 0, 0 };

   float[] RED = { 1, 0, 0 };

   float[] DIM_RED = { 0.3f, 0.0f, 0.0f };

   public PresonusAtom(final PresonusAtomDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();
      mApplication = host.createApplication();

      final MidiIn midiIn = host.getMidiInPort(0);

      // midiIn.setMidiCallback(getMidiCallbackToUseForLayers());
      mNoteInput = midiIn.createNoteInput("Pads", "80????", "90????", "a0????");
      mNoteInput.setShouldConsumeEvents(false);
      mArpeggiator = mNoteInput.arpeggiator();
      mArpeggiator.isEnabled().markInterested();
      mArpeggiator.period().markInterested();
      mArpeggiator.shuffle().markInterested();
      mArpeggiator.usePressureToVelocity().set(true);

      mMidiOut = host.getMidiOutPort(0);
      mMidiIn = host.getMidiInPort(0);

      mCursorTrack = host.createCursorTrack(0, LAUNCHER_SCENES);
      mCursorTrack.arm().markInterested();
      mSceneBank = host.createSceneBank(LAUNCHER_SCENES);

      for (int s = 0; s < LAUNCHER_SCENES; s++)
      {
         final ClipLauncherSlot slot = mCursorTrack.clipLauncherSlotBank().getItemAt(s);
         slot.color().markInterested();
         slot.isPlaying().markInterested();
         slot.isRecording().markInterested();
         slot.isPlaybackQueued().markInterested();
         slot.isRecordingQueued().markInterested();
         slot.hasContent().markInterested();

         final Scene scene = mSceneBank.getScene(s);
         scene.color().markInterested();
         scene.exists().markInterested();
      }

      mCursorDevice = mCursorTrack.createCursorDevice("ATOM", "Atom", 0,
         CursorDeviceFollowMode.FIRST_INSTRUMENT);

      mRemoteControls = mCursorDevice.createCursorRemoteControlsPage(4);
      mRemoteControls.setHardwareLayout(HardwareControlType.ENCODER, 4);
      for (int i = 0; i < 4; ++i)
         mRemoteControls.getParameter(i).setIndication(true);

      mTransport = host.createTransport();
      mTransport.isPlaying().markInterested();
      mTransport.getPosition().markInterested();

      mCursorClip = host.createLauncherCursorClip(16, 1);
      mCursorClip.color().markInterested();
      mCursorClip.clipLauncherSlot().color().markInterested();
      mCursorClip.clipLauncherSlot().isPlaying().markInterested();
      mCursorClip.clipLauncherSlot().isRecording().markInterested();
      mCursorClip.clipLauncherSlot().isPlaybackQueued().markInterested();
      mCursorClip.clipLauncherSlot().isRecordingQueued().markInterested();
      mCursorClip.clipLauncherSlot().hasContent().markInterested();
      mCursorClip.getLoopLength().markInterested();
      mCursorClip.getLoopStart().markInterested();

      mDrumPadBank = mCursorDevice.createDrumPadBank(16);
      mDrumPadBank.exists().markInterested();
      mCursorTrack.color().markInterested();

      createHardwareSurface();

      initLayers();

      // Turn on Native Mode
      mMidiOut.sendMidi(0x8f, 0, 127);
   }

   @Override
   public void flush()
   {
      mHardwareSurface.updateHardware();
   }

   @Override
   public void exit()
   {
      // Turn off Native Mode
      mMidiOut.sendMidi(0x8f, 0, 0);
   }

   private void createHardwareSurface()
   {
      final ControllerHost host = getHost();
      final HardwareSurface surface = host.createHardwareSurface();
      mHardwareSurface = surface;

      surface.setPhysicalSize(202, 195);

      mShiftButton = createCCButton(CC_SHIFT);
      mShiftButton.setLabel("Shift");

      // NAV section
      mUpButton = createCCButton(CC_UP);
      mUpButton.setLabel("Up");
      mDownButton = createCCButton(CC_DOWN);
      mDownButton.setLabel("Down");
      mLeftButton = createCCButton(CC_LEFT);
      mLeftButton.setLabel("Left");
      mRightButton = createCCButton(CC_RIGHT);
      mRightButton.setLabel("Right");
      mSelectButton = createCCButton(CC_SELECT);
      mSelectButton.setLabel("Select");
      mZoomButton = createCCButton(CC_ZOOM);
      mZoomButton.setLabel("Zoom");

      // TRANS section
      mClickCountInButton = createCCButton(CC_CLICK_COUNT_IN);
      mClickCountInButton.setLabel("Click\nCount in");
      mRecordSaveButton = createCCButton(CC_RECORD_SAVE);
      mRecordSaveButton.setLabel("Record\nSave");
      mPlayLoopButton = createCCButton(CC_PLAY_LOOP_TOGGLE);
      mPlayLoopButton.setLabel("Play\nLoop");
      mStopUndoButton = createCCButton(CC_STOP_UNDO);
      mStopUndoButton.setLabel("Stop\nUndo");

      // SONG section
      mSetupButton = createCCButton(CC_SETUP);
      mSetupButton.setLabel("Setup");
      mSetLoopButton = createCCButton(CC_SET_LOOP);
      mSetLoopButton.setLabel("Set Loop");

      // EVENT section
      mEditorButton = createCCButton(CC_EDITOR);
      mEditorButton.setLabel("Editor");
      mNudgeQuantizeButton = createCCButton(CC_NUDGE_QUANTIZE);
      mNudgeQuantizeButton.setLabel("Nudge\nQuantize");

      // INST section
      mShowHideButton = createCCButton(CC_SHOW_HIDE);
      mShowHideButton.setLabel("Show/\nHide");
      mPresetPadSelectButton = createCCButton(CC_PRESET_PAD_SELECT);
      mPresetPadSelectButton.setLabel("Preset +-\nFocus");
      mBankButton = createCCButton(CC_BANK_TRANSPOSE);
      mBankButton.setLabel("Bank");

      // MODE section
      mFullLevelButton = createCCButton(CC_FULL_LEVEL);
      mFullLevelButton.setLabel("Full Level");
      mNoteRepeatButton = createCCButton(CC_NOTE_REPEAT);
      mNoteRepeatButton.setLabel("Note\nRepeat");

      // Pads
      for (int i = 0; i < 16; i++)
      {
         createPadButton(i);
      }
   }

   private void initLayers()
   {
      initBaseLayer();
   }

   private void initBaseLayer()
   {
      mBaseLayer.bindIsPressed(mShiftButton, this::setIsShiftPressed);
      mBaseLayer.bindToggle(mClickCountInButton, mTransport.isMetronomeEnabled());

      mBaseLayer.bindPressed(mPlayLoopButton, () ->
      {
         if (mShift) mTransport.isArrangerLoopEnabled().toggle();
         else mTransport.togglePlay();
      });
      mBaseLayer.bind(mTransport.isPlaying(), mPlayLoopButton);

      mBaseLayer.activate();
   }

   private void setIsShiftPressed(final boolean value)
   {
      mShift = value;
   }

   private Layer createLayer(final String name)
   {
      return new Layer(mLayers, name);
   }

   private int velocityForPlayingNote(final int padIndex)
   {
      if (mPlayingNotes != null)
      {
         for (final PlayingNote playingNote : mPlayingNotes)
         {
            if (playingNote.pitch() == 36 + padIndex)
            {
               return playingNote.velocity();
            }
         }
      }

      return 0;
   }

   private double getPageLengthInBeatTime()
   {
      return 4;
   }

   private float[] mixColorWithWhite(final float[] color, final int velocity)
   {
      final float x = velocity / 127.f;
      final float[] mixed = new float[3];
      for (int i = 0; i < 3; i++)
         mixed[i] = color[i] * (1 - x) + x;

      return mixed;
   }

   private HardwareButton createCCButton(final int controlNumber)
   {
      final HardwareButton button = mHardwareSurface.createHardwareButton();
      final MidiExpressions midiExpressions = getHost().midiExpressions();

      button.pressedAction().setActionMatcher(mMidiIn
         .createActionMatcher(midiExpressions.createIsCCExpression(0, controlNumber) + " && data2 > 0"));
      button.releasedAction().setActionMatcher(mMidiIn.createCCActionMatcher(0, controlNumber, 0));

      final OnOffHardwareLight light = mHardwareSurface.createOnOffHardwareLight();
      button.setBackgroundLight(light);

      light.isOn().onUpdateHardware(value -> {
         mMidiOut.sendMidi(0xB0, controlNumber, value ? 127 : 0);
      });

      return button;
   }

   private void createPadButton(final int index)
   {
      final HardwareButton button = mHardwareSurface.createHardwareButton();

      final String pressedExpression = "status == 0x90 && data1 == " + (0x24 + index) + " && data2 > 0";
      button.pressedAction().setActionMatcher(mMidiIn.createActionMatcher(pressedExpression));

      final String releasedExpression = "status == 0x90 && data1 == " + (0x24 + index) + " && data2 == 0";
      button.releasedAction().setActionMatcher(mMidiIn.createActionMatcher(releasedExpression));

      mPadButtons[index] = button;

      final MultiStateHardwareLight light = mHardwareSurface
         .createMultiStateHardwareLight(PresonusAtom::padLightStateToColor);

      button.setBackgroundLight(light);

      mPadLights[index] = light;
   }

   private static Color padLightStateToColor(final int padState)
   {
      final int red = (padState & 0xFF0000) >> 16;
      final int green = (padState & 0xFF00) >> 8;
      final int blue = (padState & 0xFF);

      return Color.fromRGB255(red, green, blue);
   }

   private void scrollKeys(final int delta)
   {
      mCurrentPadForSteps = (mCurrentPadForSteps + delta) & 0xf;
      mCursorClip.scrollToKey(36 + mCurrentPadForSteps);
   }

   private void scrollPage(final int delta)
   {
      mCurrentPageForSteps += delta;
      mCurrentPageForSteps = Math.max(0, Math.min(mCurrentPageForSteps, getNumStepPages() - 1));
      mCursorClip.scrollToStep(16 * mCurrentPageForSteps);
   }

   private int getNumStepPages()
   {
      return (int)Math.ceil(
         (mCursorClip.getLoopStart().get() + mCursorClip.getLoopLength().get()) / (16.0 * getStepSize()));
   }

   private double getStepSize()
   {
      return 0.25;
   }

   private void save()
   {
      final Action saveAction = mApplication.getAction("Save");
      if (saveAction != null)
      {
         saveAction.invoke();
      }
   }

   float[] getClipColor(final ClipLauncherSlot s)
   {
      if (s.isRecordingQueued().get())
      {
         return RGBButtonTarget.mix(RED, BLACK, getTransportPulse(1.0, 1));
      }
      else if (s.hasContent().get())
      {
         if (s.isPlaybackQueued().get())
         {
            return RGBButtonTarget.mixWithValue(s.color(), WHITE, 1 - getTransportPulse(4.0, 1));
         }
         else if (s.isRecording().get())
         {
            return RED;
         }
         else if (s.isPlaying().get() && mTransport.isPlaying().get())
         {
            return RGBButtonTarget.mixWithValue(s.color(), WHITE, 1 - getTransportPulse(1.0, 1));
         }

         return RGBButtonTarget.getFromValue(s.color());
      }
      else if (mCursorTrack.arm().get())
      {
         return RGBButtonTarget.mix(BLACK, RED, 0.1f);
      }

      return BLACK;
   }

   private float getTransportPulse(final double multiplier, final double amount)
   {
      final double p = mTransport.getPosition().get() * multiplier;
      return (float)((0.5 + 0.5 * Math.cos(p * 2 * Math.PI)) * amount);
   }

   /* API Objects */
   private CursorTrack mCursorTrack;

   private PinnableCursorDevice mCursorDevice;

   private CursorRemoteControlsPage mRemoteControls;

   private Transport mTransport;

   private MidiIn mMidiIn;

   private MidiOut mMidiOut;

   private Application mApplication;

   private DrumPadBank mDrumPadBank;

   private boolean mShift;

   private NoteInput mNoteInput;

   private PlayingNote[] mPlayingNotes;

   private Clip mCursorClip;

   private int mPlayingStep;

   private int[] mStepData = new int[16];

   private int mCurrentPadForSteps;

   private int mCurrentPageForSteps;

   private HardwareSurface mHardwareSurface;

   private HardwareButton mShiftButton, mUpButton, mDownButton, mLeftButton, mRightButton, mSelectButton,
      mZoomButton, mClickCountInButton, mRecordSaveButton, mPlayLoopButton, mStopUndoButton, mSetupButton,
      mSetLoopButton, mEditorButton, mNudgeQuantizeButton, mShowHideButton, mPresetPadSelectButton,
      mBankButton, mFullLevelButton, mNoteRepeatButton;

   private HardwareButton[] mPadButtons = new HardwareButton[16];

   private MultiStateHardwareLight[] mPadLights = new MultiStateHardwareLight[16];

   private final Layers mLayers = new Layers(this)
   {
      @Override
      protected void activeLayersChanged()
      {
         super.activeLayersChanged();

         final boolean shouldPlayDrums = !mStepsLayer.isActive() && !mNoteRepeatShiftLayer.isActive()
            && !mLauncherClipsLayer.isActive() && !mStepsZoomLayer.isActive()
            && !mStepsSetupLoopLayer.isActive();

         mNoteInput
            .setKeyTranslationTable(shouldPlayDrums ? NoteInputUtils.ALL_NOTES : NoteInputUtils.NO_NOTES);
      }
   };

   private Layer mStepsLayer = createLayer("Steps");

   private Layer mLauncherClipsLayer = createLayer("Launcher Clips");

   private Layer mBaseLayer = createLayer("Base");

   private Layer mNoteRepeatLayer = createLayer("Note Repeat");

   private Layer mNoteRepeatShiftLayer = createLayer("Note Repeat Shift");

   private Layer mStepsZoomLayer = createLayer("Steps Zoom");

   private Layer mStepsSetupLoopLayer = createLayer("Steps Setup Loop");

   private Arpeggiator mArpeggiator;

   private SceneBank mSceneBank;
}
