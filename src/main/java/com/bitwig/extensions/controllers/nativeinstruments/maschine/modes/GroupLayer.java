package com.bitwig.extensions.controllers.nativeinstruments.maschine.modes;

import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineExtension;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineLayer;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.ModifierState;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.NIColorUtil;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.RgbLedState;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.buttons.GroupButton;

/**
 * this layer uses Maschines Group Buttons A-H to control 8 tracks.
 */
public class GroupLayer extends MaschineLayer {
	private final MaschineLayer shiftLayer;
	private final MaschineLayer armLayer;
	private final MaschineLayer eraseLayer;
	private final MaschineLayer duplicateLayer;
	private final MaschineLayer muteLayer;
	private final MaschineLayer soloLayer;
	private final MaschineLayer colorChooseLayer;

	int selectedTrackIndex = -1;
	private ModifierState state = ModifierState.NONE;
	int numberOfTracks = 0;
	int scrollPosition = 0;

	boolean shiftDown = false;

	public GroupLayer(final MaschineExtension driver, final String name) {
		super(driver, name);
		armLayer = new MaschineLayer(driver, "select-" + name);
		eraseLayer = new MaschineLayer(driver, "clear-" + name);
		duplicateLayer = new MaschineLayer(driver, "duplicate-" + name);
		muteLayer = new MaschineLayer(driver, "mute-" + name);
		soloLayer = new MaschineLayer(driver, "solo-" + name);
		shiftLayer = new MaschineLayer(driver, "shift-" + name);
		colorChooseLayer = new MaschineLayer(driver, "variation-" + name);

		final TrackBank trackBank = driver.getMixerTrackBank();
		trackBank.channelCount().addValueObserver(v -> {
			numberOfTracks = v;
		});
		trackBank.scrollPosition().addValueObserver(v -> {
			scrollPosition = v;
			if (scrollPosition % 8 != 0) {
				trackBank.scrollIntoView(v / 8 * 8);
			}
		});

		trackBank.setChannelScrollStepSize(1);

		final GroupButton[] buttons = driver.getGroupButtons();

		for (int i = 0; i < trackBank.getSizeOfBank(); i++) {
			final int index = i;
			final Track track = trackBank.getItemAt(i);
			final GroupButton button = buttons[i];

			bindPressed(button, () -> handleSelect(track, index));

			shiftLayer.bindPressed(button, () -> handleTrackOverview(track, index));
			armLayer.bindPressed(button, () -> handleArm(track, index));
			eraseLayer.bindPressed(button, () -> handleErase(track));
			duplicateLayer.bindPressed(button, () -> handleDuplicate(track));
			soloLayer.bindPressed(button, () -> handleSolo(track));
			muteLayer.bindPressed(button, () -> handleMute(track));
			colorChooseLayer.bindPressed(button, () -> handleColorSelection(track));
			bindLightState(() -> computeGridLedState(track, index), button);
			track.addIsSelectedInEditorObserver(v -> {
				notifySelectedInmixer(index, track, v);
			});
//			track.color().addValueObserver((red, green, blue) -> {
//				final int rv = (int) Math.floor(red * 255);
//				final int gv = (int) Math.floor(green * 255);
//				final int bv = (int) Math.floor(blue * 255);
//				final int lookupIndex = rv << 16 | gv << 8 | bv;
//			});
		}
	}

	private void handleTrackOverview(final Track track, final int index) {
		if (numberOfTracks < 8) {
			handleSelect(track, index);
		} else {
			final int trackOffset = index * 8;
			if (trackOffset < numberOfTracks) {
				getDriver().getMixerTrackBank().scrollBy(trackOffset - scrollPosition);
			} else if (numberOfTracks % 8 == 0 && trackOffset == numberOfTracks) {
				getDriver().getApplication().createInstrumentTrack(-1);
				getDriver().getMixerTrackBank().scrollIntoView(trackOffset + 8);
			}

		}
	}

	private void notifySelectedInmixer(final int index, final Track track, final boolean selected) {
		if (selected) {
			selectedTrackIndex = index;
		} else if (selectedTrackIndex == index) {
			selectedTrackIndex = -1;
		}
	}

