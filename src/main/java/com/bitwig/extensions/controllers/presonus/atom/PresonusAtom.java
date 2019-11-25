package com.bitwig.extensions.controllers.presonus.atom;

import java.util.function.Consumer;
import java.util.function.Supplier;

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
import com.bitwig.extension.controller.api.HardwareLightVisualState;
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
import com.bitwig.extensions.framework.BooleanObject;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.util.NoteInputUtils;

public class PresonusAtom extends ControllerExtension
{
   private final static int CC_ENCODER_1 = 0x0E;

   private final static int CC_SHIFT = 0x20;

   private final static int CC_NOTE_REPEAT = 0x18;

   private final static int CC_FULL_LEVEL = 0x19;

   private final static int CC_BANK_TRANSPOSE = 0x1A;

   private final static int CC_PRESET_PAD_SELECT = 0x1B;

   private final static int CC_SHOW_HIDE = 0x1D;

   private final static int CC_NUDGE_QUANTIZE = 0x1E;

   private final static int CC_EDITOR = 0x1F;

   private final static int CC_SET_LOOP = 0x55;

   private final static int CC_SETUP = 0x56;

   private final static int CC_UP = 0x57;

   private final static int CC_DOWN = 0x59;

   private final static int CC_LEFT = 0x5A;

   private final static int CC_RIGHT = 0x66;

   private final static int CC_SELECT = 0x67;

   private final static int CC_ZOOM = 0x68;

   private final static int CC_CLICK_COUNT_IN = 0x69;

   private final static int CC_RECORD_SAVE = 0x6B;

   private final static int CC_PLAY_LOOP_TOGGLE = 0x6D;

   private final static int CC_STOP_UNDO = 0x6F;

   private final static int LAUNCHER_SCENES = 16;

   private static final Color WHITE = Color.fromRGB(1, 1, 1);

   private static final Color BLACK = Color.fromRGB(0, 0, 0);

   private static final Color RED = Color.fromRGB(1, 0, 0);

   private static final Color DIM_RED = Color.fromRGB(0.3, 0.0, 0.0);

   private static final Color GREEN = Color.fromRGB(0, 1, 0);

   private static final Color ORANGE = Color.fromRGB(1, 1, 0);

   private static final Color BLUE = Color.fromRGB(0, 0, 1);

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

