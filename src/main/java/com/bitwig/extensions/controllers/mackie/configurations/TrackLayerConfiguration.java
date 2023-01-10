package com.bitwig.extensions.controllers.mackie.configurations;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;
import com.bitwig.extensions.controllers.mackie.StringUtil;
import com.bitwig.extensions.controllers.mackie.VPotMode;
import com.bitwig.extensions.controllers.mackie.bindings.ButtonBinding;
import com.bitwig.extensions.controllers.mackie.devices.CursorDeviceControl;
import com.bitwig.extensions.controllers.mackie.devices.DeviceManager;
import com.bitwig.extensions.controllers.mackie.devices.DeviceTypeFollower;
import com.bitwig.extensions.controllers.mackie.devices.ParameterPage;
import com.bitwig.extensions.controllers.mackie.display.DisplayLayer;
import com.bitwig.extensions.controllers.mackie.display.MotorSlider;
import com.bitwig.extensions.controllers.mackie.display.RingDisplayType;
import com.bitwig.extensions.controllers.mackie.layer.DisplayLocation;
import com.bitwig.extensions.controllers.mackie.layer.EncoderLayer;
import com.bitwig.extensions.controllers.mackie.layer.MixerLayerGroup;
import com.bitwig.extensions.controllers.mackie.section.InfoSource;
import com.bitwig.extensions.controllers.mackie.section.MixControl;
import com.bitwig.extensions.controllers.mackie.section.MixerSectionHardware;
import com.bitwig.extensions.controllers.mackie.section.ParamElement;
import com.bitwig.extensions.controllers.mackie.value.ModifierValueObject;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;

import java.util.Optional;
import java.util.function.BiConsumer;

public class TrackLayerConfiguration extends LayerConfiguration {

   protected final Layer faderLayer;
   protected final EncoderLayer encoderLayer;
   protected final DisplayLayer displayLayer;
   protected final DisplayLayer infoLayer;

   protected DeviceManager deviceManager;

   private CursorDeviceControl cursorDeviceControl;
   protected DeviceMenuConfiguration menuConfig;

   public TrackLayerConfiguration(final String name, final MixControl mixControl) {
      super(name, mixControl);
      final Layers layers = this.mixControl.getDriver().getLayers();
      final int sectionIndex = mixControl.getHwControls().getSectionIndex();

      faderLayer = new Layer(layers, name + "_FADER_LAYER_" + sectionIndex);
      encoderLayer = new EncoderLayer(mixControl, name + "_ENCODER_LAYER_" + sectionIndex);
      displayLayer = new DisplayLayer(name, sectionIndex, layers, mixControl.getHwControls());
      infoLayer = new DisplayLayer(name + "_INFO", sectionIndex, layers, mixControl.getHwControls());
      infoLayer.enableFullTextMode(true);
   }

   public void setDeviceManager(final DeviceManager deviceManager, final DeviceMenuConfiguration menuConfig) {
      final MackieMcuProExtension driver = mixControl.getDriver();
      cursorDeviceControl = driver.getCursorDeviceControl();
      this.deviceManager = deviceManager;
      this.deviceManager.setInfoLayer(infoLayer);
      this.menuConfig = menuConfig;

      final CursorRemoteControlsPage remotes = cursorDeviceControl.getRemotes();
      final PinnableCursorDevice device = cursorDeviceControl.getCursorDevice();
      if (remotes != null) {
         remotes.pageCount()
            .addValueObserver(
               count -> evaluateTextDisplay(count, deviceManager.isSpecificDevicePresent(), device.name().get()));
      }
      device.name()
         .addValueObserver(
            name -> evaluateTextDisplay(deviceManager.getPageCount(), deviceManager.isSpecificDevicePresent(), name));
   }

   @Override
   public void doActivate() {
      if (menuConfig != null) {
         menuConfig.setDeviceManager(deviceManager);
      }
   }

