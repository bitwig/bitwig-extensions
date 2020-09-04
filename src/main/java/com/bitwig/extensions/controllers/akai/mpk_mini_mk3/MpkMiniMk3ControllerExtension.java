package com.bitwig.extensions.controllers.akai.mpk_mini_mk3;

import java.nio.charset.StandardCharsets;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.AbsoluteHardwareValueMatcher;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extension.controller.api.PianoKeyboard;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.RelativePosition;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class MpkMiniMk3ControllerExtension extends ControllerExtension
{
   public MpkMiniMk3ControllerExtension(
      final MpkMiniMk3ControllerExtensionDefinition definition,
      final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();

      mMidiIn = host.getMidiInPort(0);

      final NoteInput keyboardInput = mMidiIn.createNoteInput("Keys", "80????", "90????", "b001??", "e0????",
         "b040??", "D0????");
      keyboardInput.setShouldConsumeEvents(true);

      final NoteInput padsInput = mMidiIn.createNoteInput("Pads", "89????", "99????", "D9????");
      padsInput.setShouldConsumeEvents(true);

      mMidiOut = host.getMidiOutPort(0);

      mCursorTrack = host.createCursorTrack(0, 0);
      mCursorDevice = mCursorTrack.createCursorDevice();
      mCursorRemoteControls = mCursorDevice.createCursorRemoteControlsPage(8);

      mHardwareSurface = getHost().createHardwareSurface();
      mHardwareSurface.setPhysicalSize(318, 181);

      final PianoKeyboard pianoKeyboard = mHardwareSurface.createPianoKeyboard("piano", 25, 3, 0);
      pianoKeyboard.setMidiIn(mMidiIn);

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

         final String id = "K" + (i + 1);

         final RelativeHardwareKnob knob = mHardwareSurface.createRelativeHardwareKnob(id);
         final AbsoluteHardwareValueMatcher absoluteCCValueMatcher = mMidiIn.createAbsoluteCCValueMatcher(0,
            70 + i);
         knob.setAdjustValueMatcher(
            mMidiIn.createRelative2sComplementValueMatcher(absoluteCCValueMatcher, 127));
         knob.isUpdatingTargetValue().markInterested();
         knob.setLabel(id);
         knob.setIndexInGroup(i);
         knob.setLabelPosition(RelativePosition.ABOVE);
         mKnobs[i] = knob;
         mainLayer.bind(knob, parameter);
      }

      for (int i = 0; i < 8; i++)
      {
         final HardwareButton pad = mHardwareSurface.createHardwareButton("pad" + (i + 1));
         pad.setLabel("PAD " + (i + 1));
         pad.setLabelPosition(RelativePosition.ABOVE);

         final int note = 40 + i;

         pad.pressedAction().setPressureActionMatcher(mMidiIn.createNoteOnVelocityValueMatcher(9, note));
         pad.releasedAction().setActionMatcher(mMidiIn.createNoteOffActionMatcher(9, note));

         final AbsoluteHardwareKnob padAftertouch = mHardwareSurface.createAbsoluteHardwareKnob("pad-aftertouch" + (i + 1));
         padAftertouch.setAdjustValueMatcher(mMidiIn.createPolyAftertouchValueMatcher(9, note));
         pad.setAftertouchControl(padAftertouch);
      }

      initHardwareControlPositions();

      mainLayer.activate();

      host.requestFlush();
   }

   private void initHardwareControlPositions()
   {
      final HardwareSurface surface = mHardwareSurface;

      surface.hardwareElementWithId("K1").setBounds(198.25, 31.5, 20.0, 20.25);
      surface.hardwareElementWithId("K2").setBounds(228.5, 31.5, 20.0, 20.25);
      surface.hardwareElementWithId("K3").setBounds(258.5, 31.5, 20.0, 20.25);
      surface.hardwareElementWithId("K4").setBounds(288.75, 31.5, 20.0, 20.25);
      surface.hardwareElementWithId("K5").setBounds(198.75, 59.75, 20.0, 20.25);
      surface.hardwareElementWithId("K6").setBounds(229.25, 59.75, 20.0, 20.25);
      surface.hardwareElementWithId("K7").setBounds(258.5, 59.75, 20.0, 20.25);
      surface.hardwareElementWithId("K8").setBounds(288.75, 59.75, 20.0, 20.25);
      surface.hardwareElementWithId("piano").setBounds(13.25, 93.5, 294.0, 82.0);
      surface.hardwareElementWithId("pad1").setBounds(55.5, 46.0, 28.5, 28.25);
      surface.hardwareElementWithId("pad2").setBounds(90.25, 46.0, 28.5, 28.25);
      surface.hardwareElementWithId("pad3").setBounds(125.25, 46.0, 28.5, 28.25);
      surface.hardwareElementWithId("pad4").setBounds(160.0, 46.0, 28.5, 28.25);
      surface.hardwareElementWithId("pad5").setBounds(55.0, 10.75, 28.5, 28.25);
      surface.hardwareElementWithId("pad6").setBounds(90.25, 10.75, 28.5, 28.25);
      surface.hardwareElementWithId("pad7").setBounds(125.25, 10.75, 28.5, 28.25);
      surface.hardwareElementWithId("pad8").setBounds(160.5, 10.75, 28.5, 28.25);

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
      String sysexBitwig2 = "F0 47 7F 49 64 01 76 00 50 47 4D 3A 42 49 54 57 " // pG.Id.v.PGM:MPC.
         + "49 47 00 00 00 00 00 00 09 01 00 04 00 00 04 01 " // ................
         + "00 00 03 00 78 00 00 00 00 02 01 01 24 00 10 25 " // ....x.......$..%
         + "01 11 26 02 12 27 03 13 28 04 14 29 05 15 2A 06 " // ..&..'..(..)..*.
         + "16 2B 07 17 2C 08 18 2D 09 19 2E 0A 1A 2F 0B 1B " // .+..,..-...../..
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

         sysexBitwig2 += configureKnob(70 + i, name);
      }

      sysexBitwig2 += " 0C F7 ";

      mMidiOut.sendSysex(sysexBitwig2);

      mShouldFlushSysex = false;
   }

   String configureKnob(final int cc, final String name)
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
