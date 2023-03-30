package com.bitwig.extensions.controllers.akai.apcmk2.layer;

import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.akai.apcmk2.ControlMode;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Activate;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractControlLayer {

    protected ControlMode mode = ControlMode.VOLUME;
    protected TrackBank trackBank;
    protected CursorRemoteControlsPage parameterBank;
    protected Map<ControlMode, Layer> layerMap = new HashMap<>();

    public AbstractControlLayer(Layers layers) {
        layerMap.put(ControlMode.VOLUME, new Layer(layers, "ENC_VOL_LAYER"));
        layerMap.put(ControlMode.PAN, new Layer(layers, "ENC_PAN_LAYER"));
        layerMap.put(ControlMode.SEND, new Layer(layers, "ENC_SEND_LAYER"));
        layerMap.put(ControlMode.DEVICE, new Layer(layers, "ENC_DEVICE_LAYER"));
    }

    @Activate
    public void activate() {
        layerMap.get(mode).setIsActive(true);
    }

    public void setMode(final ControlMode mode) {
        if (mode != this.mode) {
            layerMap.get(this.mode).setIsActive(false);
            layerMap.get(mode).setIsActive(true);
            this.mode = mode;
        } else {
            if (mode == ControlMode.SEND) {
                advanceSendBank();
            } else if (mode == ControlMode.DEVICE) {
                parameterBank.selectNextPage(true);
            }
        }
    }

    private void advanceSendBank() {
        for (int i = 0; i < 8; i++) {
            Track track = trackBank.getItemAt(i);
            final SendBank sendBank = track.sendBank();
            if (sendBank.scrollPosition().get() + 1 >= sendBank.itemCount().get()) {
                sendBank.scrollPosition().set(0);
            } else {
                sendBank.scrollBy(1);
            }
        }
    }

    public ControlMode getMode() {
        return mode;
    }

}
