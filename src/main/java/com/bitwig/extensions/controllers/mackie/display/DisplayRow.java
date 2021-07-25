package com.bitwig.extensions.controllers.mackie.display;

class DisplayRow {
	int rowIndex = 0;
	DisplayCell[] cells = new DisplayCell[8];
	String fullText = "";
	boolean centered = false;
	boolean fullTextMode = false;

	public DisplayRow(final int rowIndex) {
		this.rowIndex = rowIndex;
		for (int i = 0; i < 8; i++) {
			cells[i] = new DisplayCell(i);
		}
	}

	DisplayCell getCell(final int index) {
		return cells[index];
	}

	public int getRowIndex() {
		return rowIndex;
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

	public void enableFullTextMode(final LcdDisplay display, final DisplaySource source, final boolean enable,
			final boolean doRefresh) {
		this.fullTextMode = enable;
		if (doRefresh) {
			refresh(display, source);
		}
	}

	public void refresh(final LcdDisplay display, final DisplaySource source) {
		if (fullTextMode) {
			if (centered) {
				display.centerText(source, rowIndex, fullText);
			} else {
				display.sendToDisplay(source, rowIndex, fullText);
			}
		} else {
			for (int i = 0; i < 8; i++) {
				display.sendToRow(source, rowIndex, i, cells[i].getDisplayValue());
			}
		}
	}
}