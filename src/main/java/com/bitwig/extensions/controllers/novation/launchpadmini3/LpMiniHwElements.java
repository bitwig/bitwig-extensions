package com.bitwig.extensions.controllers.novation.launchpadmini3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extensions.controllers.novation.commonsmk3.DrumButton;
import com.bitwig.extensions.controllers.novation.commonsmk3.GridButton;
import com.bitwig.extensions.controllers.novation.commonsmk3.LabeledButton;
import com.bitwig.extensions.controllers.novation.commonsmk3.LpHwElements;
import com.bitwig.extensions.controllers.novation.commonsmk3.MidiProcessor;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.di.Inject;
import com.bitwig.extensions.framework.di.PostConstruct;

@Component
public class LpMiniHwElements implements LpHwElements {
    private final GridButton[][] gridButtons = new GridButton[8][8];
    private final Map<LabelCcAssignmentsMini, LabeledButton> labeledButtons = new HashMap<>();
    private final List<LabeledButton> sceneLaunchButtons = new ArrayList<>();
    private final List<DrumButton> drumGridButtons = new ArrayList<>();
    
    private MultiStateHardwareLight novationLight;
    
    private final int[] SCENE_CCS = {0x59, 0x4f, 0x45, 0x3B, 0x31, 0x27, 0x1D, 0x13};
    
    @Inject
    private MidiProcessor midiProcessor;
    
    @PostConstruct
    public void init(final HardwareSurface surface) {
        initGridButtons(surface, midiProcessor);
        for (final LabelCcAssignmentsMini labelAssignment : LabelCcAssignmentsMini.values()) {
            if (!labelAssignment.isIndexReference()) {
                final LabeledButton button = new LabeledButton(surface, midiProcessor, labelAssignment);
                labeledButtons.put(labelAssignment, button);
            }
        }
        for (int i = 0; i < 8; i++) {
            final LabeledButton sceneButton =
                new LabeledButton("SCENE_LAUNCH_" + (i + 1), surface, midiProcessor, SCENE_CCS[i]);
            sceneLaunchButtons.add(sceneButton);
        }
        novationLight = surface.createMultiStateHardwareLight("NovationLight");
        novationLight.state().onUpdateHardware(hwState -> midiProcessor.updatePadLed(hwState, 0x63));
    }
    
    public void refresh() {
        sceneLaunchButtons.forEach(LabeledButton::refresh);
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                gridButtons[row][col].refresh();
            }
        }
    }
    
    private void initGridButtons(final HardwareSurface surface, final MidiProcessor midiProcessor) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                gridButtons[row][col] = new GridButton(surface, midiProcessor, row, col);
            }
        }
        for (int i = 0; i < 64; i++) {
            final int noteValue = 36 + i;
            drumGridButtons.add(new DrumButton(surface, midiProcessor, 8, noteValue));
        }
    }
    
    public List<DrumButton> getDrumGridButtons() {
        return drumGridButtons;
    }
    
    public GridButton getGridButton(final int row, final int col) {
        return gridButtons[row][col];
    }
    
    public LabeledButton getLabeledButton(final LabelCcAssignmentsMini assignment) {
        return labeledButtons.get(assignment);
    }
    
    public List<LabeledButton> getSceneLaunchButtons() {
        return sceneLaunchButtons;
    }
    
    public MultiStateHardwareLight getNovationLight() {
        return novationLight;
    }
    
}
