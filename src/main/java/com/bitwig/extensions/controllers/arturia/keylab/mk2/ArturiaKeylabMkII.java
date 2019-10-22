package com.bitwig.extensions.controllers.arturia.keylab.mk2;

import com.bitwig.extension.api.util.midi.SysexBuilder;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.Action;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.BrowserFilterItem;
import com.bitwig.extension.controller.api.BrowserResultsItem;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareControlType;
import com.bitwig.extension.controller.api.MasterTrack;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PopupBrowser;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.LayeredControllerExtension;
import com.bitwig.extensions.framework.targets.ButtonTarget;
import com.bitwig.extensions.framework.targets.EncoderTarget;

// TODO
// Multi (mixer) mode
// Figure out how to control the lights, category/preset buttons
// Stuck notes?

public class ArturiaKeylabMkII extends LayeredControllerExtension
{
   public ArturiaKeylabMkII(
      final ArturiaKeylabMkIIControllerExtensionDefinition definition,
      final ControllerHost host)
   {
      super(definition, host);

      mNumberOfKeys = definition.getNumberOfKeys();
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();

      mTransport = host.createTransport();
      mTransport.isPlaying().markInterested();
      mTransport.isArrangerRecordEnabled().markInterested();
      mTransport.isArrangerLoopEnabled().markInterested();
      mTransport.isArrangerAutomationWriteEnabled().markInterested();
      mTransport.isPunchInEnabled().markInterested();
      mTransport.isPunchOutEnabled().markInterested();
      mTransport.isMetronomeEnabled().markInterested();
      mCursorTrack = host.createCursorTrack(4, 0);
      mCursorTrack.volume().setIndication(true);
      mCursorTrack.solo().markInterested();
      mCursorTrack.mute().markInterested();
      mCursorTrack.arm().markInterested();
      mCursorTrack.exists().markInterested();
      mCursorTrack.hasNext().markInterested();
      mCursorTrack.hasPrevious().markInterested();
      mDevice = mCursorTrack.createCursorDevice();
      mDevice.exists().markInterested();
      mDevice.hasNext().markInterested();
      mRemoteControls = mDevice.createCursorRemoteControlsPage(8);
      mRemoteControls.setHardwareLayout(HardwareControlType.ENCODER, 8);
      mDeviceEnvelopes = mDevice.createCursorRemoteControlsPage("envelope", 9, "envelope");
      mDeviceEnvelopes.setHardwareLayout(HardwareControlType.SLIDER, 9);

      mApplication = getHost().createApplication();
      mSaveAction = mApplication.getAction("Save");

      mPopupBrowser = host.createPopupBrowser();
      mPopupBrowser.exists().markInterested();

      mBrowserResult = mPopupBrowser.resultsColumn().createCursorItem();
      mBrowserCategory = mPopupBrowser.categoryColumn().createCursorItem();
      mBrowserCreator = mPopupBrowser.creatorColumn().createCursorItem();
      mBrowserResult.name().markInterested();
      mBrowserCategory.name().markInterested();
      mBrowserCreator.name().markInterested();

      for(int i=0; i<8; i++)
      {
         mRemoteControls.getParameter(i).setIndication(true);
         mRemoteControls.getParameter(i).name().markInterested();
         mRemoteControls.getParameter(i).value().markInterested();
         mRemoteControls.getParameter(i).value().displayedValue().markInterested();
      }

      for(int i=0; i<9; i++)
      {
         mDeviceEnvelopes.getParameter(i).setIndication(true);
         mDeviceEnvelopes.getParameter(i).setLabel("F" + Integer.toString(i+1));
         mDeviceEnvelopes.getParameter(i).name().markInterested();
         mDeviceEnvelopes.getParameter(i).value().markInterested();
         mDeviceEnvelopes.getParameter(i).value().displayedValue().markInterested();
      }

      mRemoteControls.getName().markInterested();
      mRemoteControls.getParameter(0).setLabel("E1");
      mRemoteControls.getParameter(1).setLabel("E2");
      mRemoteControls.getParameter(2).setLabel("E3");
      mRemoteControls.getParameter(3).setLabel("E4");
      mRemoteControls.getParameter(4).setLabel("E5");
      mRemoteControls.getParameter(5).setLabel("E6");
      mRemoteControls.getParameter(6).setLabel("E7");
      mRemoteControls.getParameter(7).setLabel("E8");

      mRemoteControls.selectedPageIndex().markInterested();

      mTrackBank = host.createTrackBank(8, 0, 0);
      mMasterTrack = host.createMasterTrack(0);

      mNoteInput = host.getMidiInPort(0).createNoteInput("Keys", "?0????", "?1????", "?2????", "?3????", "?4????", "?5????", "?6????", "?7????", "?8????");
      mNoteInput.setShouldConsumeEvents(true);

      final NoteInput drumPadsInput = host.getMidiInPort(0).createNoteInput("Pads", "?9????");

      mCursorTrack.name().markInterested();
      mDevice.name().markInterested();
      mDevice.presetName().markInterested();
      mDevice.presetCategory().markInterested();
      mDevice.presetCreator().markInterested();

      host.getMidiInPort(1).setMidiCallback(getMidiCallbackToUseForLayers());

      mIsTransportDown = new boolean[5];

      final ControllerExtensionDefinition definition = getExtensionDefinition();

      sendSysex("F0 00 20 6B 7F 42 02 00 40 52 00 F7"); // Init DAW preset in Default MCU mode

      sendSysex("F0 00 20 6B 7F 42 05 02 F7"); // Set to DAW mode

      updateIndications();

      initButtons();
      initControls();
      mDisplay = addElement(new Display());
      activateLayer(mBaseLayer);

      mBaseLayer.bind(mDisplay, new DisplayTarget()
      {
         @Override
         public String getUpperText()
         {
            return mCursorTrack.name().getLimited(16);
         }

         @Override
         public String getLowerText()
         {
            return mCursorTrack.exists().get()
               ? mDevice.exists().get() ? (mDevice.name().getLimited(16)) : "No Device"
               : "";
         }
      });

      mPopupBrowser.exists().addValueObserver(exists -> {
         if (exists) activateLayer(mBrowserLayer);
         else deactivateLayer(mBrowserLayer);
      });

      mBrowserLayer.bind(mDisplay, new DisplayTarget()
      {
         @Override
         public String getUpperText()
         {
            return mBrowserCategory.name().getLimited(16);
         }

         @Override
         public String getLowerText()
         {
            return mBrowserResult.name().getLimited(16);
         }
      });

      Button multi = addElement(new Button(Buttons.SELECT_MULTI));
      mBaseLayer.bindLayerToggle(this, multi, mMultiLayer);
   }

