package com.bitwig.extensions.controllers.expressivee.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;

import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.ChainSelector;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorClip;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorDeviceFollowMode;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DeviceLayer;
import com.bitwig.extension.controller.api.DeviceLayerBank;
import com.bitwig.extension.controller.api.DeviceMatcher;
import com.bitwig.extension.controller.api.MasterTrack;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.RelativePosition;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareActionBindable;

public class TrackManager extends Manager {
        // *=============================================*//
        // * PRIVATE CONSTANTS *//
        // *=============================================*//

        private static final int TRACK_SENDS_COUNT = 2;
        private static final int TRACK_SCENES_COUNT = 8;

        private static final String CTRL_E_ID = "ABCDEF019182FAEB4578707243747265";
        private static final String INSTRUMENT_LAYER_ID = "5024be2e-65d6-4d40-bbfe-8b2ea993c445";
        private static final String INSTRUMENT_SELECTOR_ID = "9588fbcf-721a-438b-8555-97e4231f7d2c";

        private static final int SEND_A_ID = 0;
        private static final int SEND_B_ID = 1;

        // Used to remove volume unit suffix as it is managed in Osmose FW
        private static final String VOLUME_DB_SUFFIX = " dB";

        // *=============================================*//
        // * PRIVATE CUSTOM VARIABLES *//
        // *=============================================*//

        // Used for CE only feature (Master volume control)
        private boolean mIsCEProduct;

        // Used for clip selection synchronization on track navigation
        private int mSelectedClipIndex;

        private boolean mActivateCtrlEMode;
        private boolean mInstrumentLayerExists;
        private boolean mInstrumentSelectorExists;

        // *=============================================*//
        // * PRIVATE BITWIG OBJECTS *//
        // *=============================================*//

        // Application object used to get acces to panel focus and highlight device on
        // track change
        private Application mApplication;

        // Used for task scheduling
        private ControllerHost mHost;

        // CursorTrack object used to follow track currently selected in Bitwig
        private CursorTrack mSelectedTrackCursor;

        // CursorDevice object used to follow instrument currently selected on track
        // currently selected in Bitwig
        private CursorDevice mSelectedDeviceCursor;

        // Device to check if Ctrl-E is detected in the current track
        private Device mCtrlEOnTrackDevice;

        // DeviceMatcher, DeviceBank and Device objects used to find Instrument Layer
        // instances on a track
        private Device mInstrumentLayerInstance;
        private DeviceLayerBank mInstrumentLayerLayerBank;
        private static final int MAX_LAYERS = 16;
        private DeviceLayer[] mInstrumentLayerLayers;

        private Device[] mCtrlEInInstrumentLayerInstances;

        // private Device mInstrumentSelectorInstance;
        // ChainSelector, DeviceLayer, DeviceBank and Device
        private Device mCtrlEActiveInInstrumentSelector;

        // CursorRemoteControlsPage object used to send infos related to the macros of
        // the selected device
        private CursorRemoteControlsPage mSelectedDeviceRemoteControlsPage;

        // ClipLauncherSlotBank object used to launch clip associated with selected
        // track
        private ClipLauncherSlotBank mSelectedTrackClipLauncher;

        // SendBank object used to send infos related to the sends of the selected track
        private SendBank mSelectedTrackSendBank;

        // MasterTrack object used to adjust master volume
        private MasterTrack mMasterTrack;

        private HardwareSurface mSurface;
        private MidiIn mMidiIn;

        private RelativeHardwareKnob mKnobMacro1;
        private RelativeHardwareKnob mKnobMacro2;
        private RelativeHardwareKnob mKnobMacro3;
        private RelativeHardwareKnob mKnobMacro4;
        private RelativeHardwareKnob mKnobMacro5;
        private RelativeHardwareKnob mKnobMacro6;
        private RelativeHardwareKnob mKnobMacro7;
        private RelativeHardwareKnob mKnobMacro8;
        private RelativeHardwareKnob mKnobTrackNav;
        private RelativeHardwareKnob mKnobSceneNav;
        private RelativeHardwareKnob mKnobTrackVol;
        private RelativeHardwareKnob mKnobTrackPan;
        private RelativeHardwareKnob mKnobTrackSendA;
        private RelativeHardwareKnob mKnobTrackSendB;
        private RelativeHardwareKnob mKnobTrackDeviceNav;

