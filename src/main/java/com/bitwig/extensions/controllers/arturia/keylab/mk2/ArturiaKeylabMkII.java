package com.bitwig.extensions.controllers.arturia.keylab.mk2;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.api.util.midi.SysexBuilder;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.AbsoluteHardwareControl;
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
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.HardwareTextDisplay;
import com.bitwig.extension.controller.api.MasterTrack;
import com.bitwig.extension.controller.api.MidiExpressions;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extension.controller.api.PianoKeyboard;
import com.bitwig.extension.controller.api.PopupBrowser;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extension.controller.api.RelativeHardwareControl;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.framework.Layers;

public abstract class ArturiaKeylabMkII extends ControllerExtension
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

      initHardwareSurface();

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

      mTrackBank.followCursorTrack(mCursorTrack);

      initLayers();

      updateIndications();

      mPopupBrowser.exists().addValueObserver(exists -> {
         if (exists)
            mBrowserLayer.activate();
         else
            mBrowserLayer.deactivate();
      });
   }

   private void initHardwareSurface()
   {
      mHardwareSurface = getHost().createHardwareSurface();

      final PianoKeyboard pianoKeyboard = mHardwareSurface.createPianoKeyboard("piano", mNumberOfKeys, 3, 0);
      pianoKeyboard.setMidiIn(getHost().getMidiInPort(0));
      pianoKeyboard.setIsVelocitySensitive(true);

      createButtons();

      for (int i = 0; i < 9; i++)
      {
         mEncoders[i] = createEncoder("encoder" + (i + 1), 0x10 + i);
      }

      for (int i = 0; i < 9; i++)
      {
         mFaders[i] = createFader(i);
      }

      mWheel = createClickEncoder("wheel", 0x3C, 20);

      mDisplay = mHardwareSurface.createHardwareTextDisplay("display", 2);
      mDisplay.line(0).text().setMaxChars(16);
      mDisplay.line(1).text().setMaxChars(16);

      mDisplay.onUpdateHardware(() -> {
         {
            final String upper = mDisplay.line(0).text().currentValue();
            final String lower = mDisplay.line(1).text().currentValue();

            final byte[] data = SysexBuilder.fromHex("F0 00 20 6B 7F 42 04 00 60 01 ").addString(upper, 16)
               .addHex("00 02").addString(lower, 16).addHex(" 00").terminate();

            getMidiOutPort(0).sendSysex(data);
         }
      });

      initHardwareLayout(mHardwareSurface);
   }

   protected abstract void initHardwareLayout(HardwareSurface surface);

   private void initLayers()
   {
      mLayers = new Layers(this);

      mBaseLayer = new Layer(mLayers, "Base");

      mDAWLayer = new Layer(mLayers, "DAW")
      {
         @Override
         protected void onActivate()
         {
            mCursorTrack.subscribe();
         }

         @Override
         protected void onDeactivate()
         {
            mCursorTrack.unsubscribe();
         }
      };

      mMultiLayer = new Layer(mLayers, "Multi")
      {
         @Override
         protected void onActivate()
         {
            mDevice.subscribe();
            mCursorTrack.subscribe();

            updateIndications();
         }

         @Override
         protected void onDeactivate()
         {
            mDevice.unsubscribe();
            mCursorTrack.unsubscribe();

            updateIndications();
         }
      };
      mBrowserLayer = new Layer(mLayers, "Browser");

      mNotificationLayer = new Layer(mLayers, "Notifications");

      initBaseLayer();
      initDAWLayer();
      initBrowserLayer();
      initMultiLayer();
      initRemoteControlAdjustingValueNotificationLayer();


      mBaseLayer.activate();
      mDAWLayer.activate();

      // DebugUtilities.createDebugLayer(mLayers, mHardwareSurface).activate();
   }

   private void initBaseLayer()
   {
      final Layer layer = mBaseLayer;
      layer.bindPressed(ButtonId.REWIND, () -> {
         mIsRewinding = true;
         repeatRewind();
      });
      layer.bindReleased(ButtonId.REWIND, () -> {
         mIsRewinding = false;
      });
      layer.bind(() -> mIsRewinding, ButtonId.REWIND);

      layer.bindPressed(ButtonId.FORWARD, () -> {
         mIsForwarding = true;
         repeatForward();
      });
      layer.bindReleased(ButtonId.FORWARD, () -> {
         mIsForwarding = false;
      });
      layer.bind(() -> mIsForwarding, ButtonId.FORWARD);
      layer.bindPressed(ButtonId.STOP, mTransport.stopAction());
      layer.bindToggle(ButtonId.PLAY_OR_PAUSE, mTransport.playAction(), mTransport.isPlaying());
      layer.bindToggle(ButtonId.RECORD, mTransport.recordAction(), mTransport.isArrangerRecordEnabled());
      layer.bindToggle(ButtonId.LOOP, mTransport.isArrangerLoopEnabled());

      layer.bindToggle(ButtonId.SOLO, mCursorTrack.solo());
      layer.bindToggle(ButtonId.MUTE, mCursorTrack.mute());
      layer.bindToggle(ButtonId.RECORD_ARM, mCursorTrack.arm());
      layer.bindToggle(ButtonId.READ, mTransport.isClipLauncherOverdubEnabled());
      layer.bindToggle(ButtonId.WRITE, mTransport.isArrangerAutomationWriteEnabled());

      layer.bindPressed(ButtonId.SAVE, mSaveAction::invoke);
      layer.bindToggle(ButtonId.PUNCH_IN, mTransport.isPunchInEnabled());
      layer.bindToggle(ButtonId.PUNCH_OUT, mTransport.isPunchOutEnabled());
      layer.bindToggle(ButtonId.METRO, mTransport.isMetronomeEnabled());
      layer.bindPressed(ButtonId.UNDO, mApplication.undoAction());
   }

   private void initDAWLayer()
   {
      final Layer layer = mDAWLayer;
      layer.bindPressed(ButtonId.SELECT_MULTI, mMultiLayer.getToggleAction());
      layer.bind((Supplier<Color>)() -> mMultiLayer.isActive() ? BLUEY : ORANGE, ButtonId.SELECT_MULTI);

      layer.bindPressed(ButtonId.PREVIOUS, mRemoteControls::selectPrevious);
      layer.bindPressed(ButtonId.NEXT, mRemoteControls::selectNext);

      for (int i = 0; i < 8; i++)
      {
         final ButtonId selectId = ButtonId.select(i);
         final int number = i;

         layer.bindPressed(selectId, () -> mRemoteControls.selectedPageIndex().set(number));
         layer.bind(() -> {
            if (number >= mRemoteControls.pageCount().get())
               return BLACK;

            final boolean isActive = mRemoteControls.selectedPageIndex().get() == number;
            return isActive ? WHITE : GREY;
         }, selectId);
      }

      layer.bindToggle(ButtonId.PRESET_PREVIOUS, mDevice.selectPreviousAction(), mDevice.hasPrevious());

      layer.bindToggle(ButtonId.PRESET_NEXT, mDevice.selectNextAction(), mDevice.hasNext());

      layer.bindPressed(ButtonId.WHEEL_CLICK, () -> {
         clearDisplay();
         startPresetBrowsing();
      });

      initPadsForCLipLauncher();

      layer.bind(mEncoders[8], mCursorTrack.volume());

      for (int i = 0; i < 8; i++)
      {
         final RemoteControl parameter = mRemoteControls.getParameter(i);

         layer.bind(mEncoders[i], parameter);

         parameter.name().markInterested();
         parameter.value().displayedValue().markInterested();

         // When the value of a remote control changes, show it briefly on the display
         parameter.exists().markInterested();
         final RelativeHardwarControlBindable showNotificationAction =
            getHost().createRelativeHardwareControlAdjustmentTarget(value -> {
               if (parameter.exists().get())
                  showNotificationOnDisplay(parameter.name(), parameter.displayedValue());
            });
         layer.bind(mEncoders[i], showNotificationAction);
      }

      layer.bind(mWheel, mCursorTrack);

      for (int i = 0; i < 9; i++)
      {
         layer.bind(mFaders[i], mDeviceEnvelopes.getParameter(i));
      }

      layer.showText(mCursorTrack.name(),
         () -> mCursorTrack.exists().get()
            ? mDevice.exists().get() ? (mDevice.name().getLimited(16)) : "No Device"
            : "");
   }

   private void initBrowserLayer()
   {
      final Layer layer = mBrowserLayer;
      layer.bindPressed(ButtonId.SELECT_MULTI, mPopupBrowser::cancel);
      layer.bind(WHITE, ButtonId.SELECT_MULTI);

      for (int i = 0; i < 4; i++)
      {
         final int number = i;
         final ButtonId selectId = ButtonId.select(i);

         layer.bindPressed(selectId, () -> mPopupBrowser.selectedContentTypeIndex().set(number));
         layer.bind(ORANGE, selectId);
      }

      final CursorBrowserFilterItem categories = (CursorBrowserFilterItem)mPopupBrowser.categoryColumn()
         .createCursorItem();
      final CursorBrowserFilterItem creators = (CursorBrowserFilterItem)mPopupBrowser.creatorColumn()
         .createCursorItem();

      layer.bindPressed(ButtonId.SELECT5, categories.selectPreviousAction());
      layer.bind(GREEN, ButtonId.SELECT5);

      layer.bindPressed(ButtonId.SELECT6, categories.selectNextAction());
      layer.bind(GREEN, ButtonId.SELECT6);

      layer.bindPressed(ButtonId.SELECT7, creators.selectPreviousAction());
      layer.bind(RED, ButtonId.SELECT7);

      layer.bindPressed(ButtonId.SELECT8, creators.selectNextAction());
      layer.bind(RED, ButtonId.SELECT8);

      layer.bindPressed(ButtonId.PRESET_PREVIOUS, mPopupBrowser.cancelAction());
      layer.bindPressed(ButtonId.PRESET_NEXT, mPopupBrowser.commitAction());

      layer.bindToggle(ButtonId.WHEEL_CLICK, mPopupBrowser.commitAction(),
         mCursorTrack.hasPrevious());
      layer.bind(mWheel, mPopupBrowser);

      layer.showText(mBrowserCategory.name(), mBrowserResult.name());
   }

   private void initMultiLayer()
   {
      final Layer layer = mMultiLayer;
      layer.bindToggle(ButtonId.PREVIOUS, mTrackBank.scrollBackwardsAction(),
         mTrackBank.canScrollBackwards());

      layer.bindToggle(ButtonId.NEXT, mTrackBank.scrollForwardsAction(),
         mTrackBank.canScrollForwards());

      for (int i = 0; i < 8; i++)
      {
         final ButtonId selectId = ButtonId.select(i);
         final int number = i;
         final BooleanValue isCursor = mCursorTrack.createEqualsValue(mTrackBank.getItemAt(number));

         layer.bindPressed(selectId, () -> mCursorTrack.selectChannel(mTrackBank.getItemAt(number)));
         layer.bind(() -> {
            return isCursor.get() ? WHITE : mTrackBank.getItemAt(number).color().get();
         }, selectId);
      }

      final ClipLauncherSlotBank cursorTrackSlots = mCursorTrack.clipLauncherSlotBank();
      cursorTrackSlots.scrollPosition().markInterested();

      layer.bindToggle(ButtonId.PRESET_PREVIOUS, () -> {
         final int current = cursorTrackSlots.scrollPosition().get();
         cursorTrackSlots.scrollPosition().set(current - 1);
         mSceneBank.scrollPosition().set(current - 1);
      }, cursorTrackSlots.canScrollBackwards());

      layer.bindToggle(ButtonId.PRESET_NEXT, () -> {
         final int current = cursorTrackSlots.scrollPosition().get();
         cursorTrackSlots.scrollPosition().set(current + 1);
         mSceneBank.scrollPosition().set(current + 1);
      }, cursorTrackSlots.canScrollForwards());

      layer.bindPressed(ButtonId.WHEEL_CLICK, () -> {
      });

      for (int i = 0; i < 8; i++)
      {
         layer.bind(mEncoders[i], mTrackBank.getItemAt(i).pan());
      }

      for (int i = 0; i < 9; i++)
      {
         if (i == 8)
            layer.bind(mFaders[i], mMasterTrack.volume());
         else
            layer.bind(mFaders[i], mTrackBank.getItemAt(i).volume());
      }

      layer.showText(mCursorTrack.name(), () -> {
         final String vol = mCursorTrack.volume().displayedValue().getLimited(8);
         final String pan = mCursorTrack.pan().displayedValue().getLimited(8);

         final StringBuilder sb = new StringBuilder(16);
         sb.append(vol);
         final int N = 16 - vol.length() - pan.length();
         for (int i = 0; i < N; i++)
            sb.append(" ");
         sb.append(pan);

         return sb.toString();
      });
   }

   private void initRemoteControlAdjustingValueNotificationLayer()
   {
      mNotificationLayer.showText(this::getNotificationTopLine, this::getNotificationBottomLine);
   }

   private void showNotificationOnDisplay(final StringValue topRowText, final StringValue bottomRowText)
   {
      final int notificationDurationInMs = 500;

      mNotificationTopRowText = topRowText;
      mNotificationBottomRowText = bottomRowText;

      // Set end time for notification. If there is an active notification, we will respect the new end time.
      mHideNotificationTime = System.currentTimeMillis() + notificationDurationInMs;

      // Enable notification layer.
      if (!mNotificationLayer.isActive())
      {
         mNotificationLayer.activate();
         scheduleHideNotificationTask(mHideNotificationTime, notificationDurationInMs);
      }
   }

   private void scheduleHideNotificationTask(final long hideNotificationTime, final int durationInMs)
   {
      if (durationInMs <= 0)
      {
         hideNotification();
         return;
      }

      getHost().scheduleTask(() -> {
         if (hideNotificationTime == mHideNotificationTime)
         {
            // No other notification was shown in between
            hideNotification();
         }
         else
         {
            // Another notification was shown since this task was scheduled. That means we should not hide the
            // notification now, but schedule another task.
            final long remainingTimeInMs = mHideNotificationTime - System.currentTimeMillis();
            scheduleHideNotificationTask(mHideNotificationTime, (int) remainingTimeInMs);
         }
      }, durationInMs);
   }

   private void hideNotification()
   {
      mNotificationLayer.deactivate();
   }

   private String getNotificationTopLine()
   {
      return mNotificationTopRowText != null ? mNotificationTopRowText.get() : "";
   }

   private String getNotificationBottomLine()
   {
      return mNotificationBottomRowText != null ? mNotificationBottomRowText.get() : "";
   }

   @Override
   public void flush()
   {
      mHardwareSurface.updateHardware();
   }

   /** Called when we receive short MIDI message on port 0. */
   private void onMidi(final ShortMidiMessage msg)
   {
      // getHost().println(msg.toString());
   }

   private void onSysex(final String s)
   {
      final String ENTER_DAW_MODE = "f000206b7f420200001500f7";
      final String EXIT_DAW_MODE = "f000206b7f42020000157ff7";

      if (s.equals(ENTER_DAW_MODE))
      {
         mDawMode = true;
         getHost().scheduleTask(() ->
         {
            mHardwareSurface.invalidateHardwareOutputState();
            mDAWLayer.activate();

            if (mShouldReactivateMultiMode) mMultiLayer.activate();
         }, 150);
      }
      else if (s.equals(EXIT_DAW_MODE))
      {
         mDawMode = false;
         mShouldReactivateMultiMode = mMultiLayer.isActive();
         mDAWLayer.deactivate();
         mMultiLayer.deactivate();
         mBrowserLayer.deactivate();
      }

      updateIndications();
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

   private void initPadsForCLipLauncher()
   {
      for (int p = 0; p < 16; p++)
      {
         final int slotIndex = p & 0x7;
         final boolean isScene = p >= 8;

         final HardwareButton pad = getButton(ButtonId.drumPad(p));

         if (isScene)
         {
            final Scene scene = mSceneBank.getScene(slotIndex);
            mBaseLayer.bindPressed(pad, scene.launchAction());
            mBaseLayer.bind(() -> {
               if (scene.exists().get())
               {
                  return Color.mix(scene.color().get(), BLACK, 0.66f);
               }

               return BLACK;
            }, pad);
         }
         else
         {
            final ClipLauncherSlot slot = mCursorTrack.clipLauncherSlotBank().getItemAt(slotIndex);

            mBaseLayer.bindPressed(pad, slot.launchAction());
            mBaseLayer.bind((Supplier<Color>)() -> {
               if (!slot.hasContent().get())
                  return null;

               if (slot.isRecordingQueued().get())
               {
                  return Color.mix(RED, BLACK, getTransportPulse(1.0, 1));
               }
               else if (slot.hasContent().get())
               {
                  if (slot.isPlaybackQueued().get())
                  {
                     return Color.mix(slot.color().get(), WHITE, 1 - getTransportPulse(1.0, 1));
                  }
                  else if (slot.isRecording().get())
                  {
                     return RED;
                  }
                  else if (slot.isPlaying().get())
                  {
                     return slot.color().get();
                  }

                  return Color.mix(slot.color().get(), BLACK, 0.66f);
               }
               else if (mCursorTrack.arm().get())
               {
                  return Color.mix(BLACK, RED, 0.1f);
               }

               return BLACK;
            }, pad);
         }
      }
   }

   private float getTransportPulse(final double multiplier, final double amount)
   {
      final double p = mTransport.getPosition().get() * multiplier;
      return (float)((0.5 + 0.5 * Math.cos(p * 2 * Math.PI)) * amount);
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

   private Layer mMultiLayer;

   private RelativeHardwareControl createEncoder(final String id, final int cc)
   {
      final RelativeHardwareKnob encoder = mHardwareSurface.createRelativeHardwareKnob(id);

      encoder.setAdjustValueMatcher(getMidiInPort(1).createRelativeSignedBitCCValueMatcher(0, cc, 100));

      return encoder;
   }

   private RelativeHardwareControl createClickEncoder(
      final String id,
      final int cc,
      final int clicksForOneRotation)
   {
      final RelativeHardwareKnob encoder = mHardwareSurface.createRelativeHardwareKnob(id);

      encoder.setAdjustValueMatcher(
         getMidiInPort(1).createRelativeSignedBitCCValueMatcher(0, cc, clicksForOneRotation));
      encoder.setStepSize(1.0 / clicksForOneRotation);

      return encoder;
   }

   private AbsoluteHardwareControl createFader(final int index)
   {
      final HardwareSlider fader = mHardwareSurface.createHardwareSlider("fader" + (index + 1));

      fader.setAdjustValueMatcher(getMidiInPort(1).createAbsolutePitchBendValueMatcher(index));

      return fader;
   }

   private void createButtons()
   {
      for (final ButtonId id : ButtonId.values())
      {
         mButtons[id.ordinal()] = createButton(id);
      }
   }

   HardwareButton getButton(final ButtonId id)
   {
      return mButtons[id.ordinal()];
   }

   private HardwareButton createButton(final ButtonId id)
   {
      final MidiIn midiIn = getHost().getMidiInPort(1);
      final HardwareButton button = mHardwareSurface.createHardwareButton(id.name());
      final MidiExpressions expressions = getHost().midiExpressions();

      final String isNoteOn = expressions.createIsNoteOnExpression(id.getChannel(), id.getKey());
      final String pressedExpression = isNoteOn + " && data2 >= 64";
      final String releasedExpression = "(" + isNoteOn + " && data2 < 64) || "
         + expressions.createIsNoteOffExpression(id.getChannel(), id.getKey());
      button.pressedAction().setActionMatcher(midiIn.createActionMatcher(pressedExpression));
      button.releasedAction().setActionMatcher(midiIn.createActionMatcher(releasedExpression));

      if (id.isRGB())
      {
         final MultiStateHardwareLight light = mHardwareSurface.createMultiStateHardwareLight(id + "_light");

         light.setColorToStateFunction(color -> new RGBLightState(color));

         button.setBackgroundLight(light);

         final Consumer<RGBLightState> sendColor = new Consumer<RGBLightState>()
         {

            @Override
            public void accept(final RGBLightState state)
            {
               final int red = state != null ? state.getRed() : 0;
               final int green = state != null ? state.getGreen() : 0;
               final int blue = state != null ? state.getBlue() : 0;

               final byte[] sysex = SysexBuilder.fromHex("F0 00 20 6B 7F 42 02 00 16")
                  .addByte(id.getSysexID()).addByte(red).addByte(green).addByte(blue).terminate();

               getMidiOutPort(0).sendSysex(sysex);
            }
         };

         light.state().onUpdateHardware(sendColor);
      }
      else
      {
         final OnOffHardwareLight light = mHardwareSurface.createOnOffHardwareLight(id + "_light");

         button.setBackgroundLight(light);

         final Consumer<Boolean> sendOnOff = new Consumer<Boolean>()
         {

            @Override
            public void accept(final Boolean v)
            {
               final boolean isOn = v.booleanValue();

               final int intensity = isOn ? 0x7f : 0x04;

               final byte[] sysex = SysexBuilder.fromHex("F0 00 20 6B 7F 42 02 00 10")
                  .addByte(id.getSysexID()).addByte(intensity).terminate();

               getMidiOutPort(0).sendSysex(sysex);
            }
         };

         light.isOn().onUpdateHardware(sendOnOff);
      }

      return button;
   }

   private void clearDisplay()
   {
      mDisplay.line(0).text().setValue("");
      mDisplay.line(1).text().setValue("");
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

   private boolean mIsRewinding;

   private boolean mIsForwarding;

   private Action mSaveAction;

   private Layer mBaseLayer;

   private Layer mDAWLayer;

   private Layer mNotificationLayer;

   private StringValue mNotificationTopRowText;

   private StringValue mNotificationBottomRowText;

   private long mHideNotificationTime;

   private Layer mBrowserLayer;

   private TrackBank mTrackBank;

   private MasterTrack mMasterTrack;

   private boolean mDawMode = true;

   private SceneBank mSceneBank;

   private HardwareSurface mHardwareSurface;

   private final HardwareButton[] mButtons = new HardwareButton[ButtonId.values().length];

   private final RelativeHardwareControl[] mEncoders = new RelativeHardwareControl[9];

   private final AbsoluteHardwareControl[] mFaders = new AbsoluteHardwareControl[9];

   private RelativeHardwareControl mWheel;

   HardwareTextDisplay mDisplay;

   private Layers mLayers;
   private boolean mShouldReactivateMultiMode;
}
