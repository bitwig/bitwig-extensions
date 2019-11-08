package com.bitwig.extensions.controllers.arturia.keylab.mk2;

import java.util.function.Supplier;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.api.util.midi.SysexBuilder;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.Action;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.BrowserFilterItem;
import com.bitwig.extension.controller.api.BrowserResultsItem;
import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorBrowserFilterItem;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareControlType;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MasterTrack;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PopupBrowser;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.framework.DebugUtilities;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.oldframework.ControlElement;
import com.bitwig.extensions.oldframework.targets.EncoderTarget;
import com.bitwig.extensions.oldframework.targets.RGBButtonTarget;

public class ArturiaKeylabMkII extends ControllerExtension
{
   private final Color GREY = Color.fromRGB(0.2f, 0.2f, 0.2f);

   private final Color WHITE = Color.fromRGB(1.f, 1.f, 1.f);

   private final Color BLACK = Color.fromRGB(0.f, 0.f, 0.f);

   private final Color ORANGE = Color.fromRGB(1.f, 0.5f, 0.f);

   private final Color BLUEY = Color.fromRGB(0.f, 0.5f, 1.f);

   private final Color GREEN = Color.fromRGB(0.f, 1.0f, 0.f);

   private final Color RED = Color.fromRGB(1.f, 0.0f, 0.f);

   final int LAUNCHER_SCENES = 8;

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
      mTransport.getPosition().markInterested();
      mCursorTrack = host.createCursorTrack(0, LAUNCHER_SCENES);
      mSceneBank = host.createSceneBank(LAUNCHER_SCENES);
      mCursorTrack.volume().setIndication(true);
      mCursorTrack.solo().markInterested();
      mCursorTrack.mute().markInterested();
      mCursorTrack.arm().markInterested();
      mCursorTrack.exists().markInterested();
      mCursorTrack.hasNext().markInterested();
      mCursorTrack.hasPrevious().markInterested();
      mCursorTrack.position().markInterested();
      mDevice = mCursorTrack.createCursorDevice();
      mDevice.exists().markInterested();
      mDevice.hasNext().markInterested();
      mRemoteControls = mDevice.createCursorRemoteControlsPage(8);
      mRemoteControls.setHardwareLayout(HardwareControlType.ENCODER, 8);
      mRemoteControls.selectedPageIndex().markInterested();
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

      for (int i = 0; i < 8; i++)
      {
         mRemoteControls.getParameter(i).name().markInterested();
         mRemoteControls.getParameter(i).value().markInterested();
         mRemoteControls.getParameter(i).value().displayedValue().markInterested();
      }

      for (int i = 0; i < 9; i++)
      {
         mDeviceEnvelopes.getParameter(i).setLabel("F" + Integer.toString(i + 1));
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
      mRemoteControls.pageCount().markInterested();

      mRemoteControls.selectedPageIndex().markInterested();

      mTrackBank = host.createTrackBank(8, 0, LAUNCHER_SCENES);

      for (int i = 0; i < 8; i++)
      {
         final Track track = mTrackBank.getItemAt(i);
         track.color().markInterested();
      }

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

      mMasterTrack = host.createMasterTrack(0);

      mNoteInput = host.getMidiInPort(0).createNoteInput("Keys", "??????");
      mNoteInput.setShouldConsumeEvents(true);

      mCursorTrack.name().markInterested();
      mCursorTrack.volume().displayedValue().markInterested();
      mCursorTrack.volume().markInterested();
      mCursorTrack.pan().displayedValue().markInterested();
      mCursorTrack.pan().markInterested();
      mDevice.name().markInterested();
      mDevice.presetName().markInterested();
      mDevice.presetCategory().markInterested();
      mDevice.presetCreator().markInterested();

      host.getMidiInPort(1).setMidiCallback((ShortMidiMessageReceivedCallback)msg -> onMidi(msg));
      host.getMidiInPort(1).setSysexCallback(this::onSysex);

      sendSysex(
         SysexBuilder.fromHex("F0 00 20 6B 7F 42 02 00 40 52").addByte(DAWMode.Live.getID()).terminate());
      // Init DAW preset in Live mode

      sendSysex("F0 00 20 6B 7F 42 05 02 F7"); // Set to DAW mode

      updateIndications();

      initButtons();
      initControls();
      initPadsForCLipLauncher();
      mDisplay = addElement(new Display());

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
         if (exists)
            mBrowserLayer.activate();
         else
            mBrowserLayer.deactivate();
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

      mMultiLayer.bind(mDisplay, new DisplayTarget()
      {
         @Override
         public String getUpperText()
         {
            return mCursorTrack.name().getLimited(16);
         }

         @Override
         public String getLowerText()
         {
            final String vol = mCursorTrack.volume().displayedValue().getLimited(8);
            final String pan = mCursorTrack.pan().displayedValue().getLimited(8);

            final StringBuilder sb = new StringBuilder(16);
            sb.append(vol);
            final int N = 16 - vol.length() - pan.length();
            for (int i = 0; i < N; i++)
               sb.append(" ");
            sb.append(pan);

            return sb.toString();
         }
      });

   }

