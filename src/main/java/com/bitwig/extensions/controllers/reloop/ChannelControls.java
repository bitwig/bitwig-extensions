package com.bitwig.extensions.controllers.reloop;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSlider;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.Send;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.controllers.reloop.display.ScreenManager;
import com.bitwig.extensions.controllers.reloop.display.ScreenMode;
import com.bitwig.extensions.controllers.reloop.display.ScreenParameterBinding;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.values.Midi;

public class ChannelControls {
    private final HardwareSlider slider;
    private final RelativeHardwareKnob panKnob;
    private final AbsoluteHardwareKnob send1Knob;
    private final AbsoluteHardwareKnob send2Knob;
    private final HardwareButton trackSelectButton;
    private final OnOffHardwareLight trackSelectlight;
    private final HardwareButton trackSoloButton;
    private final OnOffHardwareLight trackSololight;
    private final HardwareButton trackArmButton;
    private final OnOffHardwareLight trackArmlight;
    private final HardwareButton shift1Button;
    private final OnOffHardwareLight shift1Buttonlight;
    private final HardwareButton shift2Button;
    private final OnOffHardwareLight shift2ButtonLight;
    private final HardwareButton shift3Button;
    private final OnOffHardwareLight shift3Buttonlight;
    private boolean encoderButtonHeld = false;
    
    private final MidiProcessor midiProcessor;
    private final int index;
    private final int channel;
    private final int baseCc;
    private final HardwareButton panButton;
    private final GlobalStates globalStates;
    
    public ChannelControls(final HardwareSurface surface, final MidiProcessor midiProcessor,
        final GlobalStates globalStates, final int index) {
        final MidiIn midiIn = midiProcessor.getMidiIn();
        this.index = index;
        this.midiProcessor = midiProcessor;
        this.globalStates = globalStates;
        channel = index % 8 + 1;
        baseCc = 0x10 + (index / 8) * 8;
        slider = surface.createHardwareSlider("FADER_" + index);
        slider.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(channel, baseCc + 7));
        panKnob = surface.createRelativeHardwareKnob("PAN_" + index);
        panKnob.setAdjustValueMatcher(midiIn.createRelative2sComplementCCValueMatcher(channel, baseCc, 64));
        panKnob.setSensitivity(1.0);
        
