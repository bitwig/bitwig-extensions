package com.bitwig.extensions.controllers.arturia.keylab.mk2;

import com.bitwig.extension.api.util.midi.SysexBuilder;
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
import com.bitwig.extension.controller.api.HardwareControlType;
import com.bitwig.extension.controller.api.MasterTrack;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PopupBrowser;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.framework.ControlElement;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.LayeredControllerExtension;
import com.bitwig.extensions.framework.targets.ButtonTarget;
import com.bitwig.extensions.framework.targets.EncoderTarget;
import com.bitwig.extensions.framework.targets.RGBButtonTarget;

public class ArturiaKeylabMkII extends LayeredControllerExtension
{
   float[] GREY = {0.2f, 0.2f, 0.2f};
   float[] WHITE = {1.f, 1.f, 1.f};
   float[] BLACK = {0.f, 0.f, 0.f};
   float[] ORANGE = {1.f, 0.5f, 0.f};
   float[] BLUEY = {0.f, 0.5f, 1.f};
   float[] GREEN = {0.f, 1.0f, 0.f};
   float[] RED = {1.f, 0.0f, 0.f};

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

      for(int i=0; i<8; i++)
      {
         mRemoteControls.getParameter(i).name().markInterested();
         mRemoteControls.getParameter(i).value().markInterested();
         mRemoteControls.getParameter(i).value().displayedValue().markInterested();
      }