   @Override
   public DeviceManager getDeviceManager() {
      return deviceManager;
   }

   @Override
   public boolean applyModifier(final ModifierValueObject modvalue) {
      if (menuConfig != null) {
         return menuConfig.applyModifier(modvalue);
      }
      return false;
   }

   public void registerFollowers(final DeviceTypeFollower... deviceTypeFollowers) {
      final PinnableCursorDevice cursorDevice = cursorDeviceControl.getCursorDevice();

      cursorDevice.exists().addValueObserver(cursorExists -> {
         if (isActive() && !cursorExists && deviceManager.isSpecificDevicePresent()) {
            cursorDevice.selectDevice(deviceManager.getCurrentFollower().getFocusDevice());
            mixControl.getIsMenuHoldActive().set(false);
         }
      });

      for (final DeviceTypeFollower deviceTypeFollower : deviceTypeFollowers) {
         final Device focusDevice = deviceTypeFollower.getFocusDevice();

         focusDevice.exists().addValueObserver(exist -> {
            if (deviceManager.getCurrentFollower() == deviceTypeFollower && isActive()) {
               evaluateTextDisplay(deviceManager.getPageCount(), exist, cursorDevice.name().get());
            }
         });
      }
   }

   @Override
   public void setCurrentFollower(final DeviceTypeFollower follower) {
      if (deviceManager == null) {
         return;
      }
      deviceManager.setCurrentFollower(follower);
      evaluateTextDisplay(deviceManager.getPageCount(), deviceManager.isSpecificDevicePresent(),
         cursorDeviceControl.getCursorDevice().name().get());
   }

   private void evaluateTextDisplay(final int count, final boolean exists, final String deviceName) {
      if (deviceManager == null) {
         return;
      }
      if (menuConfig != null) {
         menuConfig.evaluateTextDisplay(deviceName);
      }
      final CursorRemoteControlsPage remotes = cursorDeviceControl.getRemotes();
      if (remotes != null) {
         if (!exists || deviceName.length() == 0) {
            setMainText(deviceManager.getCurrentFollower().getPotMode());
         } else if (count == 0) {
            displayLayer.setMainText(deviceName + " has no Parameter Pages",
               "<<configure Parameter Pages in Bitwig Studio>>", true);
            displayLayer.enableFullTextMode(true);
         } else {
            displayLayer.enableFullTextMode(false);
         }
      } else if (!exists) {
         setMainText(deviceManager.getCurrentFollower().getPotMode());
      } else {
         displayLayer.enableFullTextMode(false);
      }
   }

   private void setMainText(final VPotMode mode) {
      final String line1 = String.format("no %s on track", mode.getTypeDisplayName());
      final String line2;
      if (mode.getDeviceName() == null) {
         line2 = String.format("<< press %s again to browse >>",
            mode.getButtonDescription(mixControl.getDriver().getControllerConfig()));
      } else {
         line2 = String.format("<< press %s again to create %s device >>",
            mode.getButtonDescription(mixControl.getDriver().getControllerConfig()), mode.getDeviceName());
      }
      displayLayer.setMainText(line1, line2, true);
      displayLayer.enableFullTextMode(true);
   }

   @Override
   public Layer getFaderLayer() {
      if (mixControl.getActiveMixGroup().isFlipped()) {
         return faderLayer;
      }
      return mixControl.getActiveMixGroup().getFaderLayer(ParamElement.VOLUME);
   }

   private boolean isMenuActive() {
      return menuConfig != null && mixControl.getIsMenuHoldActive().get();
   }

   @Override
   public EncoderLayer getEncoderLayer() {
      if (isMenuActive()) {
         return menuConfig.getEncoderLayer();
      }

      final MixerLayerGroup activeGroup = mixControl.getActiveMixGroup();
      final Optional<EncoderLayer> overrideLayer = activeGroup.getModeEncoderLayer();
      if (overrideLayer.isPresent()) {
         return overrideLayer.get();
      }
      final MixerLayerGroup activeMixGroup = mixControl.getActiveMixGroup();
      if (activeMixGroup.isFlipped()) {
         return activeMixGroup.getEncoderLayer(ParamElement.VOLUME);
      }
      return encoderLayer;
   }

