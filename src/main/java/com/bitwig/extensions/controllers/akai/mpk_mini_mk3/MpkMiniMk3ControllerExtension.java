package com.bitwig.extensions.controllers.akai.mpk_mini_mk3;

import java.nio.charset.StandardCharsets;

import com.bitwig.extension.callback.StringValueChangedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.AbsoluteHardwareValueMatcher;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class MpkMiniMk3ControllerExtension extends ControllerExtension
{
   public MpkMiniMk3ControllerExtension(MpkMiniMk3ControllerExtensionDefinition definition, ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();

      mMidiIn = host.getMidiInPort(0);

      final NoteInput keyboardInput = mMidiIn.createNoteInput("Keys", "80????", "90????", "b001??", "e0????", "b040??", "D0????");
      keyboardInput.setShouldConsumeEvents(true);

      final NoteInput padsInput = mMidiIn.createNoteInput("Pads", "89????", "99????", "D9????");
      padsInput.setShouldConsumeEvents(true);

      mMidiOut = host.getMidiOutPort(0);

      mCursorTrack = host.createCursorTrack(0, 0);
      mCursorDevice = mCursorTrack.createCursorDevice();
      mCursorRemoteControls = mCursorDevice.createCursorRemoteControlsPage(8);

      mHardwareSurface = getHost().createHardwareSurface();
      mHardwareSurface.setPhysicalSize(318, 181);

      final Layers layers = new Layers(this);
      final Layer mainLayer = new Layer(layers, "Main");

      mKnobs = new RelativeHardwareKnob[8];
      for (int i = 0; i < 8; ++i)
      {
         final RemoteControl parameter = mCursorRemoteControls.getParameter(i);
         parameter.setIndication(true);
         parameter.markInterested();
         parameter.exists().markInterested();
         parameter.name().markInterested();
         parameter.name().addValueObserver(newValue -> {
            mShouldFlushSysex = true;
            getHost().requestFlush();
         });

         final RelativeHardwareKnob knob = mHardwareSurface.createRelativeHardwareKnob("K" + i);
         final AbsoluteHardwareValueMatcher absoluteCCValueMatcher = mMidiIn.createAbsoluteCCValueMatcher(0, 70 + i);
         knob.setAdjustValueMatcher(mMidiIn.createRelative2sComplementValueMatcher(absoluteCCValueMatcher, 127));
         knob.isUpdatingTargetValue().markInterested();
         knob.setLabel("K" + i);
         knob.setIndexInGroup(i);
         mKnobs[i] = knob;
         mainLayer.bind(knob, parameter);
      }

      mainLayer.activate();

      host.requestFlush();
   }

   @Override
   public void exit()
   {
   }

   @Override
   public void flush()
   {
      mHardwareSurface.updateHardware();

      if (mShouldFlushSysex)
         sendSysex();
   }

   void sendSysex()
   {
      String sysexBitwig2 =
         "F0 47 7F 49 64 01 76 00 50 47 4D 3A 42 49 54 57 "      //    pG.Id.v.PGM:MPC.
            + "49 47 00 00 00 00 00 00 09 01 00 04 00 00 04 01 " //    ................
            + "00 00 03 00 78 00 00 00 00 02 01 01 24 00 10 25 " //    ....x.......$..%
            + "01 11 26 02 12 27 03 13 28 04 14 29 05 15 2A 06 " //    ..&..'..(..)..*.
            + "16 2B 07 17 2C 08 18 2D 09 19 2E 0A 1A 2F 0B 1B " //    .+..,..-...../..
            + "30 0C 1C 31 0D 1D 32 0E 1E 33 0F 1F ";

      for (int i = 0; i < 8; ++i)
      {
         String name = "none - " + (i + 1);
         final RemoteControl parameter = mCursorRemoteControls.getParameter(i);
         if (parameter.exists().get())
         {
            final String paramName = parameter.name().get();
            if (!paramName.isBlank())
               name = paramName;
         }
         final String s = parameter.name().get();
         sysexBitwig2 += configureKnob(70 + i, name);
      }

      sysexBitwig2 += " 0C F7 ";

      mMidiOut.sendSysex(sysexBitwig2);

      mShouldFlushSysex = false;
   }

   private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
   String configureKnob(int cc, String name)
   {
      String str = " 01 " + Integer.toHexString(cc) + " 00 7F ";
      final byte[] bytes = name.getBytes(StandardCharsets.US_ASCII);
      for (int i = 0; i < 16; ++i)
      {
         if (i < bytes.length)
            str += Integer.toHexString(bytes[i]);
         else
            str += "00 ";
      }
      return str;
   }

   private MidiIn mMidiIn;
   private MidiOut mMidiOut;
   private CursorTrack mCursorTrack;
   private PinnableCursorDevice mCursorDevice;
   private CursorRemoteControlsPage mCursorRemoteControls;
   private RelativeHardwareKnob[] mKnobs;
   private HardwareSurface mHardwareSurface;
   private boolean mShouldFlushSysex = true;
}