      for(int i=0; i<9; i++)
      {
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
      mRemoteControls.pageCount().markInterested();

      mRemoteControls.selectedPageIndex().markInterested();

      mTrackBank = host.createTrackBank(8, 0, LAUNCHER_SCENES);

      for(int i=0; i<8; i++)
      {
         Track track = mTrackBank.getItemAt(i);
         track.color().markInterested();
      }

      for(int s = 0; s < LAUNCHER_SCENES; s++)
      {
         ClipLauncherSlot slot = mCursorTrack.clipLauncherSlotBank().getItemAt(s);
         slot.color().markInterested();
         slot.isPlaying().markInterested();
         slot.isRecording().markInterested();
         slot.isPlaybackQueued().markInterested();
         slot.isRecordingQueued().markInterested();
         slot.hasContent().markInterested();

         Scene scene = mSceneBank.getScene(s);
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

      host.getMidiInPort(1).setMidiCallback(getMidiCallbackToUseForLayers());
      host.getMidiInPort(1).setSysexCallback(this::onSysex);

      sendSysex(SysexBuilder.fromHex("F0 00 20 6B 7F 42 02 00 40 52")
         .addByte(DAWMode.Live.getID()).terminate());
      // Init DAW preset in Live mode

      sendSysex("F0 00 20 6B 7F 42 05 02 F7"); // Set to DAW mode

      updateIndications();

      initButtons();
      initControls();
      initPadsForCLipLauncher();
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
            String vol = mCursorTrack.volume().displayedValue().getLimited(8);
            String pan = mCursorTrack.pan().displayedValue().getLimited(8);

            StringBuilder sb = new StringBuilder(16);
            sb.append(vol);
            int N = 16 - vol.length() - pan.length();
            for(int i=0; i<N; i++) sb.append(" ");
            sb.append(pan);

            return sb.toString();
         }
      });

      RGBButton multi = addElement(new RGBButton(Buttons.SELECT_MULTI));
      mBaseLayer.bind(multi, new RGBButtonTarget()
      {
         @Override
         public float[] getRGB()
         {
            return isLayerActive(mMultiLayer) ? BLUEY : ORANGE;
         }

         @Override
         public boolean get()
         {
            return false;
         }

         @Override
         public void set(final boolean pressed)
         {
            if (pressed) toggleLayer(mMultiLayer);
         }
      });

      mBrowserLayer.bind(multi, new RGBButtonTarget()
      {
         @Override
         public float[] getRGB()
         {
            return WHITE;
         }

         @Override
         public boolean get()
         {
            return false;
         }

         @Override
         public void set(final boolean pressed)
         {
            if (pressed) mPopupBrowser.cancel();
         }
      });
   }

   private void onSysex(final String s)
   {
      final String ENTER_DAW_MODE = "f000206b7f420200001500f7";
      final String EXIT_DAW_MODE = "f000206b7f42020000157ff7";

      if (s.equals(ENTER_DAW_MODE))
      {
         mDawMode = true;

         activateLayer(mBaseLayer);

         getHost().scheduleTask(() ->
         {
            reset();
         }, 150);
      }
      else if (s.equals(EXIT_DAW_MODE))
      {
         mDawMode = false;
         deactivateLayer(mBaseLayer);
      }

      updateIndications();
   }

   private void reset()
   {
      for (ControlElement element : getElements())
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

   @Override
   protected void toggleLayer(final Layer layer)
   {
      super.toggleLayer(layer);
   }

   private void updateIndications()
   {
      mCursorTrack.volume().setIndication(mDawMode);

      boolean isMixer = isLayerActive(mMultiLayer);

      mDevice.setIsSubscribed(!isMixer && mDawMode);
      mCursorTrack.setIsSubscribed(mDawMode);

      for(int i=0; i<8; i++)
      {
         mRemoteControls.getParameter(i).setIndication(!isMixer && mDawMode);
         Track track = mTrackBank.getItemAt(i);
         track.volume().setIndication(isMixer && mDawMode);
         track.pan().setIndication(isMixer && mDawMode);
      }

      for(int i=0; i<9; i++)
      {
         mDeviceEnvelopes.getParameter(i).setIndication(!isMixer && mDawMode);
      }

      for(int s = 0; s < LAUNCHER_SCENES; s++)
      {
         ClipLauncherSlot slot = mCursorTrack.clipLauncherSlotBank().getItemAt(s);
         slot.setIndication(mDawMode);
         mSceneBank.getScene(s).setIndication(mDawMode);
      }
   }

   private void initButtons()
   {
      Button rewind = addElement(new Button(Buttons.REWIND));
      mBaseLayer.bind(rewind, new ButtonTarget()
      {
         @Override
         public boolean get()
         {
            return mIsRewinding;
         }

         @Override
         public void set(final boolean pressed)
         {
            mIsRewinding = pressed;

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
            return mIsForwarding;
         }

         @Override
         public void set(final boolean pressed)
         {
            mIsForwarding = pressed;

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

      Button read = addElement(new Button(Buttons.READ));
      mBaseLayer.bindToggle(read, mTransport.isClipLauncherOverdubEnabled());

      Button prev = addElement(new Button(Buttons.PREVIOUS));
      mBaseLayer.bindPressedRunnable(prev, null, mRemoteControls::selectPrevious);
      mMultiLayer.bindPressedRunnable(prev, mTrackBank.canScrollBackwards(), mTrackBank::scrollBackwards);
      Button next = addElement(new Button(Buttons.NEXT));
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

      mBrowserLayer.bindPressedRunnable(select1, ORANGE, () -> mPopupBrowser.selectedContentTypeIndex().set(0));
      mBrowserLayer.bindPressedRunnable(select2, ORANGE, () -> mPopupBrowser.selectedContentTypeIndex().set(1));
      mBrowserLayer.bindPressedRunnable(select3, ORANGE, () -> mPopupBrowser.selectedContentTypeIndex().set(2));
      mBrowserLayer.bindPressedRunnable(select4, ORANGE, () -> mPopupBrowser.selectedContentTypeIndex().set(3));

      CursorBrowserFilterItem categories = (CursorBrowserFilterItem)mPopupBrowser.categoryColumn().createCursorItem();
      CursorBrowserFilterItem creators = (CursorBrowserFilterItem)mPopupBrowser.creatorColumn().createCursorItem();

      mBrowserLayer.bindPressedRunnable(select5, GREEN, () -> categories.selectPrevious());
      mBrowserLayer.bindPressedRunnable(select6, GREEN, () -> categories.selectNext());
      mBrowserLayer.bindPressedRunnable(select7, RED, () -> creators.selectPrevious());
      mBrowserLayer.bindPressedRunnable(select8, RED, () -> creators.selectNext());

      Button presetPrev = addElement(new Button(Buttons.PRESET_PREVIOUS));
      mBaseLayer.bindPressedRunnable(presetPrev, mDevice.hasPrevious(), mDevice::selectPrevious);
      ClipLauncherSlotBank cursorTrackSlots = mCursorTrack.clipLauncherSlotBank();
      cursorTrackSlots.scrollPosition().markInterested();

      mMultiLayer.bindPressedRunnable(presetPrev, cursorTrackSlots.canScrollBackwards(), () ->
      {
         final int current = cursorTrackSlots.scrollPosition().get();
         cursorTrackSlots.scrollPosition().set(current-1);
         mSceneBank.scrollPosition().set(current-1);
      });
      mBrowserLayer.bindPressedRunnable(presetPrev, null, mPopupBrowser::cancel);

      Button presetNext = addElement(new Button(Buttons.PRESET_NEXT));
      mBaseLayer.bindPressedRunnable(presetNext, mDevice.hasNext(), mDevice::selectNext);
      mMultiLayer.bindPressedRunnable(presetNext, cursorTrackSlots.canScrollForwards(), () ->
      {
         final int current = cursorTrackSlots.scrollPosition().get();
         cursorTrackSlots.scrollPosition().set(current+1);
         mSceneBank.scrollPosition().set(current+1);
      });
      mBrowserLayer.bindPressedRunnable(presetNext, null, mPopupBrowser::commit);

      Button wheelClick = addElement(new Button(Buttons.WHEEL_CLICK));
      mBaseLayer.bindPressedRunnable(wheelClick, null, () ->
      {
         mDisplay.reset();
         startPresetBrowsing();
      });
      mMultiLayer.bindPressedRunnable(wheelClick, null, () ->
      {
      });

      mBrowserLayer.bindPressedRunnable(wheelClick, mCursorTrack.hasPrevious(), mPopupBrowser::commit);
   }

   private void initPadsForCLipLauncher()
   {
      for(int p=0; p<16; p++)
      {
         final int slot = p & 0x7;
         final boolean isScene = p >= 8;

         RGBButton pad = addElement(new RGBButton(Buttons.drumPad(p)));

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
                  ClipLauncherSlot s = getSlot();

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
                  double p = mTransport.getPosition().get() * multiplier;
                  return (float) ((0.5 + 0.5 * Math.cos(p * 2 * Math.PI)) * amount);
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
      RGBButton button = addElement(new RGBButton(id));
      mBaseLayer.bind(button, new RGBButtonTarget()
      {
         @Override
         public float[] getRGB()
         {
            if (number >= mRemoteControls.pageCount().get()) return BLACK;

            boolean isActive = mRemoteControls.selectedPageIndex().get() == number;
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

      BooleanValue isCursor = mCursorTrack.createEqualsValue(mTrackBank.getItemAt(number));
      isCursor.markInterested();

      mMultiLayer.bind(button, new RGBButtonTarget()
      {
         @Override
         public float[] getRGB()
         {
            return isCursor.get() ? WHITE : RGBButtonTarget.getFromValue(mTrackBank.getItemAt(number).color());
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
         if (steps > 0) mCursorTrack.selectNext();
         else mCursorTrack.selectPrevious();
         mCursorTrack.makeVisibleInMixer();
         mTrackBank.scrollIntoView(mCursorTrack.position().get());
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
   private Layer mBaseLayer = new Layer();
   private Layer mBrowserLayer = new Layer();
   private TrackBank mTrackBank;
   private MasterTrack mMasterTrack;
   private boolean mDawMode = true;
   private SceneBank mSceneBank;
}