   private void initHardwareSurface()
   {
      mHardwareSurface = getHost().createHardwareSurface();
   }

   private void initLayers()
   {
      mLayers = new Layers(this);
      mBaseLayer = new Layer(mLayers, "Base");
      mBrowserLayer = new Layer(mLayers, "Browser");
      mMultiLayer = new Layer(mLayers, "Multi");

      initBaseLayer();
      initBrowserLayer();
      initMultiLayer();

      mBaseLayer.activate();

      final Layer debugLayer = DebugUtilities.createDebugLayer(mLayers, mHardwareSurface);
      debugLayer.activate();
   }

   private void initBaseLayer()
   {
      mBaseLayer.bindToggle(mSelectMultiButton, mMultiLayer);
      mBaseLayer.bind((Supplier<Color>)() -> mMultiLayer.isActive() ? BLUEY : ORANGE, mSelectMultiButton);

      mBaseLayer.bindPressed(mRewindButton, () -> {
         mIsRewinding = true;
         repeatRewind();
      });
      mBaseLayer.bind(() -> mIsRewinding, mRewindButton);

      mBaseLayer.bindPressed(mForwardButton, () -> {
         mIsForwarding = true;
         repeatForward();
      });
      mBaseLayer.bind(() -> mIsForwarding, mForwardButton);

      mBaseLayer.bindPressed(mStopButton, mTransport.stopAction());
      mBaseLayer.bindToggle(mPlayOrPauseButton, mTransport.playAction(), mTransport.isPlaying());
      mBaseLayer.bindToggle(mRecordButton, mTransport.recordAction(), mTransport.isArrangerRecordEnabled());
      mBaseLayer.bindToggle(mLoopButton, mTransport.isArrangerLoopEnabled());
      mBaseLayer.bindToggle(mMetroButton, mTransport.isMetronomeEnabled());
      mBaseLayer.bindPressed(mUndoButton, mApplication.undoAction());
      mBaseLayer.bindPressed(mSaveButton, mSaveAction::invoke);

      mBaseLayer.bindToggle(mPunchInButton, mTransport.isPunchInEnabled());
      mBaseLayer.bindToggle(mPunchOutButton, mTransport.isPunchOutEnabled());

      mBaseLayer.bindToggle(mSoloButton, mCursorTrack.solo());
      mBaseLayer.bindToggle(mMuteButton, mCursorTrack.mute());
      mBaseLayer.bindToggle(mRecordArmButton, mCursorTrack.arm());
      mBaseLayer.bindToggle(mWriteButton, mTransport.isArrangerAutomationWriteEnabled());

      mBaseLayer.bindToggle(mReadButton, mTransport.isClipLauncherOverdubEnabled());
   }

   private void initBrowserLayer()
   {
      mBrowserLayer.bindPressed(mSelectMultiButton, mPopupBrowser::cancel);
      mBrowserLayer.bind(() -> WHITE, mSelectMultiButton);
   }

   private void initMultiLayer()
   {

   }

   @Override
   public void flush()
   {
      mHardwareSurface.updateHardware();
   }

   /** Called when we receive short MIDI message on port 0. */
   private void onMidi(final ShortMidiMessage msg)
   {
      getHost().println(msg.toString());
   }

