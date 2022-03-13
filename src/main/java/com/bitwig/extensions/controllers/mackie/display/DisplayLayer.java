package com.bitwig.extensions.controllers.mackie.display;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.mackie.devices.ParameterPage;
import com.bitwig.extensions.controllers.mackie.section.MixerSectionHardware;
import com.bitwig.extensions.controllers.mackie.value.*;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

import java.util.HashMap;
import java.util.Map;

public class DisplayLayer extends Layer implements DisplaySource {
   private LcdDisplay display;

   private final LcdDisplay mainDisplay;
   private final LcdDisplay alternateDisplay;

   private final DisplayRow topRow = new DisplayRow(0);
   private final DisplayRow bottomRow = new DisplayRow(1);

   private final ExpansionTask expansionTask = new ExpansionTask();
   private final Map<Integer, CellExpander> expanders = new HashMap<>();
   private final Map<Integer, CellExpander> fixedExpanders = new HashMap<>();
   private boolean usesLevelMeteringInLcd = true;
   private boolean showTrackInformation = false;
   private boolean nameValueState = false;

   static class ExpansionTask {
      private int expansionBottomIndex = -1;
      private long startTime = -1L;
      private CellExpander currentExpander = null;

      boolean isActive() {
         return expansionBottomIndex >= 0;
      }

      public void setExpansionBottomIndex(final int expansionBottomIndex) {
         this.expansionBottomIndex = expansionBottomIndex;
      }

      public int getExpansionBottomIndex() {
         return expansionBottomIndex;
      }

      public void setExpander(final CellExpander currentExpander) {
         this.currentExpander = currentExpander;
      }

      public boolean isBlocked(final int cellIndex) {
         return currentExpander != null && currentExpander.inRange(cellIndex);
      }

      void trigger(final int index, final CellExpander expander) {
         startTime = System.currentTimeMillis();
         if (expansionBottomIndex != -1 && expansionBottomIndex != index && currentExpander != null) {
            currentExpander.reset();
         }
         if (currentExpander == null && expander != null) {
            currentExpander = expander;
            currentExpander.display();
         }
         expansionBottomIndex = index;
      }

      long getDuration() {
         if (startTime == -1) {
            return -1;
         }
         return System.currentTimeMillis() - startTime;
      }

      public void reset() {
         startTime = -1;
         expansionBottomIndex = -1;
         if (currentExpander != null) {
            currentExpander.reset();
         }
         currentExpander = null;
      }

      public boolean active(final int rowIndex, final int startIndex) {
         return startTime > 0 && rowIndex == 1 && expansionBottomIndex == startIndex;
      }

      /**
       * Determine if the task is active and running on some other than the given
       * expander.
       *
       * @param cellExpander the expander to check
       * @return false if task is not active or the current expander is the one given.
       */
      public int overlapsWithOther(final CellExpander cellExpander) {
         if (!isActive()) {
            return 0;
         }
         if (currentExpander == cellExpander) {
            return 0;
         }
         return currentExpander.overlaps(cellExpander);
      }
   }

   class CellExpander {
      private final DisplayCell cell;
      private final DisplayRow row;
      private final int span;
      private final String frameChar;
      private final ExpansionTask task;

      public CellExpander(final DisplayCell cell, final DisplayRow row, final int span, final StringValue name,
                          final ObjectProxy sourceOfExistence, final String emptyText, final String frameChar,
                          final ExpansionTask task) {
         this.cell = cell;
         this.row = row;
         this.span = span;
         this.frameChar = frameChar;
         this.task = task;

         name.addValueObserver(this::handleNameChange);
         if (sourceOfExistence != null) {
            sourceOfExistence.exists().addValueObserver(this::handleExistence);
         }
      }

      public int overlaps(final CellExpander otherExpander) {
         if (otherExpander == this) {
            return 0;
         }
         final int over = cell.getIndex() + span - otherExpander.cell.getIndex();
         return Math.max(over, 0);
      }

      public boolean inRange(final int cellIndex) {
         final int startIndex = cell.getIndex();
         return startIndex <= cellIndex && cellIndex <= startIndex + span;
      }

      public void display() {
         sendFullLength(cell.getDisplayValue());
      }

