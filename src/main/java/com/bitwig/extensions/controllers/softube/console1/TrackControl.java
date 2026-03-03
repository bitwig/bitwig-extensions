package com.bitwig.extensions.controllers.softube.console1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bitwig.extension.controller.api.Channel;
import com.bitwig.extension.controller.api.ChannelBank;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.DeviceMatcher;
import com.bitwig.extension.controller.api.Transport;

public class TrackControl {
    private final ControllerHost host;
    private final ConsoleMidiProcessor midiProcessor;
    private final List<TrackSlot> slots = new ArrayList<>();
    private final DeviceMatcher console1Matcher;
    private Object updateTask = null;
    private final Map<String, TrackSlot> trackIdLookup = new HashMap<>();
    private final Map<String, TrackId> existingTrackIds = new HashMap<>();
    private boolean blockMidi = true;
    private boolean trackTouchAutomationActive;
    private final Transport transport;
    private final Map<String, MeterCall> meteringCalls = new HashMap<>();
    private boolean connected;
    
    record MeterCall(int left, int right) {
        public String getJson(final String trackId) {
            return "{\"trackId\":\"" + trackId + //
                "\",\"meter\":[" + //
                (left / 100.0) + "," + (right / 100.0) + //
                "]}";
        }
    }
    
    public TrackControl(final ChannelBank<? extends Channel> trackBank, final ControllerHost host,
        final ConsoleMidiProcessor midiProcessor, final Transport transport) {
        this.host = host;
        this.midiProcessor = midiProcessor;
        console1Matcher = createDeviceMatcherC1(host);
        this.trackTouchAutomationActive = false;
        this.transport = transport;
        transport.isArrangerAutomationWriteEnabled().markInterested();
        transport.isClipLauncherAutomationWriteEnabled().markInterested();
        transport.automationWriteMode().addValueObserver(this::updateAutomationMode);
        for (int index = 0; index < trackBank.getSizeOfBank(); index++) {
            slots.add(new TrackSlot(index, trackBank.getItemAt(index), this));
        }
        midiProcessor.addConnectionListener(this::handleConnection);
        host.scheduleTask(this::handleMeterTask, 100);
    }
    
    private void handleConnection(final boolean connected) {
        //host.println(" Track Control Connect > " + connected);
        this.connected = connected;
    }
    
    public boolean isConnected() {
        return connected;
    }
    
    public void println(final String formated, final Object... args) {
        this.host.println(formated.formatted(args));
    }
    
    private void handleMeterTask() {
        if (!meteringCalls.isEmpty()) {
            final StringBuilder sb = new StringBuilder(256); //
            sb.append("{\"trackBatch\":["); //
            boolean first = true; //
            for (final Map.Entry<String, MeterCall> entry : meteringCalls.entrySet()) { //
                if (!first) {
                    sb.append(','); //
                }
                first = false; //
                sb.append(entry.getValue().getJson(entry.getKey())); //
            } //
            sb.append("]}"); //
            midiProcessor.sendJsonSysEx(sb.toString()); //
            meteringCalls.clear(); //
        }
        host.scheduleTask(this::handleMeterTask, 50);
    }
    
    private void updateAutomationMode(final String mode) {
        trackTouchAutomationActive = "touch".equals(mode);
    }
    
    public boolean touchAutomationInPlace() {
        return trackTouchAutomationActive && (transport.isClipLauncherAutomationWriteEnabled().get()
            || transport.isArrangerAutomationWriteEnabled().get());
    }
    
    private DeviceMatcher createDeviceMatcherC1(final ControllerHost host) {
        final DeviceMatcher console1Matcher;
        final DeviceMatcher vst3Matcher = host.createVST3DeviceMatcher("2FF966F3A2DA4112BBB38DC29B336457");
        final DeviceMatcher vst2Matcher = host.createVST2DeviceMatcher(1399017577);
        console1Matcher = host.createOrDeviceMatcher(vst2Matcher, vst3Matcher);
        return console1Matcher;
    }
    
