package com.bitwig.extension.controllers.studiologic;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareLight;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;

public class SLMixfaceExtension extends ControllerExtension
{
   protected SLMixfaceExtension(final SLMixfaceExtensionDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();

      mTransport = host.createTransport();
      mTransport.isPlaying().markInterested();
      mTrackBank = host.createTrackBank(8, 0, 0);

      final MidiIn midiIn = host.getMidiInPort(0);
      mMidiOut = host.getMidiOutPort(0);

      midiIn.setMidiCallback((ShortMidiMessageReceivedCallback)msg -> onMidi0(msg));
      midiIn.setSysexCallback((final String data) -> onSysex0(data));

      defineHardwareControls(host, midiIn);

      updateBindings();
   }

   private void updateBindings()
   {
      mPlayButton.pressedAction().setBinding(mTransport.playAction());
      mRecordButton.pressedAction().setBinding(mTransport.recordAction());

      for (int i = 0; i < 8; i++)
      {
         final Track track = mTrackBank.getItemAt(i);
         mVolumeSliders[i].setBinding(track.volume());
         mPanKnobs[i].setBinding(track.pan());

         final HardwareButton armButton = mArmButtons[i];

         armButton.pressedAction().setBinding(track.arm());
         armButton.backgroundLight().isOn().set(track.arm());
      }
   }

   private void defineHardwareControls(final ControllerHost host, final MidiIn midiIn)
   {
      for (int i = 0; i < 8; i++)
      {
         final AbsoluteHardwareKnob panKnob = host.createAbsoluteHardwareKnob();
         panKnob.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(15, 2 + i));
         final String panLabel = "Pan" + (i + 1);
         panKnob.setLabel(panLabel);
         panKnob.value().addValueObserver(value -> host.println(panLabel + ": " + value));
         mPanKnobs[i] = panKnob;

         final HardwareSlider slider = host.createHardwareSlider();
         slider.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(15, 16 + i));
         final String volLabel = "Volume " + (i + 1);
         slider.setLabel(panLabel);
         slider.value().addValueObserver(value -> host.println(volLabel + ": " + value));
         mVolumeSliders[i] = slider;

         final HardwareButton armButton = host.createHardwareButton();
         final int armCC = 48 + i;
         armButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(15, armCC, 127));
         armButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(15, armCC, 0));
         final String armLabel = "Arm " + (i + 1);
         armButton.setLabel(armLabel);
         armButton.pressedAction().onAction(() -> host.println(armLabel + " presesd"));
         armButton.releasedAction().onAction(() -> host.println(armLabel + " released"));
         final HardwareLight armBackgroundLight = host.createHardwareLight();

         armBackgroundLight.isOn().onUpdateHardware(value -> {
            sendCC(15, armCC, value ? 127 : 0);
         });

         armButton.setBackgroundLight(armBackgroundLight);

         mArmButtons[i] = armButton;
      }

      mMasterVolumeSlider = host.createHardwareSlider();
      mMasterVolumeSlider.setLabel("Master Volume");
      mMasterVolumeSlider.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(15, 24));
      mMasterVolumeSlider.value().addValueObserver(value -> host.println("Master Volume " + value));

      mPlayButton = host.createHardwareButton();
      mPlayButton.setLabel("Play");
      final String playPressedExpression = midiIn.createIsCCValueExpression(15, 32, 127) + " || "
         + midiIn.createIsCCValueExpression(15, 33, 127);
      mPlayButton.pressedAction().setActionMatcher(midiIn.createActionMatcher(playPressedExpression));
      mPlayButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 45, 0));
      mPlayButton.pressedAction().onAction(() -> host.println("Play pressed"));
      mPlayButton.releasedAction().onAction(() -> host.println("Play released"));
      final HardwareLight playLight = host.createHardwareLight();
      playLight.isOn().set(mTransport.isPlaying());
      playLight.isOn().onUpdateHardware(value -> {
         sendCC(15, 33, value ? 127 : 0);
      });
      mPlayButton.setBackgroundLight(playLight);

      mRecordButton = host.createHardwareButton();
      mRecordButton.setLabel("Record");
      mRecordButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 34, 127));
      mRecordButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 34, 0));
      mRecordButton.pressedAction().onAction(() -> host.println("Rec pressed"));
      mRecordButton.releasedAction().onAction(() -> host.println("Rec released"));
      final HardwareLight recordLight = host.createHardwareLight();
      recordLight.isOn().set(mTransport.isArrangerRecordEnabled());
      recordLight.isOn().onUpdateHardware(value -> {
         sendCC(15, 34, value ? 127 : 0);
      });
      mRecordButton.setBackgroundLight(recordLight);
   }

   private void sendMidi(final int status, final int data1, final int data2)
   {
      mMidiOut.sendMidi(status, data1, data2);
   }

   private void sendCC(final int channel, final int cc, final int value)
   {
      sendMidi(0xb0 | channel, cc, value);
   }

   @Override
   public void exit()
   {
      // TODO: Perform any cleanup once the driver exits
      // For now just show a popup notification for verification that it is no longer running.
      getHost().showPopupNotification("SL Mixface Exited");
   }

   @Override
   public void flush()
   {
      getHost().updateHardware();
   }

   /** Called when we receive short MIDI message on port 0. */
   private void onMidi0(final ShortMidiMessage msg)
   {
      // TODO: Implement your MIDI input handling code here.

      getHost().println(msg.toString());
   }

   /** Called when we receive sysex MIDI message on port 0. */
   private void onSysex0(final String data)
   {
      // MMC Transport Controls:
      if (data.equals("f07f7f0605f7"))
         mTransport.rewind();
      else if (data.equals("f07f7f0604f7"))
         mTransport.fastForward();
      else if (data.equals("f07f7f0601f7"))
         mTransport.stop();
      else if (data.equals("f07f7f0602f7"))
         mTransport.play();
      else if (data.equals("f07f7f0606f7"))
         mTransport.record();
   }

   private Transport mTransport;

   private final AbsoluteHardwareKnob[] mPanKnobs = new AbsoluteHardwareKnob[8];

   private final HardwareSlider[] mVolumeSliders = new HardwareSlider[8];

   private HardwareSlider mMasterVolumeSlider;

   private final HardwareButton[] mArmButtons = new HardwareButton[8];

   private HardwareButton mPlayButton, mRecordButton, mFastForwardButton, mRewindButton;

   private TrackBank mTrackBank;

   private MidiOut mMidiOut;
}
