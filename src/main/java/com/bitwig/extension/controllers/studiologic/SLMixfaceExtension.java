package com.bitwig.extension.controllers.studiologic;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.AbsoluteHardwareControlToRangedValueBinding;
import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.MidiIn;
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
      mTrackBank = host.createTrackBank(8, 0, 0);

      MidiIn midiIn = host.getMidiInPort(0);

      midiIn.setMidiCallback((ShortMidiMessageReceivedCallback)msg -> onMidi0(msg));
      midiIn.setSysexCallback((String data) -> onSysex0(data));

      defineHardwareControls(host, midiIn);

      mTransport.playAction().addBinding(mPlayButton.pressedAction());
      mTransport.recordAction().addBinding(mRecordButton.pressedAction());

      for (int i = 0; i < 8; i++)
      {
         AbsoluteHardwareControlToRangedValueBinding binding = mTrackBank.getItemAt(i).volume()
            .addBinding(mVolumeSliders[i]);
      }
   }

   private void defineHardwareControls(final ControllerHost host, MidiIn midiIn)
   {
      for (int i = 0; i < 8; i++)
      {
         AbsoluteHardwareKnob panKnob = host.createAbsoluteHardwareKnob();
         panKnob.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(15, 2 + i));
         String panLabel = "Pan" + (i + 1);
         panKnob.setLabel(panLabel);
         panKnob.value().addValueObserver(value -> host.println(panLabel + ": " + value));
         mPanKnobs[i] = panKnob;

         HardwareSlider slider = host.createHardwareSlider();
         slider.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(15, 16 + i));
         String volLabel = "Volume " + (i + 1);
         slider.setLabel(panLabel);
         slider.value().addValueObserver(value -> host.println(volLabel + ": " + value));
         mVolumeSliders[i] = slider;

         HardwareButton armButton = host.createHardwareButton();
         armButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 48, 127));
         armButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 48, 0));
         String armLabel = "Arm " + (i + 1);
         armButton.setLabel(armLabel);
         armButton.pressedAction().onAction(() -> host.println(armLabel + " presesd"));
         armButton.releasedAction().onAction(() -> host.println(armLabel + " released"));
         mArmButtons[i] = armButton;
      }

      mMasterVolumeSlider = host.createHardwareSlider();
      mMasterVolumeSlider.setLabel("Master Volume");
      mMasterVolumeSlider.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(15, 24));
      mMasterVolumeSlider.value().addValueObserver(value -> host.println("Master Volume " + value));

      mPlayButton = host.createHardwareButton();
      mPlayButton.setLabel("Play");
      mPlayButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 32));
      mPlayButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 45, 0));
      mPlayButton.pressedAction().onAction(() -> host.println("Play pressed"));
      mPlayButton.releasedAction().onAction(() -> host.println("Play released"));

      mRecordButton = host.createHardwareButton();
      mRecordButton.setLabel("Record");
      mRecordButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 34, 127));
      mRecordButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(15, 34, 0));
      mRecordButton.pressedAction().onAction(() -> host.println("Rec pressed"));
      mRecordButton.releasedAction().onAction(() -> host.println("Rec released"));
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
      // TODO Send any updates you need here.
   }

   /** Called when we receive short MIDI message on port 0. */
   private void onMidi0(ShortMidiMessage msg)
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
}