        private AbsoluteHardwareKnob mKnobMasterTrackVolume;

        private HardwareButton mButtonClipLaunch;
        private HardwareButton mButtonTrackMute;
        private HardwareButton mButtonTrackSolo;
        private HardwareButton mButtonOpenGui;

        // *=============================================*//
        // * PUBLIC METHODS *//
        // *=============================================*//

        public TrackManager(final ControllerHost host, HardwareSurface surface, MidiIn midiIn, MidiOut midiOut,
                        Boolean isCEProduct) {
                super(midiOut);

                mHost = host;
                mSurface = surface;
                mMidiIn = midiIn;

                /** Init private custom variables */
                mIsCEProduct = isCEProduct;
                mSelectedClipIndex = 0;
                mActivateCtrlEMode = false;
                mInstrumentLayerExists = false;
                mInstrumentSelectorExists = false;

                /** Init private Bitwig objects */

                mApplication = host.createApplication();

                mSelectedTrackCursor = host.createCursorTrack(TRACK_SENDS_COUNT, TRACK_SCENES_COUNT);

                // Find Ctrl-E instance on track
                final DeviceMatcher CtrlEMatcher = host.createVST3DeviceMatcher(CTRL_E_ID);
                final DeviceBank CtrlEOnTrackBank = mSelectedTrackCursor.createDeviceBank(1);
                CtrlEOnTrackBank.setDeviceMatcher(CtrlEMatcher);
                mCtrlEOnTrackDevice = CtrlEOnTrackBank.getItemAt(0);
                mCtrlEOnTrackDevice.exists().markInterested();

                // Find Ctrl-E instance in Instrument Layer
                final DeviceMatcher instrumentLayerMatcher = host.createBitwigDeviceMatcher(
                                java.util.UUID.fromString(INSTRUMENT_LAYER_ID));
                final DeviceBank instrumentLayerBank = mSelectedTrackCursor.createDeviceBank(1);
                instrumentLayerBank.setDeviceMatcher(instrumentLayerMatcher);
                mInstrumentLayerInstance = instrumentLayerBank.getItemAt(0);
                mInstrumentLayerInstance.exists().markInterested();

                final CursorDevice instrumentLayerCursor = mSelectedTrackCursor.createCursorDevice(
                                "RackDetector",
                                "Detector for the unique rack",
                                0,
                                CursorDeviceFollowMode.FIRST_INSTRUMENT);
                instrumentLayerCursor.exists().markInterested();
                instrumentLayerCursor.hasLayers().markInterested();
                mInstrumentLayerLayerBank = instrumentLayerCursor.createLayerBank(MAX_LAYERS);
                mInstrumentLayerLayerBank.itemCount().markInterested();
                mInstrumentLayerLayers = new DeviceLayer[MAX_LAYERS];
                final DeviceBank[] ctrlEInInstrumentLayerBanks = new DeviceBank[MAX_LAYERS];
                mCtrlEInInstrumentLayerInstances = new Device[MAX_LAYERS];
                for (int i = 0; i < MAX_LAYERS; i++) {
                        mInstrumentLayerLayers[i] = mInstrumentLayerLayerBank.getItemAt(i);
                        mInstrumentLayerLayers[i].exists().markInterested();

                        ctrlEInInstrumentLayerBanks[i] = mInstrumentLayerLayers[i].createDeviceBank(1);
                        ctrlEInInstrumentLayerBanks[i].setDeviceMatcher(CtrlEMatcher);

                        mCtrlEInInstrumentLayerInstances[i] = ctrlEInInstrumentLayerBanks[i].getItemAt(0);
                        mCtrlEInInstrumentLayerInstances[i].exists().markInterested();
                }

                // Find active Ctrl-E instance in Instrument Selector
                final DeviceMatcher instrumentSelectorMatcher = host.createBitwigDeviceMatcher(
                                java.util.UUID.fromString(INSTRUMENT_SELECTOR_ID));
                final DeviceBank instrumentSelectorBank = mSelectedTrackCursor.createDeviceBank(1);
                instrumentSelectorBank.setDeviceMatcher(instrumentSelectorMatcher);
                final Device instrumentSelectorInstance = instrumentSelectorBank.getItemAt(0);
                final ChainSelector chainSelector = instrumentSelectorInstance.createChainSelector();
                final DeviceLayer instrumentSelectorActiveChain = chainSelector.activeChain();
                final DeviceBank instrumentSelectorActiveDeviceBank = instrumentSelectorActiveChain.createDeviceBank(1);
                instrumentSelectorActiveDeviceBank.setDeviceMatcher(CtrlEMatcher);
                mCtrlEActiveInInstrumentSelector = instrumentSelectorActiveDeviceBank.getItemAt(0);
                mCtrlEActiveInInstrumentSelector.exists().markInterested();
                instrumentSelectorInstance.exists().markInterested();

                mSelectedDeviceCursor = mSelectedTrackCursor.createCursorDevice(
                                "DeviceCursor",
                                "Cursor for devices on selected track",
                                0,
                                CursorDeviceFollowMode.FOLLOW_SELECTION);
                mSelectedDeviceCursor.exists().markInterested();
                mSelectedDeviceCursor.name().markInterested();

                mSelectedDeviceRemoteControlsPage = mSelectedDeviceCursor
                                .createCursorRemoteControlsPage(Constant.MACRO_COUNT);

                mSelectedTrackClipLauncher = mSelectedTrackCursor.clipLauncherSlotBank();

                mSelectedTrackSendBank = mSelectedTrackCursor.sendBank();

                mMasterTrack = host.createMasterTrack(0);

                host.createArranger().isClipLauncherVisible().set(true);

                mKnobMacro1 = surface.createRelativeHardwareKnob("Macro1");
                mKnobMacro1.setLabel("Macro1");
                mKnobMacro1.setLabelPosition(RelativePosition.ABOVE);
                mKnobMacro1.setAdjustValueMatcher(
                                mMidiIn.createRelative2sComplementCCValueMatcher(0, Constant.MACRO1_CC, 128));

                mKnobMacro2 = surface.createRelativeHardwareKnob("Macro2");
                mKnobMacro2.setLabel("Macro2");
                mKnobMacro2.setLabelPosition(RelativePosition.ABOVE);
                mKnobMacro2.setAdjustValueMatcher(
                                mMidiIn.createRelative2sComplementCCValueMatcher(0, Constant.MACRO2_CC, 128));

                mKnobMacro3 = surface.createRelativeHardwareKnob("Macro3");
                mKnobMacro3.setLabel("Macro3");
                mKnobMacro3.setLabelPosition(RelativePosition.ABOVE);
                mKnobMacro3.setAdjustValueMatcher(
                                mMidiIn.createRelative2sComplementCCValueMatcher(0, Constant.MACRO3_CC, 128));

                mKnobMacro4 = surface.createRelativeHardwareKnob("Macro4");
                mKnobMacro4.setLabel("Macro4");
                mKnobMacro4.setLabelPosition(RelativePosition.ABOVE);
                mKnobMacro4.setAdjustValueMatcher(
                                mMidiIn.createRelative2sComplementCCValueMatcher(0, Constant.MACRO4_CC, 128));

                mKnobMacro5 = surface.createRelativeHardwareKnob("Macro5");
                mKnobMacro5.setLabel("Macro5");
                mKnobMacro5.setLabelPosition(RelativePosition.ABOVE);
                mKnobMacro5.setAdjustValueMatcher(
                                mMidiIn.createRelative2sComplementCCValueMatcher(0, Constant.MACRO5_CC, 128));

                mKnobMacro6 = surface.createRelativeHardwareKnob("Macro6");
                mKnobMacro6.setLabel("Macro6");
                mKnobMacro6.setLabelPosition(RelativePosition.ABOVE);
                mKnobMacro6.setAdjustValueMatcher(
                                mMidiIn.createRelative2sComplementCCValueMatcher(0, Constant.MACRO6_CC, 128));

                mKnobMacro7 = surface.createRelativeHardwareKnob("Macro7");
                mKnobMacro7.setLabel("Macro7");
                mKnobMacro7.setLabelPosition(RelativePosition.ABOVE);
                mKnobMacro7.setAdjustValueMatcher(
                                mMidiIn.createRelative2sComplementCCValueMatcher(0, Constant.MACRO7_CC, 128));

                mKnobMacro8 = surface.createRelativeHardwareKnob("Macro8");
                mKnobMacro8.setLabel("Macro8");
                mKnobMacro8.setLabelPosition(RelativePosition.ABOVE);
                mKnobMacro8.setAdjustValueMatcher(
                                mMidiIn.createRelative2sComplementCCValueMatcher(0, Constant.MACRO8_CC, 128));

                mKnobTrackNav = surface.createRelativeHardwareKnob("TrackNav");
                mKnobTrackNav.setLabel("TrackNav");
                mKnobTrackNav.setLabelPosition(RelativePosition.ABOVE);
                mKnobTrackNav.setAdjustValueMatcher(
                                mMidiIn.createRelative2sComplementCCValueMatcher(0, Constant.TRACK_NAVIGATION_CC, 128));

                mKnobSceneNav = surface.createRelativeHardwareKnob("SceneNav");
                mKnobSceneNav.setLabel("SceneNav");
                mKnobSceneNav.setLabelPosition(RelativePosition.ABOVE);
                mKnobSceneNav.setAdjustValueMatcher(
                                mMidiIn.createRelative2sComplementCCValueMatcher(0, Constant.SCENE_NAVIGATION_CC, 128));

                mKnobTrackVol = surface.createRelativeHardwareKnob("TrackVol");
                mKnobTrackVol.setLabel("TrackVol");
                mKnobTrackVol.setLabelPosition(RelativePosition.ABOVE);
                mKnobTrackVol.setAdjustValueMatcher(
                                mMidiIn.createRelative2sComplementCCValueMatcher(0, Constant.TRACK_VOLUME_CC, 128));

                mKnobTrackPan = surface.createRelativeHardwareKnob("TrackPan");
                mKnobTrackPan.setLabel("TrackPan");
                mKnobTrackPan.setLabelPosition(RelativePosition.ABOVE);
                mKnobTrackPan
                                .setAdjustValueMatcher(mMidiIn.createRelative2sComplementCCValueMatcher(0,
                                                Constant.TRACK_PAN_CC, 128));

                mKnobTrackSendA = surface.createRelativeHardwareKnob("TrackSendA");
                mKnobTrackSendA.setLabel("TrackSendA");
                mKnobTrackSendA.setLabelPosition(RelativePosition.ABOVE);
                mKnobTrackSendA.setAdjustValueMatcher(
                                mMidiIn.createRelative2sComplementCCValueMatcher(0, Constant.TRACK_SEND_A_CC, 128));

                mKnobTrackSendB = surface.createRelativeHardwareKnob("TrackSendB");
                mKnobTrackSendB.setLabel("TrackSendB");
                mKnobTrackSendB.setLabelPosition(RelativePosition.ABOVE);
                mKnobTrackSendB.setAdjustValueMatcher(
                                mMidiIn.createRelative2sComplementCCValueMatcher(0, Constant.TRACK_SEND_B_CC, 128));

                mKnobMasterTrackVolume = surface.createAbsoluteHardwareKnob("MasterTrackVolume");
                mKnobMasterTrackVolume.setLabel("MasterTrackVolume");
                mKnobMasterTrackVolume.setLabelPosition(RelativePosition.ABOVE);
                mKnobMasterTrackVolume
                                .setAdjustValueMatcher(
                                                mMidiIn.createAbsoluteCCValueMatcher(0, Constant.MASTER_VOLUME_CC));

                mKnobTrackDeviceNav = surface.createRelativeHardwareKnob("TrackDeviceNav");
                mKnobTrackDeviceNav.setLabel("TrackDeviceNav");
                mKnobTrackDeviceNav.setLabelPosition(RelativePosition.ABOVE);
                mKnobTrackDeviceNav.setAdjustValueMatcher(
                                mMidiIn.createRelative2sComplementCCValueMatcher(0, Constant.DEVICE_NAVIGATION_CC,
                                                128));

                mButtonClipLaunch = surface.createHardwareButton("ClipLauch");
                mButtonClipLaunch.setLabel("ClipLauch");
                mButtonClipLaunch.setLabelPosition(RelativePosition.ABOVE);
                mButtonClipLaunch.pressedAction()
                                .setActionMatcher(mMidiIn.createCCActionMatcher(Constant.MIDI_CHANNEL_1,
                                                Constant.LAUNCHING_CLIP_CC));

                mButtonTrackMute = surface.createHardwareButton("TrackMute");
                mButtonTrackMute.setLabel("TrackMute");
                mButtonTrackMute.setLabelPosition(RelativePosition.ABOVE);
                mButtonTrackMute.pressedAction()
                                .setActionMatcher(mMidiIn.createCCActionMatcher(Constant.MIDI_CHANNEL_1,
                                                Constant.TRACK_MUTE_CC));

                mButtonTrackSolo = surface.createHardwareButton("TrackSolo");
                mButtonTrackSolo.setLabel("TrackSolo");
                mButtonTrackSolo.setLabelPosition(RelativePosition.ABOVE);
                mButtonTrackSolo.pressedAction()
                                .setActionMatcher(mMidiIn.createCCActionMatcher(Constant.MIDI_CHANNEL_1,
                                                Constant.TRACK_SOLO_CC));

                mButtonOpenGui = surface.createHardwareButton("OpenGui");
                mButtonOpenGui.setLabel("OpenGui");
                mButtonOpenGui.setLabelPosition(RelativePosition.ABOVE);
                mButtonOpenGui.pressedAction()
                                .setActionMatcher(mMidiIn.createCCActionMatcher(Constant.MIDI_CHANNEL_1,
                                                Constant.OPEN_GUI_CC));
        }