   private void repeatRewind()
   {
      if (mIsTransportDown[0])
      {
         mTransport.rewind();
         getHost().scheduleTask(this::repeatRewind, 100);
      }
   }

   private void repeatForward()
   {
      if (mIsTransportDown[1])
      {
         mTransport.fastForward();
         getHost().scheduleTask(this::repeatForward, 100);
      }
   }

   private void setMonochromeLed(Buttons button, final int intensity)
   {
      SysexBuilder sb = SysexBuilder.fromHex("F0 00 20 6B 7F 42 02 00 10");
      sb.addByte(button.getSysexID());
      sb.addByte(intensity);
      sendSysex(sb.terminate());
   }

   private void updateIndications()
   {
      mDevice.setIsSubscribed(true);
      mCursorTrack.setIsSubscribed(true);
   }

   private void initButtons()
   {
      Button rewind = addElement(new Button(Buttons.REWIND));
      mBaseLayer.bind(rewind, new ButtonTarget()
      {
         @Override
         public boolean get()
         {
            return mIsTransportDown[0];
         }

         @Override
         public void set(final boolean pressed)
         {
            mIsTransportDown[0] = pressed;

            if (pressed)
            {
               repeatRewind();
            }
         }
      });

      Button forward = addElement(new Button(Buttons.FORWARD));
      mBaseLayer.bind(forward, new ButtonTarget()
      {
         @Override
         public boolean get()
         {
            return mIsTransportDown[1];
         }

         @Override
         public void set(final boolean pressed)
         {
            mIsTransportDown[1] = pressed;

            if (pressed)
            {
               repeatForward();
            }
         }
      });

      Button stop = addElement(new Button(Buttons.STOP));
      mBaseLayer.bindPressedRunnable(stop, null, mTransport::stop);
      Button play = addElement(new Button(Buttons.PLAY_OR_PAUSE));
      mBaseLayer.bindPressedRunnable(play, mTransport.isPlaying(), mTransport::play);
      Button record = addElement(new Button(Buttons.RECORD));
      mBaseLayer.bindPressedRunnable(record, mTransport.isArrangerRecordEnabled(), mTransport::record);
      Button loop = addElement(new Button(Buttons.LOOP));
      mBaseLayer.bindToggle(loop, mTransport.isArrangerLoopEnabled());
      Button metronome = addElement(new Button(Buttons.METRO));
      mBaseLayer.bindToggle(metronome, mTransport.isMetronomeEnabled());
      Button undo = addElement(new Button(Buttons.UNDO));
      mBaseLayer.bindPressedRunnable(undo, null, mApplication::undo);
      Button save = addElement(new Button(Buttons.SAVE));
      mBaseLayer.bindPressedRunnable(save, null, mSaveAction::invoke);

      Button punchIn = addElement(new Button(Buttons.PUNCH_IN));
      mBaseLayer.bindToggle(punchIn, mTransport.isPunchInEnabled());
      Button punchOut = addElement(new Button(Buttons.PUNCH_OUT));
      mBaseLayer.bindToggle(punchOut, mTransport.isPunchOutEnabled());

      Button solo = addElement(new Button(Buttons.SOLO));
      mBaseLayer.bindToggle(solo, mCursorTrack.solo());
      Button mute = addElement(new Button(Buttons.MUTE));
      mBaseLayer.bindToggle(mute, mCursorTrack.mute());
      Button arm = addElement(new Button(Buttons.RECORD_ARM));
      mBaseLayer.bindToggle(arm, mCursorTrack.arm());
      Button write = addElement(new Button(Buttons.WRITE));
      mBaseLayer.bindToggle(write, mTransport.isArrangerAutomationWriteEnabled());

      Button prev = addElement(new Button(Buttons.PREVIOUS));
      mBaseLayer.bindPressedRunnable(prev, null, mRemoteControls::selectPrevious);
      Button next = addElement(new Button(Buttons.NEXT));
      mBaseLayer.bindPressedRunnable(next, null, mRemoteControls::selectNext);

      addPageButtonMapping(Buttons.SELECT1, "preset");
      addPageButtonMapping(Buttons.SELECT2, "overview");
      addPageButtonMapping(Buttons.SELECT3, "oscillator");
      addPageButtonMapping(Buttons.SELECT4, "mix");
      addPageButtonMapping(Buttons.SELECT5, "filter");
      addPageButtonMapping(Buttons.SELECT6, "lfo");
      addPageButtonMapping(Buttons.SELECT7, "envelope");
      addPageButtonMapping(Buttons.SELECT8, "fx");

      Button presetPrev = addElement(new Button(Buttons.PRESET_PREVIOUS));
      mBaseLayer.bindPressedRunnable(presetPrev, mCursorTrack.hasPrevious(), mCursorTrack::selectPrevious);
      mBrowserLayer.bindPressedRunnable(presetPrev, mCursorTrack.hasPrevious(), mPopupBrowser::cancel);

      Button presetNext = addElement(new Button(Buttons.PRESET_NEXT));
      mBaseLayer.bindPressedRunnable(presetNext, mCursorTrack.hasNext(), mCursorTrack::selectNext);
      mBrowserLayer.bindPressedRunnable(presetNext, mCursorTrack.hasPrevious(), mPopupBrowser::commit);

      Button wheelClick = addElement(new Button(Buttons.WHEEL_CLICK));
      mBaseLayer.bindPressedRunnable(wheelClick, null, () ->
      {
         mDisplay.reset();
         startPresetBrowsing();
      });

      mBrowserLayer.bindPressedRunnable(wheelClick, mCursorTrack.hasPrevious(), mPopupBrowser::commit);
   }

