package com.bitwig.extensions.controllers.nativeinstruments.maschine.display;

import java.util.function.BooleanSupplier;

import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.Send;
import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineExtension;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.buttons.ModeButton;

public class MixerLayer extends DisplayLayer implements NameContainer {

	private final String[] names = new String[8];
	private final String[] fullNames = new String[8];
	private final boolean[] trackExists = new boolean[8];
	private String currentSendName = "";

	private final RelativeHardwareKnob[] knobs;

	private KnobParamLayer currentParamLayer;

	private final KnobParamLayer volumeLayer;
	private final KnobParamLayer panLayer;
	private final KnobParamLayer sendLayer;

	public MixerLayer(final MaschineExtension driver, final String name) {
		super(driver, name);
		final TrackBank mixerBank = driver.getMixerTrackBank();

		knobs = driver.getDisplayKnobs();

		volumeLayer = new KnobParamLayer(driver, "MIXER_VOL_LAYER", "Levels", this);
		panLayer = new KnobParamLayer(driver, "MIXER_PAN_LAYER", "Panning", this);
		sendLayer = new KnobParamLayer(driver, "MIXER_SEND_LAYER", "Sends ", this);

		final ModeButton leftNav = driver.getNavLeftButton();
		final ModeButton rightNav = driver.getNavRightButton();

		bindPressed(leftNav, () -> incLayer(-1));
		bindPressed(rightNav, () -> incLayer(1));
		bindLightState((BooleanSupplier) () -> currentParamLayer != volumeLayer, leftNav);
		bindLightState((BooleanSupplier) () -> currentParamLayer != sendLayer || sendLayer.canScrollForwards(),
				rightNav);
		for (int i = 0; i < knobs.length; i++) {
			final int index = i;
			final Track track = mixerBank.getItemAt(i);

			track.name().markInterested();
			track.volume().displayedValue().markInterested();
			track.pan().displayedValue().markInterested();

			names[i] = DisplayUtil.padString(track.name().get(), 6);
			fullNames[i] = DisplayUtil.padString(track.name().get(), 12);

			final SendBank sendBank = track.sendBank();

			track.name().addValueObserver(v -> updateTrackName(v, index));

			volumeLayer.bind(knobs[index], track.volume());
			volumeLayer.bindDiplayValue(index, track.volume());

			track.exists().addValueObserver(exsists -> {
				trackExists[index] = exsists;
				refreschTrackName(index / 4);
			});

			panLayer.bind(knobs[index], track.pan());
			panLayer.bindDiplayValue(index, track.pan());

			final Send sendItem = sendBank.getItemAt(0);
			sendItem.exists().markInterested();
			sendItem.value().markInterested();
			sendItem.name().markInterested();
			sendLayer.bind(knobs[index], sendItem.value());
			sendLayer.bindDiplayValue(index, sendItem.value());
			sendLayer.bindScrollable(index, sendBank);
			sendItem.name().addValueObserver(this::updateSendName);
			currentSendName = sendItem.name().get();
		}
		currentParamLayer = volumeLayer;
	}

	@Override
	public String getValueString(final int index, final String[] value) {
		if (!trackExists[index]) {
			return " ---- ";
		}
		if (getDriver().getTouchHandler().isTouched(index) || isInfoModeActive()) {
			return value[index];
		}
		return names[index];
	}

	public void updateSendName(final String name) {
		currentSendName = name;
		updateDetail();
	}

	@Override
	public void updateDetail() {
		if (!isActive()) {
			return;
		}
		final StringBuilder b = new StringBuilder();
		String detailSegment2 = "";
		final int currentParamIndex = getFocusTouchIndex();
		if (isInfoModeActive()) {
			b.append(" ---- | ---- |<PARAM|PARAM>");
			detailSegment2 = " ---- | ---- | ---- | //// ";
		} else if (getDriver().getTouchHandler().isTouched() //
				&& currentParamIndex != -1 //
				&& trackExists[currentParamIndex]) {
			b.append(fullNames[currentParamIndex] + " = " + currentParamLayer.getValue(currentParamIndex));
		} else {
			b.append(currentParamLayer.getTitle() + (currentParamLayer == sendLayer ? " " + currentSendName : ""));
		}
		sendToDisplay(0, b.toString());
		sendToDisplay(1, detailSegment2);
	}

	public void incLayer(final int inc) {
		KnobParamLayer nextMode = null;
		if (inc > 0) {
			if (currentParamLayer == volumeLayer) {
				nextMode = panLayer;
			} else if (currentParamLayer == panLayer) {
				nextMode = sendLayer;
			} else if (currentParamLayer == sendLayer && currentParamLayer.canScrollForwards()) {
				currentParamLayer.scrollForwards();
			}
		} else if (inc < 0) {
			if (currentParamLayer == panLayer) {
				nextMode = volumeLayer;
			} else if (currentParamLayer == sendLayer && currentParamLayer.canScrollBackwards()) {
				currentParamLayer.scrollBackwards();
			} else if (currentParamLayer == sendLayer) {
				nextMode = panLayer;
			}
		}
		if (nextMode != null) {
			currentParamLayer.deactivate();
			currentParamLayer = nextMode;
			currentParamLayer.getValueDescriptors();
			currentParamLayer.activate();
			sendToDisplay(0,
					currentParamLayer.getTitle() + (currentParamLayer == sendLayer ? " " + currentSendName : ""));
			sendToDisplay(1, "");
		}
	}

	@Override
	protected void doNotifyMainTouched(final boolean touched) {
		updateDetail();
		currentParamLayer.refreshValue(0);
		currentParamLayer.refreshValue(1);
	}

	@Override
	protected void notifyEncoderTouched(final int index, final boolean touched) {
		refreschTrackName(index / 4);
		currentParamLayer.refreshValue(index / 4);
	}

	private void refreschTrackName(final int section) {
		if (!isActive()) {
			return;
		}
		if (section == 0) {
			currentParamLayer.refreshValue(section);
		} else if (section == 1) {
			currentParamLayer.refreshValue(section);
		}
		updateDetail();
	}

	@Override
	protected void doNotifyMacroDown(final boolean active) {
		setKnobSensitivity(isMacroDown ? 1.0 : 4.0);
	}

	private void updateTrackName(final String name, final int index) {
		names[index] = DisplayUtil.padString(name, 6);
		fullNames[index] = DisplayUtil.padString(name, 12);
		refreschTrackName(index / 4);
	}

	@Override
	protected void doDeactivate() {
		super.doDeactivate();
		clearDisplay();
		currentParamLayer.deactivate();
	}

	@Override
	protected void doActivate() {
		super.doActivate();
		updateDetail();
		currentParamLayer.activate();
	}

	@Override
	public boolean isControlDisplay() {
		return true;
	}

}
