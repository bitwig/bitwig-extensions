package com.bitwig.extensions.controllers.presonus;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareControlType;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Transport;

public class PresonusAtomControllerExtension extends ControllerExtension
{
   final static int CC_TRANSPORT_PLAY = 109;
   final static int CC_TRANSPORT_RECORD = 107;
   final static int CC_TRANSPORT_STOP = 111;
   final static int CC_TRANSPORT_CLICK = 105;

   final static int CC_ENCODER_1 = 14;
   final static int CC_ENCODER_2 = 15;
   final static int CC_ENCODER_3 = 16;
   final static int CC_ENCODER_4 = 17;

   final static int CC_NAV_UP = 87;
   final static int CC_NAV_DOWN = 89;
   final static int CC_NAV_LEFT = 90;
   final static int CC_NAV_RIGHT = 102;
   final static int CC_NAV_SELECT = 103;
   final static int CC_NAV_ZOOM = 91;

   public PresonusAtomControllerExtension(
      final PresonusAtomControllerExtensionDefinition definition,
      final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();
      mApplication = host.createApplication();

      final MidiIn midiIn = host.getMidiInPort(0);

      midiIn.setMidiCallback(this::onMidi);
      midiIn.setSysexCallback(this::onSysex);
      midiIn.createNoteInput("", "8?????", "9?????", "b?01??", "e?????", "b040??").setShouldConsumeEvents(true);

      mMidiOut = host.getMidiOutPort(0);

      mCursorTrack = host.createCursorTrack(0, 0);

      mCursorDevice =
         mCursorTrack.createCursorDevice("ATOM", "Atom", 0, CursorDeviceFollowMode.FIRST_INSTRUMENT);

      mRemoteControls = mCursorDevice.createCursorRemoteControlsPage(4);
      mRemoteControls.setHardwareLayout(HardwareControlType.KNOB, 4);
      for (int i = 0; i < 4; ++i)
         mRemoteControls.getParameter(i).setIndication(true);

      mTransport = host.createTransport();
   }

   private void onSysex(final String sysex)
   {
      getHost().println("received sysex: " + sysex);
   }

   private void onMidi(final int status, final int data1, final int data2)
   {
      final int channel = status & 0xf;
      final int msg = (status >> 4) & 0xf;

      if (status == 176)
      {
         if (data1 >= CC_ENCODER_1 && data1 <= CC_ENCODER_4)
         {
            int index = data1 - CC_ENCODER_1;
            mRemoteControls.getParameter(index).set(data2, 128);
         }
         else if (data2 == 127)
         {
            switch (data1)
            {
               case CC_TRANSPORT_PLAY:
                  mTransport.togglePlay();
                  break;
               case CC_TRANSPORT_STOP:
                  mTransport.stop();
                  break;
               case CC_TRANSPORT_RECORD:
                  mTransport.record();
                  break;
               case CC_TRANSPORT_CLICK:
                  mTransport.isMetronomeEnabled().toggle();
                  break;
               case CC_NAV_UP:
                  mApplication.arrowKeyUp();
                  break;
               case CC_NAV_DOWN:
                  mApplication.arrowKeyDown();
                  break;
               case CC_NAV_LEFT:
                  mApplication.arrowKeyLeft();
                  break;
               case CC_NAV_RIGHT:
                  mApplication.arrowKeyRight();
                  break;
               case CC_NAV_SELECT:
                  mApplication.enter();
                  break;
               case CC_NAV_ZOOM:
                  mApplication.zoomToFit();
                  break;
            }
         }
      }
   }

   @Override
   public void exit()
   {
   }

   @Override
   public void flush()
   {
   }

   /* API Objects */
   private CursorTrack mCursorTrack;
   private PinnableCursorDevice mCursorDevice;
   private CursorRemoteControlsPage mRemoteControls;
   private Transport mTransport;
   private MidiOut mMidiOut;
   private Application mApplication;
}