   private void onSysex(final String s)
   {
      final String ENTER_DAW_MODE = "f000206b7f420200001500f7";
      final String EXIT_DAW_MODE = "f000206b7f42020000157ff7";

      if (s.equals(ENTER_DAW_MODE))
      {
         mDawMode = true;

         mBaseLayer.activate();

         getHost().scheduleTask(() -> {
            reset();
         }, 150);
      }
      else if (s.equals(EXIT_DAW_MODE))
      {
         mDawMode = false;
         mBaseLayer.deactivate();
      }

      updateIndications();
   }

   private void reset()
   {
      for (final ControlElement element : getElements())
      {
         if (element instanceof Resetable)
         {
            ((Resetable)element).reset();
         }
      }
   }

   private void repeatRewind()
   {
      if (mIsRewinding)
      {
         mTransport.rewind();
         getHost().scheduleTask(this::repeatRewind, 100);
      }
   }

   private void repeatForward()
   {
      if (mIsForwarding)
      {
         mTransport.fastForward();
         getHost().scheduleTask(this::repeatForward, 100);
      }
   }

   private void updateIndications()
   {
      mCursorTrack.volume().setIndication(mDawMode);

      final boolean isMixer = mMultiLayer.isActive();

      mDevice.setIsSubscribed(!isMixer && mDawMode);
      mCursorTrack.setIsSubscribed(mDawMode);

      for (int i = 0; i < 8; i++)
      {
         mRemoteControls.getParameter(i).setIndication(!isMixer && mDawMode);
         final Track track = mTrackBank.getItemAt(i);
         track.volume().setIndication(isMixer && mDawMode);
         track.pan().setIndication(isMixer && mDawMode);
      }

      for (int i = 0; i < 9; i++)
      {
         mDeviceEnvelopes.getParameter(i).setIndication(!isMixer && mDawMode);
      }

      for (int s = 0; s < LAUNCHER_SCENES; s++)
      {
         final ClipLauncherSlot slot = mCursorTrack.clipLauncherSlotBank().getItemAt(s);
         slot.setIndication(mDawMode);
         mSceneBank.getScene(s).setIndication(mDawMode);
      }
   }

