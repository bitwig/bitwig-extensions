package com.bitwig.extensions.controllers.arturia.minilab3;

import java.util.EnumSet;

import com.bitwig.extensions.framework.Layer;

public class PadBankLayerSelector
{
    private final MiniLab3Extension driver;
    private EnumSet<PadBank> padBankSelection;

    private final Layer layer;

    public PadBankLayerSelector(final MiniLab3Extension driver, final Layer layer, EnumSet<PadBank> availablePadBanks)
    {
        this.driver = driver;
        this.layer = layer;
        var host = driver.getHost();
        padBankSelection = EnumSet.noneOf(PadBank.class);
        for (PadBank value : availablePadBanks)
        {
            var setting = host.getPreferences().getBooleanSetting(value.displayName(), layer.getName(), false);
            setting.addValueObserver(active -> handleSettingChanged(value, active));
        }
    }

    public PadBankLayerSelector(final MiniLab3Extension driver, final Layer layer)
    {
        this(driver, layer, EnumSet.allOf(PadBank.class));
    }

    private void handleSettingChanged(PadBank padBank, boolean active)
    {
        if (active)
            padBankSelection.add(padBank);
        else
            padBankSelection.remove(padBank);
        update();
    }

    public void update()
    {
        PadBank currentPadBank = driver.getPadBankWithTransport().get();
        boolean isActive = padBankSelection.contains(currentPadBank);
        if (layer.isActive() != isActive)
            MiniLab3Extension.println("%s layer : %s", layer.getName(), isActive ? "ON" : "OFF");
        layer.setIsActive(isActive);
    }
}
