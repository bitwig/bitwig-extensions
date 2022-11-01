package com.bitwig.extensions.controllers.novation.launchpad_pro_mk3;

import java.util.List;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extensions.framework.Layer;

public class LayerManagement {

    private LaunchpadProMK3Workflow driver;

    private Layer lastLayer;
    private Layer tempLastLayer;
    private Sysex sysex;
    private LongPress longPress;

    private Boolean isTemporarySwitch = false;

    public LayerManagement(LaunchpadProMK3Workflow d) {
        driver = d;
        longPress = new LongPress(driver);
        sysex = new Sysex();

    }

    public Layer getLastLayer() {
        return lastLayer;
    }

    public void switchLayer(Layer layer) {
        driver.mMidiOut.sendSysex(sysex.DAW_FADER_OFF + sysex.DAW_VOLUME);
        lastLayer = layer;
        driver.mHost.scheduleTask(() -> resetLastLayer(), (long) 100);
        // Sends
        for (int i = 0; i < 8; i++) {
            if (driver.mActiveBindings[i] != null)
                driver.mActiveBindings[i].removeBinding();
        }

        List<Layer> l = driver.mLayers.getLayers();
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i) == layer && !layer.isActive())
                l.get(i).activate();
            else
                l.get(i).deactivate();
        }

        Boolean b = true;
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i) != driver.mSessionLayer && l.get(i).isActive()) {
                b = false;
                break;
            }
        }
        if (b)
            tempLastLayer = driver.mSessionLayer;

        if (driver.mSendsLayer.isActive()) {
            for (int j = 0; j < 8; j++) {
                driver.mActiveBindings[7 - j] = driver.mTrackBank.getItemAt(7 - j).sendBank().getItemAt(driver.mSendIndex)
                        .addBinding(driver.mFader[(7 - j) + 16]);
            }
        }
        driver.mSessionLayer.activate();
        //driver.sendFaderValues();
    }

    public void tempLayerSwitch(Layer layer) {
        if (!layer.isActive()) {
            longPress.delayedAction(() -> isTemporarySwitch = true, () -> {
                tempLastLayer = layer;
            });
            switchLayer(layer);
        } else {
            switchLayer(layer);
        }
    }

    public void tempLayerSwitch(Layer layer, String faderSysex) {
        tempLayerSwitch(layer);
        if (layer.isActive())
            driver.mMidiOut.sendSysex(sysex.DAW_FADER_ON + faderSysex);
        else
            driver.mMidiOut.sendSysex(sysex.DAW_FADER_OFF + faderSysex);
    }

    public void tempLayerSwitchRelease() {
        longPress.releasedAction();
        if (isTemporarySwitch) {
            switchLayer(tempLastLayer);
            if (driver.mDeviceLayer.isActive())
                driver.mMidiOut.sendSysex(sysex.DAW_FADER_ON + sysex.DAW_DEVICE);
            else if (driver.mVolumeLayer.isActive())
                driver.mMidiOut.sendSysex(sysex.DAW_FADER_ON + sysex.DAW_VOLUME);
            else if (driver.mPanLayer.isActive())
                driver.mMidiOut.sendSysex(sysex.DAW_FADER_ON + sysex.DAW_PAN);
            else if (driver.mSendsLayer.isActive())
                driver.mMidiOut.sendSysex(sysex.DAW_FADER_ON + sysex.DAW_SENDS);
            else
                driver.mMidiOut.sendSysex(sysex.DAW_FADER_OFF + sysex.DAW_VOLUME);
        }
        isTemporarySwitch = false;
    }

    private void resetLastLayer() {
        lastLayer = null;
    }

}
