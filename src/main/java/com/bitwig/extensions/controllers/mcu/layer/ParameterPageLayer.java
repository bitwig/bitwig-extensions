package com.bitwig.extensions.controllers.mcu.layer;

import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extensions.controllers.mcu.bindings.display.StringRowDisplayBinding;
import com.bitwig.extensions.controllers.mcu.display.DisplayRow;
import com.bitwig.extensions.controllers.mcu.value.Orientation;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.values.BasicStringValue;

public class ParameterPageLayer extends MixerModeLayer {
    
    private final CursorRemoteControlsPage remotePages;
    private final Layer topRowValueLayer;
    private final Layer bottomRowValueLayer;
    private final BasicStringValue topRowInfoText;
    private final BasicStringValue bottomRowInfoText;
    private String[] pages = new String[0];
    private int remoteIndex;
    private int pageCount;
    private final String label;
    private String currentPageName = "";
    protected String infoText = null;
    
    public ParameterPageLayer(final Layers layers, final ControlMode mode, final MixerSection mixer, final String label,
        final CursorRemoteControlsPage remotePages) {
        super(mode, mixer);
        topRowInfoText = new BasicStringValue("");
        bottomRowInfoText = new BasicStringValue(label);
        this.label = label;
        this.topRowValueLayer = new Layer(layers, "INFO_LABEL_%s".formatted(mode));
        this.bottomRowValueLayer = new Layer(layers, "INFO_VALUE_%s".formatted(mode));
        this.remotePages = remotePages;
        
        this.topRowValueLayer.addBinding(
            new StringRowDisplayBinding(mixer.getDisplayManager(), mode, DisplayRow.LABEL, mixer.getSectionIndex(),
                topRowInfoText));
        this.bottomRowValueLayer.addBinding(
            new StringRowDisplayBinding(mixer.getDisplayManager(), mode, DisplayRow.VALUE, mixer.getSectionIndex(),
                bottomRowInfoText));
        remotePages.pageNames().addValueObserver(pages -> {
            this.pages = pages;
            update();
        });
        remotePages.selectedPageIndex().addValueObserver(pagesIndex -> {
            this.remoteIndex = pagesIndex;
            update();
        });
        remotePages.pageCount().addValueObserver(pages -> {
            this.pageCount = pages;
            update();
        });
    }
    
    private void update() {
        updateRemotePageName();
        if (active) {
            reassign();
        }
    }
    
    private void updateRemotePageName() {
        if (this.remoteIndex != -1 && this.remoteIndex < this.pages.length) {
            this.currentPageName = pages[this.remoteIndex];
        } else {
            this.currentPageName = "";
        }
        if (pageCount == 0) {
            topRowInfoText.set("No Parameter Pages set");
            bottomRowInfoText.set("Configure in Bitwig");
        } else if (remoteIndex == -1) {
            topRowInfoText.set("");
            bottomRowInfoText.set("No Parameter Page selected");
        } else if (this.pages != null && remoteIndex < this.pages.length) {
            topRowInfoText.set("");
            bottomRowInfoText.set("Page: %s".formatted(pages[this.remoteIndex]));
        }
    }
    
    public CursorRemoteControlsPage getRemotePages() {
        return remotePages;
    }
    
    @Override
    public void handleInfoState(final boolean start, final Orientation orientation) {
        if (active) {
            if (start) {
                infoText = "%s Remote Page: %s".formatted(label, currentPageName);
            } else {
                infoText = null;
            }
            reassign();
        }
    }
    
    @Override
    public void assign() {
        final ControlMode mainMode = !mixer.isFlipped() ? mode : ControlMode.VOLUME;
        final ControlMode lowMode = mixer.isFlipped() ? mode : ControlMode.VOLUME;
        encoderLayer = mixer.getLayerSource(mainMode).getEncoderLayer();
        faderLayer = mixer.getLayerSource(lowMode).getFaderLayer();
        
        if (mixer.hasLowerDisplay()) {
            //assignDualDisplay();
        } else {
            assignSingleDisplay(mainMode, lowMode);
        }
        assignIfMenuModeActive();
    }
    
    
    private void assignSingleDisplay(final ControlMode mainMode, final ControlMode lowMode) {
        final boolean touched = mixer.isTouched();
        final boolean nameValue = mixer.isNameValue();
        final ControlMode displayMode = touched ? lowMode : mainMode;
        if (displayMode == mode) {
            displayLabelLayer = mixer.getLayerSource(displayMode).getDisplayLabelLayer();
            if (nameValue) {
                displayValueLayer = bottomRowValueLayer;
            } else {
                displayValueLayer = mixer.getLayerSource(displayMode).getDisplayValueLayer();
            }
            if (pageCount == 0 || remoteIndex == -1) {
                displayLabelLayer = topRowValueLayer;
                displayValueLayer = bottomRowValueLayer;
            }
        } else {
            displayLabelLayer = nameValue
                ? mixer.getLayerSource(ControlMode.VOLUME).getDisplayLabelLayer()
                : mixer.getTrackDisplayLayer();
            displayValueLayer = mixer.getLayerSource(displayMode).getDisplayValueLayer();
        }
        if (infoText != null) {
            topRowInfoText.set(infoText);
            displayLabelLayer = topRowValueLayer;
        }
    }
}
