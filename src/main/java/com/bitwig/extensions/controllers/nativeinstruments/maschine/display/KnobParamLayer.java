package com.bitwig.extensions.controllers.nativeinstruments.maschine.display;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.Scrollable;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineExtension;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineLayer;
import com.bitwig.extensions.framework.values.BooleanValueObject;

public class KnobParamLayer extends MaschineLayer {
   private final BooleanValueObject active = new BooleanValueObject();
   private final String[] values = new String[8];
   private final Scrollable[] scrollabe = new Scrollable[8];

   boolean hasScrollable = false;
   private final String title;
   private final NameContainer nameContainer;

   // private String lastValue = "";

   public KnobParamLayer(final MaschineExtension driver, final String name, final String title,
                         final NameContainer nameContainer) {
      super(driver, name);
      for (int i = 0; i < values.length; i++) {
         values[i] = "";
      }
      this.title = title;
      this.nameContainer = nameContainer;
   }

   public String getTitle() {
      return title;
   }

   public void updateDisplayValues(final String value, final int index) {
      values[index] = DisplayUtil.padString(value, 6);
      refreshValue(index / 4);
   }

   public void bindDiplayValue(final int index, final SettableRangedValue param) {
      param.displayedValue().addValueObserver(v -> updateDisplayValues(v, index));
      values[index] = DisplayUtil.padString(param.displayedValue().get(), 6);
   }

   public void bindScrollable(final int index, final Scrollable scrollable) {
      this.scrollabe[index] = scrollable;
      hasScrollable = true;
      scrollable.canScrollBackwards().markInterested();
      scrollable.canScrollForwards().markInterested();
   }

   public boolean canScrollForwards() {
      if (!hasScrollable) {
         return false;
      }
      for (int i = 0; i < this.scrollabe.length; i++) {
         if (scrollabe[i].canScrollForwards().get()) {
            return true;
         }
      }
      return false;
   }

   public boolean canScrollBackwards() {
      if (!hasScrollable) {
         return false;
      }
      for (int i = 0; i < this.scrollabe.length; i++) {
         if (scrollabe[i].canScrollBackwards().get()) {
            return true;
         }
      }
      return false;
   }

   public void scrollForwards() {
      if (hasScrollable) {
         for (int i = 0; i < this.scrollabe.length; i++) {
            scrollabe[i].scrollForwards();
         }
      }
   }

   public void scrollBackwards() {
      if (hasScrollable) {
         for (int i = 0; i < this.scrollabe.length; i++) {
            scrollabe[i].scrollBackwards();
         }
      }
   }

   public void refreshValue(final int section) {
      if (!isActive()) {
         return;
      }
      if (section == 0) {
         final StringBuilder b = new StringBuilder();
         for (int i = 0; i < 4; i++) {
            b.append(getValueSegment(i));
            if (i < 3) {
               b.append('|');
            }
         }
         getDriver().sendToDisplay(2, b.toString());
      } else if (section == 1) {
         final StringBuilder b = new StringBuilder();
         for (int i = 4; i < 8; i++) {
            b.append(getValueSegment(i));
            if (i < 7) {
               b.append('|');
            }
         }
         getDriver().sendToDisplay(3, b.toString());
      }
      if (nameContainer != null) {
         nameContainer.updateDetail();
      }
   }

   private String getValueSegment(final int index) {
      if (nameContainer == null) {
         return values[index];
      }
      return nameContainer.getValueString(index, values);
   }

   @Override
   final protected void onActivate() {
      doActivate();
      active.set(true);
   }

   protected void doActivate() {
      refreshValue(0);
      refreshValue(1);
   }

   protected void doDeactivate() {
      /* for subclasses */
   }

   @Override
   final protected void onDeactivate() {
      doDeactivate();
      active.set(false);
   }

   public BooleanValue getActive() {
      return active;
   }

   public String[] getValueDescriptors() {
      return values;
   }

   public String getValue(final int index) {
      if (index >= 0 && index < values.length) {
         return values[index];
      }
      return "";
   }
}
