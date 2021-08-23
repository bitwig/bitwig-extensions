package com.bitwig.extensions.controllers.mackie.display;

import java.util.HashMap;
import java.util.Map;

import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.DoubleValue;
import com.bitwig.extension.controller.api.ObjectProxy;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.mackie.bindings.ValueConverter;
import com.bitwig.extensions.controllers.mackie.bindings.ValueStringConverter;
import com.bitwig.extensions.controllers.mackie.devices.ParameterPage;
import com.bitwig.extensions.controllers.mackie.layer.MixControl;
import com.bitwig.extensions.framework.Layer;

public class DisplayLayer extends Layer implements DisplaySource {
	private final LcdDisplay display;
	private final DisplayRow topRow = new DisplayRow(0);
	private final DisplayRow bottomRow = new DisplayRow(1);
	private final ExpansionTask expansionTask = new ExpansionTask();
	private final Map<Integer, CellExpander> expanders = new HashMap<>();
	private final Map<Integer, CellExpander> fixedExpanders = new HashMap<>();
	private boolean usesLevelMeteringInLcd = true;

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
		public boolean runningOther(final CellExpander cellExpander) {
			return isActive() && currentExpander != cellExpander;
		}
	}

	class CellExpander {
		private final DisplayCell cell;
		private final DisplayRow row;
		private final int span;
		private final String frameChar;
		private final ExpansionTask task;

		public CellExpander(final DisplayCell cell, final DisplayRow row, final int span, final StringValue name,
				final ObjectProxy sourceOfExistance, final String emptyText, final String frameChar,
				final ExpansionTask task) {
			this.cell = cell;
			this.row = row;
			this.span = span;
			this.frameChar = frameChar;
			this.task = task;

			name.addValueObserver(this::handleNameChange);
			if (sourceOfExistance != null) {
				sourceOfExistance.exists().addValueObserver(this::handleExistence);
			}
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
			if (!isActive() || bottomRow.isFullTextMode() || expansionTask.runningOther(this)) {
				return;
			}
			final String dv = frameChar != null ? frameChar.charAt(0) + v : v;
			for (int i = 0; i < span; i++) {
				final int index = cell.getIndex() + i;
				final String sendString = splitString(dv, i);
				if (i < span - 1) {
					display.sendToRowFull(DisplayLayer.this, row.getRowIndex(), index, sendString);
				} else {
					final char endc = frameChar != null ? frameChar.charAt(1) : ' ';
					display.sendToRow(DisplayLayer.this, row.getRowIndex(), index, padEndChar(sendString, endc, 5));
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
				if (!isActive() || bottomRow.isFullTextMode() || expansionTask.runningOther(this)) {
					return;
				}
				display.sendToRow(DisplayLayer.this, row.getRowIndex(), cell.getIndex(), cell.getDisplayValue());
			}
		}
	}

	public DisplayLayer(final String name, final MixControl mixControl) {
		super(mixControl.getDriver().getLayers(),
				name + "_" + mixControl.getHwControls().getSectionIndex() + "_Display");
		this.display = mixControl.getDisplay();
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
			final ValueStringConverter valueFormatter) {
		bindExists(index, parameter);
		parameter.displayedValue().addValueObserver(newStringValue -> {
			bottomRow.getCell(index).lastValue = valueFormatter.convert(newStringValue);
			if (isActive() && !bottomRow.isFullTextMode()) {
				display.sendToRow(this, 1, index, bottomRow.getCell(index).getDisplayValue());
			}
		});
		bottomRow.getCell(index).lastValue = valueFormatter.convert(parameter.displayedValue().get());
	}

	public void bindParameterValue(final int index, final Parameter parameter, final ValueConverter converter) {
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
		parameter.displayedValue().addValueObserver(v -> {
			handleDisplayValueChanged(index, v);
		});
		bottomRow.getCell(index).lastValue = parameter.displayedValue().get();
	}

	public void bindParameterValue(final int index, final DoubleValue value, final ObjectProxy existSource,
			final String nonExistText, final ValueConverter converter) {
		bindExists(index, existSource, nonExistText);
		value.addValueObserver(v -> {
			bottomRow.getCell(index).lastValue = converter.convert(v);
			if (isActive() && !bottomRow.isFullTextMode()) {
				display.sendToRow(this, 1, index, bottomRow.getCell(index).getDisplayValue());
			}
		});
		bottomRow.getCell(index).lastValue = converter.convert(value.get());
	}

	public void bindParameterValue(final int index, final DoubleValue value, final ValueConverter converter) {
		value.addValueObserver(v -> {
			bottomRow.getCell(index).lastValue = converter.convert(v);
			if (isActive() && !bottomRow.isFullTextMode()) {
				display.sendToRow(this, 1, index, bottomRow.getCell(index).getDisplayValue());
			}
		});
		bottomRow.getCell(index).lastValue = converter.convert(value.get());
	}

	public void bindName(final int index, final StringValue name, final ObjectProxy sourceOfExistance,
			final String emptyText) {
		bindName(0, index, name, sourceOfExistance, emptyText);
	}

	public void bindName(final int rowIndex, final int index, final StringValue name) {
		final DisplayRow row = rowIndex == 0 ? topRow : bottomRow;
		final DisplayCell cell = row.getCell(index);
		cell.setEmptyValue("");
		name.addValueObserver(v -> {
			cell.lastValue = v;
			if (isActive() && !row.isFullTextMode()) {
				display.sendToRow(this, rowIndex, index, cell.getDisplayValue());
			}
		});
		cell.lastValue = name.get();
	}

	public void bindName(final int rowIndex, final int index, final StringValue name,
			final ObjectProxy sourceOfExistance, final String emptyText) {
		final DisplayRow row = rowIndex == 0 ? topRow : bottomRow;
		final DisplayCell cell = row.getCell(index);
		cell.setEmptyValue(emptyText);
		sourceOfExistance.exists().addValueObserver(exist -> {
			cell.setExist(exist);
			if (isActive() && !row.isFullTextMode()) {
				display.sendToRow(this, rowIndex, index, cell.getDisplayValue());
			}
		});
		cell.setExist(sourceOfExistance.exists().get());
		name.addValueObserver(v -> {
			cell.lastValue = v;
			if (isActive() && !row.isFullTextMode()) {
				display.sendToRow(this, rowIndex, index, cell.getDisplayValue());
			}
		});
		cell.lastValue = name.get();
	}

	/**
	 * A name binding that takes up a number of cells for a certain time and then
	 * shrinks back to a single cell occupation.
	 *
	 * @param rowIndex          the row
	 * @param startIndex        the cell index
	 * @param tempSpan          the temporary span
	 * @param name              name value observed
	 * @param sourceOfExistance source of existence
	 * @param emptyText         default empty text
	 */
	public void bindNameTemp(final int rowIndex, final int startIndex, final int tempSpan, final StringValue name,
			final ObjectProxy sourceOfExistance, final String emptyText) {
		assert startIndex >= 0 && startIndex < 8;
		assert startIndex + tempSpan < 8;
		assert tempSpan > 0;

		final DisplayRow row = rowIndex == 0 ? topRow : bottomRow;
		final DisplayCell cell = row.getCell(startIndex);
		cell.setEmptyValue(emptyText);

		final CellExpander expander = new CellExpander(cell, row, tempSpan, name, sourceOfExistance, emptyText, "[]",
				expansionTask);
		expanders.put(startIndex, expander);

		cell.setExist(sourceOfExistance.exists().get());
		cell.lastValue = name.get();
	}

	public void bindName(final int rowIndex, final int startIndex, final int span, final StringValue name,
			final String enclosing) {
		assert startIndex >= 0 && startIndex < 8;
		assert startIndex + span < 8;
		assert span > 0;

		final DisplayRow row = rowIndex == 0 ? topRow : bottomRow;
		final DisplayCell cell = row.getCell(startIndex);

		final CellExpander expander = new CellExpander(cell, row, span, name, null, "", enclosing, null);
		fixedExpanders.put(startIndex, expander);
		for (int i = 0; i < span; i++) {
			fixedExpanders.put(i + startIndex, expander);
		}

		cell.lastValue = name.get();
	}

	public void bindName(final int rowIndex, final int startIndex, final int span, final StringValue name,
			final ObjectProxy sourceOfExistance, final String emptyText, final char enclosing) {
		assert startIndex >= 0 && startIndex < 8;
		assert startIndex + span < 8;
		assert span > 0;

		final DisplayRow row = rowIndex == 0 ? topRow : bottomRow;
		final DisplayCell cell = row.getCell(startIndex);

		final CellExpander expander = new CellExpander(cell, row, span, name, sourceOfExistance, emptyText, "<>", null);
		fixedExpanders.put(startIndex, expander);
		for (int i = 0; i < span; i++) {
			fixedExpanders.put(i + startIndex, expander);
		}

		cell.setEmptyValue(emptyText);
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

	public void bindName(final int index, final StringValue name) {
		name.addValueObserver(v -> {
			topRow.getCell(index).lastValue = v;
			if (isActive() && !topRow.isFullTextMode()) {
				display.sendToRow(this, 0, index, v);
			}
		});
		topRow.getCell(index).lastValue = name.get();
	}

	public void bind(final int index, final ParameterPage parameter) {
		parameter.addNameObserver(v -> {
			topRow.getCell(index).lastValue = v;
			if (isActive() && !topRow.isFullTextMode()) {
				display.sendToRow(this, 0, index, v);
			}
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
