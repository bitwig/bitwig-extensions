package com.bitwig.extensions.controllers.arturia.keylab.mk1;

import static com.bitwig.extensions.controllers.arturia.keylab.mk1.ArturiaKeylabControllerExtension
   .DisplayMode.MESSAGE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.api.util.midi.SysexBuilder;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.BrowserFilterItem;
import com.bitwig.extension.controller.api.BrowserResultsItem;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorBrowserFilterItem;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareControlType;
import com.bitwig.extension.controller.api.MasterTrack;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PopupBrowser;
import com.bitwig.extension.controller.api.RangedValue;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.Send;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import static com.bitwig.extensions.controllers.arturia.keylab.mk1.ArturiaKeylabControllerExtension.DisplayMode.BROWSER;

import static com.bitwig.extensions.controllers.arturia.keylab.mk1.ArturiaKeylabControllerExtension
   .DisplayMode.PARAMETER;
import static com.bitwig.extensions.controllers.arturia.keylab.mk1.ArturiaKeylabControllerExtension.DisplayMode.PARAMETER_PAGE;

public class ArturiaKeylabControllerExtension extends ControllerExtension
{
   final static int[] ENCODER1_CCS = {74, 71, 76, 77, 93, 18, 19, 16, 17, 91};
   final static int[] ENCODER2_CCS = {35, 36, 37, 38, 39, 40, 41, 42, 43, 44};
   final static int[] FADER1_CCS = {73, 75, 79, 72, 80, 81, 82, 83, 85};
   final static int[] FADER2_CCS = {67, 68, 69, 70, 87, 88, 89, 90, 92};
   final static int[] BUTTON_CCS = {22, 23, 24, 25, 26, 27, 28, 29, 30, 31};
   final static int[] BUTTON_LONG_PRESS_CCS = {104, 105, 106, 107, 108, 109, 110, 111, 116, 117};

   final static int[] ENCODER1_SYSEX = {1, 2, 3, 4, 9, 5, 6, 7, 8, 0x6e};
   final static int[] ENCODER2_SYSEX = {0x21, 0x22, 0x23, 0x24, 0x29, 0x25, 0x26, 0x27, 0x28, 0x2a};
   final static int[] BUTTONS_SYSEX = {0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x6A, 0x6B, 0x62};

   final static int SOUND_CC = 118;
   final static int MULTI_CC = 119;

   final static int BANK1_CC = 47;
   final static int BANK2_CC = 46;

   final static int PARAM_CC = 112;
   final static int PARAM_CLICK_CC = 113;
   final static int VALUE_CC = 114;
   final static int VALUE_CLICK_CC = 115;

   final static int VOLUME_CC = 7;

   final static int LOOP_CC = 55;

   final static int REW_CC = 32;
   final static int FWD_CC = 33;
   final static int STOP_CC = 34;
   final static int PLAY_CC = 20;
   final static int REC_CC = 21;

   private boolean mIsInArturiaMode = false;

   enum DisplayMode
   {
      PARAMETER,
      PARAMETER_PAGE,
      BROWSER,
      BROWSER_CREATOR,
      MESSAGE,
   }

   DisplayMode mDisplayMode = null;

   void setDisplayMode(final DisplayMode displayMode)
   {
      mDisplayMode = displayMode;

      mLastDisplayTimeStamp = System.currentTimeMillis();
   }

   public ArturiaKeylabControllerExtension(
      final ArturiaKeylabControllerExtensionDefinition definition,
      final ControllerHost host)
   {
      super(definition, host);

      mHasDrumPads = definition.hasDrumPads();
      mNumberOfKeys = definition.getNumberOfKeys();
   }

   class ConvertEncoderToAbsolute implements CCAction
   {
      ConvertEncoderToAbsolute(final int[] indexToCC)
      {
         mIndexToCC = indexToCC;
         mValues = new int[10];
         Arrays.fill(mValues, 64);
      }

