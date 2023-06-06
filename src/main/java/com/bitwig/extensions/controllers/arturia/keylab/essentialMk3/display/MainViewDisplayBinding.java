package com.bitwig.extensions.controllers.arturia.keylab.essentialMk3.display;

import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.framework.Binding;

public class MainViewDisplayBinding extends Binding<StringValue[], ContextPageConfiguration> {

   private final LcdDisplay display;

   public MainViewDisplayBinding(ContextPageConfiguration target, LcdDisplay display, StringValue... source) {
      super(target, source, target);
      if (source.length < 2) {
         throw new IllegalArgumentException("At least 2 sources needed");
      }
      this.display = display;
      source[0].addValueObserver(this::handleValueChanged1);
      source[1].addValueObserver(this::handleValueChanged2);
   }

   private void handleValueChanged1(String value) {
      if (!isActive()) {
         return;
      }
      getTarget().setMainText(value);
      display.sendNavigationPage(getTarget(), false);
   }

   private void handleValueChanged2(String value) {
      if (!isActive()) {
         return;
      }
      getTarget().setSecondaryText(value);
      display.sendNavigationPage(getTarget(), false);
   }

   @Override
   protected void deactivate() {
   }

   @Override
   protected void activate() {
      getTarget().setMainText(getSource()[0].get());
      getTarget().setSecondaryText(getSource()[1].get());
      display.sendNavigationPage(getTarget(), false);
   }
}
