package com.bitwig.extensions.controllers.mcu.devices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceMatcher;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extensions.controllers.mcu.CursorDeviceControl;
import com.bitwig.extensions.controllers.mcu.GlobalStates;
import com.bitwig.extensions.controllers.mcu.VPotMode;
import com.bitwig.extensions.controllers.mcu.ViewControl;
import com.bitwig.extensions.framework.di.Component;

@Component
public class DeviceTypeBank {
    
    public interface ExistenceChangedListener {
        void changed(VPotMode typ, boolean exists);
    }
    
    public enum GeneralDeviceType {
        INSTRUMENT(VPotMode.INSTRUMENT),
        AUDIO_EFFECT(VPotMode.PLUGIN),
        NOTE_EFFECT(VPotMode.MIDI_EFFECT),
        EQ_PLUS(VPotMode.EQ),
        TRACK_REMOTE(VPotMode.TRACK_REMOTE),
        PROJECT_REMOTE(VPotMode.PROJECT_REMOTE),
        NONE(null);
        
        private final VPotMode mode;
        
        GeneralDeviceType(final VPotMode mode) {
            this.mode = mode;
        }
        
        public VPotMode getMode() {
            return mode;
        }
        
        public static GeneralDeviceType fromMode(final VPotMode mode) {
            return Arrays.stream(GeneralDeviceType.values())//
                .filter(type -> type.getMode().equals(mode)) //
                .findFirst()  //
                .orElse(NONE);
        }
    }
    
    private final EqDevice eqDevice;
    private final Map<VPotMode, DeviceTypeFollower> types = new HashMap<>();
    private final Map<VPotMode, DeviceManager> managers = new HashMap<>();
    private final Map<VPotMode, Boolean> focusStatus = new HashMap<>();
    private final Map<VPotMode, Boolean> typeExistsStatus = new HashMap<>();
    private final CursorDeviceControl cursorDeviceControl;
    private final List<ExistenceChangedListener> listeners = new ArrayList<>();
    private final List<Consumer<GeneralDeviceType>> typeListeners = new ArrayList<>();
    private GeneralDeviceType deviceType = GeneralDeviceType.NONE;
    private boolean deviceExists = false;
    private String currentDeviceType = "";
    
    public DeviceTypeBank(final ControllerHost host, final ViewControl viewControl, final GlobalStates states) {
        cursorDeviceControl = viewControl.getCursorDeviceControl();
        Arrays.stream(VPotMode.values()).forEach(mode -> focusStatus.put(mode, false));
        
        addFollower(cursorDeviceControl, VPotMode.INSTRUMENT, host.createInstrumentMatcher());
        addFollower(cursorDeviceControl, VPotMode.PLUGIN, host.createAudioEffectMatcher());
        addFollower(cursorDeviceControl, VPotMode.MIDI_EFFECT, host.createNoteEffectMatcher());
        
        final DeviceMatcher eqDeviceMatcher = host.createBitwigDeviceMatcher(SpecialDevices.EQ_PLUS.getUuid());
        final DeviceTypeFollower eqFollower = addFollower(cursorDeviceControl, VPotMode.EQ, eqDeviceMatcher);
        eqDevice = new EqDevice(cursorDeviceControl, eqFollower, states);
        
        final PinnableCursorDevice cursorDevice = cursorDeviceControl.getCursorDevice();
        cursorDevice.exists().addValueObserver(exist -> {
            this.deviceExists = exist;
            updateInstrumentType();
        });
        cursorDevice.deviceType().addValueObserver(type -> {
            currentDeviceType = type;
            updateInstrumentType();
        });
    }
    
    public void ensureDeviceSelection(final VPotMode mode) {
        if (deviceType.getMode() == null) {
            return;
        }
        if (deviceType.getMode() != mode) {
            types.get(mode).ensurePosition();
        }
    }
    
    public void addDeviceTypeListener(final Consumer<GeneralDeviceType> listener) {
        this.typeListeners.add(listener);
    }
    
    public SpecificDevice getEqDevice() {
        return eqDevice;
    }
    
    public boolean hasDeviceType(final VPotMode mode) {
        final Boolean exists = typeExistsStatus.get(mode);
        return exists != null ? exists : false;
    }
    
    private void updateInstrumentType() {
        GeneralDeviceType type = GeneralDeviceType.NONE;
        if (deviceExists) {
            if (focusStatus.get(VPotMode.EQ)) {
                type = GeneralDeviceType.EQ_PLUS;
            } else {
                for (final VPotMode mode : VPotMode.values()) {
                    if (focusStatus.get(mode)) {
                        type = GeneralDeviceType.fromMode(mode);
                    }
                }
            }
            if (type == GeneralDeviceType.NONE && currentDeviceType.equals("audio_to_audio")) {
                type = GeneralDeviceType.AUDIO_EFFECT;
            }
        }
        if (type != deviceType) {
            this.deviceType = type;
            //McuExtension.println("### CURRENT TYPE = %s exist=%s", this.deviceType, deviceExists);
            this.typeListeners.forEach(listener -> listener.accept(this.deviceType));
        }
    }
    
    public CursorRemoteControlsPage getCursorRemotes() {
        return cursorDeviceControl.getRemotes();
    }
    
    private DeviceTypeFollower addFollower(final CursorDeviceControl deviceControl, final VPotMode mode,
        final DeviceMatcher matcher) {
        final DeviceTypeFollower follower = new DeviceTypeFollower(deviceControl, matcher, mode);
        types.put(mode, follower);
        final Device device = follower.getFocusDevice();
        device.exists().addValueObserver(followExists -> {
            typeExistsStatus.put(mode, followExists);
            listeners.forEach(l -> l.changed(mode, followExists));
        });
        follower.addOnCursorListener(onCursor -> {
            focusStatus.put(mode, onCursor);
            updateInstrumentType();
        });
        return follower;
    }
    
    public DeviceTypeFollower[] getStandardFollowers() {
        final DeviceTypeFollower[] result = new DeviceTypeFollower[3];
        result[0] = types.get(VPotMode.INSTRUMENT);
        result[1] = types.get(VPotMode.PLUGIN);
        result[2] = types.get(VPotMode.MIDI_EFFECT);
        return result;
    }
    
    public void addExistenceListener(final ExistenceChangedListener listener) {
        listeners.add(listener);
    }
    
    public DeviceTypeFollower getFollower(final VPotMode mode) {
        return types.get(mode);
    }
}