        public void init() {
                /** Callbacks related to selected track changes */

                mSelectedTrackCursor.position().addValueObserver((int position) -> {
                        updateCtrlEStatus();
                        mSelectedTrackCursor.selectSlot(mSelectedClipIndex);
                });
                mSelectedTrackCursor.position().markInterested();

                mSelectedTrackCursor.name().addValueObserver((String name) -> {
                        sendSysexString(Constant.SYSEX_TRACK_NAME, name);
                });
                mSelectedTrackCursor.name().markInterested();

                mSelectedTrackCursor.volume().value().displayedValue().addValueObserver((String value) -> {
                        sendTrackVolume(value);
                });
                mSelectedTrackCursor.volume().markInterested();

                mSelectedTrackCursor.pan().displayedValue().addValueObserver((String value) -> {
                        sendSysexString(Constant.SYSEX_TRACK_PAN, value);
                });
                mSelectedTrackCursor.pan().displayedValue().markInterested();
                mSelectedTrackCursor.pan().markInterested();

                mSelectedTrackCursor.solo().addValueObserver((boolean soloEnabled) -> {
                        int value = soloEnabled ? Constant.MIDI_VALUE_ON : Constant.MIDI_VALUE_OFF;
                        sendCC(Constant.MIDI_CHANNEL_1, Constant.TRACK_SOLO_CC, value);
                });
                mSelectedTrackCursor.solo().markInterested();

                mSelectedTrackCursor.mute().addValueObserver((boolean muteEnabled) -> {
                        int value = muteEnabled ? Constant.MIDI_VALUE_ON : Constant.MIDI_VALUE_OFF;
                        sendCC(Constant.MIDI_CHANNEL_1, Constant.TRACK_MUTE_CC, value);
                });
                mSelectedTrackCursor.mute().markInterested();

                mSelectedTrackSendBank.getItemAt(SEND_A_ID).displayedValue().addValueObserver((String value) -> {
                        sendSysexString(Constant.SYSEX_TRACK_SEND_A, value);
                });
                mSelectedTrackSendBank.getItemAt(SEND_A_ID).markInterested();

                mSelectedTrackSendBank.getItemAt(SEND_B_ID).displayedValue().addValueObserver((String value) -> {
                        sendSysexString(Constant.SYSEX_TRACK_SEND_B, value);
                });
                mSelectedTrackSendBank.getItemAt(SEND_B_ID).markInterested();

                mCtrlEOnTrackDevice.exists().addValueObserver(exists -> updateCtrlEStatus());

                mInstrumentLayerInstance.exists().addValueObserver(exists -> {
                        mInstrumentLayerExists = exists;
                        updateCtrlEStatus();
                });

                for (int i = 0; i < MAX_LAYERS; i++) {
                        if (mInstrumentLayerLayers[i] != null && mCtrlEInInstrumentLayerInstances[i] != null) {
                                mInstrumentLayerLayers[i].exists().addValueObserver(exists -> updateCtrlEStatus());
                                mCtrlEInInstrumentLayerInstances[i].exists()
                                                .addValueObserver(exists -> updateCtrlEStatus());
                        }
                }

                mCtrlEActiveInInstrumentSelector.exists().addValueObserver(exists -> {
                        mInstrumentSelectorExists = exists;
                        updateCtrlEStatus();
                });

                mSelectedDeviceCursor.name().addValueObserver(deviceName -> {
                        String nameToSend = (deviceName != null) ? deviceName : "";
                        sendSysexString(Constant.SYSEX_PLUGIN_NAME, nameToSend);
                });

                /** Callbacks related to VST macros */
                mSelectedDeviceRemoteControlsPage.getParameter(0).value()
                                .addValueObserver((double value) -> sendMacroValue(0, Constant.MACRO1_CC, value));
                mSelectedDeviceRemoteControlsPage.getParameter(1).value()
                                .addValueObserver((double value) -> sendMacroValue(0, Constant.MACRO2_CC, value));
                mSelectedDeviceRemoteControlsPage.getParameter(2).value()
                                .addValueObserver((double value) -> sendMacroValue(0, Constant.MACRO3_CC, value));
                mSelectedDeviceRemoteControlsPage.getParameter(3).value()
                                .addValueObserver((double value) -> sendMacroValue(0, Constant.MACRO4_CC, value));
                mSelectedDeviceRemoteControlsPage.getParameter(4).value()
                                .addValueObserver((double value) -> sendMacroValue(0, Constant.MACRO5_CC, value));
                mSelectedDeviceRemoteControlsPage.getParameter(5).value()
                                .addValueObserver((double value) -> sendMacroValue(0, Constant.MACRO6_CC, value));
                mSelectedDeviceRemoteControlsPage.getParameter(6).value()
                                .addValueObserver((double value) -> sendMacroValue(0, Constant.MACRO7_CC, value));
                mSelectedDeviceRemoteControlsPage.getParameter(7).value()
                                .addValueObserver((double value) -> sendMacroValue(0, Constant.MACRO8_CC, value));

                mSelectedDeviceRemoteControlsPage.getParameter(0).displayedValue()
                                .addValueObserver((String value) -> sendMacroValueString(0, 0, value));
                mSelectedDeviceRemoteControlsPage.getParameter(1).displayedValue()
                                .addValueObserver((String value) -> sendMacroValueString(0, 1, value));
                mSelectedDeviceRemoteControlsPage.getParameter(2).displayedValue()
                                .addValueObserver((String value) -> sendMacroValueString(0, 2, value));
                mSelectedDeviceRemoteControlsPage.getParameter(3).displayedValue()
                                .addValueObserver((String value) -> sendMacroValueString(0, 3, value));
                mSelectedDeviceRemoteControlsPage.getParameter(4).displayedValue()
                                .addValueObserver((String value) -> sendMacroValueString(0, 4, value));
                mSelectedDeviceRemoteControlsPage.getParameter(5).displayedValue()
                                .addValueObserver((String value) -> sendMacroValueString(0, 5, value));
                mSelectedDeviceRemoteControlsPage.getParameter(6).displayedValue()
                                .addValueObserver((String value) -> sendMacroValueString(0, 6, value));
                mSelectedDeviceRemoteControlsPage.getParameter(7).displayedValue()
                                .addValueObserver((String value) -> sendMacroValueString(0, 7, value));

                mSelectedDeviceRemoteControlsPage.getParameter(0).name()
                                .addValueObserver((String name) -> sendMacroName(0, 0, name));
                mSelectedDeviceRemoteControlsPage.getParameter(1).name()
                                .addValueObserver((String name) -> sendMacroName(0, 1, name));
                mSelectedDeviceRemoteControlsPage.getParameter(2).name()
                                .addValueObserver((String name) -> sendMacroName(0, 2, name));
                mSelectedDeviceRemoteControlsPage.getParameter(3).name()
                                .addValueObserver((String name) -> sendMacroName(0, 3, name));
                mSelectedDeviceRemoteControlsPage.getParameter(4).name()
                                .addValueObserver((String name) -> sendMacroName(0, 4, name));
                mSelectedDeviceRemoteControlsPage.getParameter(5).name()
                                .addValueObserver((String name) -> sendMacroName(0, 5, name));
                mSelectedDeviceRemoteControlsPage.getParameter(6).name()
                                .addValueObserver((String name) -> sendMacroName(0, 6, name));
                mSelectedDeviceRemoteControlsPage.getParameter(7).name()
                                .addValueObserver((String name) -> sendMacroName(0, 7, name));

                /** Callbacks related to clips */
                mSelectedTrackClipLauncher.itemCount().markInterested();
                mSelectedTrackClipLauncher.addIsSelectedObserver((int index, boolean selected) -> {
                        clipSelectedCallback(index, selected);
                });

                mKnobMacro1.addBinding(mSelectedDeviceRemoteControlsPage.getParameter(0));
                mKnobMacro2.addBinding(mSelectedDeviceRemoteControlsPage.getParameter(1));
                mKnobMacro3.addBinding(mSelectedDeviceRemoteControlsPage.getParameter(2));
                mKnobMacro4.addBinding(mSelectedDeviceRemoteControlsPage.getParameter(3));
                mKnobMacro5.addBinding(mSelectedDeviceRemoteControlsPage.getParameter(4));
                mKnobMacro6.addBinding(mSelectedDeviceRemoteControlsPage.getParameter(5));
                mKnobMacro7.addBinding(mSelectedDeviceRemoteControlsPage.getParameter(6));
                mKnobMacro8.addBinding(mSelectedDeviceRemoteControlsPage.getParameter(7));

                mKnobTrackNav.addBindingWithSensitivity(
                                mHost.createRelativeHardwareControlStepTarget(
                                                mSelectedTrackCursor.selectNextAction(),
                                                mSelectedTrackCursor.selectPreviousAction()),
                                12.8);

                final RelativeHardwarControlBindable clipNav = mHost.createRelativeHardwareControlAdjustmentTarget(
                                inc -> this.clipNavInc(inc));
                mKnobSceneNav.addBindingWithSensitivity(clipNav, 128.0);
                mKnobTrackVol.addBinding(mSelectedTrackCursor.volume());
                mKnobTrackPan.addBinding(mSelectedTrackCursor.pan());
                mKnobTrackSendA.addBinding(mSelectedTrackSendBank.getItemAt(SEND_A_ID).value());
                mKnobTrackSendB.addBinding(mSelectedTrackSendBank.getItemAt(SEND_B_ID).value());
                mKnobMasterTrackVolume.addBinding(mMasterTrack.volume());

                final RelativeHardwarControlBindable deviceNav = mHost.createRelativeHardwareControlAdjustmentTarget(
                                inc -> {
                                        if (inc > 0) {
                                                mSelectedDeviceCursor.selectNext();
                                        } else {
                                                mSelectedDeviceCursor.selectPrevious();
                                        }
                                        mSelectedDeviceCursor.selectInEditor();
                                        mHost.scheduleTask(() -> {
                                                mApplication.focusPanelAbove();
                                                mApplication.focusPanelBelow();

                                                if (mSelectedDeviceCursor.exists().get()) {
                                                        mSelectedDeviceCursor.selectInEditor();
                                                }
                                        }, 50);
                                });

                mKnobTrackDeviceNav.addBindingWithSensitivity(
                                mHost.createRelativeHardwareControlStepTarget(
                                                mSelectedDeviceCursor.selectNextAction(),
                                                mSelectedDeviceCursor.selectPreviousAction()),
                                12.8);

                final HardwareActionBindable launchClip = mHost.createAction(() -> {
                        mSelectedTrackClipLauncher.launch(mSelectedClipIndex);
                }, () -> "launchClip");

                mButtonClipLaunch.pressedAction().addBinding(launchClip);
                mButtonTrackMute.pressedAction().addBinding(mSelectedTrackCursor.mute().toggleAction());
                mButtonTrackSolo.pressedAction().addBinding(mSelectedTrackCursor.solo().toggleAction());
                mButtonOpenGui.pressedAction().addBinding(mSelectedDeviceCursor.isWindowOpen().toggleAction());
        }

