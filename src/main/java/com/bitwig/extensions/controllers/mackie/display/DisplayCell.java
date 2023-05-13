package com.bitwig.extensions.controllers.mackie.display;

class DisplayCell {
   String lastValue = "";
   private boolean exist = true;
   private String emptyValue = "";
   private final int index;
   private ValueRefresher refresher;

   public interface ValueRefresher {
      String getValue();
   }

   public DisplayCell(final int index) {
      this.index = index;
   }

   public String getDisplayValue() {
      return exist ? lastValue : emptyValue;
   }

   public void setRefresher(final ValueRefresher refresher) {
      this.refresher = refresher;
   }

   public void refresh() {
      if (refresher != null) {
         lastValue = refresher.getValue();
      }
   }

   public int getIndex() {
      return index;
   }

   public void setExist(final boolean exist) {
      this.exist = exist;
   }

   public void setEmptyValue(final String emptyText) {
      emptyValue = emptyText;
   }

}
