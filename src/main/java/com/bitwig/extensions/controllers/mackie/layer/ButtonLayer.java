package com.bitwig.extensions.controllers.mackie.layer;

import com.bitwig.extensions.controllers.mackie.NoteHandler;
import com.bitwig.extensions.controllers.mackie.NoteOnAssignment;
import com.bitwig.extensions.framework.Layer;

public class ButtonLayer extends Layer {

	private NoteHandler noteHandler;
	private final int baseOffset;

	public ButtonLayer(final String name, final MixControl mixControl, final NoteOnAssignment base) {
		super(mixControl.getDriver().getLayers(),
				name + "_" + mixControl.getHwControls().getSectionIndex() + "_BUTTON");
		this.baseOffset = base.getNoteNo();
	}

	public void setNoteHandler(final NoteHandler noteHandler) {
		this.noteHandler = noteHandler;
	}

	@Override
	protected void onActivate() {
		super.onActivate();
		if (noteHandler != null) {
			noteHandler.activate(baseOffset);
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