	private MaschineLayer getLayer(final ModifierState modstate) {
		switch (modstate) {
		case SHIFT:
			return shiftLayer;
		case DUPLICATE:
			return duplicateLayer;
		case ARM:
			return armLayer;
		case ERASE:
			return eraseLayer;
		case SOLO:
			return soloLayer;
		case MUTE:
			return muteLayer;
		case VARIATION:
			return colorChooseLayer;
		default:
			return null;
		}
	}

	private void updateState(final ModifierState state, final boolean active) {
		if (active) {
			this.state = state;
		} else if (this.state == state) {
			this.state = ModifierState.NONE;
		}
	}

	public void setModifierState(final ModifierState modstate, final boolean active) {
		final MaschineLayer layer = getLayer(modstate);
		shiftDown = modstate == ModifierState.SHIFT;
		updateState(modstate, active);
		if (layer != null) {
			if (active) {
				layer.activate();
			} else {
				layer.deactivate();
			}
		}
	}

	private void handleArm(final Track track, final int index) {
		track.arm().toggle();
	}

	private void handleSelect(final Track track, final int index) {
		if (scrollPosition + index == numberOfTracks && state != ModifierState.SELECT) {
			getDriver().getApplication().createInstrumentTrack(-1);
		} else {
			track.selectInEditor();
		}
	}

	private void handleErase(final Track track) {
		track.deleteObject();
	}

	private void handleDuplicate(final Track track) {
		track.duplicate();
	}

	private void handleSolo(final Track track) {
		track.solo().toggle();
	}

	private void handleMute(final Track track) {
		track.mute().toggle();
	}

	private void handleColorSelection(final Track track) {
		if (track.exists().get()) {
			getDriver().enterColorSelection(color -> {
				color.set(track.color());
			});
		}
	}

	private InternalHardwareLightState computeGridLedState(final Track track, final int index) {
		assert track.isSubscribed();

		if (state == ModifierState.SHIFT) {
			if (numberOfTracks < 8) {
				return trackToRgbLed(track, index);
			} else {
				final int trackOffset = index * 8;
				if (trackOffset < numberOfTracks) {
					if (scrollPosition >= trackOffset && scrollPosition < trackOffset + 8) {
						return RgbLedState.GROUP_TRACK_ACTIVE;
					}
					return RgbLedState.GROUP_TRACK_EXISTS;
				} else if (numberOfTracks % 8 == 0 && trackOffset == numberOfTracks) {
					return RgbLedState.CREATE_TRACK;
				}
			}
			return RgbLedState.OFF;
		}
		if (!track.exists().get() && state != ModifierState.SELECT) {
			if (scrollPosition + index == numberOfTracks) {
				return RgbLedState.CREATE_TRACK;
			}
			return RgbLedState.OFF;
		} else if (state == ModifierState.MUTE) {
			return track.mute().get() ? RgbLedState.TRACK_OFF : RgbLedState.TRACK_ON;
		} else if (state == ModifierState.SOLO) {
			return track.solo().get() ? RgbLedState.SOLO_ON : RgbLedState.SOLO_OFF;
		} else if (state == ModifierState.ARM) {
			return track.arm().get() ? RgbLedState.ARM_ON : RgbLedState.ARM_OFF;
		}
		return trackToRgbLed(track, index);
	}

	private InternalHardwareLightState trackToRgbLed(final Track track, final int index) {
		int color = NIColorUtil.convertColor(track.color());
		if (index == selectedTrackIndex) {
			color += 2;
		}
		return RgbLedState.colorOf(color);
	}

	@Override
	protected void onActivate() {
		super.onActivate();
		final TrackBank trackBank = getDriver().getMixerTrackBank();

		for (int i = 0; i < 8; ++i) {
			final Track track = trackBank.getItemAt(i);
			track.subscribe();
		}
	}

	@Override
	protected void onDeactivate() {
		super.onDeactivate();
		final TrackBank trackBank = getDriver().getMixerTrackBank();

		for (int i = 0; i < 8; ++i) {
			final Track track = trackBank.getItemAt(i);
			track.unsubscribe();
		}
	}

}
