package com.bitwig.extensions.controllers.arturia.keylab.mk2;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
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
import com.bitwig.extension.controller.api.MidiExpressions;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extension.controller.api.PopupBrowser;
import com.bitwig.extension.controller.api.RelativeHardwareControl;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.Scene;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.framework.DebugUtilities;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.oldframework.ControlElement;

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

      updateIndications();

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

      mTrackBank.followCursorTrack(mCursorTrack);
   }

   private void initHardwareSurface()
   {
      mHardwareSurface = getHost().createHardwareSurface();

      createButtons();

      for (int i = 0; i < 8; i++)
      {
         mEncoders[i] = createEncoder(0x10 + i);
      }

      mWheel = createEncoder(0x3C);
   }

   private void initLayers()
   {
      mLayers = new Layers(this);
      mBaseLayer = new Layer(mLayers, "Base");
      mBrowserLayer = new Layer(mLayers, "Browser");
      mMultiLayer = new Layer(mLayers, "Multi")
      {
         @Override
         public void setIsActive(final boolean active)
         {
            super.setIsActive(active);

            updateIndications();
         }
      };

      initBaseLayer();
      initBrowserLayer();
      initMultiLayer();

      mBaseLayer.activate();

      DebugUtilities.createDebugLayer(mLayers, mHardwareSurface).activate();
      ;
   }

   private void initBaseLayer()
   {
      mBaseLayer.bindToggle(ButtonId.SELECT_MULTI, mMultiLayer);
      mBaseLayer.bind((Supplier<Color>)() -> mMultiLayer.isActive() ? BLUEY : ORANGE, ButtonId.SELECT_MULTI);

      mBaseLayer.bindPressed(ButtonId.REWIND, () -> {
         mIsRewinding = true;
         repeatRewind();
      });
      mBaseLayer.bind(() -> mIsRewinding, ButtonId.REWIND);

      mBaseLayer.bindPressed(ButtonId.FORWARD, () -> {
         mIsForwarding = true;
         repeatForward();
      });
      mBaseLayer.bind(() -> mIsForwarding, ButtonId.FORWARD);

      mBaseLayer.bindPressed(ButtonId.STOP, mTransport.stopAction());
      mBaseLayer.bindToggle(ButtonId.PLAY_OR_PAUSE, mTransport.playAction(), mTransport.isPlaying());
      mBaseLayer.bindToggle(ButtonId.RECORD, mTransport.recordAction(), mTransport.isArrangerRecordEnabled());
      mBaseLayer.bindToggle(ButtonId.LOOP, mTransport.isArrangerLoopEnabled());
      mBaseLayer.bindToggle(ButtonId.METRO, mTransport.isMetronomeEnabled());
      mBaseLayer.bindPressed(ButtonId.UNDO, mApplication.undoAction());
      mBaseLayer.bindPressed(ButtonId.SAVE, mSaveAction::invoke);

      mBaseLayer.bindToggle(ButtonId.PUNCH_IN, mTransport.isPunchInEnabled());
      mBaseLayer.bindToggle(ButtonId.PUNCH_OUT, mTransport.isPunchOutEnabled());

      mBaseLayer.bindToggle(ButtonId.SOLO, mCursorTrack.solo());
      mBaseLayer.bindToggle(ButtonId.MUTE, mCursorTrack.mute());
      mBaseLayer.bindToggle(ButtonId.RECORD_ARM, mCursorTrack.arm());
      mBaseLayer.bindToggle(ButtonId.WRITE, mTransport.isArrangerAutomationWriteEnabled());

      mBaseLayer.bindToggle(ButtonId.READ, mTransport.isClipLauncherOverdubEnabled());

      mBaseLayer.bindPressed(ButtonId.PREVIOUS, mRemoteControls::selectPrevious);
      mBaseLayer.bindPressed(ButtonId.NEXT, mRemoteControls::selectNext);

      for (int i = 0; i < 8; i++)
      {
         final ButtonId selectId = ButtonId.select(i);
         final int number = i;

         mBaseLayer.bindPressed(selectId, () -> mRemoteControls.selectedPageIndex().set(number));
         mBaseLayer.bind(() -> {
            if (number >= mRemoteControls.pageCount().get())
               return BLACK;

            final boolean isActive = mRemoteControls.selectedPageIndex().get() == number;
            return isActive ? WHITE : GREY;
         }, selectId);
      }

      mBaseLayer.bindToggle(ButtonId.PRESET_PREVIOUS, mDevice.selectPreviousAction(), mDevice.hasPrevious());

      mBaseLayer.bindToggle(ButtonId.PRESET_NEXT, mDevice.selectNextAction(), mDevice.hasNext());

      mBaseLayer.bindPressed(ButtonId.WHEEL_CLICK, () -> {
         mDisplay.reset();
         startPresetBrowsing();
      });

      initPadsForCLipLauncher();

      mBaseLayer.bind(mEncoders[8], mCursorTrack.volume());

      for (int i = 0; i < 8; i++)
      {
         mBaseLayer.bind(mEncoders[i], mRemoteControls.getParameter(i));
      }

      mBaseLayer.bind(mWheel, mCursorTrack);
   }

   private void initBrowserLayer()
   {
      mBrowserLayer.bindPressed(ButtonId.SELECT_MULTI, mPopupBrowser::cancel);
      mBrowserLayer.bind(WHITE, ButtonId.SELECT_MULTI);

      for (int i = 0; i < 4; i++)
      {
         final int number = i;
         final ButtonId selectId = ButtonId.select(i);

         mBrowserLayer.bindPressed(selectId, () -> mPopupBrowser.selectedContentTypeIndex().set(number));
         mBrowserLayer.bind(ORANGE, selectId);
      }

      final CursorBrowserFilterItem categories = (CursorBrowserFilterItem)mPopupBrowser.categoryColumn()
         .createCursorItem();
      final CursorBrowserFilterItem creators = (CursorBrowserFilterItem)mPopupBrowser.creatorColumn()
         .createCursorItem();

      mBrowserLayer.bindPressed(ButtonId.SELECT5, categories.selectPreviousAction());
      mBrowserLayer.bind(GREEN, ButtonId.SELECT5);

      mBrowserLayer.bindPressed(ButtonId.SELECT6, categories.selectNextAction());
      mBrowserLayer.bind(GREEN, ButtonId.SELECT6);

      mBrowserLayer.bindPressed(ButtonId.SELECT7, creators.selectPreviousAction());
      mBrowserLayer.bind(RED, ButtonId.SELECT7);

      mBrowserLayer.bindPressed(ButtonId.SELECT8, creators.selectNextAction());
      mBrowserLayer.bind(RED, ButtonId.SELECT8);

      mBrowserLayer.bindPressed(ButtonId.PRESET_PREVIOUS, mPopupBrowser.cancelAction());
      mBrowserLayer.bindPressedRunnable(ButtonId.PRESET_NEXT, mPopupBrowser.commitAction());

      mBrowserLayer.bindToggle(ButtonId.WHEEL_CLICK, mPopupBrowser.commitAction(),
         mCursorTrack.hasPrevious());
      mBrowserLayer.bind(mWheel, mPopupBrowser);
   }

   private void initMultiLayer()
   {
      mMultiLayer.bindToggle(ButtonId.PREVIOUS, mTrackBank.scrollBackwardsAction(),
         mTrackBank.canScrollBackwards());

      mMultiLayer.bindToggle(ButtonId.NEXT, mTrackBank.scrollForwardsAction(),
         mTrackBank.canScrollForwards());

      for (int i = 0; i < 8; i++)
      {
         final ButtonId selectId = ButtonId.select(i);
         final int number = i;
         final BooleanValue isCursor = mCursorTrack.createEqualsValue(mTrackBank.getItemAt(number));

         mBaseLayer.bindPressed(selectId, () -> mRemoteControls.selectedPageIndex().set(number));
         mBaseLayer.bind(() -> {
            return isCursor.get() ? WHITE : mTrackBank.getItemAt(number).color().get();
         }, selectId);
      }

      final ClipLauncherSlotBank cursorTrackSlots = mCursorTrack.clipLauncherSlotBank();
      cursorTrackSlots.scrollPosition().markInterested();

      mMultiLayer.bindToggle(ButtonId.PRESET_PREVIOUS, () -> {
         final int current = cursorTrackSlots.scrollPosition().get();
         cursorTrackSlots.scrollPosition().set(current - 1);
         mSceneBank.scrollPosition().set(current - 1);
      }, cursorTrackSlots.canScrollBackwards());

      mMultiLayer.bindToggle(ButtonId.PRESET_NEXT, () -> {
         final int current = cursorTrackSlots.scrollPosition().get();
         cursorTrackSlots.scrollPosition().set(current + 1);
         mSceneBank.scrollPosition().set(current + 1);
      }, cursorTrackSlots.canScrollForwards());

      mMultiLayer.bindPressed(ButtonId.WHEEL_CLICK, () -> {
      });

      for (int i = 0; i < 8; i++)
      {
         mMultiLayer.bind(mEncoders[i], mTrackBank.getItemAt(i).pan());
      }
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

   private void initControls()
   {

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

   private Layer mMultiLayer;

   private RelativeHardwareControl createEncoder(final int cc)
   {
      final RelativeHardwareKnob encoder = mHardwareSurface.createRelativeHardwareKnob();

      encoder.setAdjustValueMatcher(getMidiInPort(1).createRelative2sComplementCCValueMatcher(0, cc));

      return encoder;
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
      final HardwareButton button = mHardwareSurface.createHardwareButton();
      final MidiExpressions expressions = getHost().midiExpressions();

      final String isNoteOn = expressions.createIsNoteOnExpression(id.getChannel(), id.getKey());
      final String pressedExpression = isNoteOn + " && data2 >= 64";
      final String releasedExpression = isNoteOn + " && data2 < 64";

      button.pressedAction().setActionMatcher(midiIn.createActionMatcher(pressedExpression));
      button.releasedAction().setActionMatcher(midiIn.createActionMatcher(releasedExpression));

      if (id.isRGB())
      {
         final MultiStateHardwareLight light = mHardwareSurface
            .createMultiStateHardwareLight(ArturiaKeylabMkII::stateToColor);

         button.setBackgroundLight(light);

         final IntConsumer sendColor = new IntConsumer()
         {

            @Override
            public void accept(final int state)
            {
               final Color c = stateToColor(state);
               final int red = fromFloat(c.getRed());
               final int green = fromFloat(c.getGreen());
               final int blue = fromFloat(c.getBlue());

               final byte[] sysex = SysexBuilder.fromHex("F0 00 20 6B 7F 42 02 00 16")
                  .addByte(id.getSysexID()).addByte(red).addByte(green).addByte(blue).terminate();

               if (mLastSysex == null || !Arrays.equals(mLastSysex, sysex))
               {
                  getMidiOutPort(1).sendSysex(sysex);
                  mLastSysex = sysex;
               }
            }

            private byte[] mLastSysex;

         };

         light.state().onUpdateHardware(sendColor);
      }
      else
      {
         final OnOffHardwareLight light = mHardwareSurface.createOnOffHardwareLight();

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

               if (mLastSysex == null || !Arrays.equals(mLastSysex, sysex))
               {
                  getMidiOutPort(1).sendSysex(sysex);
                  mLastSysex = sysex;
               }
            }

            private byte[] mLastSysex;
         };

         light.isOn().onUpdateHardware(sendOnOff);
      }

      return button;
   }

   private static int colorToState(final Color c)
   {
      if (c == null) // Off
         return 0;

      return c.getRed255() << 16 | c.getGreen255() << 8 | c.getGreen255();
   }

   private static Color stateToColor(final int state)
   {
      final int red = (state & 0xFF0000) >> 16;
      final int green = (state & 0xFF00) >> 8;
      final int blue = (state & 0xFF);

      return Color.fromRGB255(red, green, blue);
   }

   private static int fromFloat(final double x)
   {
      return Math.max(0, Math.min((int)(31.0 * x), 31));
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

   private Display mDisplay;

   private Action mSaveAction;

   private Layer mBaseLayer;

   private Layer mBrowserLayer;

   private TrackBank mTrackBank;

   private MasterTrack mMasterTrack;

   private boolean mDawMode = true;

   private SceneBank mSceneBank;

   private HardwareSurface mHardwareSurface;

   private final HardwareButton[] mButtons = new HardwareButton[ButtonId.values().length];

   private RelativeHardwareControl[] mEncoders = new RelativeHardwareControl[9];

   private RelativeHardwareControl mWheel;

   private Layers mLayers;
}
