package com.bitwig.extensions.controllers.mackie.layer;

import com.bitwig.extensions.controllers.mackie.NoteAssignment;
import com.bitwig.extensions.controllers.mackie.section.DrumNoteHandler;
import com.bitwig.extensions.controllers.mackie.section.MixControl;
import com.bitwig.extensions.framework.Layer;

public class ButtonLayer extends Layer {

   private DrumNoteHandler noteHandler;
   private final int baseOffset;
   private final int sectionOffset;

   public ButtonLayer(final String name, final MixControl mixControl, final NoteAssignment base) {
      super(mixControl.getDriver().getLayers(), name + "_" + mixControl.getHwControls().getSectionIndex() + "_BUTTON");
      baseOffset = base.getNoteNo();
      sectionOffset = mixControl.getHwControls().getSectionIndex() * 8;
   }

   public void setNoteHandler(final DrumNoteHandler noteHandler) {
      this.noteHandler = noteHandler;
   }

   public void activateNotePlaying() {
      if (noteHandler != null) {
         noteHandler.activate(baseOffset, sectionOffset);
      }
   }

   public void deactivateNotePlaying() {
      if (noteHandler != null) {
         noteHandler.deactivate();
      }
   }

   @Override
   protected void onActivate() {
      super.onActivate();
      activateNotePlaying();
   }

   @Override
   protected void onDeactivate() {
      super.onDeactivate();
      if (noteHandler != null) {
         noteHandler.deactivate();
      }
      deactivateNotePlaying();
   }

}
