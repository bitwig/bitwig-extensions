package com.bitwig.extensions.controllers.softube.console1;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.bitwig.extension.controller.api.Channel;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.Send;
import com.bitwig.extension.controller.api.SendBank;

public class TrackSlot {
    private final int channelIndex;
    private long lastReleased;
    public static final String NEG_INFINITY = "\"-Infinity\"";
    private static final String CONSOLE_1 = "2FF966F3A2DA4112BBB38DC29B336457";
    private static final String FLOW_MIXING_SUITE = "74D14512EBBF4BBA9F0E508E4A0EAEC6";
    
    private String trackId;
    private final Channel track;
    private final TrackControl control;
    
    private int meterLeft = 0;
    private int meterRight = 0;
    
    private String name;
    private boolean active;
    private boolean selected;
    private boolean solo;
    private boolean mute;
    private String colorValue = "0";
    private String volume = NEG_INFINITY;
    private String pan = "0.5";
    private final String[] sends = new String[6];
    private final boolean[] sendsActive = new boolean[6];
    private final int channels = 2;
    private final String maxVolumeValue = "6.02";
    private final String maxSendValue = "0";
    private final String meterType = "VU";
    private boolean containsConsole1 = false;
    private boolean meteringActive = true;
    private long lastConsoleLoadInvocation = 0;
    
    private static class ValueUpdate {
        private Object lastValue = new Object();
        private final Parameter parameter;
        
        public ValueUpdate(final Parameter parameter) {
            this.parameter = parameter;
        }
        
        public void updateValue(final Object v) {
            parameter.value().setImmediately(fromDouble(v));
        }
        
        public void updateDecibel(final Object v) {
            final double vd = fromDecibel(v);
            final double currentRaw = parameter.value().getRaw();
            if (currentRaw != vd) {
                parameter.value().setRaw(vd);
                lastValue = v;
            }
        }
        
        public void update0Decibel(final Object v) {
            parameter.value().setImmediately(from0Decibel(v));
        }
        
        public void touch(final boolean touchState) {
            parameter.touch(touchState);
        }
    }
    
    private final Map<String, ValueUpdate> valueHandlers = new HashMap<>();
    
    public TrackSlot(final int index, final Channel track, final TrackControl control) {
        this.channelIndex = index + 1;
        this.track = track;
        this.trackId = null;
        this.control = control;
        
        valueHandlers.put("volume", new ValueUpdate(track.volume()));
        valueHandlers.put("pan", new ValueUpdate(track.pan()));
        for (int i = 0; i < 6; i++) {
            valueHandlers.put("send%d".formatted(i + 1), new ValueUpdate(track.sendBank().getItemAt(i)));
        }
        Arrays.fill(sendsActive, false);
        Arrays.fill(sends, NEG_INFINITY);
        track.name().addValueObserver(this::handNameChanged);
        track.channelId().addValueObserver(this::handleIdChanged);
        track.exists().addValueObserver(this::handleExistChanged);
        track.color().addValueObserver(this::handleColorChanged);
        track.volume().value().addRawValueObserver(this::volumeChange);
        track.pan().value().addValueObserver(this::panChange);
        track.solo().addValueObserver(this::handleSoloChanged);
        track.mute().addValueObserver(this::handleMuteChanged);
        
        //track.channelIndex().addValueObserver(this::handleChannelIndex);
        
        track.addIsSelectedInMixerObserver(this::handleSelectedChanged);
        track.addVuMeterObserver(100, 0, false, this::handleMeterLeft);
        track.addVuMeterObserver(100, 1, false, this::handleMeterRight);
        final DeviceBank deviceBank = track.createDeviceBank(1);
        deviceBank.setDeviceMatcher(control.getConsole1Matcher());
        final Device device = deviceBank.getDevice(0);
        device.exists().addValueObserver(this::handleConsole1Appeared);
        final SendBank bank = track.sendBank();
        for (int j = 0; j < bank.getSizeOfBank(); j++) {
            final int sendIndex = j;
            final Send send = bank.getItemAt(sendIndex);
            send.exists().addValueObserver(exists -> this.handleSendExists(sendIndex, exists));
            send.value().addValueObserver(value -> this.handleSendChanged(sendIndex, value));
        }
    }
    
    private void handleConsole1Appeared(final boolean exists) {
        //Console1Extension.println(" Console1 %d %s".formatted(this.slotIndex, exists));
        containsConsole1 = exists;
        // control.launchUpdateTask();
        //control.updateTrackJson(this.trackId, "plugin", exists);
        //control.activate(this);
    }
    
    private void handleMeterLeft(final int value) {
        if (meteringActive && value != meterLeft) {
            meterLeft = value;
            control.sendMeterJson(trackId, meterLeft, meterRight);
        }
    }
    
    private void handleMeterRight(final int value) {
        if (meteringActive && value != meterRight) {
            meterRight = value;
            control.sendMeterJson(trackId, meterLeft, meterRight);
        }
    }
    
