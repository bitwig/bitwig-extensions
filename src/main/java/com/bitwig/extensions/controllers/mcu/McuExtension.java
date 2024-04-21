package com.bitwig.extensions.controllers.mcu;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNames;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extensions.controllers.mcu.config.ControllerConfig;
import com.bitwig.extensions.controllers.mcu.control.MainHardwareSection;
import com.bitwig.extensions.controllers.mcu.control.MixerSectionHardware;
import com.bitwig.extensions.controllers.mcu.definitions.AbstractMcuControllerExtensionDefinition;
import com.bitwig.extensions.controllers.mcu.layer.MainSection;
import com.bitwig.extensions.controllers.mcu.layer.MixerSection;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.di.Context;

public class McuExtension extends ControllerExtension {
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("hh:mm:ss SSS");
    private static ControllerHost debugHost;
    private Layer mainLayer;
    private HardwareSurface surface;
    private final ControllerConfig controllerConfig;
    private final List<MixerSectionHardware> mixerHardwareSections = new ArrayList<>();
    private final List<MainHardwareSection> mainHardwareSections = new ArrayList<>();
    private final List<MidiProcessor> midiProcessors = new ArrayList<>();
    
    public static void println(final String format, final Object... args) {
        if (debugHost != null) {
            final LocalDateTime now = LocalDateTime.now();
            debugHost.println(now.format(DF) + " > " + String.format(format, args));
        }
    }
    
    public McuExtension(final AbstractMcuControllerExtensionDefinition definition, final ControllerHost host,
        final ControllerConfig controllerConfig) {
        super(definition, host);
        this.controllerConfig = controllerConfig;
        this.controllerConfig.setNrOfExtenders(definition.getNrOfExtenders());
    }
    
    @Override
    public void init() {
        final ControllerHost host = getHost();
        debugHost = host;
        //this.project = getHost().getProject();
        final Context diContext = new Context(this);
        diContext.registerService(ControllerConfig.class, controllerConfig);
        mainLayer = diContext.createLayer("MAIN_LAYER");
        surface = diContext.getService(HardwareSurface.class);
        MainSection mainControl = null;
        final List<MainSection> mainSections = new ArrayList<>();
        final List<MixerSection> mixerSections = new ArrayList<>();
        
        //showPortINfos(PlatformType.WINDOWS);
        
        for (int portIndex = 0; portIndex < controllerConfig.getNrOfExtenders() + 1; portIndex++) {
            final MidiProcessor midiProcessor = new MidiProcessor(diContext, portIndex);
            midiProcessors.add(midiProcessor);
            
            if (portIndex == 0 || !controllerConfig.isSingleMainUnit()) {
                final MainHardwareSection mainHardwareSection =
                    new MainHardwareSection(diContext, midiProcessor, portIndex);
                mainHardwareSections.add(mainHardwareSection);
                mainControl = new MainSection(diContext, mainHardwareSection);
                mainSections.add(mainControl);
            }
            
            final MixerSectionHardware mixerSectionHardware =
                new MixerSectionHardware(portIndex, diContext, midiProcessor, portIndex * 8);
            final MixerSection mixerLayer =
                new MixerSection(diContext, mixerSectionHardware, mainControl, portIndex, portIndex == 0);
            mixerHardwareSections.add(mixerSectionHardware);
            mixerSections.add(mixerLayer);
        }
        diContext.activate();
        mainSections.forEach(MainSection::activate);
        mixerSections.forEach(MixerSection::activate);
        if (controllerConfig.getForceUpdateOnStartup() != -1) {
            host.scheduleTask(this::doForceUpdate, controllerConfig.getForceUpdateOnStartup());
        }
    }
    
    private void doForceUpdate() {
        println(" FORCE UPDATE ");
        for (final MidiProcessor midiProcessor : midiProcessors) {
            midiProcessor.forceUpdate();
        }
    }
    
    private void showPortINfos(final PlatformType platformType) {
        final AutoDetectionMidiPortNamesList portNames =
            getExtensionDefinition().getAutoDetectionMidiPortNamesList(platformType);
        for (int i = 0; i < portNames.getCount(); i++) {
            println("PORTINFOS - %d LCL=%s", i + 1, Locale.getDefault());
            final AutoDetectionMidiPortNames adpm = portNames.getPortNamesAt(i);
            final String[] inputNames = adpm.getInputNames();
            final String[] outputNames = adpm.getOutputNames();
            println(" ###### INPUTS #########");
            for (int j = 0; j < inputNames.length; j++) {
                println(" [%s]", inputNames[j]);
            }
            println(" ###### OUTPUTS #########");
            for (int j = 0; j < outputNames.length; j++) {
                println(" [%s]", outputNames[j]);
            }
        }
    }
    
    @Override
    public void exit() {
        midiProcessors.forEach(MidiProcessor::exit);
        mainHardwareSections.forEach(MainHardwareSection::clearAll);
        mixerHardwareSections.forEach(MixerSectionHardware::clearAll);
    }
    
    @Override
    public void flush() {
        surface.updateHardware();
    }
}
