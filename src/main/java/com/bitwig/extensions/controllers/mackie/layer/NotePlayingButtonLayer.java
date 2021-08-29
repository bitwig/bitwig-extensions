package com.bitwig.extensions.controllers.mackie.layer;

import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;
import com.bitwig.extensions.controllers.mackie.NoteOnAssignment;
import com.bitwig.extensions.controllers.mackie.section.MixControl;

public class NotePlayingButtonLayer extends ButtonLayer {

	private final MackieMcuProExtension driver;

	public NotePlayingButtonLayer(final MixControl mixControl, final int layerOffset) {
		super("NOTEPLAYER", mixControl, NoteOnAssignment.REC_BASE, layerOffset);
		driver = mixControl.getDriver();
	}

}
