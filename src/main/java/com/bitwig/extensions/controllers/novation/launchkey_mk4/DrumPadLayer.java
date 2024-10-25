package com.bitwig.extensions.controllers.novation.launchkey_mk4;

import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.launchkey_mk4.control.RgbButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;
import com.bitwig.extensions.framework.di.Component;

@Component
public class DrumPadLayer extends Layer {
    
    private final RgbButton[] drumButtons;
    
    public DrumPadLayer(final Layers layers, final LaunchkeyHwElements hwElements, final MidiProcessor midiProcessor,
        final ViewControl viewControl) {
        super(layers, "DRUM_LAYER");
        drumButtons = hwElements.getDrumButtons();
        for (int i = 0; i < 16; i++) {
            final int index = i;
            final RgbButton button = drumButtons[i];
            button.bindLight(this, () -> RgbState.RED_LO);
            button.bindIsPressed(this, pressed -> {
                LaunchkeyMk4Extension.println(" WTF %d", index);
            });
        }
        midiProcessor.addModeListener(this::handleModeChange);
    }
    
    private void handleModeChange(final ModeType modeType, final int id) {
        if (modeType == ModeType.PAD && id == 15) {
            LaunchkeyMk4Extension.println("drum PAD MODE = %d", id);
        }
    }
    
    @Activate
    public void init() {
        this.setIsActive(true);
    }
    
}
