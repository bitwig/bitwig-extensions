package com.bitwig.extensions.controllers.arturia.minilab3;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.Application;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.Transport;

public class TransportButtonAction
{
    private SettableEnumValue behaviorSetting;
    private TransportButtonBehavior behavior;

    private OledDisplay oled;

    private Transport transport;

    private Application application;

    public TransportButtonAction(
        final ControllerExtension controller,
        final String buttonName,
        OledDisplay oled,
        Transport transport,
        Application application)
    {
        var host = controller.getHost();
        this.oled = oled;
        this.transport = transport;
        this.application = application;
        behaviorSetting = host.getPreferences().getEnumSetting(String.format("%s button behavior", buttonName),
            "Transport Pads",
            new String[] { TransportButtonBehavior.LOOP.displayName(), TransportButtonBehavior.TAP.displayName(),
                    TransportButtonBehavior.METRONOME.displayName(), TransportButtonBehavior.UNDO.displayName() },
            buttonName);
        behaviorSetting.addValueObserver(this::handleBehaviorChanged);
    }

    public void pressedAction()
    {
        switch (behavior)
        {
        case LOOP -> {
            transport.isArrangerLoopEnabled().toggle();
            final boolean loopEnabled = transport.isArrangerLoopEnabled().get();
            oled.sendText(DisplayMode.LOOP_VALUE, "Loop Mode", loopEnabled ? "ON" : "OFF");
        }
        case TAP -> {
            transport.tapTempo();
            final int tempo = (int)Math.round(transport.tempo().value().getRaw());
            oled.sendText(DisplayMode.TEMPO, "Tap Tempo", String.format("%d BPM", tempo));
        }
        case METRONOME -> {
            oled.sendText(DisplayMode.STATE_INFO, "Metronome",
                transport.isMetronomeEnabled().get() ? "Off" : "On");
            transport.isMetronomeEnabled().toggle();
        }
        case UNDO -> {
            oled.sendText(DisplayMode.STATE_INFO, "Undo", application.canUndo().get() ? "OK" : "Impossible");
            application.undo();
        }
        }
    }

    public RgbLightState getLightState(boolean pressed)
    {
        return switch (behavior)
        {
        case LOOP -> transport.isArrangerLoopEnabled().get() ? RgbLightState.ORANGE
            : RgbLightState.ORANGE_DIMMED;
        case TAP -> pressed ? RgbLightState.WHITE : RgbLightState.WHITE_DIMMED;
        case METRONOME -> transport.isMetronomeEnabled().get() ? RgbLightState.MAGENTA
            : RgbLightState.MAGENTA_DIMMED;
        case UNDO -> application.canUndo().get() ? RgbLightState.YELLOW_DIMMED : RgbLightState.OFF;
        };
    }

    private void handleBehaviorChanged(String behaviorName){
        behavior = TransportButtonBehavior.valueOf(behaviorName.toUpperCase());
    }
}
