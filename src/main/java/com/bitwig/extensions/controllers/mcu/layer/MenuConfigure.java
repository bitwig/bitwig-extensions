package com.bitwig.extensions.controllers.mcu.layer;

import com.bitwig.extension.controller.api.Arranger;
import com.bitwig.extension.controller.api.BeatTimeFormatter;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CueMarker;
import com.bitwig.extension.controller.api.CueMarkerBank;
import com.bitwig.extension.controller.api.DetailEditor;
import com.bitwig.extension.controller.api.Groove;
import com.bitwig.extension.controller.api.SettableBeatTimeValue;
import com.bitwig.extension.controller.api.Transport;
import com.bitwig.extensions.controllers.mcu.ViewControl;
import com.bitwig.extensions.controllers.mcu.control.MixerSectionHardware;
import com.bitwig.extensions.controllers.mcu.control.RingDisplayType;
import com.bitwig.extensions.controllers.mcu.devices.SslPlugins;
import com.bitwig.extensions.controllers.mcu.display.DisplayManager;
import com.bitwig.extensions.controllers.mcu.value.IEnumDisplayValue;
import com.bitwig.extensions.controllers.mcu.value.SettableEnumValueSelect;
import com.bitwig.extensions.framework.di.Context;

public class MenuConfigure {
    
    private final MixerSectionHardware hwElements;
    private final DisplayManager displayManager;
    private final ViewControl viewControl;
    private final Context context;
    private final BeatTimeFormatter formatter;
    private final Transport transport;
    
    public MenuConfigure(final Context context, final MixerSectionHardware hwElements,
        final DisplayManager displayManager) {
        this.context = context;
        formatter = context.getService(ControllerHost.class).createBeatTimeFormatter(":", 2, 1, 1, 0);
        this.viewControl = context.getService(ViewControl.class);
        this.hwElements = hwElements;
        this.transport = context.getService(Transport.class);
        this.displayManager = displayManager;
    }
    
    public LayerGroup createMetroMenu() {
        final IEnumDisplayValue preRoll = new SettableEnumValueSelect(
            transport.preRoll(), //
            new SettableEnumValueSelect.Value("none", "None", 0), //
            new SettableEnumValueSelect.Value("one_bar", "1bar", 3), //
            new SettableEnumValueSelect.Value("two_bars", "2bar", 6), //
            new SettableEnumValueSelect.Value("four_bars", "4bar", 11));
        
        final MenuBuilder builder = new MenuBuilder("METRO", context, hwElements, displayManager);
        builder.addToggleParameterMenu("Pre ->", transport.isMetronomeAudibleDuringPreRoll());
        builder.addEnumValue("Roll", preRoll);
        builder.addValue("Clck.Vol", transport.metronomeVolume(), RingDisplayType.FILL_LR, 2.0);
        builder.addToggleParameterMenu("M.Tick", transport.isMetronomeTickPlaybackEnabled());
        return builder.getLayerGroup();
    }
    
    public LayerGroup createSslMenu() {
        final MenuBuilder builder = new MenuBuilder("SSL_DEVICES", context, hwElements, displayManager);
        builder.addActionMenu("+Meter", () -> insertSslDevice(SslPlugins.METER));
        builder.addActionMenu("+4K B", () -> insertSslDevice(SslPlugins.S4K_B));
        builder.addActionMenu("+4K E", () -> insertSslDevice(SslPlugins.S4K_E));
        builder.addActionMenu("+Ch.Strip", () -> insertSslDevice(SslPlugins.CHANNEL_STRIP));
        builder.addActionMenu("+Comp", () -> insertSslDevice(SslPlugins.BUS_COMPRESSOR));
        builder.addActionMenu("+360Link", () -> insertSslDevice(SslPlugins.LINK_360));
        return builder.getLayerGroup();
    }
    