      public void reset() {
         final int startIndex = cell.getIndex();
         for (int i = 0; i < span; i++) {
            final int index = startIndex + i;
            final CellExpander fixedExpander = fixedExpanders.get(index);
            if (fixedExpander != null) {
               if (fixedExpander.cell.getIndex() == index) {
                  fixedExpander.display();
               }
            } else {
               final DisplayCell otherCell = row.getCell(index);
               sendToValueRow(otherCell);
            }
         }
      }

      void handleNameChange(final String v) {
         cell.lastValue = v;
         if (!isActive() || row.isFullTextMode()) {
            return;
         }
         if (task == null) {
            sendFullLength(v);
         } else if (task.active(row.getRowIndex(), cell.getIndex())) {
            task.setExpander(this);
            sendFullLength(v);
         } else {
            sendToValueRowFull(cell);
         }
      }

      private void sendFullLength(final String v) {
         if (!isActive() || bottomRow.isFullTextMode()) {
            return;
         }
         final int overlap = expansionTask.overlapsWithOther(this);
         if (overlap >= span) {
            return;
         }
         final String dv = frameChar != null ? frameChar.charAt(0) + v : v;
         for (int i = overlap; i < span; i++) {
            final int index = cell.getIndex() + i;
            final String sendString = DisplayLayer.splitString(dv, i);

            if (i < span - 1) {
               display.sendToRowFull(DisplayLayer.this, row.getRowIndex(), index, sendString);
            } else {
               final char endCharacter = frameChar != null ? frameChar.charAt(1) : ' ';
               display.sendToRow(DisplayLayer.this, row.getRowIndex(), index,
                  DisplayLayer.padEndChar(sendString, endCharacter, 5));
            }
         }
      }

      void handleExistence(final boolean exist) {
         cell.setExist(exist);
         if (!isActive() || row.isFullTextMode()) {
            return;
         }
         if (task == null) {
            sendFullLength(cell.getDisplayValue());
         } else if (expansionTask.active(row.getRowIndex(), cell.getIndex())) {
            sendFullLength(cell.getDisplayValue());
         } else {
            if (!isActive() || bottomRow.isFullTextMode() || expansionTask.overlapsWithOther(this) > 0) {
               return;
            }
            display.sendToRow(DisplayLayer.this, row.getRowIndex(), cell.getIndex(), cell.getDisplayValue());
         }
      }
   }

   public DisplayLayer(final String name, final int sectionIndex, final Layers layers,
                       final MixerSectionHardware hwControls) {
      this(name, sectionIndex, layers, hwControls.getMainDisplay(), hwControls.getBottomDisplay());
   }

   public DisplayLayer(final String name, final int sectionIndex, final Layers layers, final LcdDisplay display,
                       final LcdDisplay bottomDisplay) {
      super(layers, name + "_" + sectionIndex + "_Display");
      mainDisplay = display;
      this.display = display;
      alternateDisplay = bottomDisplay;
   }

   public void focusOnTop() {
      display = mainDisplay;
      invokeRefresh();
   }

   public void focusOnBottom() {
      if (alternateDisplay != null) {
         display = alternateDisplay;
         invokeRefresh();
      }
   }

   public void setNameValueState(final boolean nameValueState) {
      this.nameValueState = nameValueState;
   }

   public void bindFixed(final int index, final String value) {
      bottomRow.getCell(index).lastValue = value;
   }

   public void bindBool(final int index, final SettableBooleanValue value, final String trueString,
                        final String falseString, final ObjectProxy existSource, final String emptyString) {
      bindExists(index, existSource, emptyString);
      value.addValueObserver(newValue -> {
         bottomRow.getCell(index).lastValue = newValue ? trueString : falseString;
         if (isActive() && !bottomRow.isFullTextMode()) {
            display.sendToRow(this, 1, index, bottomRow.getCell(index).getDisplayValue());
         }
      });
      bottomRow.getCell(index).lastValue = value.get() ? trueString : falseString;
   }

   public void bindBool(final int index, final BooleanValue value, final String trueString, final String falseString) {
      value.addValueObserver(newValue -> {
         bottomRow.getCell(index).lastValue = newValue ? trueString : falseString;
         if (isActive() && !bottomRow.isFullTextMode()) {
            display.sendToRow(this, 1, index, bottomRow.getCell(index).getDisplayValue());
         }
      });
      bottomRow.getCell(index).lastValue = value.get() ? trueString : falseString;
   }