    private void handleIdChanged(final String channelId) {
        //Console1Extension.println("IC: %d - %s :  %s", channelIndex, trackId, channelId);
        if (!channelId.isBlank()) {
            trackId = channelId;
        } else {
            trackId = null;
        }
        control.launchUpdateTask();
    }
    
    private void handNameChanged(final String name) {
        this.name = name;
        control.activate(this);
    }
    
    public int getIndex() {
        return channelIndex;
    }
    
    public String getName() {
        return name;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public boolean isDefined() {
        return trackId != null && active;
    }
    
    public TrackId toTrackId() {
        return new TrackId(channelIndex, trackId, name);
    }
    
    public void setMeteringActive(final boolean meteringActive) {
        this.meteringActive = meteringActive;
    }
    
    private void handleExistChanged(final boolean exists) {
        if (!exists && !this.active) {
            return;
        }
        this.active = exists;
        control.launchUpdateTask();
    }
    
    private void handleSoloChanged(final boolean value) {
        this.solo = value;
        control.updateTrackJson(trackId, "solo", this.solo);
    }
    
    private void handleMuteChanged(final boolean value) {
        this.mute = value;
        control.updateTrackJson(trackId, "mute", this.mute);
    }
    
    private void handleSelectedChanged(final boolean value) {
        this.selected = value;
        control.updateTrackJson(trackId, "selected", this.selected);
    }
    
    private void handleSendExists(final int index, final boolean exists) {
        setSendsActive(index, exists);
        control.updateTrackJson(trackId, "send" + (index + 1) + "On", sendsActive[index]);
    }
    
    private void handleSendChanged(final int index, final double value) {
        setSend(index, to0dbLevel(value));
        control.updateTrackJson(trackId, "send" + (index + 1), sends[index]);
    }
    
    private void volumeChange(final double newValue) {
        this.volume = to6dbLevel(newValue);
        control.updateTrackJson(trackId, "volume", this.volume);
    }
    
    private void panChange(final double newValue) {
        this.pan = Double.toString(newValue);
        control.updateTrackJson(trackId, "pan", this.pan);
    }
    
    private void handleColorChanged(final float r, final float g, final float b) {
        final int color = toColor(r, g, b);
        this.colorValue = Integer.toString(color);
        control.updateTrackJson(trackId, "color", this.colorValue);
    }
    
    private int toColor(final float r, final float g, final float b) {
        final int rv = (int) Math.floor(r * 255) << 0;
        final int gv = (int) Math.floor(g * 255) << 8;
        final int bv = (int) Math.floor(b * 255) << 16;
        return rv | gv | bv;
    }
    
    public String getTrackId() {
        return trackId;
    }
    
    public void setSend(final int index, final String val) {
        this.sends[index] = val;
    }
    
    public void setSendsActive(final int index, final boolean sendsActive) {
        this.sendsActive[index] = sendsActive;
    }
    
    public String toString() {
        final String sb =
            " %02d - %8s %12s %s %s".formatted(
                this.channelIndex, this.trackId, name, this.active ? "X" : "-", containsConsole1 ? "o" : " ");
        return sb;
    }
    
    public String getJsonData() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"trackId\":\"").append(trackId).append("\","); //
        sb.append("\"track\":").append(channelIndex).append(","); //
        sb.append("\"name\":\"").append(name).append("\","); //
        sb.append("\"color\":").append(colorValue).append(","); //
        sb.append("\"isActive\":").append(active).append(","); //
        sb.append("\"selected\":").append(selected).append(","); //
        sb.append("\"mute\":").append(mute).append(","); //
        sb.append("\"solo\":").append(solo).append(","); //
        sb.append("\"volume\":").append(volume).append(","); //
        sb.append("\"pan\":").append(pan).append(","); //
        
        for (int i = 0; i < sends.length; i++) { //
            sb.append("\"send").append(i + 1).append("\":").append(sends[i]).append(","); //
            sb.append("\"send").append(i + 1).append("On\":").append(sendsActive[i]).append(","); //
        } //
        