    private void insertSslDevice(final SslPlugins device) {
        viewControl.getCursorDeviceControl().insertVst3Device(device.getId());
    }
    
    public LayerGroup createGrooveMenu() {
        final Groove groove = context.getService(ControllerHost.class).createGroove();
        final MenuBuilder builder = new MenuBuilder("GROOVE", context, hwElements, displayManager);
        
        builder.addToggleParameterMenu("Groove", groove.getEnabled());
        builder.addValue("Sfl.Rt", groove.getShuffleRate().value(), RingDisplayType.FILL_LR, 0.1);
        builder.addValue("Sfl.Am", groove.getShuffleAmount().value(), RingDisplayType.FILL_LR, 2);
        builder.fillNext();
        builder.addValue("Acc.Rt", groove.getAccentRate().value(), RingDisplayType.FILL_LR, 0.1);
        builder.addValue("Acc.Am", groove.getAccentAmount().value(), RingDisplayType.FILL_LR, 2);
        builder.addValue("Acc.Ph", groove.getAccentPhase().value(), RingDisplayType.FILL_LR, 2);
        builder.addToggleParameterMenu("Fill", transport.isFillModeActive());
        
        return builder.getLayerGroup();
    }
    
    public LayerGroup createLoopMenu() {
        final MenuBuilder builder = new MenuBuilder("LOOP", context, hwElements, displayManager);
        
        final SettableBeatTimeValue cycleStart = transport.arrangerLoopStart();
        final SettableBeatTimeValue cycleLength = transport.arrangerLoopDuration();
        
        builder.addToggleParameterMenu("LOOP", transport.isArrangerLoopEnabled());
        builder.fillNext();
        builder.addPositionAdjustment("START", cycleStart, formatter);
        builder.addPositionAdjustment("LENGTH", cycleLength, formatter);
        return builder.getLayerGroup();
    }
    
    public LayerGroup createTempoMenu() {
        final MenuBuilder builder = new MenuBuilder("TEMPO", context, hwElements, displayManager);
        builder.addIncParameter(transport.tempo(), 1.0);
        builder.addActionMenu("TAP", transport::tapTempo);
        return builder.getLayerGroup();
    }
    
    public LayerGroup createCueMarkerMenu(final CueMarkerBank cueMarkerBank) {
        final MenuBuilder builder = new MenuBuilder("CUE_MARKER", context, hwElements, displayManager);
        for (int i = 0; i < 8; i++) {
            final CueMarker cueMarker = cueMarkerBank.getItemAt(i);
            cueMarker.exists().markInterested();
            builder.addCueMenu(cueMarker, transport, formatter);
        }
        return builder.getLayerGroup();
    }
    
    public LayerGroup createViewZoomMenu() {
        final Arranger arranger = viewControl.getArranger();
        final DetailEditor detailEditor = viewControl.getDetailEditor();
        final MenuBuilder builder = new MenuBuilder("ZOOM", context, hwElements, displayManager);
        
        builder.addStepValue(arranger.zoomLevel(), () -> arranger.zoomToFit(), "ARRANGE");
        builder.addStepValue(detailEditor.zoomLevel(), () -> detailEditor.zoomToFit(), "DETAIL");
        builder.addStepValue(arranger.zoomLaneHeightsAllStepper(), () -> {}, "ALL.TR");
        builder.addStepIncDecValue(
            arranger.zoomInLaneHeightsSelectedAction(), arranger.zoomOutLaneHeightsSelectedAction(), "SEl.TR");
        builder.addToggleParameterMenu("PB.Flw", arranger.isPlaybackFollowEnabled());
        builder.addToggleParameterMenu("CL.VIS", arranger.isClipLauncherVisible());
        builder.addToggleParameterMenu("FX.VIS", arranger.areEffectTracksVisible());
        builder.addToggleParameterMenu("TL.VIS", arranger.isTimelineVisible());
        
        return builder.getLayerGroup();
    }
    
}