      @Override
      public void onMidi(final ShortMidiMessage data, final int index)
      {
         final int oldValue = mValues[index];
         final int inc = data.getData2() - 64;
         mValues[index] = Math.max(0, Math.min(127, oldValue + inc));

         if (mValues[index] != oldValue)
         {
            final int CC = mIndexToCC != null ? mIndexToCC[index] : data.getData1();
            mNoteInput.sendRawMidiEvent(data.getStatusByte(), CC, mValues[index]);
         }
      }a

      private final int[] mValues;
      private final int[] mIndexToCC;
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();

      mTransport = host.createTransport();
      mCursorTrack = host.createCursorTrack(4, 0);
      mCursorTrack.volume().setIndication(true);
      mCursorTrack.exists().markInterested();
      mDevice = mCursorTrack.createCursorDevice();
      mDevice.exists().markInterested();
      mDevice.hasNext().markInterested();
      mRemoteControls = mDevice.createCursorRemoteControlsPage(8);
      mRemoteControls.setHardwareLayout(HardwareControlType.ENCODER, 4);
      mDeviceEnvelopes = mDevice.createCursorRemoteControlsPage("envelope", 9, "envelope");
      mDeviceEnvelopes.setHardwareLayout(HardwareControlType.SLIDER, 9);

      mApplication = getHost().createApplication();

      mPopupBrowser = host.createPopupBrowser();
      mPopupBrowser.exists().markInterested();

      mBrowserResult = mPopupBrowser.resultsColumn().createCursorItem();
      mBrowserCategory = mPopupBrowser.categoryColumn().createCursorItem();
      final BrowserFilterItem browserCreator = mPopupBrowser.creatorColumn().createCursorItem();
      mBrowserResult.name().markInterested();
      mBrowserCategory.name().markInterested();
      browserCreator.name().markInterested();

      for(int i=0; i<8; i++)
      {
         mRemoteControls.getParameter(i).setIndication(true);
         mRemoteControls.getParameter(i).name().markInterested();
         mRemoteControls.getParameter(i).value().markInterested();
         mRemoteControls.getParameter(i).value().displayedValue().markInterested();
      }

      for(int i=0; i<9; i++)
      {
         mDeviceEnvelopes.getParameter(i).setIndication(false);
         mDeviceEnvelopes.getParameter(i).setLabel("F" + Integer.toString(i+1));
         mDeviceEnvelopes.getParameter(i).name().markInterested();
         mDeviceEnvelopes.getParameter(i).value().markInterested();
         mDeviceEnvelopes.getParameter(i).value().displayedValue().markInterested();
      }

      for(int s=0; s<4; s++)
      {
         mCursorTrack.sendBank().getItemAt(s).name().markInterested();
         mCursorTrack.sendBank().getItemAt(s).value().markInterested();
         mCursorTrack.sendBank().getItemAt(s).value().displayedValue().markInterested();
      }

      mRemoteControls.getName().markInterested();
      mRemoteControls.getParameter(0).setLabel("P1");
      mRemoteControls.getParameter(1).setLabel("P2");
      mRemoteControls.getParameter(2).setLabel("P3");
      mRemoteControls.getParameter(3).setLabel("P4");
      mRemoteControls.getParameter(4).setLabel("P5");
      mRemoteControls.getParameter(5).setLabel("P6");
      mRemoteControls.getParameter(6).setLabel("P7");
      mRemoteControls.getParameter(7).setLabel("P8");

      mRemoteControls.selectedPageIndex().addValueObserver(this::setButtonLightExclusive);

      mTrackBank = host.createTrackBank(8, 0, 0);
      mMasterTrack = host.createMasterTrack(0);

      mNoteInput = host.getMidiInPort(0).createNoteInput("Keys", "80????", "90????", "B001??", "B002??", "B00B??", "B040??", "C0????", "D0????", "E0????");
      mNoteInput.setShouldConsumeEvents(false);