    public DeviceMatcher getConsole1Matcher() {
        return console1Matcher;
    }
    
    public void setBlockMidi(final boolean blockMidi) {
        this.blockMidi = blockMidi;
    }
    
    public void updateTrackJson(final String trackId, final String key, final String value) {
        if (trackId == null || !connected) {
            return;
        }
        sendJsonInfo("{\"trackId\":\"%s\",\"%s\":%s}".formatted(trackId, key, value));
    }
    
    private String trackIdValue(final String trackId, final String key, final boolean value) {
        return "{\"trackId\":\"" + trackId + "\",\"" + key + "\":" + value + "}";
    }
    
    public void meterActivate(final List<String> activeMeters) {
        for (final TrackSlot slot : slots) {
            slot.setMeteringActive(false);
        }
        //Console1Extension.println(" Active meters = " + activeMeters);
        for (final String id : activeMeters) {
            final TrackSlot slot = trackIdLookup.get(id);
            if (slot != null) {
                slot.setMeteringActive(true);
            }
        }
    }
    
    public void updateTrackJson(final String trackId, final String key, final boolean value) {
        if (trackId == null) {
            return;
        }
        sendJson(trackIdValue(trackId, key, value));
    }
    
    public void sendMeterJson(final String trackId, final int left, final int right) {
        if (blockMidi || !connected) {
            return;
        }
        meteringCalls.put(trackId, new MeterCall(left, right));
    }
    
    
    private void sendJson(final String json) {
        if (blockMidi || !connected) {
            return;
        }
        //Console1Extension.println(" SEND JSON <%s>  %s", blockMidi, json);
        midiProcessor.sendJsonSysEx(json);
    }
    
    private void sendJsonInfo(final String json) {
        if (blockMidi || !connected) {
            return;
        }
        midiProcessor.sendJsonSysEx(json);
    }
    
    public void update(final JsonObject obj) {
        final String trackId = obj.getString("trackId");
        final TrackSlot slot = trackIdLookup.get(trackId);
        if (slot != null) {
            slot.applyUpdate(obj, host);
        }
    }
    
    public void updateAllTracks() {
        for (final TrackSlot slot : slots) {
            if (!slot.isActive()) {
                deactivate(slot);
            }
        }
        for (final TrackSlot slot : slots) {
            if (slot.isActive()) {
                activate(slot);
            }
        }
    }
    
    public void resetAll() {
        slots.stream().forEach(slot -> deactivate(slot));
    }
    
    public void activate(final TrackSlot slot) {
        if (slot.getTrackId() == null || !connected) {
            return;
        }
        sendJsonInfo(slot.getJsonData());
    }
    
    public void deactivate(final TrackSlot slot) {
        if (slot.getTrackId() == null || !connected) {
            return;
        }
        sendJsonInfo(slot.getDisableData());
    }
    
    public void launchUpdateTask() {
        if (updateTask == null) {
            updateTask = 0;
            host.scheduleTask(this::collectTrackUpdates, 300);
        }
    }
    
    private void collectTrackUpdates() {
        slots.stream().filter(TrackSlot::isDefined).map(TrackSlot::getTrackId)
            .forEach(id -> existingTrackIds.remove(id));
        for (final TrackId id : existingTrackIds.values()) {
            sendJsonInfo(id.getDisableData());
        }
        
        trackIdLookup.clear();
        slots.stream().filter(TrackSlot::isDefined).forEach(this::activate);
        slots.stream().filter(TrackSlot::isDefined).forEach(slot -> trackIdLookup.put(slot.getTrackId(), slot));
        
        existingTrackIds.clear();
        slots.stream().filter(TrackSlot::isDefined)
            .forEach(slot -> existingTrackIds.put(slot.getTrackId(), slot.toTrackId()));
        
        updateTask = null;
    }
    
}