   private void initButtons()
   {

      final Button prev = addElement(new Button(Buttons.PREVIOUS));
      mBaseLayer.bindPressedRunnable(prev, null, mRemoteControls::selectPrevious);
      mMultiLayer.bindPressedRunnable(prev, mTrackBank.canScrollBackwards(), mTrackBank::scrollBackwards);
      final Button next = addElement(new Button(Buttons.NEXT));
      mBaseLayer.bindPressedRunnable(next, null, mRemoteControls::selectNext);
      mMultiLayer.bindPressedRunnable(next, mTrackBank.canScrollForwards(), mTrackBank::scrollForwards);

      final RGBButton select1 = addPageButtonMapping(0, Buttons.SELECT1);
      final RGBButton select2 = addPageButtonMapping(1, Buttons.SELECT2);
      final RGBButton select3 = addPageButtonMapping(2, Buttons.SELECT3);
      final RGBButton select4 = addPageButtonMapping(3, Buttons.SELECT4);
      final RGBButton select5 = addPageButtonMapping(4, Buttons.SELECT5);
      final RGBButton select6 = addPageButtonMapping(5, Buttons.SELECT6);
      final RGBButton select7 = addPageButtonMapping(6, Buttons.SELECT7);
      final RGBButton select8 = addPageButtonMapping(7, Buttons.SELECT8);

      mBrowserLayer.bindPressedRunnable(select1, ORANGE,
         () -> mPopupBrowser.selectedContentTypeIndex().set(0));
      mBrowserLayer.bindPressedRunnable(select2, ORANGE,
         () -> mPopupBrowser.selectedContentTypeIndex().set(1));
      mBrowserLayer.bindPressedRunnable(select3, ORANGE,
         () -> mPopupBrowser.selectedContentTypeIndex().set(2));
      mBrowserLayer.bindPressedRunnable(select4, ORANGE,
         () -> mPopupBrowser.selectedContentTypeIndex().set(3));

      final CursorBrowserFilterItem categories = (CursorBrowserFilterItem)mPopupBrowser.categoryColumn()
         .createCursorItem();
      final CursorBrowserFilterItem creators = (CursorBrowserFilterItem)mPopupBrowser.creatorColumn()
         .createCursorItem();

      mBrowserLayer.bindPressedRunnable(select5, GREEN, () -> categories.selectPrevious());
      mBrowserLayer.bindPressedRunnable(select6, GREEN, () -> categories.selectNext());
      mBrowserLayer.bindPressedRunnable(select7, RED, () -> creators.selectPrevious());
      mBrowserLayer.bindPressedRunnable(select8, RED, () -> creators.selectNext());

      final Button presetPrev = addElement(new Button(Buttons.PRESET_PREVIOUS));
      mBaseLayer.bindPressedRunnable(presetPrev, mDevice.hasPrevious(), mDevice::selectPrevious);
      final ClipLauncherSlotBank cursorTrackSlots = mCursorTrack.clipLauncherSlotBank();
      cursorTrackSlots.scrollPosition().markInterested();

      mMultiLayer.bindPressedRunnable(presetPrev, cursorTrackSlots.canScrollBackwards(), () -> {
         final int current = cursorTrackSlots.scrollPosition().get();
         cursorTrackSlots.scrollPosition().set(current - 1);
         mSceneBank.scrollPosition().set(current - 1);
      });
      mBrowserLayer.bindPressedRunnable(presetPrev, null, mPopupBrowser::cancel);

      final Button presetNext = addElement(new Button(Buttons.PRESET_NEXT));
      mBaseLayer.bindPressedRunnable(presetNext, mDevice.hasNext(), mDevice::selectNext);
      mMultiLayer.bindPressedRunnable(presetNext, cursorTrackSlots.canScrollForwards(), () -> {
         final int current = cursorTrackSlots.scrollPosition().get();
         cursorTrackSlots.scrollPosition().set(current + 1);
         mSceneBank.scrollPosition().set(current + 1);
      });
      mBrowserLayer.bindPressedRunnable(presetNext, null, mPopupBrowser::commit);

      final Button wheelClick = addElement(new Button(Buttons.WHEEL_CLICK));
      mBaseLayer.bindPressedRunnable(wheelClick, null, () -> {
         mDisplay.reset();
         startPresetBrowsing();
      });
      mMultiLayer.bindPressedRunnable(wheelClick, null, () -> {
      });

      mBrowserLayer.bindPressedRunnable(wheelClick, mCursorTrack.hasPrevious(), mPopupBrowser::commit);
   }