      if (mHasDrumPads)
      {
         final NoteInput drumPadsInput = host.getMidiInPort(0).createNoteInput("Pads", "?9????");
         drumPadsInput.setShouldConsumeEvents(false);
      }

      mCursorTrack.name().markInterested();
      mDevice.name().markInterested();
      mDevice.presetName().markInterested();
      mDevice.presetCategory().markInterested();
      mDevice.presetCreator().markInterested();

      host.getMidiInPort(0).setMidiCallback((ShortMidiMessageReceivedCallback) this::onMidiIn);
      host.getMidiInPort(0).setSysexCallback(this::onSysex);

      final BooleanSupplier isBitwig = () -> !mIsInArturiaMode;
      final BooleanSupplier isArturia = () -> mIsInArturiaMode;


      final BooleanSupplier isSoundMode = () -> !mIsInArturiaMode && !mIsMultiMode;
      final BooleanSupplier isMultiMode = () -> !mIsInArturiaMode && mIsMultiMode;

      addCCAction(BANK1_CC, this::setToBitwigMode);
      addCCAction(BANK2_CC, this::setToArturiaMode);

      mIsTransportDown = new boolean[5];

      addCCAction(REW_CC, (d,i) ->
      {
         mIsTransportDown[0] = d.getData2() > 64;
         repeatRewind();
      });
      addCCAction(FWD_CC, (d,i) ->
      {
         mIsTransportDown[1] = d.getData2() > 64;
         repeatForward();
      });
      addCCAction(STOP_CC, (d,i) ->
      {
         if (d.getData2() >= 64) mTransport.stop();
      });
      addCCAction(PLAY_CC, (d,i) ->
      {
         if (d.getData2() >= 64) mTransport.play();
      });
      addCCAction(REC_CC, (d,i) ->
      {
         if (d.getData2() < 64) mTransport.record();
      });

      addCCAction(LOOP_CC, (d,i) ->
      {
         if (d.getData2() < 64) mTransport.isArrangerLoopEnabled().toggle();
      });

      // BITWIG mode
      addCCAction(ENCODER1_CCS, this::onEncoderSound, isSoundMode);
      addCCAction(FADER1_CCS, this::onFaderSound, isSoundMode);
      addCCAction(BUTTON_CCS, this::onButtonPressSound, isSoundMode);
      addCCAction(VOLUME_CC, (data, index) -> incrementFromEncoder(mCursorTrack.volume(), data), isBitwig);
      addCCAction(SOUND_CC, this::setToSoundMode, isBitwig);
      addCCAction(MULTI_CC, this::setToMultiMode, isBitwig);
      addCCAction(VALUE_CC, (data, index) -> {}, isBitwig);
      addCCAction(PARAM_CC, this::paramEncoder, isBitwig);
      addCCAction(PARAM_CLICK_CC, this::paramEncoderClick, isBitwig);
      addCCAction(VALUE_CC, this::valueEncoder, isBitwig);
      addCCAction(VALUE_CLICK_CC, this::valueEncoderClick, isBitwig);

      // BITWIG mode - MULTI
      addCCAction(ENCODER1_CCS, this::onEncoderMulti, isMultiMode);
      addCCAction(FADER1_CCS, this::onFaderMulti, isMultiMode);
      addCCAction(BUTTON_CCS, this::onButtonMulti, isMultiMode);
      //addCCAction(BUTTON_LONG_PRESS_CCS, this::onButtonLongPressMulti, isMultiMode);

