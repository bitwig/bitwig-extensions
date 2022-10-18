package com.bitwig.extensions.controllers.arturia.minilab3;

import java.util.LinkedList;
import java.util.Queue;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiOut;

public class SysExHandler {

    public static final String MINILAB_PREFIX = "f000206b7f42020040";
    public static final String ARTURIA_CLEAR_SCREEN = "f0 00 20 6B 7f 42 04 01 60 0a 0a 5f 51 00 f7";

    public enum GeneralMode {
        DAW_MODE,
        ANALOG_LAB
    }

    final byte[] ARTURIA_MODE_BANK_UPDATE = {//
            (byte) 0xF0, 0x00, 0x20, 0x6B, 0x7F, 0x42, 0x04, 0x01, 0x16, //
            0x00, // Bank Id - 9
            0x00, 0x00, 0x28, // Pad 1 10
            0x00, 0x00, 0x28, // Pad 2 13
            0x00, 0x00, 0x28, // Pad 3 16
            0x00, 0x00, 0x28, // Pad 4 19
            0x00, 0x00, 0x28, // Pad 5 22
            0x00, 0x00, 0x28, // Pad 6 25
            0x00, 0x00, 0x28, // Pad 7 28
            0x00, 0x00, 0x28, // Pad 8 31
            (byte) 0xF7};

    private final byte[] RGB_PAD_COMMAND = {(byte) 0xF0, 0x00, 0x20, 0x6B, 0x7F, 0x42, 0x02, //
            0x02, // 7 - Patch Id
            0x16, // 8 - Command
            0x02, // 9 - Pad ID
            0x00, // 10 - Red
            0x00, // 11 - Green
            0x00, // 12 - blue
            (byte) 0xF7};

    private final byte[] RGB_BANK_COMMAND = {//
            (byte) 0xF0, 0x00, 0x20, 0x6B, 0x7F, 0x42, 0x04, 0x02, 0x16, //
            0x00, // Bank Id - 9
            0x00, 0x00, 0x00, // Pad 1 10
            0x00, 0x00, 0x00, // Pad 2 13
            0x00, 0x00, 0x00, // Pad 3 16
            0x00, 0x00, 0x00, // Pad 4 19
            0x00, 0x00, 0x00, // Pad 5 22
            0x00, 0x00, 0x00, // Pad 6 25
            0x00, 0x00, 0x00, // Pad 7 28
            0x00, 0x00, 0x00, // Pad 8 31
            (byte) 0xF7};

    private final MidiOut midiOut;
    private final ControllerHost host;
    private String lastExpression = "";
    private final byte[][] padCommands = new byte[128][14];
    private final byte[][] bankCommands = new byte[2][35];
    private boolean toConsole;
    private final int delaySysex = 2;
    private boolean processingReady = false;
    private final Queue<Runnable> preprocessTasks = new LinkedList<>();
    private final RgbBankLightState[] lastBankState = new RgbBankLightState[2];
    private GeneralMode mode;

    public SysExHandler(final MidiOut midiOut, final ControllerHost host) {
        this.midiOut = midiOut;
        this.host = host;

        for (int i = 0; i < padCommands.length; i++) {
            System.arraycopy(RGB_PAD_COMMAND, 0, padCommands[i], 0, RGB_PAD_COMMAND.length);
            padCommands[i][9] = (byte) i;
        }
        for (int i = 0; i < bankCommands.length; i++) {
            System.arraycopy(RGB_BANK_COMMAND, 0, bankCommands[i], 0, RGB_BANK_COMMAND.length);
        }
    }

    public void enableProcessing() {
        processingReady = true;
        while (!preprocessTasks.isEmpty()) {
            preprocessTasks.poll().run();
        }
    }

    public void setConsole(final boolean active) {
        toConsole = active;
    }

    public void deviceInquiry() {
        midiOut.sendSysex("f0 7e 7f 06 01 f7"); // Universal Request
    }

