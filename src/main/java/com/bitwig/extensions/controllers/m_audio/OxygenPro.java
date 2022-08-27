package com.bitwig.extensions.controllers.m_audio;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.*;

public class OxygenPro extends ControllerExtension {

    protected OxygenPro(ControllerExtensionDefinition definition, ControllerHost host, String model) {
        super(definition, host);
        modelName = model;
    }

    @Override
    public void init() {
        mHost = getHost();
        mApplication = mHost.createApplication();

        mMidiIn1 = mHost.getMidiInPort(0);
        mMidiIn2 = mHost.getMidiInPort(1);
        mMidiOut1 = mHost.getMidiOutPort(0);
        mMidiOut2 = mHost.getMidiOutPort(1);

        initSysexMessages();

        mWorkflow = new Workflow(this, modelName);

        mHardwareSurface = mWorkflow.getHardwareSurface();
        //initHardwareLayout();

        mHost.showPopupNotification("Oxygen Pro " + modelName + " initialized...");
    }

    private void initSysexMessages() {
        mMidiOut2.sendSysex("F0 7E 7F 06 01 F7");
        mMidiOut2.sendSysex("F0 00 01 05 7F 00 00 6D 00 01 02 F7"); // Changing to the Bitwig DAW-Program
        mMidiOut2.sendSysex("F0 00 01 05 7F 00 00 6E 00 01 02 F7");
        mMidiOut2.sendSysex("F0 00 01 05 7F 00 00 6E 00 01 07 F7");
        mMidiOut2.sendSysex("F0 00 01 05 7F 00 00 6B 00 01 01 F7");
        mMidiOut2.sendSysex("F0 00 01 05 7F 00 00 6C 00 01 03 F7"); // activate Light Controll
    }

    private void initHardwareLayout() {
        HardwareSurface surface = mHardwareSurface;
        surface.setPhysicalSize(300, 300);
        surface.hardwareElementWithId("stop").setBounds(259.0, 39.25, 10.0, 10.0);
        surface.hardwareElementWithId("play").setBounds(271.0, 39.25, 10.0, 10.0);
        surface.hardwareElementWithId("record").setBounds(283.0, 39.25, 10.0, 10.0);
        surface.hardwareElementWithId("rewind").setBounds(259.25, 25.75, 10.0, 10.0);
        surface.hardwareElementWithId("forward").setBounds(271.25, 25.75, 10.0, 10.0);
        surface.hardwareElementWithId("loop").setBounds(283.25, 25.75, 10.0, 10.0);
        surface.hardwareElementWithId("prevBank").setBounds(259.5, 13.25, 16.0, 10.0);
        surface.hardwareElementWithId("nextBank").setBounds(277.5, 13.25, 16.0, 10.0);
        surface.hardwareElementWithId("volumeLayer").setBounds(313.5, 54.75, 10.0, 10.0);
        surface.hardwareElementWithId("panLayer").setBounds(325.5, 54.75, 10.0, 10.0);
        surface.hardwareElementWithId("deviceLayer").setBounds(337.5, 54.75, 10.0, 10.0);
        surface.hardwareElementWithId("sendsLayer").setBounds(349.5, 54.75, 10.0, 10.0);
        surface.hardwareElementWithId("save").setBounds(362.25, 54.75, 10.0, 10.0);
        surface.hardwareElementWithId("quantize").setBounds(374.25, 54.75, 10.0, 10.0);
        surface.hardwareElementWithId("view").setBounds(386.25, 54.75, 10.0, 10.0);
        surface.hardwareElementWithId("undo").setBounds(398.25, 54.75, 10.0, 10.0);
        surface.hardwareElementWithId("metronome").setBounds(236.0, 13.25, 14.5, 10.0);
        surface.hardwareElementWithId("knob0").setBounds(313.0, 10.25, 10.0, 10.0);
        surface.hardwareElementWithId("knob1").setBounds(325.0, 10.25, 10.0, 10.0);
        surface.hardwareElementWithId("knob2").setBounds(337.0, 10.25, 10.0, 10.0);
        surface.hardwareElementWithId("knob3").setBounds(349.0, 10.25, 10.0, 10.0);
        surface.hardwareElementWithId("knob4").setBounds(361.0, 10.25, 10.0, 10.0);
        surface.hardwareElementWithId("knob5").setBounds(373.0, 10.25, 10.0, 10.0);
        surface.hardwareElementWithId("knob6").setBounds(385.0, 10.25, 10.0, 10.0);
        surface.hardwareElementWithId("knob7").setBounds(397.0, 10.25, 10.0, 10.0);
        surface.hardwareElementWithId("padButton0").setBounds(313.5, 38.75, 10.0, 10.0);
        surface.hardwareElementWithId("padButton1").setBounds(325.5, 38.75, 10.0, 10.0);
        surface.hardwareElementWithId("padButton2").setBounds(337.5, 38.75, 10.0, 10.0);
        surface.hardwareElementWithId("padButton3").setBounds(349.5, 38.75, 10.0, 10.0);
        surface.hardwareElementWithId("padButton4").setBounds(313.5, 25.75, 10.0, 10.0);
        surface.hardwareElementWithId("padButton5").setBounds(325.5, 25.75, 10.0, 10.0);
        surface.hardwareElementWithId("padButton6").setBounds(337.5, 25.75, 10.0, 10.0);
        surface.hardwareElementWithId("padButton7").setBounds(349.5, 25.75, 10.0, 10.0);
        surface.hardwareElementWithId("padButton8").setBounds(362.0, 38.75, 10.0, 10.0);
        surface.hardwareElementWithId("padButton9").setBounds(374.0, 38.75, 10.0, 10.0);
        surface.hardwareElementWithId("padButton10").setBounds(386.0, 38.75, 10.0, 10.0);
        surface.hardwareElementWithId("padButton11").setBounds(398.0, 38.75, 10.0, 10.0);
        surface.hardwareElementWithId("padButton12").setBounds(362.0, 25.75, 10.0, 10.0);
        surface.hardwareElementWithId("padButton13").setBounds(374.0, 25.75, 10.0, 10.0);
        surface.hardwareElementWithId("padButton14").setBounds(386.0, 25.75, 10.0, 10.0);
        surface.hardwareElementWithId("padButton15").setBounds(398.25, 25.75, 10.0, 10.0);
        surface.hardwareElementWithId("sceneLaunch1").setBounds(414.75, 25.5, 35.5, 10.0);
        surface.hardwareElementWithId("sceneLaunch2").setBounds(414.75, 38.5, 35.5, 10.0);
        surface.hardwareElementWithId("fader").setBounds(176.25, 24.25, 10.0, 10.0);
        surface.hardwareElementWithId("backShift").setBounds(196.75, 39.25, 21.5, 10.0);
        surface.hardwareElementWithId("encoderKnob").setBounds(221.75, 39.25, 29.5, 10.0);
        surface.hardwareElementWithId("encoder").setBounds(224.25, 51.5, 24.5, 10.0);

    }

    @Override
    public void exit() {
        mMidiOut2.sendSysex("F0 00 01 05 7F 00 00 6C 00 01 00 F7");

        getHost().showPopupNotification("M-Audio Oxygen Pro "+ modelName +" Exited");
    }

    @Override
    public void flush() {
        if (mHardwareSurface != null)
            mHardwareSurface.updateHardware();

    }

    private ControllerHost mHost;
    private HardwareSurface mHardwareSurface;
    private Application mApplication;

    private MidiIn mMidiIn1, mMidiIn2;
    private MidiOut mMidiOut1, mMidiOut2;

    private String modelName;
    private Workflow mWorkflow;

}
