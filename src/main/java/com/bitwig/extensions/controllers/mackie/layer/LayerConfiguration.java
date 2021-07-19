package com.bitwig.extensions.controllers.mackie.layer;

import java.util.function.IntConsumer;

import com.bitwig.extensions.controllers.mackie.devices.DeviceManager;
import com.bitwig.extensions.controllers.mackie.devices.DeviceTypeFollower;
import com.bitwig.extensions.framework.Layer;

public abstract class LayerConfiguration {
	private final String name;
	protected final MixControl mixControl;
	protected IntConsumer navigateHorizontalHandler;
	protected IntConsumer navigateVerticalHandler;

	public LayerConfiguration(final String name, final MixControl mixControl) {
		this.name = name;
		this.mixControl = mixControl;
	}

	public MixControl getMixControl() {
		return mixControl;
	}

	public void setNavigateHorizontalHandler(final IntConsumer navigateHorizontalHandler) {
		this.navigateHorizontalHandler = navigateHorizontalHandler;
	}

	public void setNavigateVerticalHandler(final IntConsumer navigateVerticalHandler) {
		this.navigateVerticalHandler = navigateVerticalHandler;
	}

	// protected abstract void navigateLeftRight(int direction);

	public abstract Layer getFaderLayer();

	public abstract EncoderLayer getEncoderLayer();

	public Layer getButtonLayer() {
		if (mixControl.driver.getGlobalViewActive().get()) {
			return mixControl.globalGroup.getMixerButtonLayer();
		}
		return mixControl.mainGroup.getMixerButtonLayer();
	}

	public abstract DisplayLayer getDisplayLayer(int which);

	public DeviceManager getDeviceManager() {
		return null;
	}

	public String getName() {
		return name;
	}

	public void navigateHorizontal(final int direction) {
		if (navigateHorizontalHandler != null) {
			navigateHorizontalHandler.accept(direction);
		}
	}

	public void navigateVertical(final int direction) {
		if (navigateVerticalHandler != null) {
			navigateVerticalHandler.accept(direction);
		}
	}

	public boolean enableInfo(final InfoSource navVertical) {
		return false;
	}

	public boolean disableInfo() {
		return false;
	}

	protected void evaluateDisplay() {

	}

	public void setCurrentFollower(final DeviceTypeFollower follower) {
	}

}