        // *=============================================*//
        // * PRIVATE HELPERS *//
        // *=============================================*//

        private void clipNavInc(double inc) {
                if (inc > 0) {
                        if (mSelectedClipIndex < mSelectedTrackClipLauncher.itemCount().get()) {
                                mSelectedTrackCursor.selectSlot(mSelectedClipIndex + 1);
                                mSelectedClipIndex += 1;
                        }
                } else {
                        if (mSelectedClipIndex > 0) {
                                mSelectedClipIndex -= 1;
                                mSelectedTrackCursor.selectSlot(mSelectedClipIndex);
                        }
                }
        }

        private boolean hasCtrlEOnMainChain() {
                return mCtrlEOnTrackDevice != null &&
                                mCtrlEOnTrackDevice.exists().get();
        }

        private boolean hasCtrlEInInstrumentLayer() {
                if (mInstrumentLayerLayers == null || mInstrumentLayerLayerBank == null) {
                        return false;
                }

                int realLayerCount = mInstrumentLayerLayerBank.itemCount().get();

                for (int i = 0; i < Math.min(realLayerCount, MAX_LAYERS); i++) {
                        boolean layerExists = mInstrumentLayerLayers[i] != null
                                        && mInstrumentLayerLayers[i].exists().get();
                        boolean ctrlEInLayer = mCtrlEInInstrumentLayerInstances[i] != null
                                        && mCtrlEInInstrumentLayerInstances[i].exists().get();

                        if (layerExists && ctrlEInLayer) {
                                return true;
                        }
                }

                return false;
        }