   private void initPadsForCLipLauncher()
   {
      for (int p = 0; p < 16; p++)
      {
         final int slot = p & 0x7;
         final boolean isScene = p >= 8;

         final RGBButton pad = addElement(new RGBButton(Buttons.drumPad(p)));

         if (isScene)
         {
            mBaseLayer.bind(pad, new RGBButtonTarget()
            {
               @Override
               public float[] getRGB()
               {
                  final Scene scene = getScene();

                  if (scene.exists().get())
                  {
                     return RGBButtonTarget.mixWithValue(scene.color(), BLACK, 0.66f);
                  }

                  return BLACK;
               }

               private Scene getScene()
               {
                  return mSceneBank.getScene(slot);
               }

               @Override
               public boolean get()
               {
                  return true;
               }

               @Override
               public void set(final boolean pressed)
               {
                  getScene().launch();
               }
            });
         }
         else
         {
            mBaseLayer.bind(pad, new RGBButtonTarget()
            {
               @Override
               public float[] getRGB()
               {
                  final ClipLauncherSlot s = getSlot();

                  if (s.isRecordingQueued().get())
                  {
                     return RGBButtonTarget.mix(RED, BLACK, getTransportPulse(1.0, 1));
                  }
                  else if (s.hasContent().get())
                  {
                     if (s.isPlaybackQueued().get())
                     {
                        return RGBButtonTarget.mixWithValue(s.color(), WHITE, 1 - getTransportPulse(1.0, 1));
                     }
                     else if (s.isRecording().get())
                     {
                        return RED;
                     }
                     else if (s.isPlaying().get())
                     {
                        return RGBButtonTarget.getFromValue(s.color());
                     }

                     return RGBButtonTarget.mixWithValue(s.color(), BLACK, 0.66f);
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

               private ClipLauncherSlot getSlot()
               {
                  return mCursorTrack.clipLauncherSlotBank().getItemAt(slot);
               }

               @Override
               public boolean get()
               {
                  return getSlot().hasContent().get();
               }

               @Override
               public void set(final boolean pressed)
               {
                  getSlot().launch();
               }
            });
         }
      }
   }

   private RGBButton addPageButtonMapping(final int number, final Buttons id)
   {
      final RGBButton button = addElement(new RGBButton(id));
      mBaseLayer.bind(button, new RGBButtonTarget()
      {
         @Override
         public float[] getRGB()
         {
            if (number >= mRemoteControls.pageCount().get())
               return BLACK;

            final boolean isActive = mRemoteControls.selectedPageIndex().get() == number;
            return isActive ? WHITE : GREY;
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
               mRemoteControls.selectedPageIndex().set(number);
            }
         }
      });

      final BooleanValue isCursor = mCursorTrack.createEqualsValue(mTrackBank.getItemAt(number));
      isCursor.markInterested();

      mMultiLayer.bind(button, new RGBButtonTarget()
      {
         @Override
         public float[] getRGB()
         {
            return isCursor.get() ? WHITE
               : RGBButtonTarget.getFromValue(mTrackBank.getItemAt(number).color());
         }

         @Override
         public boolean get()
         {
            return true;
         }

         @Override
         public void set(final boolean pressed)
         {
            mTrackBank.getItemAt(number).selectInMixer();
         }
      });

      return button;
   }

   private void initControls()
   {
      final Encoder encoder9 = addElement(new Encoder(24));

      mBaseLayer.bindEncoder(encoder9, mCursorTrack.volume(), 101);

      for (int i = 0; i < 8; i++)
      {
         final Encoder encoder = addElement(new Encoder(0x10 + i));

         mBaseLayer.bindEncoder(encoder, mRemoteControls.getParameter(i), 101);
         mMultiLayer.bindEncoder(encoder, mTrackBank.getItemAt(i).pan(), 101);
      }

      final Encoder wheel = addElement(new Encoder(0x3C));
      mBaseLayer.bind(wheel, (EncoderTarget)steps -> {
         if (steps > 0)
            mCursorTrack.selectNext();
         else
            mCursorTrack.selectPrevious();
         mCursorTrack.makeVisibleInMixer();
         mTrackBank.scrollIntoView(mCursorTrack.position().get());
      });

      mBrowserLayer.bind(wheel, (EncoderTarget)steps -> {
         if (steps > 0)
            mPopupBrowser.selectNextFile();
         else
            mPopupBrowser.selectPreviousFile();
      });

      for (int i = 0; i < 9; i++)
      {
         final Fader fader = addElement(new Fader(i));
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

   private Layer mMultiLayer = new Layer()
   {
      @Override
      public void setActivate(final boolean active)
      {
         super.setActivate(active);

         updateIndications();
      }
   };

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

   private boolean mIsRewinding;

   private boolean mIsForwarding;

   private Display mDisplay;

   private Action mSaveAction;

   private Layer mBaseLayer;

   private Layer mBrowserLayer;

   private TrackBank mTrackBank;

   private MasterTrack mMasterTrack;

   private boolean mDawMode = true;

   private SceneBank mSceneBank;

   private HardwareSurface mHardwareSurface;

   private HardwareButton mChordButton, mTransButton, mOctMinusButton, mOctPlusButton, mPadButton,
      mMidiChButton, mSoloButton, mMuteButton, mRecordArmButton, mReadButton, mWriteButton, mSaveButton,
      mPunchInButton, mPunchOutButton, mMetroButton, mUndoButton, mRewindButton, mForwardButton, mStopButton,
      mPlayOrPauseButton, mRecordButton, mLoopButton, mCategoryButton, mPresetButton, mPresetPreviousButton,
      mPresetNextButton, mWheelClickButton, mAnalogLabButton, mDAWButtom, mUserButton, mNexButton,
      mPreviousButton, mBankButton, mSelectMultiButton;

   private final HardwareButton[] mPadButtons = new HardwareButton[16];

   private final HardwareButton[] mSelectButtons = new HardwareButton[8];

   private Layers mLayers;
}