   private void bindExists(final int index, final ObjectProxy existSource, final String emptyString) {
      final DisplayCell cell = bottomRow.getCell(index);
      cell.setEmptyValue(emptyString);
      existSource.exists().addValueObserver(exist -> {
         cell.setExist(exist);
         if (isActive() && !bottomRow.isFullTextMode()) {
            display.sendToRow(this, 1, index, cell.getDisplayValue());
         }
      });
      cell.setExist(existSource.exists().get());
   }

   private void sendToValueRow(final DisplayCell cell) {
      if (isActive() && !bottomRow.isFullTextMode()) {
         display.sendToRow(this, 1, cell.getIndex(), cell.getDisplayValue());
      }
   }

   private void sendToValueRowFull(final DisplayCell cell) {
      if (isActive() && !bottomRow.isFullTextMode()) {
         display.sendToRowFull(this, 1, cell.getIndex(), cell.getDisplayValue());
      }
   }

   private void bindExists(final int index, final ObjectProxy existSource) {
      existSource.exists().addValueObserver(exist -> {
         bottomRow.getCell(index).setExist(exist);
         if (isActive() && !bottomRow.isFullTextMode()) {
            display.sendToRow(this, 1, index, bottomRow.getCell(index).getDisplayValue());
         }
      });
      bottomRow.getCell(index).setExist(existSource.exists().get());
   }

   public void bindDisplayParameterValue(final int index, final Parameter parameter,
                                         final StringValueConverter valueFormatter) {
      bindExists(index, parameter);
      parameter.displayedValue().addValueObserver(newStringValue -> {
         bottomRow.getCell(index).lastValue = valueFormatter.convert(newStringValue);
         if (isActive() && !bottomRow.isFullTextMode()) {
            display.sendToRow(this, 1, index, bottomRow.getCell(index).getDisplayValue());
         }
      });
      bottomRow.getCell(index).lastValue = valueFormatter.convert(parameter.displayedValue().get());
   }

   public void bindValue(final int index, final SettableRangedValue value, final DoubleValueConverter converter) {
      value.addValueObserver(v -> {
         bottomRow.getCell(index).lastValue = converter.convert(v);
         if (isActive() && !bottomRow.isFullTextMode()) {
            display.sendToRow(this, 1, index, bottomRow.getCell(index).getDisplayValue());
         }
      });
      bottomRow.getCell(index).lastValue = converter.convert(value.get());
   }

   public void bindParameterValue(final int index, final Parameter parameter, final DoubleValueConverter converter) {
      bindExists(index, parameter);
      parameter.value().addValueObserver(v -> {
         bottomRow.getCell(index).lastValue = converter.convert(v);
         if (isActive() && !bottomRow.isFullTextMode()) {
            display.sendToRow(this, 1, index, bottomRow.getCell(index).getDisplayValue());
         }
      });
      bottomRow.getCell(index).lastValue = converter.convert(parameter.value().get());
   }

   public void bindParameterValue(final int index, final Parameter parameter) {
      bindExists(index, parameter);
      parameter.displayedValue().addValueObserver(v -> handleDisplayValueChanged(index, v));
      bottomRow.getCell(index).lastValue = parameter.displayedValue().get();
   }

   public void bindParameterValue(final int index, final DoubleValue value, final ObjectProxy existSource,
                                  final String nonExistText, final DoubleValueConverter converter) {
      bindExists(index, existSource, nonExistText);
      value.addValueObserver(v -> {
         bottomRow.getCell(index).lastValue = converter.convert(v);
         if (isActive() && !bottomRow.isFullTextMode()) {
            display.sendToRow(this, 1, index, bottomRow.getCell(index).getDisplayValue());
         }
      });
      bottomRow.getCell(index).lastValue = converter.convert(value.get());
   }

