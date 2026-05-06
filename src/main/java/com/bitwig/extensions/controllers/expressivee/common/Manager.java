package com.bitwig.extensions.controllers.expressivee.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.bitwig.extension.controller.api.MidiOut;

public class Manager {

    private MidiOut mMidiOut;

    public Manager(MidiOut midiOut) {
        mMidiOut = midiOut;
    }

    protected void sendCC(int channel, int id, int value) {
        mMidiOut.sendMidi(Constant.MIDI_CC_STATUS_BYTE | channel, id, value);
    }

    public void sendSysexConnectionInfos(boolean connected) {
        String dawName = "Bitwig";
        try {
            final ByteArrayOutputStream msg = new ByteArrayOutputStream();
            final int stringLengthWithNull = dawName.length() + 1;
            final int totalPayloadLength = stringLengthWithNull + 3; // +3 for length byte + version + status

            msg.write(totalPayloadLength);
            msg.write(stringLengthWithNull);
            msg.write(dawName.getBytes(StandardCharsets.UTF_8));
            msg.write(Constant.SYSEX_STRING_TERMINATOR);
            msg.write(Constant.DAW_PROTOCOL_VERSION);
            msg.write(connected ? 1 : 0);
            sendSysex(Constant.SYSEX_CONNECTION, msg.toByteArray());
        } catch (IOException e) {
        }
    }

    protected void sendSysexDouble(int cmd, double value) {
        sendSysexString(cmd, String.format("%.2f", value));
    }

    protected void sendSysexDoubleAsInt(int cmd, double value) {
        sendSysexString(cmd, String.format("%.0f", value));
    }

    protected void sendSysexString(int cmd, String string) {
        String normalizedString = "";

        if (string != null && !string.isEmpty()) {
            normalizedString = Helper.normalize(string);

            if (cmd == Constant.SYSEX_TRACK_SEND_A ||
                    cmd == Constant.SYSEX_TRACK_SEND_B ||
                    cmd == Constant.SYSEX_TRACK_VOLUME) {
                normalizedString = normalizedString.replaceAll("(?i)db", "dB");
            }
        }

        try {
            final ByteArrayOutputStream stream = new ByteArrayOutputStream();
            stream.write(normalizedString.length() + 1); // +1 for "\0"

            if (!normalizedString.isEmpty()) {
                stream.write(normalizedString.getBytes(StandardCharsets.UTF_8));
            }

            stream.write(Constant.SYSEX_STRING_TERMINATOR);

            sendSysex(cmd, stream.toByteArray());
        } catch (IOException e) {
        }
    }

    protected void sendSysex(int cmd, byte[] stream) {
        final ByteArrayOutputStream msg = new ByteArrayOutputStream();
        try {
            msg.write(Constant.SYSEX_HEADER);
            msg.write((cmd >> 8) & 0xFF);
            msg.write(cmd & 0xFF);
            msg.write(stream);
            msg.write(Constant.MIDI_SYSEX_END);
            mMidiOut.sendSysex(msg.toByteArray());
        } catch (IOException e) {

        }
    }
}
