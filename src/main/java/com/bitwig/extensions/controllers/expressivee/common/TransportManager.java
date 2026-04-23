package com.bitwig.extensions.controllers.expressivee.common;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.RelativePosition;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extension.controller.api.HardwareActionBindable;

public class TransportManager extends Manager {

        // ===== LOCAL CONSTANTS =====
        private static final float BPM_SCALING_FACTOR = 12700f; // Osmose high-res BPM steps
        private static final int BPM_STEPS_FOR_FULL_ROTATION = 646;
        private static final long PLAYHEAD_LATENCY = 50;
        private Transport mTransport;
        private ControllerHost mHost;

        private double mPlayheadInc = 0;
        private boolean mPlayheadFlushScheduled = false;

        private RelativeHardwareKnob mKnobMarkerMove;
        private RelativeHardwareKnob mKnobPlayhead;
        private RelativeHardwareKnob mKnobLoopLength;
        private RelativeHardwareKnob mKnobLoopStart;
        private RelativeHardwareKnob mKnobBpmValue;

        private HardwareButton mButtonPlay;
        private HardwareButton mButtonStop;
        private HardwareButton mButtonRecord;
        private HardwareButton mButtonMetronome;
        private HardwareButton mButtonLoopEnable;
        private HardwareButton mButtonBpmTempo;
        private HardwareButton mButtonMarkerAdd;

        private MidiIn mMidiIn;

        public TransportManager(final ControllerHost host, HardwareSurface surface, MidiIn midiIn, MidiOut midiOut) {
                super(midiOut);
                mMidiIn = midiIn;
                mHost = host;
                mTransport = mHost.createTransport();

                mKnobMarkerMove = surface.createRelativeHardwareKnob("MarkerMove");
                mKnobMarkerMove.setLabel("MarkerMove");
                mKnobMarkerMove.setLabelPosition(RelativePosition.ABOVE);
                mKnobMarkerMove.setAdjustValueMatcher(
                                mMidiIn.createRelative2sComplementCCValueMatcher(0, Constant.MARKER_MOVE_CC, 128));

                mKnobPlayhead = surface.createRelativeHardwareKnob("Playhead");
                mKnobPlayhead.setLabel("Playhead");
                mKnobPlayhead.setLabelPosition(RelativePosition.ABOVE);
                mKnobPlayhead
                                .setAdjustValueMatcher(mMidiIn.createRelative2sComplementCCValueMatcher(0,
                                                Constant.PLAYHEAD_CC, 128));

                mKnobLoopLength = surface.createRelativeHardwareKnob("LoopLength");
                mKnobLoopLength.setLabel("LoopLength");
                mKnobLoopLength.setLabelPosition(RelativePosition.ABOVE);
                mKnobLoopLength.setAdjustValueMatcher(
                                mMidiIn.createRelative2sComplementCCValueMatcher(0, Constant.LOOP_LENGTH_CC, 128));

                mKnobLoopStart = surface.createRelativeHardwareKnob("LoopStart");
                mKnobLoopStart.setLabel("LoopStart");
                mKnobLoopStart.setLabelPosition(RelativePosition.ABOVE);
                mKnobLoopStart.setAdjustValueMatcher(
                                mMidiIn.createRelative2sComplementCCValueMatcher(0, Constant.LOOP_START_CC, 128));

                mKnobBpmValue = surface.createRelativeHardwareKnob("BpmValue");
                mKnobBpmValue.setLabel("BpmValue");
                mKnobBpmValue.setLabelPosition(RelativePosition.ABOVE);
                mKnobBpmValue
                                .setAdjustValueMatcher(mMidiIn.createRelative2sComplementCCValueMatcher(0,
                                                Constant.BPM_VALUE_CC, BPM_STEPS_FOR_FULL_ROTATION));

                mButtonPlay = surface.createHardwareButton("Play");
                mButtonPlay.setLabel("Play");
                mButtonPlay.setLabelPosition(RelativePosition.ABOVE);
                mButtonPlay.pressedAction().setActionMatcher(
                                mMidiIn.createCCActionMatcher(Constant.MIDI_CHANNEL_1,
                                                Constant.TRANSPORT_PLAY_PAUSE_CC));

                mButtonRecord = surface.createHardwareButton("Record");
                mButtonRecord.setLabel("Record");
                mButtonRecord.setLabelPosition(RelativePosition.ABOVE);
                mButtonRecord.pressedAction()
                                .setActionMatcher(mMidiIn.createCCActionMatcher(Constant.MIDI_CHANNEL_1,
                                                Constant.TRANSPORT_RECORD_CC));

                mButtonMetronome = surface.createHardwareButton("Metronome");
                mButtonMetronome.setLabel("Metronome");
                mButtonMetronome.setLabelPosition(RelativePosition.ABOVE);
                mButtonMetronome.pressedAction().setActionMatcher(
                                mMidiIn.createCCActionMatcher(Constant.MIDI_CHANNEL_1,
                                                Constant.TRANSPORT_METRONOME_ENABLE_CC));

                mButtonStop = surface.createHardwareButton("Stop");
                mButtonStop.setLabel("Stop");
                mButtonStop.setLabelPosition(RelativePosition.ABOVE);
                mButtonStop.pressedAction().setActionMatcher(mMidiIn.createCCActionMatcher(Constant.MIDI_CHANNEL_1,
                                Constant.TRANSPORT_STOP_CC, Constant.MIDI_VALUE_ON));

                mButtonLoopEnable = surface.createHardwareButton("LoopEnable");
                mButtonLoopEnable.setLabel("LoopEnable");
                mButtonLoopEnable.setLabelPosition(RelativePosition.ABOVE);
                mButtonLoopEnable.pressedAction()
                                .setActionMatcher(mMidiIn.createCCActionMatcher(Constant.MIDI_CHANNEL_1,
                                                Constant.LOOP_ENABLE_CC));

                mButtonBpmTempo = surface.createHardwareButton("BpmTempo");
                mButtonBpmTempo.setLabel("BpmTempo");
                mButtonBpmTempo.setLabelPosition(RelativePosition.ABOVE);
                mButtonBpmTempo.pressedAction()
                                .setActionMatcher(mMidiIn.createCCActionMatcher(Constant.MIDI_CHANNEL_1,
                                                Constant.BPM_TAP_CC));

                mButtonMarkerAdd = surface.createHardwareButton("MarkerAdd");
                mButtonMarkerAdd.setLabel("MarkerAdd");
                mButtonMarkerAdd.setLabelPosition(RelativePosition.ABOVE);
                mButtonMarkerAdd.pressedAction()
                                .setActionMatcher(mMidiIn.createCCActionMatcher(Constant.MIDI_CHANNEL_1,
                                                Constant.ADD_MARKER_CC));

        }