      // ARTURIA mode
      final CCAction forwardToNoteInput = (d, i) -> mNoteInput.sendRawMidiEvent(d.getStatusByte(), d.getData1(), d.getData2());
      addCCAction(FADER2_CCS, this::onFaderArturia);
      addCCAction(ENCODER2_CCS, new ConvertEncoderToAbsolute(ENCODER1_CCS));
      addCCAction(PARAM_CC, forwardToNoteInput, isArturia);
      addCCAction(PARAM_CLICK_CC, forwardToNoteInput, isArturia);
      addCCAction(VALUE_CC, forwardToNoteInput, isArturia);
      addCCAction(VALUE_CLICK_CC, forwardToNoteInput, isArturia);
      addCCAction(VOLUME_CC, new ConvertEncoderToAbsolute(null), isArturia);
      addCCAction(BUTTON_CCS, forwardToNoteInput, isArturia);
      addCCAction(SOUND_CC, forwardToNoteInput, isArturia);
      addCCAction(MULTI_CC, forwardToNoteInput, isArturia);
      addCCAction(BUTTON_LONG_PRESS_CCS, forwardToNoteInput, isArturia);

      final ControllerExtensionDefinition definition = getExtensionDefinition();

      sendTextToKeyLab(
         definition.getHardwareVendor(),
         definition.getHardwareModel() + " " + definition.getVersion());

      KeylabSysex.configureDeviceUsingSysex(getMidiOutPort(0), mNumberOfKeys == 88);

      updateIndications();

      /*String[] accelerations = {"x1", "x2", "x3", "x4", "x6", "x8", "x10", "x12", "x14", "x17" };
      host.getPreferences().getEnumSetting("Acceleration","Encoders", accelerations, accelerations[0])
         .addValueObserver((e) ->
            {
            int index = 0;
            int i = 0;
            for (String s : accelerations)
            {
               if (s.equals(e)) index = i;
               i++;
            }
            sendSysex(SysexBuilder.fromHex("F000206B7F4202004104").addByte(index).terminate());
            });*/

      setupTransport();

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

   private void setupTransport()
   {
      mTransport.isArrangerLoopEnabled().addValueObserver(v ->
         sendSysex("F0 00 20 6B 7F 42 02 00 10 5D " + (v ? "01" : "00") + " F7"));

      mTransport.isPlaying().addValueObserver(v ->
         {
            sendSysex("F0 00 20 6B 7F 42 02 00 10 5A " + (!v ? "01" : "00") + " F7");
            sendSysex("F0 00 20 6B 7F 42 02 00 10 5B " + (v ? "01" : "00") + " F7");
         });

      mTransport.isArrangerRecordEnabled().addValueObserver(v ->
         sendSysex("F0 00 20 6B 7F 42 02 00 10 5C " + (v ? "01" : "00") + " F7"));
   }

   // Keep checking the display for mode changes
   private void displayRefreshTimer()
   {

      //mLastText = null;
      //flush();

      if (mDisplayMode != null)
      {
         final long duration = System.currentTimeMillis() - mLastDisplayTimeStamp;
         final long timeout = 1000;

         switch (mDisplayMode)
         {
            case PARAMETER:
            case PARAMETER_PAGE:
            case MESSAGE:
               if (duration > timeout)
               {
                  setDisplayMode(null);
                  mValueForPopup = null;
                  mPopupParameterName = null;
               }
               break;

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
            setDisplayMode(BROWSER);
         }
      }

      getHost().scheduleTask(this::displayRefreshTimer, 100);
   }

   private void updateIndications()
   {
      mDevice.setIsSubscribed(!mIsInArturiaMode && !mIsMultiMode);
      mCursorTrack.setIsSubscribed(!mIsInArturiaMode);
      mTrackBank.setIsSubscribed(!mIsInArturiaMode && mIsMultiMode);
      mMasterTrack.setIsSubscribed(!mIsInArturiaMode && mIsMultiMode);

      for(int i=0; i<8; i++)
      {
         mTrackBank.getItemAt(i).volume().setIndication(!mIsInArturiaMode && mIsMultiMode);
      }
   }

   private void setToBitwigMode(final ShortMidiMessage d, final int i)
   {
      mIsInArturiaMode = false;
      updateIndications();
      setDisplayMode(MESSAGE);
      sendTextToKeyLab("Mode:", "Bitwig " + (mIsMultiMode ? "Multi" : "Sound"));
   }