      mCursorClip = mCursorTrack.createLauncherCursorClip(16, 1);
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
      // getHost().println(msg.toString());
   }

   private void createHardwareSurface()
   {
      final ControllerHost host = getHost();
      final HardwareSurface surface = host.createHardwareSurface();
      mHardwareSurface = surface;

      surface.setPhysicalSize(202, 195);

      mShiftButton = createToggleButton("shift", CC_SHIFT, ORANGE);
      mShiftButton.setLabel("Shift");

      // NAV section
      mUpButton = createToggleButton("up", CC_UP, ORANGE);
      mUpButton.setLabel("Up");
      mDownButton = createToggleButton("down", CC_DOWN, ORANGE);
      mDownButton.setLabel("Down");
      mLeftButton = createToggleButton("left", CC_LEFT, ORANGE);
      mLeftButton.setLabel("Left");
      mRightButton = createToggleButton("right", CC_RIGHT, ORANGE);
      mRightButton.setLabel("Right");
      mSelectButton = createRGBButton("select", CC_SELECT);
      mSelectButton.setLabel("Select");
      mZoomButton = createToggleButton("zoom", CC_ZOOM, ORANGE);
      mZoomButton.setLabel("Zoom");

      // TRANS section
      mClickCountInButton = createToggleButton("click_count_in", CC_CLICK_COUNT_IN, BLUE);
      mClickCountInButton.setLabel("Click\nCount in");
      mRecordSaveButton = createToggleButton("record_save", CC_RECORD_SAVE, RED);
      mRecordSaveButton.setLabel("Record\nSave");
      mPlayLoopButton = createToggleButton("play_loop", CC_PLAY_LOOP_TOGGLE, GREEN);
      mPlayLoopButton.setLabel("Play\nLoop");
      mStopUndoButton = createToggleButton("stop_undo", CC_STOP_UNDO, ORANGE);
      mStopUndoButton.setLabel("Stop\nUndo");

      // SONG section
      mSetupButton = createToggleButton("setup", CC_SETUP, ORANGE);
      mSetupButton.setLabel("Setup");
      mSetLoopButton = createToggleButton("set_loop", CC_SET_LOOP, ORANGE);
      mSetLoopButton.setLabel("Set Loop");

      // EVENT section
      mEditorButton = createToggleButton("editor", CC_EDITOR, ORANGE);
      mEditorButton.setLabel("Editor");
      mNudgeQuantizeButton = createToggleButton("nudge_quantize", CC_NUDGE_QUANTIZE, ORANGE);
      mNudgeQuantizeButton.setLabel("Nudge\nQuantize");

      // INST section
      mShowHideButton = createToggleButton("show_hide", CC_SHOW_HIDE, ORANGE);
      mShowHideButton.setLabel("Show/\nHide");
      mPresetPadSelectButton = createToggleButton("preset_pad_select", CC_PRESET_PAD_SELECT, WHITE);
      mPresetPadSelectButton.setLabel("Preset +-\nFocus");
      mBankButton = createToggleButton("bank", CC_BANK_TRANSPOSE, WHITE);
      mBankButton.setLabel("Bank");

      // MODE section
      mFullLevelButton = createToggleButton("full_level", CC_FULL_LEVEL, RED);
      mFullLevelButton.setLabel("Full Level");
      mNoteRepeatButton = createToggleButton("note_repeat", CC_NOTE_REPEAT, RED);
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

      initHardwareLayout();
   }

   private void initHardwareLayout()
   {
      final HardwareSurface surface = mHardwareSurface;
      surface.hardwareElementWithId("shift").setBounds(12.25, 175.25, 12.0, 9.0);
      surface.hardwareElementWithId("up").setBounds(178.25, 21.75, 14.0, 10.0);
      surface.hardwareElementWithId("down").setBounds(178.25, 37.0, 14.0, 10.0);
      surface.hardwareElementWithId("left").setBounds(178.25, 52.0, 14.0, 10.0);
      surface.hardwareElementWithId("right").setBounds(178.25, 67.25, 14.0, 10.0);
      surface.hardwareElementWithId("select").setBounds(178.25, 82.5, 14.0, 10.0);
      surface.hardwareElementWithId("zoom").setBounds(178.25, 97.75, 14.0, 10.0);
      surface.hardwareElementWithId("click_count_in").setBounds(178.75, 129.75, 14.0, 10.0);
      surface.hardwareElementWithId("record_save").setBounds(178.75, 145.0, 14.0, 10.0);
      surface.hardwareElementWithId("play_loop").setBounds(178.75, 160.0, 14.0, 10.0);
      surface.hardwareElementWithId("stop_undo").setBounds(178.75, 175.25, 14.0, 10.0);
      surface.hardwareElementWithId("setup").setBounds(11.5, 21.5, 14.0, 10.0);
      surface.hardwareElementWithId("set_loop").setBounds(11.5, 37.5, 14.0, 10.0);
      surface.hardwareElementWithId("editor").setBounds(11.25, 56.5, 14.0, 10.0);
      surface.hardwareElementWithId("nudge_quantize").setBounds(11.25, 72.5, 14.0, 10.0);
      surface.hardwareElementWithId("show_hide").setBounds(11.0, 93.5, 14.0, 10.0);
      surface.hardwareElementWithId("preset_pad_select").setBounds(11.0, 108.25, 14.0, 10.0);
      surface.hardwareElementWithId("bank").setBounds(11.0, 123.25, 14.0, 10.0);
      surface.hardwareElementWithId("full_level").setBounds(11.25, 144.75, 14.0, 10.0);
      surface.hardwareElementWithId("note_repeat").setBounds(11.25, 160.75, 14.0, 10.0);
      surface.hardwareElementWithId("pad1").setBounds(34.75, 154.75, 30.0, 30.0);
      surface.hardwareElementWithId("pad2").setBounds(69.75, 154.75, 30.0, 30.0);
      surface.hardwareElementWithId("pad3").setBounds(104.75, 154.75, 30.0, 30.0);
      surface.hardwareElementWithId("pad4").setBounds(139.75, 154.75, 30.0, 30.0);
      surface.hardwareElementWithId("pad5").setBounds(34.75, 121.75, 30.0, 30.0);
      surface.hardwareElementWithId("pad6").setBounds(69.75, 121.75, 30.0, 30.0);
      surface.hardwareElementWithId("pad7").setBounds(104.75, 121.75, 30.0, 30.0);
      surface.hardwareElementWithId("pad8").setBounds(139.75, 121.75, 30.0, 30.0);
      surface.hardwareElementWithId("pad9").setBounds(34.75, 88.75, 30.0, 30.0);
      surface.hardwareElementWithId("pad10").setBounds(69.75, 88.75, 30.0, 30.0);
      surface.hardwareElementWithId("pad11").setBounds(104.75, 88.75, 30.0, 30.0);
      surface.hardwareElementWithId("pad12").setBounds(139.75, 88.75, 30.0, 30.0);
      surface.hardwareElementWithId("pad13").setBounds(34.75, 55.75, 30.0, 30.0);
      surface.hardwareElementWithId("pad14").setBounds(69.75, 55.75, 30.0, 30.0);
      surface.hardwareElementWithId("pad15").setBounds(104.75, 55.75, 30.0, 30.0);
      surface.hardwareElementWithId("pad16").setBounds(139.75, 55.75, 30.0, 30.0);
      surface.hardwareElementWithId("encoder1").setBounds(38.75, 21.5, 25.0, 25.0);
      surface.hardwareElementWithId("encoder2").setBounds(72.5, 21.5, 25.0, 25.0);
      surface.hardwareElementWithId("encoder3").setBounds(106.25, 21.5, 25.0, 25.0);
      surface.hardwareElementWithId("encoder4").setBounds(140.25, 21.5, 25.0, 25.0);
   }

   private void initLayers()
   {
      mBaseLayer = createLayer("Base");
      mStepsLayer = createLayer("Steps");
      mStepsZoomLayer = createLayer("Steps Zoom");
      mStepsSetupLoopLayer = createLayer("Steps Setup Loop");
      mLauncherClipsLayer = createLayer("Launcher Clips");
      mNoteRepeatLayer = createLayer("Note Repeat");
      mNoteRepeatShiftLayer = createLayer("Note Repeat Shift");

      initBaseLayer();
      initStepsLayer();
      initStepsZoomLayer();
      initStepsSetupLoopLayer();
      initLauncherClipsLayer();
      initNoteRepeatLayer();
      initNoteRepeatShiftLayer();

      // DebugUtilities.createDebugLayer(mLayers, mHardwareSurface).activate();
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
      mBaseLayer.bindReleased(mSelectButton, mLauncherClipsLayer.getDeactivateAction());
      mBaseLayer.bind(() -> getClipColor(mCursorClip.clipLauncherSlot()), mSelectButton);

      mBaseLayer.bindToggle(mEditorButton, mStepsLayer);

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

         mNoteInput.setVelocityTranslationTable(
            fullLevelIsOn.getAsBoolean() ? NoteInputUtils.FULL_VELOCITY : NoteInputUtils.NORMAL_VELOCITY);
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

   private void initStepsLayer()
   {
      mStepsLayer.bindToggle(mUpButton, () -> scrollKeys(1), mCursorClip.canScrollKeysUp());
      mStepsLayer.bindToggle(mDownButton, () -> scrollKeys(-1), mCursorClip.canScrollKeysDown());
      mStepsLayer.bindToggle(mLeftButton, () -> scrollPage(-1), mCursorClip.canScrollStepsBackwards());
      mStepsLayer.bindToggle(mRightButton, () -> scrollPage(1), mCursorClip.canScrollStepsForwards());

      mStepsLayer.bindToggle(mZoomButton, mStepsZoomLayer);
      mStepsLayer.bindToggle(mSetLoopButton, mStepsSetupLoopLayer);

      for (int i = 0; i < 16; i++)
      {
         final HardwareButton padButton = mPadButtons[i];

         final int padIndex = i;

         mStepsLayer.bindPressed(padButton, pressure -> {
            if (mShift)
            {
               mCursorClip.scrollToKey(36 + padIndex);
               mCurrentPadForSteps = padIndex;
               mCursorTrack.playNote(36 + padIndex, 100);
            }
            else
               mCursorClip.toggleStep(padIndex, 0, (int)Math.round(pressure * 127));
         });
         mStepsLayer.bind(() -> getStepsPadColor(padIndex), padButton);
      }
   }

   private void initStepsZoomLayer()
   {
      for (int i = 0; i < 16; i++)
      {
         final HardwareButton padButton = mPadButtons[i];

         final int padIndex = i;

         mStepsZoomLayer.bindPressed(padButton, () -> {
            mCurrentPageForSteps = padIndex;
            mCursorClip.scrollToStep(16 * mCurrentPageForSteps);
         });
         mStepsZoomLayer.bind(() -> getStepsZoomPadColor(padIndex), padButton);
      }
   }

   private void initStepsSetupLoopLayer()
   {
      for (int i = 0; i < 16; i++)
      {
         final HardwareButton padButton = mPadButtons[i];

         final int padIndex = i;

         mStepsSetupLoopLayer.bindPressed(padButton, () -> {
            if (padIndex == 14)
            {
               mCursorClip.getLoopLength().set(Math.max(getPageLengthInBeatTime(),
                  mCursorClip.getLoopLength().get() - getPageLengthInBeatTime()));
            }
            else if (padIndex == 15)
            {
               mCursorClip.getLoopLength().set(mCursorClip.getLoopLength().get() + getPageLengthInBeatTime());
            }
            else
            {
               // mCursorClip.getLoopStart().set(padIndex * getPageLengthInBeatTime());
            }
         });
         mStepsZoomLayer.bind(() -> getStepsSetupLoopPadColor(padIndex), padButton);
      }
   }

   private void initLauncherClipsLayer()
   {
      for (int i = 0; i < 16; i++)
      {
         final HardwareButton padButton = mPadButtons[i];

         final int padIndex = i;

         final ClipLauncherSlot slot = mCursorTrack.clipLauncherSlotBank().getItemAt(padIndex);

         mLauncherClipsLayer.bindPressed(padButton, () -> {
            slot.select();
            slot.launch();
         });
         mLauncherClipsLayer.bind(() -> slot.hasContent().get() ? getClipColor(slot) : (Color)null,
            padButton);
      }
   }

   private void initNoteRepeatLayer()
   {
      mNoteRepeatLayer.bindToggle(mShiftButton, mNoteRepeatShiftLayer);
   }

   private void initNoteRepeatShiftLayer()
   {
      final double timings[] = { 1, 1.0 / 2, 1.0 / 4, 1.0 / 8, 3.0 / 4.0, 3.0 / 8.0, 3.0 / 16.0, 3.0 / 32.0 };

      final Runnable doNothing = () -> {
      };
      final Supplier<Color> noColor = () -> null;

      for (int i = 0; i < 8; i++)
      {
         final double timing = timings[i];

         final HardwareButton padButton = mPadButtons[i];

         mNoteRepeatShiftLayer.bindPressed(padButton, () -> mArpeggiator.period().set(timing));
         mNoteRepeatShiftLayer.bind(() -> mArpeggiator.period().get() == timing ? RED : DIM_RED, padButton);

         mNoteRepeatShiftLayer.bindPressed(mPadButtons[i + 8], doNothing);
         mNoteRepeatShiftLayer.bind(noColor, mPadButtons[i + 8]);
      }
   }

   private Color getStepsZoomPadColor(final int padIndex)
   {
      final int numStepPages = getNumStepPages();

      final int playingPage = mCursorClip.playingStep().get() / 16;

      if (padIndex < numStepPages)
      {
         Color clipColor = mCursorClip.color().get();

         if (padIndex != mCurrentPageForSteps)
            clipColor = Color.mix(clipColor, BLACK, 0.5f);

         if (padIndex == playingPage)
            return Color.mix(clipColor, WHITE, 1 - getTransportPulse(1.0, 1));

         return clipColor;
      }

      return BLACK;
   }

   private Color getStepsSetupLoopPadColor(final int padIndex)
   {
      if (padIndex == 14 || padIndex == 15)
      {
         return WHITE;
      }

      final int numStepPages = getNumStepPages();

      final int playingPage = mCursorClip.playingStep().get() / 16;

      if (padIndex < numStepPages)
      {
         final Color clipColor = mCursorClip.color().get();

         if (padIndex == playingPage)
            return Color.mix(clipColor, WHITE, 1 - getTransportPulse(1.0, 1));

         return clipColor;
      }

      return BLACK;
   }

   private Color getStepsPadColor(final int padIndex)
   {
      if (mShift)
      {
         if (mCurrentPadForSteps == padIndex)
         {
            return WHITE;
         }

         final int playingNote = velocityForPlayingNote(padIndex);

         if (playingNote > 0)
         {
            return mixColorWithWhite(clipColor(0.3f), playingNote);
         }

         return clipColor(0.3f);
      }

      if (mPlayingStep == padIndex + mCurrentPageForSteps * 16)
      {
         return WHITE;
      }

      final boolean isNewNote = mStepData[padIndex] == 2;
      final boolean hasData = mStepData[padIndex] > 0;

      if (isNewNote)
         return Color.mix(mCursorClip.color().get(), WHITE, 0.5f);
      else if (hasData)
         return mCursorClip.color().get();
      else
         return Color.mix(mCursorClip.color().get(), BLACK, 0.8f);
   }

   private Color clipColor(final float scale)
   {
      final Color c = mCursorClip.color().get();

      return Color.fromRGB(c.getRed() * scale, c.getGreen() * scale, c.getBlue() * scale);
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

   private HardwareButton createToggleButton(
      final String id,
      final int controlNumber,
      final Color onLightColor)
   {
      final HardwareButton button = createButton(id, controlNumber);
      final OnOffHardwareLight light = mHardwareSurface.createOnOffHardwareLight(id + "_light");

      final Color offColor = Color.mix(onLightColor, Color.blackColor(), 0.5);

      light.setStateToVisualStateFuncation(
         isOn -> isOn ? HardwareLightVisualState.createForColor(onLightColor, Color.blackColor())
            : HardwareLightVisualState.createForColor(offColor, Color.blackColor()));

      button.setBackgroundLight(light);

      light.isOn().onUpdateHardware(value -> {
         mMidiOut.sendMidi(0xB0, controlNumber, value ? 127 : 0);
      });

      return button;
   }

   private HardwareButton createRGBButton(final String id, final int controlNumber)
   {
      final HardwareButton button = createButton(id, controlNumber);

      final MultiStateHardwareLight light = mHardwareSurface.createMultiStateHardwareLight(id + "_light");
      light.setLabelColor(BLACK);

      light.state().onUpdateHardware(new LightStateSender(0xB0, controlNumber));

      light.setColorToStateFunction(color -> new RGBLightState(color));

      button.setBackgroundLight(light);

      return button;
   }

   private HardwareButton createButton(final String id, final int controlNumber)
   {
      final HardwareButton button = mHardwareSurface.createHardwareButton(id);
      final MidiExpressions midiExpressions = getHost().midiExpressions();

      button.pressedAction().setActionMatcher(mMidiIn
         .createActionMatcher(midiExpressions.createIsCCExpression(0, controlNumber) + " && data2 > 0"));
      button.releasedAction().setActionMatcher(mMidiIn.createCCActionMatcher(0, controlNumber, 0));
      button.setLabelColor(BLACK);

      return button;
   }

   private void createPadButton(final int index)
   {
      final HardwareButton pad = mHardwareSurface.createHardwareButton("pad" + (index + 1));
      pad.setLabel("Pad " + (index + 1));
      pad.setLabelColor(BLACK);

      final int note = 0x24 + index;
      pad.pressedAction().setPressureActionMatcher(mMidiIn.createNoteOnVelocityValueMatcher(0, note));
      pad.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(0, note));

      mPadButtons[index] = pad;

      final MultiStateHardwareLight light = mHardwareSurface
         .createMultiStateHardwareLight("pad_light" + (index + 1));

      light.state().onUpdateHardware(new LightStateSender(0x90, 0x24 + index));

      light.setColorToStateFunction(color -> new RGBLightState(color));

      pad.setBackgroundLight(light);

      mPadLights[index] = light;
   }

   private class LightStateSender implements Consumer<RGBLightState>
   {
      protected LightStateSender(final int statusStart, final int data1)
      {
         super();
         mStatusStart = statusStart;
         mData1 = data1;
      }

      @Override
      public void accept(final RGBLightState state)
      {
         mValues[0] = state != null ? (state.isOn() ? 127 : 0) : 0;
         mValues[1] = state != null ? state.getRed() : 0;
         mValues[2] = state != null ? state.getGreen() : 0;
         mValues[3] = state != null ? state.getBlue() : 0;

         for (int i = 0; i < 4; i++)
         {
            if (mValues[i] != mLastSent[i])
            {
               mMidiOut.sendMidi(mStatusStart + i, mData1, mValues[i]);
               mLastSent[i] = mValues[i];
            }
         }
      }

      private final int mStatusStart, mData1;

      private final int[] mLastSent = new int[4];

      private final int[] mValues = new int[4];
   }

   private void createEncoder(final int index)
   {
      final RelativeHardwareKnob encoder = mHardwareSurface
         .createRelativeHardwareKnob("encoder" + (index + 1));
      encoder.setLabel(String.valueOf(index + 1));
      encoder.setAdjustValueMatcher(mMidiIn.createRelativeSignedBitCCValueMatcher(0, CC_ENCODER_1 + index));
      encoder.setSensitivity(2.5);

      mEncoders[index] = encoder;
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

   private Layer mBaseLayer, mStepsLayer, mLauncherClipsLayer, mNoteRepeatLayer, mNoteRepeatShiftLayer,
      mStepsZoomLayer, mStepsSetupLoopLayer;

   private Arpeggiator mArpeggiator;

   private SceneBank mSceneBank;
}
