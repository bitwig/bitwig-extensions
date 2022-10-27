package com.bitwig.extensions.controllers.mackie.section;

import com.bitwig.extensions.controllers.mackie.configurations.LayerConfiguration;
import com.bitwig.extensions.controllers.mackie.display.DisplayLayer;
import com.bitwig.extensions.framework.Layer;

public interface LayerStateHandler {
   Layer getButtonLayer();

   LayerConfiguration getCurrentConfig();

   DisplayLayer getActiveDisplayLayer();

   DisplayLayer getBottomDisplayLayer();

   boolean hasBottomDisplay();
}
