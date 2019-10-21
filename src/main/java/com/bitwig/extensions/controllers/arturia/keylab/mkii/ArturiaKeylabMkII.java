package com.bitwig.extensions.controllers.arturia.keylab.mkii;

import java.util.Arrays;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
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
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PopupBrowser;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.presonus.framework.Layer;
import com.bitwig.extensions.controllers.presonus.framework.LayeredControllerExtension;
import com.bitwig.extensions.controllers.presonus.framework.targets.ButtonTarget;
import com.bitwig.extensions.controllers.presonus.framework.targets.EncoderTarget;

// TODO
// Multi (mixer) mode
// Figure out how to control the lights, category/preset buttons
// Stuck notes?

public class ArturiaKeylabMkII extends LayeredControllerExtension
{
   DisplayMode mDisplayMode = null;

   enum DisplayMode
   {
      BROWSER,
   }

   void setDisplayMode(final DisplayMode displayMode)
   {
      mDisplayMode = displayMode;

      mLastDisplayTimeStamp = System.currentTimeMillis();
   }

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

      mNoteInput = host.getMidiInPort(0).createNoteInput("Keys", "?0????", "?1????", "?2????", "?3????", "?4????", "?5????", "?6????", "?7????", "?8????");
      mNoteInput.setShouldConsumeEvents(true);

      final NoteInput drumPadsInput = host.getMidiInPort(0).createNoteInput("Pads", "?9????");

      mCursorTrack.name().markInterested();
      mDevice.name().markInterested();
      mDevice.presetName().markInterested();
      mDevice.presetCategory().markInterested();
      mDevice.presetCreator().markInterested();

      host.getMidiInPort(1).setMidiCallback(this::onMidi);

      mIsTransportDown = new boolean[5];

      final ControllerExtensionDefinition definition = getExtensionDefinition();

      sendSysex("F0 00 20 6B 7F 42 02 00 40 52 00 F7"); // Init DAW preset in Default MCU mode

      sendSysex("F0 00 20 6B 7F 42 05 02 F7"); // Set to DAW mode

      sendTextToKeyLab(
         definition.getHardwareVendor(),
         definition.getHardwareModel() + " " + definition.getVersion());

      updateIndications();

      initButtons();
      initControls();
      activateLayer(mBaseLayer);

      host.scheduleTask(this::displayRefreshTimer, 1000);
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

   private void setRGBLed(Buttons button, final int red, final int green, final int blue)
   {
      SysexBuilder sb = SysexBuilder.fromHex("F0 00 20 6B 7F 42 02 00 16");
      sb.addByte(button.getSysexID());
      sb.addByte(red);
      sb.addByte(green);
      sb.addByte(blue);
      sendSysex(sb.terminate());
   }

   private void setMonochromeLed(Buttons button, final int intensity)
   {
      SysexBuilder sb = SysexBuilder.fromHex("F0 00 20 6B 7F 42 02 00 10");
      sb.addByte(button.getSysexID());
      sb.addByte(intensity);
      sendSysex(sb.terminate());
   }

   // Keep checking the display for mode changes
   private void displayRefreshTimer()
   {
      if (mDisplayMode != null)
      {
         switch (mDisplayMode)
         {
            case BROWSER:
               if (!mPopupBrowser.exists().get())
               {
                  setDisplayMode(null);
               }
               break;
         }
      }
      else
      {
         if (mPopupBrowser.exists().get())
         {
            setDisplayMode(DisplayMode.BROWSER);
         }
      }

      getHost().scheduleTask(this::displayRefreshTimer, 100);
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
      mBaseLayer.bindPressedRunnable(presetPrev, mCursorTrack.hasPrevious(), () ->
      {
         if (isInBrowser())
         {
            mPopupBrowser.cancel();
         }
         else if (mDisplayMode == null)
         {
            mCursorTrack.selectPrevious();
         }
      });

      Button presetNext = addElement(new Button(Buttons.PRESET_NEXT));
      mBaseLayer.bindPressedRunnable(presetNext, mCursorTrack.hasNext(), () ->
      {
         if (isInBrowser())
         {
            mPopupBrowser.commit();
         }
         else if (mDisplayMode == null)
         {
            mCursorTrack.selectNext();
         }
      });

      Button wheelClick = addElement(new Button(Buttons.WHEEL_CLICK));
      mBaseLayer.bindPressedRunnable(wheelClick, null, () ->
      {
         mLastText = null;

         if (isInBrowser())
         {
            mPopupBrowser.commit();
            setDisplayMode(null);
         }
         else
         {
            startPresetBrowsing();
         }
      });
   }

