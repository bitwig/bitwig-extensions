package com.bitwig.extensions.controllers.allenheath.xonek3.layer;

import java.util.List;

import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extensions.controllers.allenheath.xonek3.DeviceHwElements;
import com.bitwig.extensions.controllers.allenheath.xonek3.TrackSpecControl;
import com.bitwig.extensions.controllers.allenheath.xonek3.ViewControl;
import com.bitwig.extensions.controllers.allenheath.xonek3.XoneHwElements;
import com.bitwig.extensions.controllers.allenheath.xonek3.XoneK3GlobalStates;
import com.bitwig.extensions.controllers.allenheath.xonek3.color.XoneRgbColor;
import com.bitwig.extensions.controllers.allenheath.xonek3.control.XoneRgbButton;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

public class EqControlLayer extends Layer {
    
    private final XoneK3GlobalStates globalStates;
    private final Layer shiftLayer;
    
    public EqControlLayer(final Layers layers, final ViewControl viewControl, final XoneHwElements hwElements,
        final XoneK3GlobalStates globalStates) {
        super(layers, "DJ_EQ");
        this.globalStates = globalStates;
        shiftLayer = new Layer(layers, "EQ_SHIFT_LAYER");
        this.globalStates.getShiftHeld().addValueObserver(active -> {
            if (isActive()) {
                shiftLayer.setIsActive(active);
            }
        });
        
        final List<TrackSpecControl> specControls = viewControl.getSpecControls();
        for (int i = 0; i < specControls.size(); i++) {
            final TrackSpecControl specControl = specControls.get(i);
            bind(i, specControl, hwElements.getDeviceElements(i / 4));
        }
    }
    
    private void bind(final int index, final TrackSpecControl control, final DeviceHwElements hwElements) {
        final int controlIndex = index % 4;
        final XoneRgbButton button1 = hwElements.getKnobButtons().get(controlIndex);
        button1.bindLight(
            this, () -> killButtonColor(
                control, control.getHiKill(), XoneRgbColor.WHITE, XoneRgbColor.BLUE_DIM,
                XoneRgbColor.BLUE));
        button1.bindPressed(
            this, () -> {
                if (control.eqExists()) {
                    handleToggle(control.getHiKill());
                } else {
                    control.insertEq();
                }
            });
        
        button1.bindLight(
            shiftLayer, () -> !control.eqExists()
                ? XoneRgbColor.WHITE
                : (control.isDjEqActive() ? XoneRgbColor.ORANGE : XoneRgbColor.ORANGE_DIM));
        button1.bindPressed(
            shiftLayer, () -> {
                if (control.eqExists()) {
                    control.toggleDjEqActive();
                }
            });
        
        final XoneRgbButton button2 = hwElements.getKnobButtons().get(4 + controlIndex);
        button2.bindLight(
            this, () -> killButtonColor(
                control, control.getMidKill(), XoneRgbColor.OFF, XoneRgbColor.YELLOW_DIM,
                XoneRgbColor.YELLOW));
        button2.bindPressed(this, () -> handleToggle(control.getMidKill()));
        
        final XoneRgbButton button3 = hwElements.getKnobButtons().get(8 + controlIndex);
        button3.bindLight(
            this, () -> killButtonColor(
                control, control.getLowKill(), XoneRgbColor.OFF, XoneRgbColor.RED_DIM,
                XoneRgbColor.RED));
        button3.bindPressed(this, () -> handleToggle(control.getLowKill()));
        
        this.bind(hwElements.getKnobs().get(controlIndex), control.getHiGain());
        this.bind(hwElements.getKnobs().get(4 + controlIndex), control.getMidGain());
        this.bind(hwElements.getKnobs().get(8 + controlIndex), control.getLowGain());
        
        shiftLayer.bind(hwElements.getKnobs().get(controlIndex), control.getHiFreq());
        shiftLayer.bind(hwElements.getKnobs().get(8 + controlIndex), control.getLowFreq());
    }
    
    private XoneRgbColor killButtonColor(final TrackSpecControl control, final Parameter toggleParam,
        final XoneRgbColor nonColor, final XoneRgbColor offColor, final XoneRgbColor onColor) {
        if (!control.eqExists()) {
            return nonColor;
        }
        return toggleParam.value().get() > 0 ? offColor : onColor;
    }
    
    private void handleToggle(final Parameter toggleParam) {
        if (toggleParam.get() == 0) {
            toggleParam.value().set(1.0);
        } else {
            toggleParam.value().set(0);
        }
    }
    
    @Override
    protected void onActivate() {
        super.onActivate();
    }
    
    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        shiftLayer.setIsActive(false);
    }
}
