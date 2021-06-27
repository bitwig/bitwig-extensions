package com.bitwig.extensions.controllers.mackie.layer;

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
				display.centerText(rowIndex, fullText);
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

		public DisplayCell() {
			super();
		}

		public String getDisplayValue() {
			return exist ? lastValue : "";
		}

		public void setExist(final boolean exist) {
			this.exist = exist;
		}

	}

	public DisplayLayer(final String name, final MixControl mixControl) {
		super(mixControl.getDriver().getLayers(),
				name + "_" + mixControl.getHwControls().getSectionIndex() + "_Display");
		this.display = mixControl.getDisplay();
	}

	private void bindExists(final int index, final Parameter parameter) {
		parameter.exists().addValueObserver(exist -> {
			bottomRow.getCell(index).setExist(exist);
			if (isActive() && !bottomRow.isFullTextMode()) {
				display.sendToRow(1, index, bottomRow.getCell(index).getDisplayValue());
			}
		});
		bottomRow.getCell(index).setExist(parameter.exists().get());
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

	public void setCenteredText(final String row1, final String row2) {
		topRow.setFullText(row1);
		topRow.setCentered(true);
		bottomRow.setFullText(row2);
		bottomRow.setCentered(true);
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
