package com.bitwig.extensions.controllers.arturia.keylab.mk2;

import java.util.function.Consumer;

import com.bitwig.extension.api.util.midi.SysexBuilder;
import com.bitwig.extension.controller.api.AbsoluteHardwareControl;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.HardwareTextDisplay;
import com.bitwig.extension.controller.api.MidiExpressions;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extension.controller.api.PianoKeyboard;
import com.bitwig.extension.controller.api.RelativeHardwareControl;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extensions.controllers.arturia.common.SysExUtil;

/** Hardware-only surface (mk3 pattern). No layer bindings here. */
class HardwareElements
{
   HardwareElements(final ControllerHost host, final int numberOfKeys)
   {
      mHost = host;
      mSurface = host.createHardwareSurface();

      final PianoKeyboard pianoKeyboard = mSurface.createPianoKeyboard("piano", numberOfKeys, 3, 0);
      pianoKeyboard.setMidiIn(host.getMidiInPort(0));
      pianoKeyboard.setIsVelocitySensitive(true);
      mPianoKeyboard = pianoKeyboard;

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

      mDisplay = mSurface.createHardwareTextDisplay("display", 2);
      mDisplay.line(0).text().setMaxChars(16);
      mDisplay.line(1).text().setMaxChars(16);
      mDisplay.onUpdateHardware(() -> {
         {
            final String upper = SysExUtil.convert(mDisplay.line(0).text().currentValue());
            final String lower = SysExUtil.convert(mDisplay.line(1).text().currentValue());

            final byte[] data = SysexBuilder.fromHex("F0 00 20 6B 7F 42 04 00 60 01 ").addString(upper, 16)
               .addHex("00 02").addString(lower, 16).addHex(" 00").terminate();

            host.getMidiOutPort(0).sendSysex(data);
         }
      });
   }

   HardwareSurface getSurface()
   {
      return mSurface;
   }

   PianoKeyboard getPianoKeyboard()
   {
      return mPianoKeyboard;
   }

   HardwareButton getButton(final ButtonId id)
   {
      return mButtons[id.ordinal()];
   }

   RelativeHardwareControl getWheel()
   {
      return mWheel;
   }

   HardwareTextDisplay getDisplay()
   {
      return mDisplay;
   }

   RelativeHardwareControl getEncoder(final int index)
   {
      return mEncoders[index];
   }

   AbsoluteHardwareControl getFader(final int index)
   {
      return mFaders[index];
   }

   private RelativeHardwareControl createEncoder(final String id, final int cc)
   {
      final RelativeHardwareKnob encoder = mSurface.createRelativeHardwareKnob(id);

      encoder.setAdjustValueMatcher(mHost.getMidiInPort(1).createRelativeSignedBitCCValueMatcher(0, cc, 100));

      return encoder;
   }

   private RelativeHardwareControl createClickEncoder(
      final String id,
      final int cc,
      final int clicksForOneRotation)
   {
      final RelativeHardwareKnob encoder = mSurface.createRelativeHardwareKnob(id);

      encoder.setAdjustValueMatcher(
         mHost.getMidiInPort(1).createRelativeSignedBitCCValueMatcher(0, cc, clicksForOneRotation));
      encoder.setStepSize(1.0 / clicksForOneRotation);

      return encoder;
   }

   private AbsoluteHardwareControl createFader(final int index)
   {
      final HardwareSlider fader = mSurface.createHardwareSlider("fader" + (index + 1));

      fader.setAdjustValueMatcher(mHost.getMidiInPort(1).createAbsolutePitchBendValueMatcher(index));

      return fader;
   }

   private void createButtons()
   {
      for (final ButtonId id : ButtonId.values())
      {
         mButtons[id.ordinal()] = createButton(id);
      }
   }

   private HardwareButton createButton(final ButtonId id)
   {
      final MidiIn midiIn = mHost.getMidiInPort(1);
      final HardwareButton button = mSurface.createHardwareButton(id.name());
      final MidiExpressions expressions = mHost.midiExpressions();

      final String isNoteOn = expressions.createIsNoteOnExpression(id.getChannel(), id.getKey());
      final String pressedExpression = isNoteOn + " && data2 >= 64";
      final String releasedExpression = "(" + isNoteOn + " && data2 < 64) || "
         + expressions.createIsNoteOffExpression(id.getChannel(), id.getKey());
      button.pressedAction().setActionMatcher(midiIn.createActionMatcher(pressedExpression));
      button.releasedAction().setActionMatcher(midiIn.createActionMatcher(releasedExpression));

      if (id.isRGB())
      {
         final MultiStateHardwareLight light = mSurface.createMultiStateHardwareLight(id + "_light");

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

               mHost.getMidiOutPort(0).sendSysex(sysex);
            }
         };

         light.state().onUpdateHardware(sendColor);
      }
      else
      {
         final OnOffHardwareLight light = mSurface.createOnOffHardwareLight(id + "_light");

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

               mHost.getMidiOutPort(0).sendSysex(sysex);
            }
         };

         light.isOn().onUpdateHardware(sendOnOff);
      }

      return button;
   }

   private final ControllerHost mHost;

   private final HardwareSurface mSurface;

   private final PianoKeyboard mPianoKeyboard;

   private final HardwareButton[] mButtons = new HardwareButton[ButtonId.values().length];

   private final RelativeHardwareControl[] mEncoders = new RelativeHardwareControl[9];

   private final AbsoluteHardwareControl[] mFaders = new AbsoluteHardwareControl[9];

   private RelativeHardwareControl mWheel;

   private HardwareTextDisplay mDisplay;
}
