package com.bitwig.extensions.controllers.esi;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.NoteInput;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class Xjam extends ControllerExtension {

    protected Xjam(ControllerExtensionDefinition definition, ControllerHost host) {
        super(definition, host);
    }

    @Override
    public void init() {
        mHost = getHost();
        mHardwareSurface = mHost.createHardwareSurface();

        mMidiIn = mHost.getMidiInPort(0);

        mNoteInput = mMidiIn.createNoteInput("pad", "9?????", "D?????", "A0????");
        mNoteInput.setShouldConsumeEvents(false);
        
        mCursorTrack = mHost.createCursorTrack(0, 0);
        mCursorDevice = mCursorTrack.createCursorDevice();
        mCursorRemoteControls = mCursorDevice.createCursorRemoteControlsPage(6);

        for (int i = 0; i < 18; i++) {
            int index = i + 10;
            int channel = 0;
            if (i == 0)
                index = 3;
            if (i == 1)
                index = 9;
            if (i > 5)
                channel = 1;
            if (i > 11)
                channel = 2;

            final AbsoluteHardwareKnob knob = mHardwareSurface.createAbsoluteHardwareKnob("enc" + i);
            knob.setLabel(String.valueOf(i + 1));
            knob.setIndexInGroup(i);
            knob.setAdjustValueMatcher(mMidiIn.createAbsoluteCCValueMatcher(channel, index));

            mAbsoluteKnobs[i] = knob;
        }
        initLayers();

        mHost.showPopupNotification("Xjam initialized...");
    }

    public void initLayers() {
        mMainLayer = new Layer(mLayers, "Main");
        initMainLayer();
    }

    private void initMainLayer() {
        for (int i = 0; i < 6; i++) {
            mCursorRemoteControls.getParameter(i).setIndication(true);
            mMainLayer.bind(mAbsoluteKnobs[i], mCursorRemoteControls.getParameter(i));
        }
        mMainLayer.activate();
    }

    @Override
    public void exit() {
    }

    @Override
    public void flush() {
        mHardwareSurface.updateHardware();
    }

    // API Elements
    private HardwareSurface mHardwareSurface;
    private ControllerHost mHost;

    private MidiIn mMidiIn;

    private NoteInput mNoteInput;

    private CursorTrack mCursorTrack;

    private CursorDevice mCursorDevice;

    private CursorRemoteControlsPage mCursorRemoteControls;



    // Hardware elements
    private AbsoluteHardwareKnob[] mAbsoluteKnobs = new AbsoluteHardwareKnob[18];

    private final Layers mLayers = new Layers(this);
    private Layer mMainLayer;

}