   private void setToArturiaMode(final ShortMidiMessage d, final int i)
   {
      mIsInArturiaMode = true;
      updateIndications();
      setDisplayMode(MESSAGE);
      sendTextToKeyLab("Mode:", "Arturia");
   }

   private void setToSoundMode(final ShortMidiMessage d, final int i)
   {
      mIsMultiMode = false;
      updateIndications();
      setDisplayMode(MESSAGE);
      sendTextToKeyLab("Mode:", "Bitwig " + (mIsMultiMode ? "Multi" : "Sound"));
   }

   private void setToMultiMode(final ShortMidiMessage d, final int i)
   {
      mIsMultiMode = true;
      updateIndications();
      setDisplayMode(MESSAGE);
      sendTextToKeyLab("Mode:", "Bitwig " + (mIsMultiMode ? "Multi" : "Sound"));
   }

   private void paramEncoder(final ShortMidiMessage d, final int i)
   {
      mLastText = null;
      final boolean next = d.getData2() >= 64;

      if (mDisplayMode == DisplayMode.BROWSER)
      {
         final CursorBrowserFilterItem cursorBrowserFilterItem = (CursorBrowserFilterItem)mBrowserCategory;
         if (next) cursorBrowserFilterItem.selectNext();
         else cursorBrowserFilterItem.selectPrevious();
      }
      else if (mDisplayMode == null)
      {
         if (next) mCursorTrack.selectNext();
         else mCursorTrack.selectPrevious();
      }
   }

   private void paramEncoderClick(final ShortMidiMessage d, final int i)
   {
      mLastText = null;
      if (d.getData2() < 64)
      {
         if (mDisplayMode == DisplayMode.BROWSER)
         {
            setDisplayMode(null);
            mPopupBrowser.cancel();
         }
      }
   }

   private void valueEncoder(final ShortMidiMessage d, final int i)
   {
      mLastText = null;
      final boolean next = d.getData2() >= 64;

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
   }

   private void valueEncoderClick(final ShortMidiMessage d, final int i)
   {
      mLastText = null;
      if (d.getData2() < 64)
      {
         if (mDisplayMode == DisplayMode.BROWSER || mDisplayMode == DisplayMode.BROWSER_CREATOR)
         {
            mPopupBrowser.commit();
            setDisplayMode(null);
         }
         else
         {
            setDisplayMode(BROWSER);

            if (mDevice.exists().get())
            {
               mDevice.replaceDeviceInsertionPoint().browse();
            }
            else
            {
               mDevice.deviceChain().endOfDeviceChainInsertionPoint().browse();
            }
         }
      }
   }

   private void incrementFromEncoder(final SettableRangedValue value, final ShortMidiMessage d)
   {
      final int increment = d.getData2() - 64;
      value.inc(increment, 101);
   }

   private void onEncoderSound(final ShortMidiMessage d, final int i)
   {
      final boolean next = d.getData2() >= 64;
      if (i == 4)
      {
         final Send send = mCursorTrack.sendBank().getItemAt(0);
         send.inc(d.getData2() - 64,101);
         showPopupForValue(send.name().getLimited(16), send.value());
      }
      else if (i == 9)
      {
         final Send send = mCursorTrack.sendBank().getItemAt(1);
         send.inc(d.getData2() - 64,101);
         showPopupForValue(send.name().getLimited(16), send.value());
      }
      else
      {
         final int parameterIndex = i > 3 ? i - 1 : i;
         final RemoteControl parameter = mRemoteControls.getParameter(parameterIndex);
         incrementFromEncoder(parameter, d);
         showPopupForValue(parameter.name().getLimited(16), parameter.value());
      }
   }

   private void showPopupForValue(final String name, final RangedValue value)
   {
      mPopupParameterName = name;
      mValueForPopup = value;
      setDisplayMode(PARAMETER);

      doSendTextToKeylab(
         mPopupParameterName,
         value.displayedValue().getLimited(16));

   }