        private boolean hasCtrlEInInstrumentSelector() {
                return mCtrlEActiveInInstrumentSelector != null &&
                                mCtrlEActiveInInstrumentSelector.exists().get();
        }

        private void updateCtrlEStatus() {
                boolean hasCtrlE = false;

                if (hasCtrlEOnMainChain()) {
                        hasCtrlE = true;
                } else if (mInstrumentLayerExists) {
                        hasCtrlE = hasCtrlEInInstrumentLayer();
                } else if (mInstrumentSelectorExists) {
                        hasCtrlE = hasCtrlEInInstrumentSelector();
                }

                if (hasCtrlE != mActivateCtrlEMode) {
                        mActivateCtrlEMode = hasCtrlE;
                        int ccValue = hasCtrlE ? Constant.MIDI_VALUE_ON : Constant.MIDI_VALUE_OFF;
                        sendCC(Constant.MIDI_CHANNEL_1, Constant.CTRL_E_DEVICE_ON_TRACK_CC, ccValue);
                }
        }

        private void clipSelectedCallback(int index, boolean selected) {
                if (selected) {
                        mSelectedClipIndex = index;
                }
        }

        private void sendTrackVolume(String value) {
                String cleanValue = value.replace(VOLUME_DB_SUFFIX, "");
                sendSysexString(Constant.SYSEX_TRACK_VOLUME, cleanValue);
        }

