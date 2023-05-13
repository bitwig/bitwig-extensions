package com.bitwig.extensions.controllers.novation.launchkey_mk3.control;

import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.LcdDisplay;
import com.bitwig.extensions.framework.Binding;


public class LcdDeviceParameterBinding extends Binding<Parameter, LcdDisplay> {
   private final int paramIndex;

   public LcdDeviceParameterBinding(final CursorRemoteControlsPage page, final Parameter source,
                                    final LcdDisplay target, final int paramIndex) {
      super(source, source, target);
      source.displayedValue().addValueObserver(this::valueChange);
      source.name().addValueObserver(this::nameChanged);
      this.paramIndex = paramIndex;

   }

   private void nameChanged(final String name) {
      if (isActive()) {
         getTarget().setParameter(name, paramIndex);
      }
   }

   private void valueChange(final String value) {
      if (isActive()) {
         getTarget().setParameter(getSource().name().get(), paramIndex);
         getTarget().setValue(value, paramIndex);
      }
   }

   @Override
   protected void deactivate() {
   }

   @Override
   protected void activate() {
      getTarget().setParameter(getSource().name().get(), paramIndex);
      getTarget().setValue(getSource().displayedValue().get(), paramIndex);
   }

   public void update() {
      if (isActive()) {
         getTarget().setParameter(getSource().name().get(), paramIndex);
         getTarget().setValue(getSource().displayedValue().get(), paramIndex);
      }
   }

}
