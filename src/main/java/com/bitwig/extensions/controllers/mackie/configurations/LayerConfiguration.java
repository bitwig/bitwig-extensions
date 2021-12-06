package com.bitwig.extensions.controllers.mackie.configurations;

import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;
import com.bitwig.extensions.controllers.mackie.devices.DeviceManager;
import com.bitwig.extensions.controllers.mackie.devices.DeviceTypeFollower;
import com.bitwig.extensions.controllers.mackie.display.DisplayLayer;
import com.bitwig.extensions.controllers.mackie.layer.EncoderLayer;
import com.bitwig.extensions.controllers.mackie.section.InfoSource;
import com.bitwig.extensions.controllers.mackie.section.MixControl;
import com.bitwig.extensions.controllers.mackie.value.ModifierValueObject;
import com.bitwig.extensions.framework.Layer;

import java.util.function.IntConsumer;

public abstract class LayerConfiguration {
   private final String name;
   protected final MixControl mixControl;
   protected IntConsumer navigateHorizontalHandler;
   protected IntConsumer navigateVerticalHandler;
   private boolean active;

   public LayerConfiguration(final String name, final MixControl mixControl) {
      this.name = name;
      this.mixControl = mixControl;
   }

   public MixControl getMixControl() {
      return mixControl;
   }

   public MackieMcuProExtension getDriver() {
      return mixControl.getDriver();
   }

   public void setNavigateHorizontalHandler(final IntConsumer navigateHorizontalHandler) {
      this.navigateHorizontalHandler = navigateHorizontalHandler;
   }

   public void setNavigateVerticalHandler(final IntConsumer navigateVerticalHandler) {
      this.navigateVerticalHandler = navigateVerticalHandler;
   }

   // protected abstract void navigateLeftRight(int direction);

   public abstract Layer getFaderLayer();

   public abstract EncoderLayer getEncoderLayer();

   /**
    * Determine if changing the modifier button state should have any effect on the
    * layout.
    *
    * @param modvalue the modifier value
    * @return true if change of modifier should lead to change of display
    */
   public boolean applyModifier(final ModifierValueObject modvalue) {
      return false;
   }

   public Layer getButtonLayer() {
      return mixControl.getActiveMixGroup().getMixerButtonLayer();
   }

   public abstract DisplayLayer getDisplayLayer(int which);

   public abstract DisplayLayer getBottomDisplayLayer(int which);

   public DeviceManager getDeviceManager() {
      return null;
   }

   public String getName() {
      return name;
   }

   public void navigateHorizontal(final int direction) {
      if (navigateHorizontalHandler != null) {
         navigateHorizontalHandler.accept(direction);
      }
   }

   public void navigateVertical(final int direction) {
      if (navigateVerticalHandler != null) {
         navigateVerticalHandler.accept(direction);
      }
   }

   public boolean enableInfo(final InfoSource navVertical) {
      return false;
   }

   public boolean disableInfo() {
      return false;
   }

   /**
    * Let the view react to NAME-VALUE being pressed or released.
    *
    * @param pressed NAME-VALUE being pressed(=true) or released(=false)
    * @return if this notification had any effect, return false if nothing happens
    */
   public boolean notifyDisplayName(final boolean pressed) {
      return false;
   }

   public void setActive(final boolean active) {
      if (active == this.active) {
         return;
      }
      this.active = active;
      if (this.active) {
         doActivate();
      } else {
         doDeactivate();
      }
   }

   public boolean isActive() {
      return active;
   }

   public void doActivate() {

   }

   public void doDeactivate() {

   }

   public void setCurrentFollower(final DeviceTypeFollower follower) {
   }

}