   public void bindParameterValue(final int index, final DoubleValue value, final DoubleValueConverter converter) {
      value.addValueObserver(v -> {
         bottomRow.getCell(index).lastValue = converter.convert(v);
         if (isActive() && !bottomRow.isFullTextMode()) {
            display.sendToRow(this, 1, index, bottomRow.getCell(index).getDisplayValue());
         }
      });
      bottomRow.getCell(index).lastValue = converter.convert(value.get());
   }

   public void bindParameterValue(final int index, final SettableEnumValue value, final EnumValueSetting values) {
      value.addValueObserver(v -> {
         bottomRow.getCell(index).lastValue = values.getDisplayValue(v);
         if (isActive() && !bottomRow.isFullTextMode()) {
            display.sendToRow(this, 1, index, bottomRow.getCell(index).getDisplayValue());
         }
      });
      bottomRow.getCell(index).lastValue = values.getDisplayValue(value.get());
   }

   public void bindParameterValue(final int index, final IntValueObject value) {
      value.addValueObserver(v -> {
         bottomRow.getCell(index).lastValue = value.displayedValue();
         if (isActive() && !bottomRow.isFullTextMode()) {
            display.sendToRow(this, 1, index, bottomRow.getCell(index).getDisplayValue());
         }
      });
      bottomRow.getCell(index).lastValue = value.displayedValue();
   }

   public <T> void bindParameterValue(final int index, final ValueObject<T> value) {
      value.addValueObserver((old, v) -> {
         bottomRow.getCell(index).lastValue = value.displayedValue();
         if (isActive() && !bottomRow.isFullTextMode()) {
            display.sendToRow(this, 1, index, bottomRow.getCell(index).getDisplayValue());
         }
      });
      bottomRow.getCell(index).lastValue = value.displayedValue();
   }

   public DisplayLayer setShowTrackInformation(final boolean showTrackInformation) {
      this.showTrackInformation = showTrackInformation;
      return this;
   }

   /**
    * A name binding that takes up a number of cells for a certain time and then
    * shrinks back to a single cell occupation.
    *
    * @param rowIndex          the row
    * @param startIndex        the cell index
    * @param tempSpan          the temporary span
    * @param name              name value observed
    * @param sourceOfExistence source of existence
    * @param emptyText         default empty text
    */
   public void bindNameTemp(final int rowIndex, final int startIndex, final int tempSpan, final StringValue name,
                            final ObjectProxy sourceOfExistence, final String emptyText) {
      assert startIndex >= 0 && startIndex < 8;
      assert startIndex + tempSpan <= 8;
      assert tempSpan > 0;

      final DisplayRow row = rowIndex == 0 ? topRow : bottomRow;
      final DisplayCell cell = row.getCell(startIndex);
      cell.setEmptyValue(emptyText);

      final CellExpander expander = new CellExpander(cell, row, tempSpan, name, sourceOfExistence, emptyText, "[]",
         expansionTask);
      expanders.put(startIndex, expander);

      cell.setExist(sourceOfExistence.exists().get());
      cell.lastValue = name.get();
   }

   public void bindTitle(final int rowIndex, final int startIndex, final int span, final StringValue name,
                         final String enclosing) {
      assert startIndex >= 0 && startIndex < 8;
      assert startIndex + span <= 8;
      assert span > 0;

      final DisplayRow row = rowIndex == 0 ? topRow : bottomRow;
      final DisplayCell cell = row.getCell(startIndex);
      final CellExpander expander = new CellExpander(cell, row, span, name, null, "", enclosing, null);
      fixedExpanders.put(startIndex, expander);
      for (int i = 0; i < span; i++) {
         fixedExpanders.put(i + startIndex, expander);
      }
      cell.lastValue = "";
   }

   public void bindTitle(final int rowIndex, final int startIndex, final int span, final StringValue name,
                         final ObjectProxy sourceOfExistence, final String emptyText, final char enclosing) {
      assert startIndex >= 0 && startIndex < 8;
      assert startIndex + span <= 8;
      assert span > 0;

      final DisplayRow row = rowIndex == 0 ? topRow : bottomRow;
      final DisplayCell cell = row.getCell(startIndex);

      final CellExpander expander = new CellExpander(cell, row, span, name, sourceOfExistence, emptyText, "<>", null);
      fixedExpanders.put(startIndex, expander);
      for (int i = 0; i < span; i++) {
         fixedExpanders.put(i + startIndex, expander);
      }

      cell.setEmptyValue(emptyText);
      cell.lastValue = name.get();
   }

