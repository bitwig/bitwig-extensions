package com.bitwig.extensions.controllers.mackie.layer;

import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.mackie.ButtonViewState;
import com.bitwig.extensions.controllers.mackie.MixerMode;
import com.bitwig.extensions.controllers.mackie.bindings.ButtonBinding;
import com.bitwig.extensions.controllers.mackie.display.DisplayLayer;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.controllers.mackie.value.BasicStringValue;
import com.bitwig.extensions.controllers.mackie.value.ValueObject;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class GlovalViewLayerConfiguration extends LayerConfiguration {
	private final EncoderLayer encoderLayer;
	private final DisplayLayer displayLayer;
	private final Layer buttonLayer;
	private int blinkTicks;

	public GlovalViewLayerConfiguration(final String name, final MixControl mixControl) {
		super(name, mixControl);
		final int sectionIndex = mixControl.getHwControls().getSectionIndex();
		final Layers layers = this.mixControl.getDriver().getLayers();
		encoderLayer = new EncoderLayer(mixControl, name);
		encoderLayer.setEncoderMode(EncoderMode.NONACCELERATED);
		displayLayer = new DisplayLayer("GLOBAL_" + name, mixControl);
		displayLayer.setUsesLevelMeteringInLcd(false);
		buttonLayer = new Layer(layers, name + "_GOBAL_BUTTON_LAYER_" + sectionIndex);
	}

	@Override
	public Layer getFaderLayer() {
		return this.mixControl.getActiveMixGroup().getFaderLayer(ParamElement.VOLUME);
	}

	@Override
	public Layer getButtonLayer() {
		return this.buttonLayer;
	}

	@Override
	public EncoderLayer getEncoderLayer() {
		return encoderLayer;
	}

	@Override
	public DisplayLayer getDisplayLayer(final int which) {
		return displayLayer;
	}

	public void notifyBlink(final int ticks) {
		blinkTicks = ticks;
	}

	public void init(final TrackBank trackBank) {
		final MixerSectionHardware hwControls = mixControl.getHwControls();
		displayLayer.setText(0, "[ShowSends]  Mute=->Drum Mix  Select=Group Fold", false);
		displayLayer.enableFullTextMode(0, true);
		for (int i = 0; i < 8; i++) {
			encoderLayer.addBinding(hwControls.createRingDisplayBinding(i, i == 0 ? 11 : 0, RingDisplayType.FILL_LR_0));
		}
		final ValueObject<MixerMode> mixMode = mixControl.getDriver().getMixerMode();
		final BasicStringValue onOff = new BasicStringValue(toValue(mixMode.get()));
		mixMode.addValueObserver(newMode -> onOff.set(toValue(newMode)));
		displayLayer.bindName(1, 0, onOff);
		final HardwareActionBindable toggleMixModeAction = hwControls.createAction(this::toggleMixMode);
		encoderLayer.addBinding(new ButtonBinding(hwControls.getEncoderPress(0), toggleMixModeAction));
		final DeviceBank[] drumFollowBanks = mixControl.getDrumFollowDeviceBanks();

		final int offset = hwControls.getSectionIndex() * 8;

		for (int i = 0; i < 8; i++) {
			final int index = i;
			final int trackIndex = index + offset;
			final Track track = trackBank.getItemAt(trackIndex);
			track.trackType().markInterested();
			track.isGroup().markInterested();
			track.isGroupExpanded().markInterested();
			final ClipLauncherSlotBank slotBank = track.clipLauncherSlotBank();
			slotBank.setIndication(true);

			final HardwareButton intoDrumButton = hwControls.getButton(2, index);
			buttonLayer.bindPressed(intoDrumButton, () -> intoDrumMode(index, track, drumFollowBanks[index]));
			final OnOffHardwareLight drumLight = (OnOffHardwareLight) intoDrumButton.backgroundLight();
			buttonLayer.bind(() -> lightStateDrum(track, drumFollowBanks[index]), drumLight);

			final HardwareButton groupButton = hwControls.getButton(3, index);
			buttonLayer.bindPressed(groupButton, () -> toggleGroupFold(track));
			final OnOffHardwareLight groupLight = (OnOffHardwareLight) groupButton.backgroundLight();
			buttonLayer.bind(() -> lightStateGroup(track), groupLight);
		}

	}

	private void intoDrumMode(final int index, final Track track, final DeviceBank drumFollow) {
		if (track.exists().get() && track.trackType().get().equals("Instrument") && drumFollow.itemCount().get() > 0) {
			track.selectInMixer();
			final ValueObject<MixerMode> mixMode = mixControl.getDriver().getMixerMode();
			mixMode.set(MixerMode.DRUM);
			mixControl.getDriver().getButtonView().set(ButtonViewState.MIXER);
		}
	}

	private void toggleGroupFold(final Track track) {
		track.isGroupExpanded().toggle();
	}

	private boolean lightStateDrum(final Track track, final DeviceBank drumFollow) {
		return track.trackType().get().equals("Instrument") && drumFollow.itemCount().get() > 0;
	}

	private boolean lightStateGroup(final Track track) {
		if (!track.isGroup().get()) {
			return false;
		}
		if (track.isGroupExpanded().get()) {
			if (blinkTicks % 4 == 0) {
				return false;
			} else {
				return true;
			}
		}
		return true;
	}

	private void toggleMixMode() {
		final ValueObject<MixerMode> mixMode = mixControl.getDriver().getMixerMode();
		switch (mixMode.get()) {
		case MAIN:
			mixMode.set(MixerMode.GLOBAL);
			break;
		case GLOBAL:
			mixMode.set(MixerMode.MAIN);
		default:
			break;
		}
	}

	private String toValue(final MixerMode mixMode) {
		switch (mixMode) {
		case MAIN:
			return "<OFF >";
		case GLOBAL:
			return "<ON  >";
		default:
			return "<DRUM>";
		}
	}

}