        public void init() {
                mTransport.isPlaying().addValueObserver((boolean isPlaying) -> isPlayingObserver(isPlaying));

                mTransport.isMetronomeEnabled()
                                .addValueObserver((boolean enabled) -> isMetronomeEnabledObserver(enabled));

                mTransport.isArrangerRecordEnabled()
                                .addValueObserver((boolean enabled) -> isRecordEnabledObserver(enabled));

                mTransport.isArrangerLoopEnabled()
                                .addValueObserver((boolean enabled) -> isLoopEnabledObserver(enabled));

                mTransport.tempo().value()
                                .addRawValueObserver((double value) -> sendSysexDoubleAsInt(Constant.SYSEX_BPM_VALUE,
                                                value));

                mTransport.tempo().value().markInterested();

                final RelativeHardwarControlBindable markerMove = mHost.createRelativeHardwareControlAdjustmentTarget(
                                inc -> {
                                        if (inc > 0) {
                                                mTransport.jumpToNextCueMarker();
                                        } else {
                                                mTransport.jumpToPreviousCueMarker();
                                        }
                                });

                mKnobMarkerMove.addBindingWithSensitivity(markerMove, 128);

                final RelativeHardwarControlBindable playHeadMove = mHost.createRelativeHardwareControlAdjustmentTarget(
                                inc -> this.playHeadInc(inc));

                mKnobPlayhead.addBindingWithSensitivity(playHeadMove, 128);
                mKnobLoopLength.addBindingWithSensitivity(mTransport.arrangerLoopDuration(), 128);
                mKnobLoopStart.addBindingWithSensitivity(mTransport.arrangerLoopStart(), 128);
                mKnobBpmValue.addBinding(mTransport.tempo().value());

                mButtonPlay.pressedAction().addBinding(mTransport.playAction());
                mButtonStop.pressedAction().addBinding(mTransport.stopAction());
                mButtonRecord.pressedAction().addBinding(mTransport.isArrangerRecordEnabled().toggleAction());
                mButtonMetronome.pressedAction().addBinding(mTransport.isMetronomeEnabled().toggleAction());
                mButtonLoopEnable.pressedAction().addBinding(mTransport.isArrangerLoopEnabled().toggleAction());
                mButtonBpmTempo.pressedAction().addBinding(mTransport.tapTempoAction());
                mButtonMarkerAdd.pressedAction().addBinding(mTransport.addCueMarkerAtPlaybackPositionAction());
        }

        private void playHeadInc(double inc) {
                mPlayheadInc += inc;
                if (!mPlayheadFlushScheduled) {
                        mHost.scheduleTask(this::flushPlayheadInc, PLAYHEAD_LATENCY);
                }
        }

        private void flushPlayheadInc() {
                if (mPlayheadInc != 0) {
                        mTransport.getPosition().inc(mPlayheadInc);
                        mPlayheadInc = 0;
                }
                mPlayheadFlushScheduled = false;
        }

        private void isLoopEnabledObserver(boolean enabled) {
                sendCC(Constant.MIDI_CHANNEL_1, Constant.LOOP_ENABLE_CC,
                                enabled ? Constant.MIDI_VALUE_ON : Constant.MIDI_VALUE_OFF);
        }

        private void isPlayingObserver(boolean isPlaying) {
                sendCC(Constant.MIDI_CHANNEL_1, Constant.TRANSPORT_PLAY_PAUSE_CC,
                                isPlaying ? Constant.MIDI_VALUE_ON : Constant.MIDI_VALUE_OFF);
        }

        private void isMetronomeEnabledObserver(boolean enabled) {
                sendCC(Constant.MIDI_CHANNEL_1, Constant.TRANSPORT_METRONOME_ENABLE_CC,
                                enabled ? Constant.MIDI_VALUE_ON : Constant.MIDI_VALUE_OFF);
        }

        private void isRecordEnabledObserver(boolean enabled) {
                sendCC(Constant.MIDI_CHANNEL_1, Constant.TRANSPORT_RECORD_CC,
                                enabled ? Constant.MIDI_VALUE_ON : Constant.MIDI_VALUE_OFF);
        }
}