        private void sendMacroName(int bank, int id, String name) {
                String normalizedValue = Helper.normalize(name);

                try {
                        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        stream.write(normalizedValue.length() + 4);
                        stream.write(bank);
                        stream.write(id);
                        stream.write(normalizedValue.length() + 1); // +1 for NULL string terminator
                        stream.write(normalizedValue.getBytes(StandardCharsets.UTF_8));
                        stream.write(Constant.SYSEX_STRING_TERMINATOR);

                        sendSysex(Constant.SYSEX_MACRO_NAME, stream.toByteArray());
                } catch (IOException e) {
                }
        }

        private void sendMacroValueString(int bank, int id, String value) {
                String normalizedValue = Helper.normalize(value);

                try {
                        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        stream.write(normalizedValue.length() + 4);
                        stream.write(bank);
                        stream.write(id);
                        stream.write(normalizedValue.length() + 1); // +1 for NULL string terminator
                        stream.write(normalizedValue.getBytes(StandardCharsets.UTF_8));
                        stream.write(Constant.SYSEX_STRING_TERMINATOR);

                        sendSysex(Constant.SYSEX_MACRO_VALUE, stream.toByteArray());
                } catch (IOException e) {
                }
        }

        private void sendMacroValue(int bank, int id, double value) {
                if (bank == Constant.MACRO_BANK_MAIN) {
                        sendCC(Constant.MIDI_CHANNEL_1, id, (int) (value * Constant.MIDI_VALUE_MAX));
                }
        }

        // Activate for debug
        // private void log(String message)
        // {
        // if (mHost != null) {
        // mHost.println(message);
        // }
        // }
}