   private void onFaderSound(final ShortMidiMessage d, final int i)
   {
      final RemoteControl parameter = mDeviceEnvelopes.getParameter(i);
      parameter.set(d.getData2(), 128);
      showPopupForValue(parameter.name().getLimited(16), parameter.value());
   }

   private void onButtonPressSound(final ShortMidiMessage d, final int i)
   {
      if (d.getData2() >= 64)
      {
         mRemoteControls.selectedPageIndex().set(i);
         final ControllerHost host = getHost();

         host.scheduleTask(() -> setDisplayMode(PARAMETER_PAGE), 50);
      }
   }

   private void onEncoderMulti(final ShortMidiMessage d, final int i)
   {
      switch (i)
      {
         case 0:
            mCursorTrack.pan().inc(d.getData2() - 64, 101);
            break;

         case 1:
         case 2:
         case 3:
         case 4:
            mCursorTrack.sendBank().getItemAt(i-1).inc(d.getData2() - 64, 128);
            break;

         case 5:
            mTransport.incPosition(d.getData2() - 64, true);
            break;

         case 6:
            mTransport.getInPosition().inc(d.getData2() - 64);
            break;

         case 7:
            mTransport.getOutPosition().inc(d.getData2() - 64);
            break;

         case 8:
            mTransport.increaseTempo(d.getData2() - 64, 647);
            break;

         case 9:
            mTrackBank.scrollPosition().inc(d.getData2() - 64);
            break;
      }
   }

   private void onFaderMulti(final ShortMidiMessage d, final int i)
   {
      if (i < 8)
      {
         mTrackBank.getItemAt(i).volume().set(d.getData2(), 128);
      }
      else if (i == 8)
      {
         mMasterTrack.volume().set(d.getData2(), 128);
      }
   }

   private void onButtonMulti(final ShortMidiMessage d, final int i)
   {
      if (d.getData2() >= 64) return;

      switch (i)
      {
         case 0:
            mApplication.toggleInspector();
            break;

         case 1:
            mApplication.toggleNoteEditor();
            break;

         case 2:
            mApplication.toggleAutomationEditor();
            break;

         case 3:
            mApplication.toggleDevices();
            break;

         case 4:
            mApplication.toggleMixer();
            break;

         case 5:
            mApplication.toggleBrowserVisibility();
            break;

         case 6:
            mApplication.nextPanelLayout();
            break;

         case 7:
            mApplication.nextProject();
            break;

         case 8:
            mTrackBank.scrollPageBackwards();
            break;

         case 9:
            mTrackBank.scrollPageForwards();
            break;
      }
   }

   private void onFaderArturia(final ShortMidiMessage d, final int i)
   {
      mNoteInput.sendRawMidiEvent(d.getStatusByte(), FADER1_CCS[i], d.getData2());
   }

   private void onMidiIn(final ShortMidiMessage data)
   {
      if (data.isControlChange())
      {
         for (final Entry entry : mCCActions)
         {
            entry.process(data);
         }
      }

      mLastText = null;
   }

