package com.bitwig.extensions.controllers.presonus.atom;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
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
import com.bitwig.extension.controller.api.DrumPad;
import com.bitwig.extension.controller.api.DrumPadBank;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareControlType;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiExpressions;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.NoteStep;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.PlayingNote;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.framework.DebugUtilities;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
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

   private static final Color WHITE = Color.fromRGB(1, 1, 1);

   private static final Color DIM_WHITE = Color.fromRGB(0.3, 0.3, 0.3);

   private static final Color BLACK = Color.fromRGB(0, 0, 0);

   private static final Color RED = Color.fromRGB(1, 0, 0);

   private static final Color DIM_RED = Color.fromRGB(0.3, 0.0, 0.0);

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

      midiIn.setMidiCallback((ShortMidiMessageReceivedCallback)msg -> onMidi0(msg));
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
      mCursorClip.playingStep().addValueObserver(s -> mPlayingStep = s, -1);
      mCursorClip.scrollToKey(36);
      mCursorClip.addNoteStepObserver(d -> {
         final int x = d.x();
         final int y = d.y();

         if (y == 0 && x >= 0 && x < mStepData.length)
         {
            final NoteStep.State state = d.state();

            if (state == NoteStep.State.NoteOn)
               mStepData[x] = 2;
            else if (state == NoteStep.State.NoteSustain)
               mStepData[x] = 1;
            else
               mStepData[x] = 0;
         }
      });
      mCursorTrack.playingNotes().addValueObserver(notes -> mPlayingNotes = notes);

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

   /** Called when we receive short MIDI message on port 0. */
   private void onMidi0(final ShortMidiMessage msg)
   {
      getHost().println(msg.toString());
   }

   private void createHardwareSurface()
   {
      final ControllerHost host = getHost();
      final HardwareSurface surface = host.createHardwareSurface();
      mHardwareSurface = surface;

      surface.setPhysicalSize(202, 195);

      mShiftButton = createToggleButton(CC_SHIFT);
      mShiftButton.setLabel("Shift");

      // NAV section
      mUpButton = createToggleButton(CC_UP);
      mUpButton.setLabel("Up");
      mDownButton = createToggleButton(CC_DOWN);
      mDownButton.setLabel("Down");
      mLeftButton = createToggleButton(CC_LEFT);
      mLeftButton.setLabel("Left");
      mRightButton = createToggleButton(CC_RIGHT);
      mRightButton.setLabel("Right");
      mSelectButton = createRGBButton(CC_SELECT);
      mSelectButton.setLabel("Select");
      mZoomButton = createToggleButton(CC_ZOOM);
      mZoomButton.setLabel("Zoom");

      // TRANS section
      mClickCountInButton = createToggleButton(CC_CLICK_COUNT_IN);
      mClickCountInButton.setLabel("Click\nCount in");
      mRecordSaveButton = createToggleButton(CC_RECORD_SAVE);
      mRecordSaveButton.setLabel("Record\nSave");
      mPlayLoopButton = createToggleButton(CC_PLAY_LOOP_TOGGLE);
      mPlayLoopButton.setLabel("Play\nLoop");
      mStopUndoButton = createToggleButton(CC_STOP_UNDO);
      mStopUndoButton.setLabel("Stop\nUndo");

      // SONG section
      mSetupButton = createToggleButton(CC_SETUP);
      mSetupButton.setLabel("Setup");
      mSetLoopButton = createToggleButton(CC_SET_LOOP);
      mSetLoopButton.setLabel("Set Loop");

      // EVENT section
      mEditorButton = createToggleButton(CC_EDITOR);
      mEditorButton.setLabel("Editor");
      mNudgeQuantizeButton = createToggleButton(CC_NUDGE_QUANTIZE);
      mNudgeQuantizeButton.setLabel("Nudge\nQuantize");

      // INST section
      mShowHideButton = createToggleButton(CC_SHOW_HIDE);
      mShowHideButton.setLabel("Show/\nHide");
      mPresetPadSelectButton = createToggleButton(CC_PRESET_PAD_SELECT);
      mPresetPadSelectButton.setLabel("Preset +-\nFocus");
      mBankButton = createToggleButton(CC_BANK_TRANSPOSE);
      mBankButton.setLabel("Bank");

      // MODE section
      mFullLevelButton = createToggleButton(CC_FULL_LEVEL);
      mFullLevelButton.setLabel("Full Level");
      mNoteRepeatButton = createToggleButton(CC_NOTE_REPEAT);
      mNoteRepeatButton.setLabel("Note\nRepeat");

      // Pads

      for (int i = 0; i < 16; i++)
      {
         final DrumPad drumPad = mDrumPadBank.getItemAt(i);
         drumPad.exists().markInterested();
         drumPad.color().markInterested();

         createPadButton(i);
      }

      for (int i = 0; i < 4; i++)
      {
         createEncoder(i);
      }
   }

   private void initLayers()
   {
      initBaseLayer();

      mDebugLayer = DebugUtilities.createDebugLayer(mLayers, mHardwareSurface);
      // mDebugLayer.activate();
   }

   private void initBaseLayer()
   {
      mBaseLayer.bindIsPressed(mShiftButton, this::setIsShiftPressed);
      mBaseLayer.bindToggle(mClickCountInButton, mTransport.isMetronomeEnabled());

      mBaseLayer.bindToggle(mPlayLoopButton, () -> {
         if (mShift)
            mTransport.isArrangerLoopEnabled().toggle();
         else
            mTransport.togglePlay();
      }, mTransport.isPlaying());

      mBaseLayer.bindToggle(mStopUndoButton, () -> {
         if (mShift)
            mApplication.undo();
         else
            mTransport.stop();
      }, () -> !mTransport.isPlaying().get());

      mBaseLayer.bindToggle(mRecordSaveButton, () -> {
         if (mShift)
            save();
         else
            mTransport.isArrangerRecordEnabled().toggle();
      }, mTransport.isArrangerRecordEnabled());

      mBaseLayer.bindToggle(mUpButton, mCursorTrack.selectPreviousAction(), mCursorTrack.hasPrevious());
      mBaseLayer.bindToggle(mDownButton, mCursorTrack.selectNextAction(), mCursorTrack.hasNext());
      mBaseLayer.bindToggle(mLeftButton, mCursorDevice.selectPreviousAction(), mCursorDevice.hasPrevious());
      mBaseLayer.bindToggle(mRightButton, mCursorDevice.selectNextAction(), mCursorDevice.hasNext());

      mBaseLayer.bindPressed(mSelectButton, () -> {
         if (mCursorClip.clipLauncherSlot().isRecording().get())
         {
            mCursorClip.clipLauncherSlot().launch();
         }
         else
            mLauncherClipsLayer.activate();
      });
      mBaseLayer.bindReleased(mSelectButton, mLauncherClipsLayer::deactivate);
      mBaseLayer.bind(() -> getClipColor(mCursorClip.clipLauncherSlot()), mSelectButton);

      mBaseLayer.bindToggle(mEditorButton, mStepsLayer::toggleIsActive, mStepsLayer::isActive);

      mBaseLayer.bindReleased(mNoteRepeatButton, () -> {
         mArpeggiator.mode().set("all");
         mArpeggiator.usePressureToVelocity().set(true);

         mNoteRepeatLayer.toggleIsActive();

         final boolean wasEnabled = mArpeggiator.isEnabled().get();

         mArpeggiator.isEnabled().set(!wasEnabled);

         if (wasEnabled)
            mNoteRepeatLayer.deactivate();
         else
            mNoteRepeatLayer.activate();
      });
      mBaseLayer.bind(mArpeggiator.isEnabled(), mNoteRepeatButton);

      final BooleanObject fullLevelIsOn = new BooleanObject();

      mBaseLayer.bindToggle(mFullLevelButton, () -> {
         fullLevelIsOn.toggle();

         mNoteInput.setVelocityTranslationTable(fullLevelIsOn.getAsBoolean()
            ? NoteInputUtils.FULL_VELOCITY
            : NoteInputUtils.NORMAL_VELOCITY);
      }, fullLevelIsOn);

      for (int i = 0; i < 4; i++)
      {
         final Parameter parameter = mRemoteControls.getParameter(i);
         final RelativeHardwareKnob encoder = mEncoders[i];

         mBaseLayer.bind(encoder, parameter);
      }

      for (int i = 0; i < 16; i++)
      {
         final HardwareButton padButton = mPadButtons[i];

         final int padIndex = i;

         mBaseLayer.bindPressed(padButton, () -> {
            mCursorClip.scrollToKey(36 + padIndex);
            mCurrentPadForSteps = padIndex;
         });

         mBaseLayer.bind(() -> getDrumPadColor(padIndex), padButton);
      }

      mBaseLayer.activate();
   }

   private Color getDrumPadColor(final int padIndex)
   {
      final DrumPad drumPad = mDrumPadBank.getItemAt(padIndex);
      final boolean padBankExists = mDrumPadBank.exists().get();
      final boolean isOn = padBankExists ? drumPad.exists().get() : true;

      if (!isOn)
         return null;

      final double darken = 0.7;

      Color drumPadColor;

      if (!padBankExists)
      {
         drumPadColor = mCursorTrack.color().get();
      }
      else
      {
         final Color sourceDrumPadColor = drumPad.color().get();
         final double red = sourceDrumPadColor.getRed() * darken;
         final double green = sourceDrumPadColor.getGreen() * darken;
         final double blue = sourceDrumPadColor.getBlue() * darken;

         drumPadColor = Color.fromRGB(red, green, blue);
      }

      final int playing = velocityForPlayingNote(padIndex);

      if (playing > 0)
      {
         return mixColorWithWhite(drumPadColor, playing);
      }

      return drumPadColor;
   }

   private void setIsShiftPressed(final boolean value)
   {
      if (value != mShift)
      {
         mShift = value;
         mLayers.setGlobalSensitivity(value ? 0.1 : 1);
      }
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

   private Color mixColorWithWhite(final Color color, final int velocity)
   {
      final float x = velocity / 127.f;
      final double red = color.getRed() * (1 - x) + x;
      final double green = color.getGreen() * (1 - x) + x;
      final double blue = color.getBlue() * (1 - x) + x;

      return Color.fromRGB(red, green, blue);
   }

   private HardwareButton createToggleButton(final int controlNumber)
   {
      final HardwareButton button = createButton(controlNumber);
      final OnOffHardwareLight light = mHardwareSurface.createOnOffHardwareLight();
      button.setBackgroundLight(light);

      light.isOn().onUpdateHardware(value -> {
         mMidiOut.sendMidi(0xB0, controlNumber, value ? 127 : 0);
      });

      return button;
   }

   private HardwareButton createRGBButton(final int controlNumber)
   {
      final HardwareButton button = createButton(controlNumber);

      final MultiStateHardwareLight light = mHardwareSurface
         .createMultiStateHardwareLight(PresonusAtom::lightStateToColor);

      light.state().onUpdateHardware(state -> sendLightState(0xB0, controlNumber, state));

      light.setColorToStateFunction(PresonusAtom::lightColorToState);

      button.setBackgroundLight(light);

      return button;
   }

   private HardwareButton createButton(final int controlNumber)
   {
      final HardwareButton button = mHardwareSurface.createHardwareButton();
      final MidiExpressions midiExpressions = getHost().midiExpressions();

      button.pressedAction().setActionMatcher(mMidiIn
         .createActionMatcher(midiExpressions.createIsCCExpression(0, controlNumber) + " && data2 > 0"));
      button.releasedAction().setActionMatcher(mMidiIn.createCCActionMatcher(0, controlNumber, 0));

      return button;
   }

   private void createPadButton(final int index)
   {
      final HardwareButton pad = mHardwareSurface.createHardwareButton();
      pad.setLabel("Pad " + (index + 1));

      final int note = 0x24 + index;
      pad.pressedAction().setActionMatcher(mMidiIn.createNoteOnActionMatcher(0, note));
      pad.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, note));

      mPadButtons[index] = pad;

      final MultiStateHardwareLight light = mHardwareSurface
         .createMultiStateHardwareLight(PresonusAtom::lightStateToColor);

      light.state().onUpdateHardware(state -> sendLightState(0x90, 0x24 + index, state));

      light.setColorToStateFunction(PresonusAtom::lightColorToState);

      pad.setBackgroundLight(light);

      mPadLights[index] = light;
   }

   private void sendLightState(final int statusStart, final int data1, final int state)
   {
      final int red = (state & 0x7F0000) >> 16;
      final int green = (state & 0x7F00) >> 8;
      final int blue = state & 0x7F;

      final int[] values = new int[4];
      values[0] = (state & 0x7F000000) >> 24;
      values[1] = red;
      values[2] = green;
      values[3] = blue;

      for (int i = 0; i < 4; i++)
      {
         // if (values[i] != mLastSent[i])
         {
            mMidiOut.sendMidi(statusStart + i, data1, values[i]);
            // mLastSent[i] = values[i];
         }
      }
   }

   private static int lightColorToState(final Color color)
   {
      if (color == null)
         return 0;

      final int red = colorPartFromFloat(color.getRed());
      final int green = colorPartFromFloat(color.getGreen());
      final int blue = colorPartFromFloat(color.getBlue());

      return 0x7F000000 | red << 16 | green << 8 | blue;
   }

   private static int colorPartFromFloat(final double x)
   {
      return Math.max(0, Math.min((int)(127.0 * x), 127));
   }

   private void createEncoder(final int index)
   {
      final RelativeHardwareKnob encoder = mHardwareSurface.createRelativeHardwareKnob();
      encoder.setLabel(String.valueOf(index + 1));
      encoder.setAdjustValueMatcher(mMidiIn.createRelativeSignedBitCCValueMatcher(0, CC_ENCODER_1 + index));
      encoder.setSensitivity(2.5);

      mEncoders[index] = encoder;
   }

   private static Color lightStateToColor(final int padState)
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

   private Color getClipColor(final ClipLauncherSlot s)
   {
      if (s.isRecordingQueued().get())
      {
         return Color.mix(RED, BLACK, getTransportPulse(1.0, 1));
      }
      else if (s.hasContent().get())
      {
         if (s.isPlaybackQueued().get())
         {
            return Color.mix(s.color().get(), WHITE, 1 - getTransportPulse(4.0, 1));
         }
         else if (s.isRecording().get())
         {
            return RED;
         }
         else if (s.isPlaying().get() && mTransport.isPlaying().get())
         {
            return Color.mix(s.color().get(), WHITE, 1 - getTransportPulse(1.0, 1));
         }

         return s.color().get();
      }
      else if (mCursorTrack.arm().get())
      {
         return Color.mix(BLACK, RED, 0.1f);
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

   private RelativeHardwareKnob[] mEncoders = new RelativeHardwareKnob[4];

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

   private Layer mDebugLayer;

   private Arpeggiator mArpeggiator;

   private SceneBank mSceneBank;
}
