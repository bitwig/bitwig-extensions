package com.bitwig.extensions.controllers.arturia.keylab;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.api.util.midi.SysexBuilder;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.Action;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.BrowserFilterItem;
import com.bitwig.extension.controller.api.BrowserResultsItem;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorBrowserFilterItem;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareControlType;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PopupBrowser;
import com.bitwig.extension.controller.api.Transport;

// TODO
// add mode to switch pads between pages and drum pads
// 9th encoder should control track volume

public class ArturiaKeylabMkIIControllerExtension extends ControllerExtension
{
   DisplayMode mDisplayMode = null;

   enum DisplayMode
   {
      BROWSER,
      BROWSER_CATEGORY,
      BROWSER_CREATOR,
   }

   void setDisplayMode(final DisplayMode displayMode)
   {
      mDisplayMode = displayMode;

      mLastDisplayTimeStamp = System.currentTimeMillis();
   }

   public ArturiaKeylabMkIIControllerExtension(
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
      mTransport.isPunchInEnabled().markInterested();
      mTransport.isMetronomeEnabled().markInterested();
      mCursorTrack = host.createCursorTrack(4, 0);
      mCursorTrack.volume().setIndication(true);
      mCursorTrack.exists().markInterested();
      mDevice = mCursorTrack.createCursorDevice();
      mDevice.exists().markInterested();
      mDevice.hasNext().markInterested();
      mRemoteControls = mDevice.createCursorRemoteControlsPage(8);
      mRemoteControls.setHardwareLayout(HardwareControlType.ENCODER, 8);
      mDeviceEnvelopes = mDevice.createCursorRemoteControlsPage("envelope", 9, "envelope");
      mDeviceEnvelopes.setHardwareLayout(HardwareControlType.SLIDER, 9);

      mApplication = getHost().createApplication();

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

      mNoteInput = host.getMidiInPort(0).createNoteInput("Keys", "?0????", "?1????", "?2????", "?3????", "?4????", "?5????", "?6????", "?7????", "?8????");
      mNoteInput.setShouldConsumeEvents(true);

      final NoteInput drumPadsInput = host.getMidiInPort(0).createNoteInput("Pads", "?9????");

      mCursorTrack.name().markInterested();
      mDevice.name().markInterested();
      mDevice.presetName().markInterested();
      mDevice.presetCategory().markInterested();
      mDevice.presetCreator().markInterested();

      host.getMidiInPort(1).setMidiCallback((ShortMidiMessageReceivedCallback) this::onDAWPortMidi);
      host.getMidiInPort(0).setMidiCallback((ShortMidiMessageReceivedCallback) this::onNotePortMidi);

      mIsTransportDown = new boolean[5];

      final ControllerExtensionDefinition definition = getExtensionDefinition();

      sendSysex("F0 00 20 6B 7F 42 02 00 40 52 00 F7"); // Init DAW preset in Default MCU mode

      sendSysex("F0 00 20 6B 7F 42 05 02 F7"); // Set to DAW mode

      sendTextToKeyLab(
         definition.getHardwareVendor(),
         definition.getHardwareModel() + " " + definition.getVersion());

      updateIndications();

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

   // Keep checking the display for mode changes
   private void displayRefreshTimer()
   {
      if (mDisplayMode != null)
      {
         switch (mDisplayMode)
         {
            case BROWSER:
            case BROWSER_CATEGORY:
            case BROWSER_CREATOR:
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

   private void onNotePortMidi(final ShortMidiMessage data)
   {
      if (data.isNoteOn() && data.getChannel() == 9)
      {
         final int key = data.getData1();

         switch (key)
         {
         case 36:
            mRemoteControls.selectNextPageMatching("preset", true);
            break;
         case 37:
            mRemoteControls.selectNextPageMatching("overview", true);
            break;
         case 38:
            mRemoteControls.selectNextPageMatching("lfo", true);
            break;
         case 39:
            mRemoteControls.selectNextPageMatching("fx", true);
            break;
         case 40:
            mRemoteControls.selectNextPageMatching("oscillator", true);
            break;
         case 41:
            mRemoteControls.selectNextPageMatching("mix", true);
            break;
         case 42:
            mRemoteControls.selectNextPageMatching("filter", true);
            break;
         case 43:
            mRemoteControls.selectNextPageMatching("envelope", true);
            break;
         }
      }
   }

   private void onDAWPortMidi(final ShortMidiMessage data)
   {
      if (data.isNoteOn() && data.getChannel() == 0)
      {
         final boolean on = data.getData2() >= 64;
         final int key = data.getData1();

         if (key == 0x5B) // REWIND
         {
            mIsTransportDown[0] = on;
            repeatRewind();
            setLED(0x5B, on);
         }
         else if (key == 0x5C) // FWD
         {
            mIsTransportDown[1] = on;
            repeatForward();
            setLED(0x5C, on);
         }
         else if (key == 0x5D) // Stop
         {
            if (on) mTransport.stop();
            setLED(0x5D, on);
         }
         else if (key == 0x5E && on) // Play
         {
            mTransport.play();
         }
         else if (key == 0x5F && !on) // Rec
         {
            mTransport.record();
         }
         else if (key == 0x56 && !on) // ReLoop/Cycle
         {
            mTransport.isArrangerLoopEnabled().toggle();
         }
         else if (key == 0x59 && !on) // Metronome
         {
            mTransport.isMetronomeEnabled().toggle();
         }
         else if (key == 0x51) // Undo
         {
            if (!on)
            {
               mApplication.undo();
            }
            setLED(0x51, on);
         }
         else if (key == 0x50) // Save
         {
            setLED(0x50, on);

            if (on)
            {
               Action saveAction = mApplication.getAction("Save");
               if (saveAction != null)
               {
                  saveAction.invoke();
               }
            }
         }
         else if (key == 0x57 && !on) // Punch
         {
            mTransport.isPunchInEnabled().toggle();
            mTransport.isPunchOutEnabled().set(!mTransport.isPunchInEnabled().get());
         }

         // Wheel section

         if (key == 0x62) // Prev
         {
            if (!on)
            {
               if (isInBrowser())
               {
                  mPopupBrowser.cancel();
               }
               else if (mDisplayMode == null)
               {
                  mCursorTrack.selectPrevious();
               }
            }

            setLED(0x62, on);
         }
         else if (key == 0x63) // Next
         {
            if (!on)
            {
               if (isInBrowser())
               {
                  mPopupBrowser.commit();
               }
               else if (mDisplayMode == null)
               {
                  mCursorTrack.selectNext();
               }
            }

            setLED(0x63, on);
         }
         else if (key == 0x65 && !on) // Cat/Char
         {
            if (mDisplayMode == DisplayMode.BROWSER || mDisplayMode == DisplayMode.BROWSER_CREATOR)
            {
               setDisplayMode(DisplayMode.BROWSER_CATEGORY);
            }
            else if (mDisplayMode == DisplayMode.BROWSER_CATEGORY)
            {
               setDisplayMode(DisplayMode.BROWSER_CREATOR);
            }
         }
         else if (key == 0x64 && !on) // Preset
         {
            if (mDisplayMode == DisplayMode.BROWSER_CATEGORY || mDisplayMode == DisplayMode.BROWSER_CREATOR)
            {
               setDisplayMode(DisplayMode.BROWSER);
            }
            else if (mDisplayMode == null)
            {
               startPresetBrowsing();
            }
         }
         else if (key == 0x54 && on) // Wheel click
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
         }

         // Encoder / Fader section

         if (key == 0x31 && !on) // Next
         {
            mRemoteControls.selectNext();
         }
         else if (key == 0x30 && !on) // Next
         {
            mRemoteControls.selectPrevious();
         }
      }

      if (data.isControlChange() && data.getChannel() == 0)
      {
         final int CC = data.getData1();
         final int encoderIndex = CC - 0x10;
         final int increment = decodeRelativeCC(data.getData2());

         if (encoderIndex >= 0 && encoderIndex < 8)
         {
            mRemoteControls.getParameter(encoderIndex).inc(increment, 101);
         }
         else if (CC == 0x3C) // Wheel
         {
            boolean next = increment > 0;

            if (mDisplayMode == DisplayMode.BROWSER)
            {
               if (next) mPopupBrowser.selectNextFile();
               else mPopupBrowser.selectPreviousFile();
            }
            else if (mDisplayMode == DisplayMode.BROWSER_CATEGORY)
            {
               final CursorBrowserFilterItem cursorBrowserFilterItem = (CursorBrowserFilterItem)mBrowserCategory;
               if (next) cursorBrowserFilterItem.selectNext();
               else cursorBrowserFilterItem.selectPrevious();
            }
            else if (mDisplayMode == DisplayMode.BROWSER_CREATOR)
            {
               final CursorBrowserFilterItem cursorBrowserFilterItem = (CursorBrowserFilterItem)mBrowserCreator;
               if (next) cursorBrowserFilterItem.selectNext();
               else cursorBrowserFilterItem.selectPrevious();
            }
            else if (mDisplayMode == null)
            {
               if (next) mDevice.selectNext();
               else mDevice.selectPrevious();
            }
         }
      }

      if (data.isPitchBend())
      {
         int channel = data.getChannel();
         int value = data.getData2() << 7 | data.getData1();

         if (channel <= 9)
         {
            mDeviceEnvelopes.getParameter(channel).set(value, 16384);
         }
      }

      mLastText = null;
   }

   private boolean isInBrowser()
   {
      return mDisplayMode == DisplayMode.BROWSER
         || mDisplayMode == DisplayMode.BROWSER_CREATOR
         || mDisplayMode == DisplayMode.BROWSER_CATEGORY;
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

   private int decodeRelativeCC(int x)
   {
      int increment = x & 0x3f;
      return ((x & 0x40) != 0) ? -increment : increment;
   }

   private void sendTextToKeyLab(final String upper, final String lower)
   {
      mUpperTextToSend = upper;
      mLowerTextToSend = lower;
   }

   @Override
   public void exit()
   {
      KeylabSysex.resetToAbsoluteMode(getMidiOutPort(0));
   }

   @Override
   public void flush()
   {
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

      setLED(0x5E, mTransport.isPlaying().get());
      setLED(0x56, mTransport.isArrangerLoopEnabled().get());
      setLED(0x5F, mTransport.isArrangerRecordEnabled().get());
      setLED(0x57, mTransport.isPunchInEnabled().get());
      setLED(0x59, mTransport.isMetronomeEnabled().get());

      setLED(0x65, mDisplayMode == DisplayMode.BROWSER_CATEGORY || mDisplayMode == DisplayMode.BROWSER_CREATOR);
      setLED(0x64, mDisplayMode == DisplayMode.BROWSER);
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

   public void setLED(final int note, final boolean state)
   {
      if (mLastLEDState[note] != state)
      {
         getHost().getMidiOutPort(1).sendMidi(0x90, note, state ? 127 : 0);
         mLastLEDState[note] = state;
      }
   }

   private boolean[] mLastLEDState = new boolean[128];
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
}
