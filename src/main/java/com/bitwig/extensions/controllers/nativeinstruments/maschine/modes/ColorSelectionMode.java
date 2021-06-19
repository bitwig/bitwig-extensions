package com.bitwig.extensions.controllers.nativeinstruments.maschine.modes;

import java.util.function.Consumer;

import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.BitWigColor;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.MaschineExtension;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.NIColorUtil;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.RgbLedState;
import com.bitwig.extensions.controllers.nativeinstruments.maschine.buttons.PadButton;

public class ColorSelectionMode extends PadMode {

	private static final BitWigColor[] PALETTE = new BitWigColor[] { BitWigColor.RED, BitWigColor.ORANGE,
			BitWigColor.AMBER, BitWigColor.YELLOW, //
			BitWigColor.OLIVE, BitWigColor.GREEN, BitWigColor.GREEN_BLUE, BitWigColor.AQUA, //
			BitWigColor.LIGHT_BLUE, BitWigColor.DARK_BLUE, BitWigColor.PLUM, BitWigColor.PURPLE, //
			BitWigColor.PINK_PURPLE, BitWigColor.LIGHT_BROWN, BitWigColor.PINK, BitWigColor.GRAY, //
	};
	private PadMode returnMode;

	private Consumer<BitWigColor> colorAction;

	public ColorSelectionMode(final MaschineExtension driver) {
		super(driver, "PAD_COLOR_SECTION_MODE");
		final PadButton[] buttons = driver.getPadButtons();
		for (int i = 0; i < 16; ++i) {
			final int index = i;
			final PadButton button = buttons[i];

			bindPressed(button, () -> {
				if (colorAction != null) {
					colorAction.accept(PALETTE[index]);
				}
				driver.cancelColorMode();
			});

			bindLightState(() -> computeGridLedState(index), button);
		}
	}

	private InternalHardwareLightState computeGridLedState(final int index) {
		final int color = NIColorUtil.convertColor(PALETTE[index]);

		return RgbLedState.colorOf(color + 2);
	}

	public void setColorAction(final Consumer<BitWigColor> colorAction) {
		this.colorAction = colorAction;
	}

	public void setReturnMode(final PadMode returnMode) {
		this.returnMode = returnMode;
	}

	public PadMode getReturnMode() {
		return returnMode;
	}

	@Override
	protected void onActivate() {
		super.onActivate();
	}

	@Override
	protected String getModeDescription() {
		return "COLOR_SELECTION";
	}

}