        panButton = surface.createHardwareButton("PanButton_" + index);
        panButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, baseCc + 6, 127));
        panButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, baseCc + 6, 0));
        
        send1Knob = surface.createAbsoluteHardwareKnob("SEND1_" + index);
        send1Knob.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(channel, baseCc + 1));
        send2Knob = surface.createAbsoluteHardwareKnob("SEND2_" + index);
        send2Knob.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(channel, baseCc + 2));
        
        trackSelectButton = surface.createHardwareButton("TRACK_SELECT_" + index);
        setCcMatcher(trackSelectButton, midiIn, 3);
        
        trackSelectlight = surface.createOnOffHardwareLight("TRACK_SELECT_LIGHT_" + index);
        trackSelectlight.isOn().onUpdateHardware(isOn -> handleLightChange(isOn, channel, baseCc + 3));
        
        trackSoloButton = surface.createHardwareButton("TRACK_SOLO_" + index);
        setCcMatcher(trackSoloButton, midiIn, 4);
        
        trackSololight = surface.createOnOffHardwareLight("TRACK_SOLO_LIGHT_" + index);
        trackSololight.isOn().onUpdateHardware(isOn -> handleLightChange(isOn, channel, baseCc + 4));
        
        trackArmButton = surface.createHardwareButton("TRACK_ARM_" + index);
        setCcMatcher(trackArmButton, midiIn, 5);
        
        trackArmlight = surface.createOnOffHardwareLight("TRACK_ARM_LIGHT_" + index);
        trackArmlight.isOn().onUpdateHardware(isOn -> handleLightChange(isOn, channel, baseCc + 5));
        
        final int shiftBlock = (index / 8) * 3 + 0x30;
        
        // shift buttons => 0x30, ergo + 0x20
        shift1Button = surface.createHardwareButton("SHIFT1_" + index);
        setCcMatcherShift(shift1Button, midiIn, shiftBlock);
        shift1Buttonlight = surface.createOnOffHardwareLight("SHIFT1__LIGHT_" + index);
        shift1Buttonlight.isOn().onUpdateHardware(isOn -> handleLightChange(isOn, channel, shiftBlock));
        
        shift2Button = surface.createHardwareButton("TRACK_STOP_" + index);
        setCcMatcherShift(shift2Button, midiIn, shiftBlock + 1);
        shift2ButtonLight = surface.createOnOffHardwareLight("TRACK_STOP_LIGHT_" + index);
        shift2ButtonLight.isOn().onUpdateHardware(isOn -> handleLightChange(isOn, channel, shiftBlock + 1));
        
        shift3Button = surface.createHardwareButton("SHIFT3_" + index);
        setCcMatcherShift(shift3Button, midiIn, shiftBlock + 2);
        shift3Buttonlight = surface.createOnOffHardwareLight("SHIFT3_LIGHT_" + index);
        shift3Buttonlight.isOn().onUpdateHardware(isOn -> handleLightChange(isOn, channel, shiftBlock + 2));
    }
    
    private void setCcMatcher(final HardwareButton button, final MidiIn midiIn, final int offset) {
        button.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, baseCc + offset, 127));
        button.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, baseCc + offset, 0));
    }
    
    private void setCcMatcherShift(final HardwareButton button, final MidiIn midiIn, final int ccValue) {
        button.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, ccValue, 127));
        button.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, ccValue, 0));
    }
    
    private void handleLightChange(final boolean isOn, final int channel, final int ccNr) {
        midiProcessor.sendMidi(Midi.CC | channel, ccNr, isOn ? 0x7F : 0x00);
    }
    
    private void sendLightUpdate(final int offset, final boolean isOn) {
        midiProcessor.sendMidi(Midi.CC | channel, baseCc + offset, isOn ? 0x7F : 0x00);
    }
    
    public void bind(final Layer layer, final Track track, final BitwigControl overview,
        final ScreenManager screenManager) {
        track.solo().markInterested();
        track.arm().markInterested();
        track.isStopped().markInterested();
        track.isGroup().markInterested();
        track.isGroupExpanded().markInterested();
        track.addIsSelectedInMixerObserver(selected -> {
            if (selected) {
                overview.setSelectTrackIndex(index);
            }
        });
        layer.bind(slider, track.volume());
        layer.addBinding(new ScreenParameterBinding(slider, track.volume(), track.name(), ScreenMode.MIXER_PARAMETER,
            screenManager));
        layer.bind(panKnob, track.pan());
        layer.addBinding(
            new ScreenParameterBinding(panKnob, track.pan(), track.name(), ScreenMode.MIXER_PARAMETER, screenManager));
        bindEncoderPress(layer, track.pan());
        final Send send1 = track.sendBank().getItemAt(0);
        final Send send2 = track.sendBank().getItemAt(1);
        layer.bind(send1Knob, send1);
        layer.addBinding(
            new ScreenParameterBinding(send1Knob, send1, track.name(), ScreenMode.MIXER_PARAMETER, screenManager));
        layer.bind(send2Knob, send2);
        layer.addBinding(
            new ScreenParameterBinding(send2Knob, send2, track.name(), ScreenMode.MIXER_PARAMETER, screenManager));
        
        layer.bind(() -> overview.getSelectTrackIndex() == index, trackSelectlight);
        layer.bind(trackSelectButton, trackSelectButton.pressedAction(), () -> {
            overview.setSelectTrackIndex(index);
            track.selectInMixer();
        });
        
        bindToggleParam(layer, trackSoloButton, track.solo());
        layer.bind(() -> track.solo().get(), trackSololight);
        bindToggleParam(layer, shift2Button, track.mute());
        layer.bind(() -> track.mute().get(), shift2ButtonLight);
        
        bindToggleParam(layer, trackArmButton, track.arm());
        layer.bind(() -> track.arm().get(), trackArmlight);
        
        layer.bind(shift1Button, shift1Button.pressedAction(), () -> {
            if (track.isGroup().get()) {
                track.isGroupExpanded().toggle();
            }
        });
        layer.bind(() -> trackGroupState(track), shift1Buttonlight);
        
        // STOP Button does not react to
        layer.bind(shift2Button, shift3Button.pressedAction(), () -> track.stop());
        layer.bind(shift2Button, shift3Button.releasedAction(), () -> track.stop());
        layer.bind(() -> !track.isStopped().get(), shift3Buttonlight);
    }
    
    private void bindEncoderPress(final Layer layer, final Parameter parameter) {
        layer.bindPressed(panButton, () -> {
            if (globalStates.getShiftState().get()) {
                parameter.reset();
            }
            encoderButtonHeld = true;
            panKnob.setSensitivity(4.0);
        });
        layer.bindReleased(panButton, () -> {
            panKnob.setSensitivity(1.0);
            encoderButtonHeld = false;
        });
    }
    
    private boolean trackGroupState(final Track track) {
        if (track.isGroup().get()) {
            if (track.isGroupExpanded().get()) {
                return midiProcessor.blinkSlow();
            }
            return true;
        }
        return false;
    }
    
    private void bindToggleParam(final Layer mainLayer, final HardwareButton button, final SettableBooleanValue value) {
        mainLayer.bind(button, button.pressedAction(), () -> value.toggle());
    }
    
    public void bindEncoder(final Layer layer, final RemoteControl remote, final StringValue targetName,
        final ScreenManager screenManager) {
        layer.bind(panKnob, remote);
        layer.addBinding(
            new ScreenParameterBinding(panKnob, remote, targetName, ScreenMode.DEVICE_PARAMETER, screenManager));
        
        bindEncoderPress(layer, remote);
    }
}

