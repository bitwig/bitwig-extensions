package com.bitwig.extensions.controllers.novation.launchpad_mk3;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiOut;

import com.bitwig.extension.controller.api.RelativePosition;


public class LaunchpadMk3 extends ControllerExtension {

    public LaunchpadMk3(ControllerExtensionDefinition definition, ControllerHost host, String model) {
        super(definition, host);
        modelName = model;
    }

    @Override
    public void init() {
        mHost = getHost();
        mApplication = mHost.createApplication();
        mWorkflow = new Workflow(this, modelName);
        mHardwareSurface = mWorkflow.getHardwareSurface();
        mHardwareSurface.setPhysicalSize(300, 300);
        mMidiOut = mWorkflow.getMidiOut();
        initHardwareLayout();

        mHost.showPopupNotification("Launchpad " + modelName + " Mk3 initialized...");
    }

    private void initHardwareLayout() {
        HardwareSurface s = mHardwareSurface;
        s.setPhysicalSize(93, 93 * 2);

        // Top Row
        s.hardwareElementWithId("up").setBounds(1, 1, 10, 10);
        s.hardwareElementWithId("down").setBounds(10 + 1, 1, 10, 10);
        s.hardwareElementWithId("left").setBounds(20 + 1, 1, 10, 10);
        s.hardwareElementWithId("right").setBounds(30 + 1, 1, 10, 10);
        s.hardwareElementWithId("session").setBounds(40 + 1, 1, 10, 10);
        s.hardwareElementWithId("note").setBounds(50 + 1, 1, 10, 10);
        s.hardwareElementWithId("custom").setBounds(60 + 1, 1, 10, 10);
        s.hardwareElementWithId("record").setBounds(70 + 1, 1, 10, 10);

        for (int i = 0; i < 8; i++) {
            // Scenes
            s.hardwareElementWithId("scene" + (7 - i)).setBounds(82, 12 + 10 * (7 - i), 10, 10);
            // Fader
            s.hardwareElementWithId("fader" + i).setBounds(1 + i * 10, 93, 10, 20);
            s.hardwareElementWithId("fader" + i).setLabel("Volume");
            s.hardwareElementWithId("fader" + i).setLabelColor(Color.whiteColor());
            s.hardwareElementWithId("fader" + i).setLabelPosition(RelativePosition.INSIDE);
            s.hardwareElementWithId("fader" + (i + 8)).setBounds(1 + i * 10, 113, 10, 20);
            s.hardwareElementWithId("fader" + (i + 8)).setLabel("Pan");
            s.hardwareElementWithId("fader" + (i + 16)).setBounds(1 + i * 10, 133, 10, 20);
            s.hardwareElementWithId("fader" + (i + 16)).setLabel("SendA");
            s.hardwareElementWithId("fader" + (i + 24)).setBounds(1 + i * 10, 153, 10, 20);
            s.hardwareElementWithId("fader" + (i + 24)).setLabel("SendB");
            for (int j = 0; j < 8; j++) {
                // Pad Matrix
                s.hardwareElementWithId("" + (i * 10 + (7 - j))).setBounds(10 * i + 1, 10 * (7 - j) + 12, 10.0, 10.0);
            }
        }
    }

    @Override
    public void exit() {
        mMidiOut.sendSysex(STANDALONE_MODE);
        mHost.showPopupNotification("Launchpad " + modelName + " Mk3 exited...");
    }

    @Override
    public void flush() {
        mHardwareSurface.updateHardware();
        mWorkflow.midiCallback("flush");
    }

    private String modelName;

    private Workflow mWorkflow;
    private final String STANDALONE_MODE = "F0 00 20 29 02 0D 10 00 F7";


    // API Objects
    private HardwareSurface mHardwareSurface;
    private ControllerHost mHost;
    private MidiOut mMidiOut;
    private Application mApplication;
}