   public void bindTitle(final int index, final StringValue name, final ObjectProxy sourceOfExistence,
                         final String emptyText) {
      bindTitle(0, index, name, sourceOfExistence, emptyText);
   }

   public void bindTitle(final int rowIndex, final int index, final StringValue name) {
      final DisplayRow row = rowIndex == 0 ? topRow : bottomRow;
      final DisplayCell cell = row.getCell(index);
      cell.setEmptyValue("");
      name.addValueObserver(v -> {
         cell.lastValue = v;
         if (isActive() && !row.isFullTextMode()) {
            display.sendToRow(this, rowIndex, index, cell.getDisplayValue());
         }
      });
      row.getCell(index).setRefresher(name::get);
      cell.lastValue = name.get();
   }

   public void bindTitle(final int rowIndex, final int index, final StringValue name,
                         final ObjectProxy sourceOfExistence, final String emptyText) {
      final DisplayRow row = rowIndex == 0 ? topRow : bottomRow;
      final DisplayCell cell = row.getCell(index);
      cell.setEmptyValue(emptyText);
      sourceOfExistence.exists().addValueObserver(exist -> {
         cell.setExist(exist);
         if (isActive() && !row.isFullTextMode()) {
            display.sendToRow(this, rowIndex, index, cell.getDisplayValue());
         }
      });
      cell.setExist(sourceOfExistence.exists().get());
      name.addValueObserver(v -> {
         cell.lastValue = v;
         if (isActive() && !row.isFullTextMode()) {
            display.sendToRow(this, rowIndex, index, cell.getDisplayValue());
         }
      });
      cell.lastValue = name.get();
   }

   private static String splitString(final String s, final int section) {
      final int startIndex = section * 7;
      if (startIndex >= s.length()) {
         return "";
      }
      return s.substring(startIndex, Math.min(startIndex + 7, s.length()));
   }

   private static String padEndChar(final String s, final char c, final int pos) {
      final StringBuilder b = new StringBuilder();
      for (int i = 0; i < pos; i++) {
         if (i < s.length()) {
            b.append(s.charAt(i));
         } else {
            b.append(' ');
         }
      }
      b.append(c);
      return b.toString();
   }

   public void bindName(final int index, final ChannelStateValueHandler nameHolder) {
      nameHolder.addValueObserver((name, exists, isGroup, isExpanded, handler) -> {
         topRow.getCell(index).lastValue = handler.toCurrentValue(null, display.getSegmentLength(), name, exists,
            isGroup, isExpanded);
         if (isActive() && !topRow.isFullTextMode()) {
            display.sendToRow(this, 0, index, topRow.getCell(index).lastValue);
         }
      });
      topRow.getCell(index).lastValue = nameHolder.toCurrentValue(null, display.getSegmentLength());
      topRow.getCell(index).setRefresher(() -> nameHolder.toCurrentValue(null, display.getSegmentLength()));
   }

   public void bindName(final int index, final ChannelStateValueHandler nameHolder, final StringValue otherValue) {
      nameHolder.addValueObserver((name, exists, isGroup, isExpanded, handler) -> {
         topRow.getCell(index).lastValue = handler.toCurrentValue(null, display.getSegmentLength(), name, exists,
            isGroup, isExpanded);
         sendToTopRow(index, topRow.getCell(index).lastValue);
      });
      otherValue.addValueObserver(v -> {
         if (nameValueState) {
            sendToTopRow(index, v);
         }
      });
      topRow.getCell(index).lastValue = nameHolder.toCurrentValue(null, display.getSegmentLength());
      topRow.getCell(index)
         .setRefresher(
            () -> nameHolder.toCurrentValue(nameValueState ? otherValue.get() : null, display.getSegmentLength()));
   }

   private void sendToTopRow(final int index, final String value) {
      if (isActive() && !topRow.isFullTextMode()) {
         display.sendToRow(this, 0, index, value);
      }
   }

