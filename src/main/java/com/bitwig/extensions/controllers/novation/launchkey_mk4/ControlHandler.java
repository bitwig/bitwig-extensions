package com.bitwig.extensions.controllers.novation.launchkey_mk4;

import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.AbsoluteEncoderBinding;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.RelAbsEncoder;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.display.DisplayControl;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;

@Component
public class ControlHandler {

    private final Layer pluginControlLayer;
    private final Layer panLayer;
    private final Layer currentLayer;

    private final EncMode mode = EncMode.DEVICE;

    private enum EncMode {
        DEVICE, PAN, VOLUME, SENDS
    }

    public ControlHandler(final Layers layers,
        final LaunchkeyHwElements hwElements,
        final ViewControl viewControl,
        final MidiProcessor midiProcessor,
        final DisplayControl displayControl) {
        pluginControlLayer = new Layer(layers, "CTL_PLUGIN");
        panLayer = new Layer(layers, "CTL_PLUGIN");
        final CursorRemoteControlsPage remotes = viewControl.getPrimaryRemotes();
        final RelAbsEncoder[] valueEncoders = hwElements.getValueEncoders();
        final CursorTrack cursorTrack = viewControl.getCursorTrack();
        final PinnableCursorDevice cursorDevice = viewControl.getCursorDevice();

        cursorTrack.name().addValueObserver(name -> displayControl.fixDisplayUpdate(0, name));
        cursorDevice.name().addValueObserver(name -> displayControl.fixDisplayUpdate(1, name));

        for (int i = 0; i < 8; i++) {
            final RelAbsEncoder encoder = valueEncoders[i];
            final RemoteControl remote = remotes.getParameter(i);
            pluginControlLayer.addBinding(new AbsoluteEncoderBinding(i,
                remote,
                encoder,
                displayControl,
                cursorTrack.name()
            ));
        }

        midiProcessor.addModeListener(this::handleModeChange);
        currentLayer = pluginControlLayer;
    }

    private void handleModeChange(final ModeType modeType, final int id) {
        if (modeType == ModeType.ENCODER) {
            final EncoderMode newMode = EncoderMode.toMode(id);
            LaunchkeyMk4Extension.println(" MODE Change => %s %s <= %d", modeType, newMode, id);
            handleEncModeChange(EncoderMode.toMode(id));
        }
    }

    private void handleEncModeChange(final EncoderMode toMode) {
        if (mode == EncMode.DEVICE) {

        } else if (mode == EncMode.VOLUME) {

        } else if (mode == EncMode.PAN) {

        }
    }

    private void switchMode(final EncMode mode) {
        currentLayer.setIsActive(false);
    }

    @Activate
    public void activate() {
        pluginControlLayer.setIsActive(true);
    }
}
