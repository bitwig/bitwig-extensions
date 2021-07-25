package com.bitwig.extensions.controllers.mackie.layer;

import com.bitwig.extensions.controllers.mackie.display.DisplayLayer;
import com.bitwig.extensions.framework.Layer;

public interface LayerStateHandler {
	Layer getButtonLayer();

	LayerConfiguration getCurrentConfig();

	DisplayLayer getActiveDisplayLayer();
}
