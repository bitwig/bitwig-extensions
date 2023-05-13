package com.bitwig.extensions.controllers.novation.launchpadpromk3.sliderlayers;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.novation.commonsmk3.LabeledButton;
import com.bitwig.extensions.controllers.novation.commonsmk3.MidiProcessor;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.commonsmk3.SliderBinding;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.HwElements;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.LpBaseMode;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.SysExHandler;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.ViewCursorControl;
import com.bitwig.extensions.controllers.novation.launchpadpromk3.layers.ControlMode;
import com.bitwig.extensions.framework.Layers;

public class SendsSliderLayer extends TrackSliderLayer {

    private TrackBank trackBank;
    private int sendItems = 0;
    private int selectedSendItem = 0;

    public SendsSliderLayer(final ViewCursorControl viewCursorControl, final HardwareSurface controlSurface,
                            final Layers layers, final MidiProcessor midiProcessor, final SysExHandler sysExHandler,
                            final HwElements hwElements) {
        super("SENDS", ControlMode.SENDS, controlSurface, layers, midiProcessor, sysExHandler);
        bind(viewCursorControl.getTrackBank());
        initSceneButtons(hwElements);
    }

    private void initSceneButtons(final HwElements hwElements) {
        for (int i = 0; i < 8; i++) {
            final int index = i;
            final LabeledButton sceneButton = hwElements.getSceneLaunchButtons().get(index);
            sceneButton.bindPressed(this, pressed -> handleSendSelect(pressed, index));
            sceneButton.bindLight(this, () -> sendsExistState(index));
        }
    }

    private void handleSendSelect(final boolean pressed, final int index) {
        if (pressed) {
            if (index < sendItems) {
                for (int i = 0; i < 8; i++) {
                    final Track track = trackBank.getItemAt(i);
                    track.sendBank().scrollPosition().set(index);
                }
            }
        }
    }

    private RgbState sendsExistState(final int index) {
        if (index < sendItems) {
            if (index == selectedSendItem) {
                return RgbState.WHITE;
            }
            return RgbState.DIM_WHITE;
        }
        return RgbState.OFF;
    }

    @Override
    protected void bind(final TrackBank trackBank) {
        this.trackBank = trackBank;

        for (int i = 0; i < 8; i++) {
            final Track track = trackBank.getItemAt(i);
            if (i == 0) {
                track.sendBank().itemCount().addValueObserver(items -> sendItems = items);
                track.sendBank().scrollPosition().addValueObserver(scrollPos -> selectedSendItem = scrollPos);
            }

           final SliderBinding binding = new SliderBinding(ControlMode.SENDS.getCcNr(), track.sendBank().getItemAt(0),
                    sliders[i], i, midiProcessor);
            addBinding(binding);
            valueBindings.add(binding);
        }
    }

    @Override
    protected void onActivate() {
        super.onActivate();
        refreshTrackColors();
        modeHandler.setFaderBank(0, mode, tracksExistsColors);
        modeHandler.changeMode(LpBaseMode.FADER, mode.getBankId());
    }
}
