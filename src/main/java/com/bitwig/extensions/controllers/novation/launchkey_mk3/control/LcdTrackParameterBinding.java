package com.bitwig.extensions.controllers.novation.launchkey_mk3.control;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.LcdDisplay;
import com.bitwig.extensions.framework.Binding;


public class LcdTrackParameterBinding extends Binding<Parameter, LcdDisplay> {
   private final int paramIndex;
   private final String type;
   private final Track track;

   public LcdTrackParameterBinding(final String type, final Track track, final Parameter source,
                                   final LcdDisplay target, final int paramIndex) {
      super(source, source, target);
      source.displayedValue().addValueObserver(this::valueChange);
      track.name().addValueObserver(this::trackNameChanged);
      this.paramIndex = paramIndex;
      this.type = type;
      this.track = track;
   }

   private void trackNameChanged(final String name) {
      if (isActive()) {
         getTarget().setParameter(type + "-" + name, paramIndex);
      }
   }

   private void valueChange(final String value) {
      if (isActive()) {
         getTarget().setParameter(type + "-" + track.name().get(), paramIndex);
         getTarget().setValue(value, paramIndex);
      }
   }

   @Override
   protected void deactivate() {
   }

   @Override
   protected void activate() {
      getTarget().setParameter(type + "-" + track.name().get(), paramIndex);
      getTarget().setValue(getSource().displayedValue().get(), paramIndex);
   }

   public void update() {
      if (isActive()) {
         getTarget().setParameter(type + "-" + track.name().get(), paramIndex);
         getTarget().setValue(getSource().displayedValue().get(), paramIndex);
      }
   }

}
