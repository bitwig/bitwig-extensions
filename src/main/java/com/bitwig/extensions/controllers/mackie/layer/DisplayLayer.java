package com.bitwig.extensions.controllers.mackie.layer;

import com.bitwig.extension.controller.api.DoubleValue;
import com.bitwig.extension.controller.api.ObjectProxy;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.StringValue;
import com.bitwig.extensions.controllers.mackie.bindings.ValueConverter;
import com.bitwig.extensions.controllers.mackie.bindings.ValueStringConverter;
import com.bitwig.extensions.controllers.mackie.devices.ParameterPage;
import com.bitwig.extensions.controllers.mackie.display.LcdDisplay;
import com.bitwig.extensions.framework.Layer;

public class DisplayLayer extends Layer {
	private final LcdDisplay display;
	private final DisplayRow topRow = new DisplayRow(0);
	private final DisplayRow bottomRow = new DisplayRow(1);
	private boolean inFullTextMode = false;

	static class DisplayRow {
		int rowIndex = 0;
		DisplayCell[] cells = new DisplayCell[8];
		String fullText = "";
		boolean centered = false;
		boolean fullTextMode = false;

		public DisplayRow(final int rowIndex) {
			this.rowIndex = rowIndex;
			for (int i = 0; i < 8; i++) {
				cells[i] = new DisplayCell();
			}
		}

		DisplayCell getCell(final int index) {
			return cells[index];
		}

		public void setFullText(final String fullText) {
			this.fullText = fullText;
		}

		public boolean isCentered() {
			return centered;
		}

		public void setCentered(final boolean centered) {
			this.centered = centered;
		}

		public boolean isFullTextMode() {
			return fullTextMode;
		}

		public void setFullTextMode(final boolean fullTextMode) {
			this.fullTextMode = fullTextMode;
		}

		public void enableFullTextMode(final LcdDisplay display, final boolean enable, final boolean doRefresh) {
			this.fullTextMode = enable;
			if (doRefresh) {
				refresh(display);
			}
		}

		public void refresh(final LcdDisplay display) {
			if (fullTextMode) {
				if (centered) {
					display.centerText(rowIndex, fullText);
				} else {
					display.sendToDisplay(rowIndex, fullText);
				}
			} else {
				for (int i = 0; i < 8; i++) {
					display.sendToRow(rowIndex, i, cells[i].getDisplayValue());
				}
			}
		}
	}

	private static class DisplayCell {
		private String lastValue = "";
		private boolean exist = true;
		private String emptyValue = "";

		public DisplayCell() {
			super();
		}

		public String getDisplayValue() {
			return exist ? lastValue : emptyValue;
		}

		public void setExist(final boolean exist) {
			this.exist = exist;
		}

		public void setEmptyValue(final String emptyText) {
			this.emptyValue = emptyText;
		}

	}

	public DisplayLayer(final String name, final MixControl mixControl) {
		super(mixControl.getDriver().getLayers(),
				name + "_" + mixControl.getHwControls().getSectionIndex() + "_Display");
		this.display = mixControl.getDisplay();
	}

	private void bindExists(final int index, final ObjectProxy existSource, final String emptyString) {
		final DisplayCell cell = bottomRow.getCell(index);
		cell.setEmptyValue(emptyString);
		existSource.exists().addValueObserver(exist -> {
			cell.setExist(exist);
			if (isActive() && !bottomRow.isFullTextMode()) {
				display.sendToRow(1, index, cell.getDisplayValue());
			}
		});
		cell.setExist(existSource.exists().get());
	}

	private void bindExists(final int index, final ObjectProxy existSource) {
		existSource.exists().addValueObserver(exist -> {
			bottomRow.getCell(index).setExist(exist);
			if (isActive() && !bottomRow.isFullTextMode()) {
				display.sendToRow(1, index, bottomRow.getCell(index).getDisplayValue());
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
				display.sendToRow(1, index, bottomRow.getCell(index).getDisplayValue());
			}
		});
		bottomRow.getCell(index).lastValue = valueFormatter.convert(parameter.displayedValue().get());
	}

	public void bindParameterValue(final int index, final Parameter parameter, final ValueConverter converter) {
		bindExists(index, parameter);
		parameter.value().addValueObserver(v -> {
			bottomRow.getCell(index).lastValue = converter.convert(v);
			if (isActive() && !bottomRow.isFullTextMode()) {
				display.sendToRow(1, index, bottomRow.getCell(index).getDisplayValue());
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
				display.sendToRow(1, index, bottomRow.getCell(index).getDisplayValue());
			}
		});
		bottomRow.getCell(index).lastValue = converter.convert(value.get());

	}

	public void bindName(final int index, final StringValue name, final ObjectProxy sourceOfExistance,
			final String emptyText) {
		final DisplayCell cell = topRow.getCell(index);
		cell.setEmptyValue(emptyText);
		sourceOfExistance.exists().addValueObserver(exist -> {
			cell.setExist(exist);
			if (isActive() && !topRow.isFullTextMode()) {
				display.sendToRow(0, index, cell.getDisplayValue());
			}
		});
		cell.setExist(sourceOfExistance.exists().get());
		name.addValueObserver(v -> {
			cell.lastValue = v;
			if (isActive() && !topRow.isFullTextMode()) {
				display.sendToRow(0, index, cell.getDisplayValue());
			}
		});
		cell.lastValue = name.get();
	}

	public void bindName(final int index, final StringValue name) {
		name.addValueObserver(v -> {
			topRow.getCell(index).lastValue = v;
			if (isActive() && !topRow.isFullTextMode()) {
				display.sendToRow(0, index, v);
			}
		});
		topRow.getCell(index).lastValue = name.get();
	}

	public void bind(final int index, final ParameterPage parameter) {
		parameter.addNameObserver(v -> {
			topRow.getCell(index).lastValue = v;
			if (isActive() && !topRow.isFullTextMode()) {
				display.sendToRow(0, index, v);
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
			display.sendToRow(1, index, bottomRow.getCell(index).getDisplayValue());
		}
	}

	public void setMainText(final String row1, final String row2, final boolean centered) {
		topRow.setFullText(row1);
		topRow.setCentered(centered);
		bottomRow.setFullText(row2);
		bottomRow.setCentered(centered);
	}

	public void enableFullTextMode(final boolean enabled) {
		final boolean active = isActive();
		topRow.enableFullTextMode(display, enabled, active);
		bottomRow.enableFullTextMode(display, enabled, active);
		inFullTextMode = enabled;
		if (active) {
			display.setFullTextMode(enabled);
		}
	}

	@Override
	protected void onActivate() {
		super.onActivate();
		display.setFullTextMode(inFullTextMode);
		topRow.refresh(display);
		bottomRow.refresh(display);
	}

	@Override
	protected void onDeactivate() {
		super.onDeactivate();
	}

}