   private void addPageButtonMapping(final Buttons id, final String group)
   {
      Button button = addElement(new Button(id));
      mBaseLayer.bindPressedRunnable(button, null, () -> mRemoteControls.selectNextPageMatching(group, true));
   }

   @Override
   protected MidiOut getMidiOutToUseForLayers()
   {
      return getMidiOutPort(1);
   }

   private void initControls()
   {
      Encoder encoder9 = addElement(new Encoder(24));

      mBaseLayer.bindEncoder(encoder9, mCursorTrack.volume(), 101);

      for(int i=0; i<8; i++)
      {
         Encoder encoder = addElement(new Encoder(0x10 + i));

         mBaseLayer.bindEncoder(encoder, mRemoteControls.getParameter(i), 101);
         mMultiLayer.bindEncoder(encoder, mTrackBank.getItemAt(i).pan(), 101);
      }

      Encoder wheel = addElement(new Encoder(0x3C));
      mBaseLayer.bind(wheel, (EncoderTarget) steps ->
      {
         if (steps > 0) mDevice.selectNext();
         else mDevice.selectPrevious();
      });

      mBrowserLayer.bind(wheel, (EncoderTarget) steps ->
      {
         if (steps > 0) mPopupBrowser.selectNextFile();
         else mPopupBrowser.selectPreviousFile();
      });

      for(int i=0; i<9; i++)
      {
         Fader fader = addElement(new Fader(i));
         mBaseLayer.bind(fader, mDeviceEnvelopes.getParameter(i));

         if (i == 8)
            mMultiLayer.bind(fader, mMasterTrack.volume());
         else
            mMultiLayer.bind(fader, mTrackBank.getItemAt(i).volume());
      }
   }

