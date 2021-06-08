package com.bitwig.extensions.controllers.mackie;

import com.bitwig.extensions.framework.Layer;

public class MackieLayer extends Layer {

	private final MackieMcuProExtension driver;

	public MackieLayer(final MackieMcuProExtension driver, final String name) {
		super(driver.getLayers(), name);
		this.driver = driver;
	}

	public MackieMcuProExtension getDriver() {
		return driver;
	}

}
