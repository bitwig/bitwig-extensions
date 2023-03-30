package com.bitwig.extensions.controllers.novation.launchpadpromk3;

import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extensions.controllers.novation.commonsmk3.DrumButton;
import com.bitwig.extensions.controllers.novation.commonsmk3.GridButton;
import com.bitwig.extensions.controllers.novation.commonsmk3.LabeledButton;
import com.bitwig.extensions.controllers.novation.commonsmk3.MidiProcessor;
import com.bitwig.extensions.framework.di.PostConstruct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HwElements {
    private final GridButton[][] gridButtons = new GridButton[8][8];
    private final Map<LabelCcAssignments, LabeledButton> labeledButtons = new HashMap<>();
    private final List<LabeledButton> sceneLaunchButtons = new ArrayList<>();
    private final List<LabeledButton> trackSelectButtons = new ArrayList<>();
    private final List<DrumButton> drumGridButtons = new ArrayList<>();

    @PostConstruct
    public void init(final HardwareSurface surface, final MidiProcessor midiProcessor) {
        initGridButtons(surface, midiProcessor);
        for (final LabelCcAssignments labelAssignment : LabelCcAssignments.values()) {
            if (!labelAssignment.isIndexReference()) {
                final LabeledButton button = new LabeledButton(surface, midiProcessor, labelAssignment);
                labeledButtons.put(labelAssignment, button);
            }
        }
        for (int i = 0; i < 8; i++) {
            final LabeledButton sceneButton = new LabeledButton("SCENE_LAUNCH_" + (i + 1), surface, midiProcessor,
                    LabelCcAssignments.R8_PRINT_TO_CLIP.getCcValue() + (7 - i) * 10);
            sceneLaunchButtons.add(sceneButton);

            final LabeledButton trackButton = new LabeledButton("TRACK_" + (i + 1), surface, midiProcessor,
                    LabelCcAssignments.TRACK_SEL_1.getCcValue() + i);
            trackSelectButtons.add(trackButton);
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

    public GridButton getGridButton(final int row, final int col) {
        return gridButtons[row][col];
    }

    public LabeledButton getLabeledButton(final LabelCcAssignments assignment) {
        return labeledButtons.get(assignment);
    }

    public List<DrumButton> getDrumGridButtons() {
        return drumGridButtons;
    }

    public List<LabeledButton> getSceneLaunchButtons() {
        return sceneLaunchButtons;
    }

    public List<LabeledButton> getTrackSelectButtons() {
        return trackSelectButtons;
    }
}