   private void startPresetBrowsing()
   {
      if (mDevice.exists().get())
      {
         mDevice.replaceDeviceInsertionPoint().browse();
      }
      else
      {
         mDevice.deviceChain().endOfDeviceChainInsertionPoint().browse();
      }
   }

   @Override
   public void exit()
   {
   }

   public void sendSysex(final byte[] data)
   {
      getHost().getMidiOutPort(0).sendSysex(data);
   }

   public void sendSysex(final String data)
   {
      getHost().getMidiOutPort(0).sendSysex(data);
   }

   private NoteInput mNoteInput;
   private final int mNumberOfKeys;
   private Transport mTransport;
   private CursorTrack mCursorTrack;
   private CursorDevice mDevice;
   private CursorRemoteControlsPage mRemoteControls;
   private CursorRemoteControlsPage mDeviceEnvelopes;
   private PopupBrowser mPopupBrowser;
   private BrowserResultsItem mBrowserResult;
   private BrowserFilterItem mBrowserCategory;
   private BrowserFilterItem mBrowserCreator;
   private Application mApplication;
   private boolean[] mIsTransportDown;
   private Display mDisplay;

   private Action mSaveAction;
   private Layer mBaseLayer = new Layer();
   private Layer mBrowserLayer = new Layer();
   private Layer mMultiLayer = new Layer();
   private TrackBank mTrackBank;
   private MasterTrack mMasterTrack;
}