   private void onSysex(final String data)
   {
      switch (data)
      {
         case "f07f7f0605f7":
            mTransport.rewind();
            break;
         case "f07f7f0604f7":
            mTransport.fastForward();
            break;
         case "f07f7f0601f7":
            mTransport.stop();
            break;
         case "f07f7f0602f7":
            mTransport.play();
            break;
         case "f07f7f0606f7":
            mTransport.record();
            break;
         case "f07f7f060bf7":
            mTransport.isArrangerLoopEnabled().toggle();
            break;
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
      KeylabSysex.resetToAbsoluteMode(getMidiOutPort(0));
   }

   @Override
   public void flush()
   {
      if (mDisplayMode == null)
      {
         final String track = "< > " + mCursorTrack.name().getLimited(12);
         final String device = mCursorTrack.exists().get() ? mDevice.exists().get() ? ("<B> " + mDevice.name().getLimited(12)) : "<+> No DEVICE" : "";

         sendTextToKeyLab(track, device);
      }
      else if (mDisplayMode == DisplayMode.BROWSER)
      {
         final String resultName = mBrowserResult.name().getLimited(12);
         sendTextToKeyLab(
            "<-> " + mBrowserCategory.name().getLimited(12),
            "<L> " + mBrowserResult.name().getLimited(12));
      }
      else if (mDisplayMode == DisplayMode.PARAMETER)
      {
         if (mValueForPopup != null)
         {
            sendTextToKeyLab(
               mPopupParameterName,
               mValueForPopup.displayedValue().getLimited(16));
         }
      }
      else if (mDisplayMode == DisplayMode.PARAMETER_PAGE)
      {
         final StringValue pageName = mRemoteControls.getName();
         final String pageNameLimited = pageName.getLimited(11);
         sendTextToKeyLab(
            mDevice.name().getLimited(16),
            pageNameLimited.isEmpty() ? "  ( no page )" : "Page: " + pageNameLimited);
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

   void setButtonLightExclusive(final int index)
   {
      for(int i = 0; i < 10; i++)
      {
         setButtonLight(i, index == i);
      }
   }

   void setButtonLight(final int index, final boolean on)
   {
      final int value = on ? 1 : 0;
      final String header = "F0 00 20 6B 7F 42 02 00 00 ";
      sendSysex(SysexBuilder.fromHex(header).addByte(BUTTONS_SYSEX[index]).addByte(value).addHex("F7").array());
   }

   public void sendSysex(final byte[] data)
   {
      getHost().getMidiOutPort(0).sendSysex(data);
   }

   public void sendSysex(final String data)
   {
      getHost().getMidiOutPort(0).sendSysex(data);
   }

   private void addCCAction(final int CC, final CCAction r)
   {
      addCCAction(CC, r, null);
   }

   private void addCCAction(final int CC, final CCAction r, final BooleanSupplier filter)
   {
      mCCActions.add(new Entry(new int[]{CC}, r, filter));
   }

   private void addCCAction(final int[] CCs, final CCAction r, final BooleanSupplier filter)
   {
      mCCActions.add(new Entry(CCs, r, filter));
   }

   private void addCCAction(final int[] CCs, final CCAction r)
   {
      addCCAction(CCs, r, null);
   }

   interface CCAction
   {
      void onMidi(final ShortMidiMessage data, int index);
   }

   class Entry
   {
      final int[] mCCs;
      final CCAction mAction;
      final BooleanSupplier mFilter;

      public Entry(final int[] CCs, final CCAction action, final BooleanSupplier filter)
      {
         mCCs = CCs;
         mAction = action;
         mFilter = filter;
      }

      public void process(final ShortMidiMessage data)
      {
         if (mFilter != null && !mFilter.getAsBoolean()) return;

         int index = 0;

         for (final int cc : mCCs)
         {
            if (cc == data.getData1())
            {
               mAction.onMidi(data, index);
            }
            index++;
         }
      }
   }

   private final List<Entry> mCCActions = new ArrayList<>();
   private NoteInput mNoteInput;
   private final boolean mHasDrumPads;
   private final int mNumberOfKeys;
   private Transport mTransport;
   private CursorTrack mCursorTrack;
   private CursorDevice mDevice;
   private CursorRemoteControlsPage mRemoteControls;
   private CursorRemoteControlsPage mDeviceEnvelopes;
   private String mUpperTextToSend;
   private String mLowerTextToSend;
   private boolean mIsMultiMode;
   private MasterTrack mMasterTrack;
   private TrackBank mTrackBank;
   private boolean mIsParamPressed;
   private String mLastText;
   private PopupBrowser mPopupBrowser;
   private BrowserResultsItem mBrowserResult;
   private BrowserFilterItem mBrowserCategory;
   private Application mApplication;
   private boolean[] mIsTransportDown;
   private long mLastDisplayTimeStamp;
   private String mPopupParameterName;
   private RangedValue mValueForPopup;
}
