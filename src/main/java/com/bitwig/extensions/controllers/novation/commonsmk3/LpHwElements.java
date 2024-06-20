package com.bitwig.extensions.controllers.novation.commonsmk3;

import java.util.List;

import com.bitwig.extensions.controllers.novation.commonsmk3.DrumButton;
import com.bitwig.extensions.controllers.novation.commonsmk3.GridButton;
import com.bitwig.extensions.controllers.novation.commonsmk3.LabeledButton;

public interface LpHwElements {
    GridButton getGridButton(int row, int col);
    
    List<DrumButton> getDrumGridButtons();
    
    List<LabeledButton> getSceneLaunchButtons();
}
