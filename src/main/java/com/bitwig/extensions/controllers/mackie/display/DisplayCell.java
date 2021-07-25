package com.bitwig.extensions.controllers.mackie.display;

class DisplayCell {
	String lastValue = "";
	private boolean exist = true;
	private String emptyValue = "";
	private final int index;

	public DisplayCell(final int index) {
		this.index = index;
	}

	public String getDisplayValue() {
		return exist ? lastValue : emptyValue;
	}

	public int getIndex() {
		return index;
	}

	public void setExist(final boolean exist) {
		this.exist = exist;
	}

	public void setEmptyValue(final String emptyText) {
		this.emptyValue = emptyText;
	}

}