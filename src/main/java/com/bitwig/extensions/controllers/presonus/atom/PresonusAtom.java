package com.bitwig.extensions.controllers.presonus.atom;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import com.bitwig.extension.controller.api.Application;
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
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.presonus.ButtonTarget;
import com.bitwig.extensions.controllers.presonus.ControllerExtensionWithModes;

public class PresonusAtom extends ControllerExtensionWithModes
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

      midiIn.setMidiCallback(this::onMidi);
      mNoteInput = midiIn.createNoteInput("Pads", "80????", "90????", "a0????");
      mNoteInput.setShouldConsumeEvents(true);

      mMidiOut = host.getMidiOutPort(0);

      mCursorTrack = host.createCursorTrack(0, 0);

      mCursorDevice =
         mCursorTrack.createCursorDevice("ATOM", "Atom", 0, CursorDeviceFollowMode.FIRST_INSTRUMENT);

      mRemoteControls = mCursorDevice.createCursorRemoteControlsPage(4);
      mRemoteControls.setHardwareLayout(HardwareControlType.KNOB, 4);
      for (int i = 0; i < 4; ++i)
         mRemoteControls.getParameter(i).setIndication(true);

      mTransport = host.createTransport();

      initPads();

      initButtons();

      // Turn on Native Mode
      mMidiOut.sendMidi(0x8f, 0, 127);
   }

   @Override
   public void exit()
   {
      // Turn off Native Mode
      mMidiOut.sendMidi(0x8f, 0, 0);
   }

   @Override
   protected MidiOut getMidiOut()
   {
      return mMidiOut;
   }

   private void initPads()
   {
      mDrumPadBank = mCursorDevice.createDrumPadBank(16);

      Pad[] pads = new Pad[16];

      for(int i=0; i<16; i++)
      {
         final int padIndex = i;
         pads[padIndex] = new Pad(mMidiOut, padIndex);
         DrumPad drumPad = mDrumPadBank.getItemAt(padIndex);
         SettableColorValue color = drumPad.color();
         color.addValueObserver((r,g,b) -> pads[padIndex].setColor(r,g,b));
         drumPad.exists().addValueObserver(e -> pads[padIndex].setHasChain(e));
         pads[padIndex].setColor(color.red(), color.green(), color.blue());
         //mFlushables.add(pads[padIndex]);
      }

      mCursorTrack.playingNotes().addValueObserver(notes ->
      {
         BitSet playing = new BitSet(128);
         for (PlayingNote note : notes)
         {
            playing.set(note.pitch());
         }

         for(int i=0; i<16; i++)
         {
            pads[i].setOn(playing.get(36 + i));
         }
      });
   }

   private void initButtons()
   {
      mTransport.isPlaying().markInterested();

      Button shiftButton = addElement(new Button(CC_SHIFT));
      bind(shiftButton, new ButtonTarget()
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
      bindToggle(clickToggle, mTransport.isMetronomeEnabled());

      Button playButton = addElement(new Button(CC_PLAY_LOOP_TOGGLE));
      bindPressedRunnable(playButton, mTransport.isPlaying(), () ->
      {
         if (mShift) mTransport.isArrangerLoopEnabled().toggle();
         else mTransport.togglePlay();
      });

      Button stopButton = addElement(new Button(CC_STOP_UNDO));
      bind(stopButton, new ButtonTarget()
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


            //initButton(CC_SHIFT, b -> mShift = b);
/*
      final Button metronome = initButton(CC_CLICK_COUNT_IN, mTransport.isMetronomeEnabled()::toggle);
      mTransport.isMetronomeEnabled().addValueObserver(record -> metronome.setState(
         record ? Button.State.ON : Button.State.OFF));

      final RGBButton playButton =
         initRGBButton(CC_PLAY_LOOP_TOGGLE, mTransport::togglePlay, mTransport.isArrangerLoopEnabled()::toggle);
      mTransport.isPlaying().addValueObserver(isPlaying -> playButton.setState(
         isPlaying ? Button.State.ON : Button.State.OFF));

      initButton(CC_STOP_UNDO, mTransport::stop, mApplication::undo);

      final Button record = initButton(CC_RECORD_SAVE, mTransport::record, () ->
      {
         Action saveAction = mApplication.getAction("Save");
         if (saveAction != null)
         {
            saveAction.invoke();
         }
      });

      mTransport.isArrangerRecordEnabled().addValueObserver(r -> record.setState(
         r ? Button.State.ON : Button.State.OFF));

      initButton(CC_UP, mCursorTrack::selectPrevious, mCursorTrack.hasPrevious());
      initButton(CC_DOWN, mCursorTrack::selectNext, mCursorTrack.hasNext());
      initButton(CC_LEFT, mCursorDevice::selectPrevious, mCursorDevice.hasPrevious());
      initButton(CC_RIGHT, mCursorDevice::selectNext, mCursorDevice.hasNext());
      final RGBButton selectButton =
         initRGBButton(CC_SELECT, mApplication::enter);

      initButton(CC_ZOOM, mApplication::zoomIn, mApplication::zoomOut);*/
   }
/*
   private Button initButton(int data1, Consumer<Boolean> booleanConsumer)
   {
      Button button = new Button(mMidiOut, data1, booleanConsumer);

      mButtons.add(button);
      return button;
   }

   private Button initButton(int data1, Runnable runnable)
   {
      Button button = new Button(mMidiOut, data1, (b) ->
      {
         if (b) runnable.run();
      });

      mButtons.add(button);
      return button;
   }

   private Button initButton(int data1, Runnable runnable, BooleanValue shouldBeOn)
   {
      Button button = new Button(mMidiOut, data1, (b) ->
      {
         if (b) runnable.run();
      });

      shouldBeOn.addValueObserver(on -> button.setState(on ? Button.State.ON : Button.State.OFF));

      mButtons.add(button);
      return button;
   }

   private RGBButton initRGBButton(int data1, Runnable runnable)
   {
      RGBButton button = new RGBButton(mMidiOut, data1, (b) ->
      {
         if (b) runnable.run();
      });

      mButtons.add(button);
      return button;
   }

   private Button initButton(int data1, Runnable runnable, Runnable shiftRunnable)
   {
      Button button = new Button(mMidiOut, data1, (b) ->
      {
         if (b) (mShift ? shiftRunnable : runnable).run();
      });

      mButtons.add(button);
      return button;
   }


   private RGBButton initRGBButton(int data1, Runnable runnable, Runnable shiftRunnable)
   {
      RGBButton button = new RGBButton(mMidiOut, data1, (b) ->
      {
         if (b) (mShift ? shiftRunnable : runnable).run();
      });

      mButtons.add(button);
      return button;
   }*/

   /*protected void onMidi(final int status, final int data1, final int data2)
   {
      if (status == 176)
      {
         if (data1 >= CC_ENCODER_1 && data1 <= CC_ENCODER_4)
         {
            int index = data1 - CC_ENCODER_1;
            int diff = data2 & 0x3f;
            if( (data2 & 0x40) != 0) diff = -diff;
            mRemoteControls.getParameter(index).inc(diff, 101);
         }
      }
   }*/

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
   private Button mMetronomeButton;
}