   @Override
   public DisplayLayer getDisplayLayer(final int which) {
      if (isMenuActive()) {
         return menuConfig.getDisplayLayer();
      }
      if (deviceManager != null && deviceManager.getInfoSource() != null) {
         return infoLayer;
      }
      final MixerLayerGroup activeGroup = mixControl.getActiveMixGroup();
      if (which == 0) {
         return activeGroup.getModeDisplayLayer().orElse(displayLayer);
      }
      return activeGroup.getDisplayConfiguration(ParamElement.VOLUME, DisplayLocation.TOP)
         .setShowTrackInformation(true);
   }

   @Override
   public DisplayLayer getBottomDisplayLayer(final int which) {
      if (which == 0) {
         final MixerLayerGroup activeMixGroup = mixControl.getActiveMixGroup();
         return activeMixGroup.getDisplayConfiguration(ParamElement.VOLUME, DisplayLocation.BOTTOM)
            .setShowTrackInformation(true);
      }
      return displayLayer;
   }

   @Override
   public boolean enableInfo(final InfoSource type) {
      if (deviceManager != null) {
         deviceManager.enableInfo(type);
      }
      return true;
   }

   @Override
   public boolean disableInfo() {
      if (deviceManager != null) {
         deviceManager.disableInfo();
      }
      return true;
   }

   public void addBindingFader(final int index, final Track track, final MotorSlider slider) {
      slider.bindParameter(faderLayer, track.volume());
      displayLayer.bindTitle(8, track.name());
      displayLayer.bindDisplayParameterValue(8, track.volume(), s -> StringUtil.condenseVolumeValue(s, 7));
   }

   public void addBinding(final int index, final ParameterPage parameter,
                          final BiConsumer<Integer, ParameterPage> resetAction) {
      final MixerSectionHardware hwControls = mixControl.getHwControls();

      encoderLayer.addBinding(parameter.getRelativeEncoderBinding(hwControls.getEncoder(index)));
      encoderLayer.addBinding(parameter.createRingBinding(hwControls.getRingDisplay(index)));
      encoderLayer.addBinding(new ButtonBinding(hwControls.getEncoderPress(index),
         hwControls.createAction(() -> resetAction.accept(index, parameter))));

      faderLayer.addBinding(parameter.getFaderBinding(hwControls.getVolumeFader(index)));
      faderLayer.addBinding(parameter.createFaderBinding(hwControls.getMotorFader(index)));

      displayLayer.bind(index, parameter);
      parameter.resetBindings();
   }

   public void addBinding(final int index, final Parameter parameter, final RingDisplayType type) {
      final MixerSectionHardware hwControls = mixControl.getHwControls();

      faderLayer.addBinding(hwControls.createMotorFaderBinding(index, parameter));
      faderLayer.addBinding(hwControls.createFaderParamBinding(index, parameter));
      faderLayer.addBinding(hwControls.createFaderTouchPressedBinding(index, () -> parameter.touch(true)));
      faderLayer.addBinding(hwControls.createFaderTouchReleaseBinding(index, () -> {
         parameter.touch(false);
         if (mixControl.getModifier().isShift()) {
            parameter.reset();
         }
      }));
      encoderLayer.addBinding(hwControls.createEncoderPressBinding(index, parameter));
      encoderLayer.addBinding(hwControls.createEncoderToParamBinding(index, parameter));
      encoderLayer.addBinding(hwControls.createRingDisplayBinding(index, parameter, type));
      displayLayer.bindTitle(index, parameter.name());
      displayLayer.bindParameterValue(index, parameter);
   }

}