   private void addPageButtonMapping(final Buttons id, final String group)
   {
      Button button = addElement(new Button(id));
      mBaseLayer.bindPressedRunnable(button, null, () -> mRemoteControls.selectNextPageMatching(group, true));
   }

   @Override
   protected MidiOut getMidiOut()
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
      }

      Encoder wheel = addElement(new Encoder(0x3C));
      mBaseLayer.bind(wheel, (EncoderTarget) steps ->
      {
         boolean next = steps > 0;

         if (mDisplayMode == DisplayMode.BROWSER)
         {
            if (next) mPopupBrowser.selectNextFile();
            else mPopupBrowser.selectPreviousFile();
         }
         else if (mDisplayMode == null)
         {
            if (next) mDevice.selectNext();
            else mDevice.selectPrevious();
         }
      });

      for(int i=0; i<9; i++)
      {
         Fader fader = addElement(new Fader(i));
         mBaseLayer.bind(fader, mDeviceEnvelopes.getParameter(i));
      }
   }

   private boolean isInBrowser()
   {
      return mDisplayMode == DisplayMode.BROWSER;
   }

   private void startPresetBrowsing()
   {
      setDisplayMode(DisplayMode.BROWSER);

      if (mDevice.exists().get())
      {
         mDevice.replaceDeviceInsertionPoint().browse();
      }
      else
      {
         mDevice.deviceChain().endOfDeviceChainInsertionPoint().browse();
      }
   }

   private void sendTextToKeyLab(final String upper, final String lower)
   {
      mUpperTextToSend = upper;
      mLowerTextToSend = lower;
   }

   @Override
   public void exit()
   {
   }

   @Override
   public void flush()
   {
      super.flush();

      if (mDisplayMode == null)
      {
         final String track = mCursorTrack.name().getLimited(16);
         final String device = mCursorTrack.exists().get() ? mDevice.exists().get() ? (mDevice.name().getLimited(16)) : "No Device" : "";

         sendTextToKeyLab(track, device);
      }
      else if (isInBrowser())
      {
         sendTextToKeyLab(
            mBrowserCategory.name().getLimited(16),
            mBrowserResult.name().getLimited(16));
      }

      if (mUpperTextToSend != null)
      {
         final String total = mUpperTextToSend + mLowerTextToSend;

         if (mLastText == null || !total.equals(mLastText))
         {
            mLastText = total;

            doSendTextToKeylab(mUpperTextToSend, mLowerTextToSend);
         }

         mUpperTextToSend = null;
         mLowerTextToSend = null;
      }
   }

   private void doSendTextToKeylab(final String upper, final String lower)
   {
      final byte[] data = SysexBuilder.fromHex("F0 00 20 6B 7F 42 04 00 60 01 ")
         .addString(upper, 16)
         .addHex("00 02")
         .addString(lower, 16)
         .addHex(" 00 F7")
         .array();

      sendSysex(data);
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
   private String mUpperTextToSend;
   private String mLowerTextToSend;
   private String mLastText;
   private PopupBrowser mPopupBrowser;
   private BrowserResultsItem mBrowserResult;
   private BrowserFilterItem mBrowserCategory;
   private BrowserFilterItem mBrowserCreator;
   private Application mApplication;
   private boolean[] mIsTransportDown;
   private long mLastDisplayTimeStamp;

   private Action mSaveAction;
   private Layer mBaseLayer = new Layer();
}