   public void bindTitle(final int index, final ChannelStateValueHandler nameHolder, final StringValue fixedName) {
      final DisplayCell cell = topRow.getCell(index);

      nameHolder.addValueObserver((name, exists, isGroup, isExpanded, handler) -> {
         cell.lastValue = handler.toCurrentValue(nameValueState ? null : fixedName.get(), display.getSegmentLength(),
            name, exists, isGroup, isExpanded);
         sendToTopRow(index, cell.lastValue);
      });
      fixedName.addValueObserver(value -> {
         if (!display.isLowerDisplay()) {
            cell.lastValue = value;
            sendToTopRow(index, cell.lastValue);
         }
      });
      cell.lastValue = nameHolder.toCurrentValue(fixedName.get(), display.getSegmentLength());
      cell.setRefresher(
         () -> nameHolder.toCurrentValue(nameValueState ? null : fixedName.get(), display.getSegmentLength()));
   }

   public void bindTitle(final int index, final StringValue name) {
      name.addValueObserver(v -> {
         topRow.getCell(index).lastValue = v;
         sendToTopRow(index, topRow.getCell(index).lastValue);
      });
      topRow.getCell(index).lastValue = name.get();
      topRow.getCell(index).setRefresher(name::get);
   }

   public void bind(final int index, final ParameterPage parameter) {
      parameter.addNameObserver(v -> {
         topRow.getCell(index).lastValue = v;
         sendToTopRow(index, v);
      });
      topRow.getCell(index).lastValue = parameter.getCurrentName();
      parameter.addStringValueObserver(v -> handleDisplayValueChanged(index, v));
      parameter.displayedValue().markInterested();
      bottomRow.getCell(index).lastValue = parameter.displayedValue().get();
   }

   private void handleDisplayValueChanged(final int index, final String newValue) {
      bottomRow.getCell(index).lastValue = newValue;
      if (isActive() && !bottomRow.isFullTextMode()) {
         display.sendToRow(this, 1, index, bottomRow.getCell(index).getDisplayValue());
      }
   }

   public void setText(final int row, final String value, final boolean centered) {
      final DisplayRow which = row == 0 ? topRow : bottomRow;
      which.setFullText(value);
      which.setCentered(centered);
   }

   public void displayFullTextMode(final boolean enabled) {
      if (isActive()) {
         display.setFullTextMode(0, topRow.fullTextMode);
         display.setFullTextMode(1, bottomRow.fullTextMode);
      }
   }

   public void enableFullTextMode(final int row, final boolean enabled) {
      final DisplayRow which = row == 0 ? topRow : bottomRow;
      final boolean active = isActive();
      which.enableFullTextMode(display, this, enabled, active);
   }

   public void setMainText(final String row1, final String row2, final boolean centered) {
      topRow.setFullText(row1);
      topRow.setCentered(centered);
      bottomRow.setFullText(row2);
      bottomRow.setCentered(centered);
   }

   public void enableFullTextMode(final boolean enabled) {
      final boolean active = isActive();
      topRow.enableFullTextMode(display, this, enabled, active);
      bottomRow.enableFullTextMode(display, this, enabled, active);
      if (active) {
         display.setFullTextMode(0, topRow.fullTextMode);
         display.setFullTextMode(1, bottomRow.fullTextMode);
      }
   }

   public void invokeRefresh() {
      if (!isActive()) {
         return;
      }
      topRow.refresh(display, this);
      bottomRow.refresh(display, this);
   }

   public void setUsesLevelMeteringInLcd(final boolean usesLevelMeteringInLcd) {
      this.usesLevelMeteringInLcd = usesLevelMeteringInLcd;
   }

   @Override
   protected void onActivate() {
      super.onActivate();
      display.setFullTextMode(0, topRow.fullTextMode);
      display.setFullTextMode(1, bottomRow.fullTextMode);
      display.setDisplayBarGraphEnabled(usesLevelMeteringInLcd);
      topRow.refresh(display, this);
      bottomRow.refresh(display, this);
      fixedExpanders.forEach((key, value) -> value.reset());
   }

   @Override
   protected void onDeactivate() {
      super.onDeactivate();
   }

   public void triggerTimer() {
      final long duration = expansionTask.getDuration();
      if (duration > 1000) {
         expansionTask.reset();
      }
   }

   public void tickExpansion(final int index) {
      expansionTask.trigger(index, expanders.get(index));
   }

}
