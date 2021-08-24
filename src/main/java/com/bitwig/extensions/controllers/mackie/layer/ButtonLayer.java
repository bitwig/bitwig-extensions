package com.bitwig.extensions.controllers.mackie.layer;

import com.bitwig.extensions.controllers.mackie.NoteHandler;
import com.bitwig.extensions.controllers.mackie.NoteOnAssignment;
import com.bitwig.extensions.controllers.mackie.section.MixControl;
import com.bitwig.extensions.framework.Layer;

public class ButtonLayer extends Layer {

	private NoteHandler noteHandler;
	private final int baseOffset;
	private final int sectionOffset;

	public ButtonLayer(final String name, final MixControl mixControl, final NoteOnAssignment base,
			final int layerOffset) {
		super(mixControl.getDriver().getLayers(),
				name + "_" + mixControl.getHwControls().getSectionIndex() + "_BUTTON");
		this.baseOffset = base.getNoteNo();
		this.sectionOffset = mixControl.getHwControls().getSectionIndex() * 8;
	}

	public void setNoteHandler(final NoteHandler noteHandler) {
		this.noteHandler = noteHandler;
	}

	@Override
	protected void onActivate() {
		super.onActivate();
		if (noteHandler != null) {
			noteHandler.activate(baseOffset, sectionOffset);
		}
	}

	@Override
	protected void onDeactivate() {
		super.onDeactivate();
		if (noteHandler != null) {
			noteHandler.deactivate();
		}
	}

}
