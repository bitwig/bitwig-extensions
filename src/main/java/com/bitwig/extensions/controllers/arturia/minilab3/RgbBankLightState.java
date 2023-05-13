package com.bitwig.extensions.controllers.arturia.minilab3;

import java.util.Arrays;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;

public class RgbBankLightState extends InternalHardwareLightState {

    private final byte[] colors;
    private final PadBank bank;

    public static class Handler {
        private final byte[] buffer = new byte[24];
        private final RgbButton[] buttons;
        private final PadBank bank;
        private RgbBankLightState lastState;

        public Handler(final PadBank bank, final RgbButton[] buttons) {
            this.buttons = buttons;
            this.bank = bank;
            final byte[] colors = new byte[24];
            calcColors(colors);
            System.arraycopy(colors, 0, buffer, 0, 24);
            lastState = new RgbBankLightState(bank, colors);
        }

        public InternalHardwareLightState getBankLightState() {
            calcColors(buffer);
            if (!lastState.equalsColorData(buffer)) {
                lastState = new RgbBankLightState(bank, buffer);
            }
            return lastState;
        }

        private void calcColors(final byte[] colorArray) {
            for (int i = 0; i < buttons.length; i++) {
                final InternalHardwareLightState state = buttons[i].getLight().state().currentValue();
                if (state instanceof RgbLightState) {
                    final RgbLightState colorState = (RgbLightState) state;
                    colorArray[i * 3] = colorState.getRed();
                    colorArray[i * 3 + 1] = colorState.getGreen();
                    colorArray[i * 3 + 2] = colorState.getBlue();
                }
            }
        }
    }

    RgbBankLightState(final PadBank bank, final byte[] colors) {
        this.bank = bank;
        this.colors = new byte[24];
        System.arraycopy(colors, 0, this.colors, 0, 24);
    }

    public PadBank getBank() {
        return bank;
    }

    public byte[] getColors() {
        return colors;
    }

    boolean equalsColorData(final byte[] colors) {
        return Arrays.equals(this.colors, colors);
    }

    @Override
    public HardwareLightVisualState getVisualState() {
        return null;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof RgbBankLightState) {
            final RgbBankLightState other = (RgbBankLightState) obj;
            return other.bank == bank && Arrays.equals(other.colors, colors);
        }
        return false;
    }
}