        sb.append("\"channels\":").append(channels).append(","); //
        sb.append("\"maxSendValue\":").append(maxSendValue).append(","); //
        sb.append("\"maxVolumeValue\":").append(maxVolumeValue).append(","); //
        sb.append("\"meterType\":\"").append(meterType).append("\""); //
        sb.append("}"); //
        return sb.toString();
    }
    
    public String getDisableData() {
        final String sb = "{\"trackId\": \"%s\",".formatted(trackId) + "\"track\": %d,".formatted(channelIndex)
            + "\"isActive\": false" + "}";
        return sb;
    }
    
    public void applyUpdate(final JsonObject obj, final ControllerHost host) {
        if (obj.contains("touchState")) {
            final boolean touched = "TOUCHED".equals(obj.getString("touchState"));
            if (touched) {
                obj.stream() //
                    .filter(entry -> !"trackId".equals(entry.getKey())) //
                    .filter(entry -> !entry.getKey().equals("touchState"))
                    .forEach(entry -> applyTouch(entry.getKey(), true, host));
            } else {
                obj.stream() //
                    .filter(entry -> !"trackId".equals(entry.getKey())) //
                    .filter(entry -> !entry.getKey().equals("touchState"))
                    .forEach(entry -> applyTouch(entry.getKey(), false, host));
            }
        } else {
            obj.stream() //
                .filter(entry -> !"trackId".equals(entry.getKey()))
                .forEach(entry -> applyChange(entry.getKey(), entry.getValue()));
        }
    }
    
    private void applyTouch(final String key, final boolean touchState, final ControllerHost host) {
        if (!touchState) {
            lastReleased = System.currentTimeMillis();
        }
        final ValueUpdate valueHandler = valueHandlers.get(key);
        if (valueHandler != null) {
            valueHandler.touch(touchState);
        } else {
            control.println(" Touch UNKNOWN TRACK ID = %s", key);
        }
    }
    
    private void applyDecibel(final String type, final Object value) {
        final ValueUpdate valueHandler = valueHandlers.get(type);
        if (valueHandler != null) {
            valueHandler.updateDecibel(value);
        } else {
            control.println(" Param UNKNOWN TRACK TYPE = %s", type);
        }
    }
    
    private void apply0Decibel(final String type, final Object value) {
        final ValueUpdate valueHandler = valueHandlers.get(type);
        if (valueHandler != null) {
            valueHandler.update0Decibel(value);
        } else {
            control.println(" Param UNKNOWN TRACK TYPE = %s", type);
        }
    }
    
    private void applyValue(final String type, final Object value) {
        final ValueUpdate valueHandler = valueHandlers.get(type);
        if (valueHandler != null) {
            valueHandler.updateValue(value);
        } else {
            control.println(" Param UNKNOWN TRACK TYPE = %s", type);
        }
    }
    
    private void applyChange(final String key, final Object value) {
        if (control.touchAutomationInPlace()) {
            final long diff = System.currentTimeMillis() - lastReleased;
            if (diff < 500) {
                //Console1Extension.println(" DEFLECT ");
                return;
            }
        }
        switch (key) {
            case "volume" -> applyDecibel(key, value);
            case "pan" -> applyValue(key, value);
            case "send1", "send2", "send3", "send4", "send5", "send6" -> apply0Decibel(key, value);
            case "mute" -> track.mute().set(getBoolean(value));
            case "solo" -> track.solo().set(getBoolean(value));
            case "selected" -> this.actionOnTrue(value, this::handleSelection);
            case "plugin" -> this.loadPlugin(value.toString());
        }
    }
    
    private void loadPlugin(final String plugin) {
        control.println(" LOAD <%s>", plugin);
        final long diff = System.currentTimeMillis() - lastConsoleLoadInvocation;
        if (diff > 10000) {
            switch (plugin) {
                case "Console 1" -> track.endOfDeviceChainInsertionPoint().insertVST3Device(CONSOLE_1);
                case "Flow Mixing Suite" -> track.endOfDeviceChainInsertionPoint().insertVST3Device(FLOW_MIXING_SUITE);
            }
            
            lastConsoleLoadInvocation = System.currentTimeMillis();
        }
    }
    
    private void handleSelection() {
        track.selectInEditor();
        track.selectInMixer();
    }
    
    private void actionOnTrue(final Object o, final Runnable action) {
        if (o instanceof final Boolean value && value) {
            action.run();
        }
    }
    
    private boolean getBoolean(final Object o) {
        if (o instanceof final Boolean value) {
            return value;
        }
        return false;
    }
    
    private static double from0Decibel(final Object o) {
        if (o instanceof final String sv && "-Infinity".equals(sv)) {
            return 0;
        } else if (o instanceof final Double value) {
            return Math.min(1, Math.pow(10, value / 60.0));
        } else if (o instanceof final Integer value) {
            return Math.pow(10, value / 60.0);
        }
        return 0;
    }
    
    protected static double fromDecibel(final Object o) {
        if (o instanceof final String sv && "-Infinity".equals(sv)) {
            return 0;
        } else if (o instanceof final Double value) {
            return Math.pow(10, value / 60.0);
        } else if (o instanceof final Integer value) {
            return Math.pow(10, value / 60.0);
        }
        return 0;
    }
    
    private static double fromDouble(final Object o) {
        if (o instanceof final Double value) {
            return Math.min(1, Math.max(0, value));
        } else if (o instanceof final Integer value) {
            return value;
        }
        return 0;
    }
    
    
    private static String to6dbLevel(final double value) {
        if (value == 0.0) {
            return NEG_INFINITY;
        }
        return Double.toString(60 * Math.log10(value));
    }
    
    private static String to0dbLevel(final double value) {
        if (value == 0) {
            return NEG_INFINITY;
        }
        return Double.toString(60 * Math.log10(value));
    }
    
}