    public void requestInitState() {
        midiOut.sendSysex("f0 00 20 6B 7f 42 02 02 40 6A 21 f7");
        pause(3);
        midiOut.sendSysex("f0 00 20 6B 7f 42 01 00 40 03 f7"); // REQUEST PAD
        pause(3);
        midiOut.sendSysex("f0 00 20 6B 7f 42 01 00 40 01 f7"); // REQUEST MODE
        pause(3);
    }

    public void disconnectState() {
        midiOut.sendSysex("f0 00 20 6B 7f 42 02 02 40 6A 20 f7");
        pause(20);
    }

    public void sendSysex(final String sysExExpression) {
        if (!lastExpression.equals(sysExExpression)) {
            midiOut.sendSysex(sysExExpression);
            lastExpression = sysExExpression;
            if (toConsole) {
                host.println(String.format("OLED : %s ", sysExExpression));
            }
            pause(delaySysex);
        }
    }

    public void sendBankState(final RgbBankLightState state) {
        final PadBank bank = state.getBank();
        final byte[] bankCommand = bankCommands[bank.getIndex()];
        bankCommand[9] = (byte) bank.getCommandId();
        System.arraycopy(state.getColors(), 0, bankCommand, 10, state.getColors().length);
        lastBankState[bank.getIndex()] = state;
        if (processingReady) {
            midiOut.sendSysex(bankCommand);
        } else {
            preprocessTasks.add(() -> midiOut.sendSysex(bankCommand));
        }
        if (toConsole) {
            host.println(String.format("SysEx => PAD Bank %s : changed %s", bank, byteArrayToHex(bankCommand)));
        }
    }

    public void sendColorBytes(final int padId, final byte red, final byte green, final byte blue) {
        final byte[] command = padCommands[padId];
        command[10] = red;
        command[11] = green;
        command[12] = blue;
        midiOut.sendSysex(command);
        if (toConsole) {
            host.println(String.format("SysEx => PAD Single padId=%d r=%02X g=%02X b=%02X %s", padId, red, green, blue,
                    byteArrayToHex(command)));
        }
        pause(2);
    }

    public void sendColor(final int padId, final PadBank bank, final byte red, final byte green, final byte blue) {
        sendColorBytes(padId, red, green, blue);
    }

    public static String byteArrayToHex(final byte[] a) {
        final StringBuilder sb = new StringBuilder(a.length * 2);
        for (final byte b : a) {
            sb.append(String.format(" %02x", b));
        }
        return sb.toString();
    }

    private void pause(final int milis) {
        try {
            Thread.sleep(milis);
        } catch (final InterruptedException e) {
            //
        }
    }

    /**
     * Invoke change for Arturia Mode.
     *
     * @param mode          cursor Track is connected to Analog Lab V  (GeneralMode.ANALOG_LAB) or not (GeneralMode.DAW)
     * @param inArturiaMode if controller itself is in Arturia Mode or Daw Mode
     */
    public void fireArturiaMode(final GeneralMode mode, final boolean inArturiaMode) {
        if (this.mode == mode) {
            return;
        }
        if (mode == GeneralMode.DAW_MODE) {
            midiOut.sendSysex(ARTURIA_CLEAR_SCREEN);
        }
        final String command = String.format("f0 00 20 6B 7f 42 02 02 40 6A %02x f7",
                mode == GeneralMode.ANALOG_LAB ? 0x11 : 0x10);
        midiOut.sendSysex(command);
        if (mode == GeneralMode.ANALOG_LAB) {
            // Not needed with upcoming FW
            midiOut.sendSysex(ARTURIA_MODE_BANK_UPDATE);
        } else if (!inArturiaMode) {
            updateBankState(0);
            updateBankState(1);
        }
        this.mode = mode;
    }

    public void updateBankState(final int index) {
        if (index == -1 || index >= lastBankState.length) {
            return;
        }
        if (lastBankState[index] != null) {
            sendBankState(lastBankState[index]);
        }
    }

}
