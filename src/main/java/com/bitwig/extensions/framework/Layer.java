package com.bitwig.extensions.framework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import com.bitwig.extension.controller.api.AbsoluteHardwarControlBindable;
import com.bitwig.extension.controller.api.AbsoluteHardwareControl;
import com.bitwig.extension.controller.api.BooleanHardwareOutputValue;
import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.ColorValue;
import com.bitwig.extension.controller.api.HardwareAction;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareControl;
import com.bitwig.extension.controller.api.HardwareLight;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extension.controller.api.RelativeHardwareControl;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.TriggerAction;

/**
 * A layer defines a number of bindings between a source object and a target that should be active when the
 * layer is active. A layer on tap will override bindings from a lower layer.
 */
public class Layer
{
   public Layer(final Layers layers, final String name)
   {
      super();
      mLayers = layers;
      mName = name;

      layers.addLayer(this);
   }

   public String getName()
   {
      return mName;
   }

   public Layers getLayers()
   {
      return mLayers;
   }

   public List<Binding> getBindings()
   {
      return Collections.unmodifiableList(mBindings);
   }

   @SuppressWarnings("rawtypes")
   public void addBinding(final Binding binding)
   {
      assert !mBindings.contains(binding);
      assert !isActive();

      mBindings.add(binding);

      binding.setLayer(this);
   }

   public AbsoluteHardwareControlBinding bind(
      final AbsoluteHardwareControl source,
      final AbsoluteHardwarControlBindable target)
   {
      final AbsoluteHardwareControlBinding binding = new AbsoluteHardwareControlBinding(source, target);

      addBinding(binding);

      return binding;
   }

   public RelativeHardwareControlToRangedValueBinding bind(
      final RelativeHardwareControl source,
      final SettableRangedValue target)
   {
      final RelativeHardwareControlToRangedValueBinding binding = new RelativeHardwareControlToRangedValueBinding(
         source, target);

      addBinding(binding);

      return binding;
   }

   public RelativeHardwareControlBinding bind(
      final RelativeHardwareControl source,
      final RelativeHardwarControlBindable target)
   {
      final RelativeHardwareControlBinding binding = new RelativeHardwareControlBinding(source, target);

      addBinding(binding);

      return binding;
   }

   public RelativeHardwareControlBinding bind(
      final RelativeHardwareControl source,
      final Runnable stepForwardsRunnable,
      final Runnable stepBackwardsRunnable)
   {
      final RelativeHardwarControlBindable target = getLayers().getControllerExtension().getHost()
         .createRelativeHardwareControlStepTarget(stepForwardsRunnable, stepBackwardsRunnable);

      return bind(source, target);
   }

   public RelativeHardwareControlBinding bind(
      final RelativeHardwareControl source,
      final TriggerAction stepForwardsAction,
      final TriggerAction stepBackwardsAction)
   {
      final RelativeHardwarControlBindable target = getLayers().getControllerExtension().getHost()
         .createRelativeHardwareControlStepTarget(stepForwardsAction, stepBackwardsAction);

      return bind(source, target);
   }

   public Binding bind(
      final Object actionOwner,
      final HardwareAction source,
      final HardwareActionBindable target)
   {
      final HarwareActionBinding binding = new HarwareActionBinding(actionOwner, source, target);

      addBinding(binding);

      return binding;
   }

   public Binding bind(final Object actionOwner, final HardwareAction source, final Runnable target)
   {
      final HardwareActionRunnableBinding binding = new HardwareActionRunnableBinding(actionOwner, source,
         target);

      addBinding(binding);

      return binding;
   }

   public Binding bindPressed(final HardwareButton button, final Runnable pressedRunnable)
   {
      return bind(button, button.pressedAction(), pressedRunnable);
   }

   public Binding bindPressed(final HardwareButton button, final HardwareActionBindable target)
   {
      return bind(button, button.pressedAction(), target);
   }

   public void bindIsPressed(final HardwareButton button, final SettableBooleanValue target)
   {
      bind(button, button.pressedAction(), target.setToTrueAction());
      bind(button, button.releasedAction(), target.setToFalseAction());

   }

   public void bindIsPressed(final HardwareButton button, final Consumer<Boolean> target)
   {
      bind(button, button.pressedAction(), () -> target.accept(true));
      bind(button, button.releasedAction(), () -> target.accept(false));
   }

   /**
    * Binds pressing of the supplied button to toggling of the target. If the button has an on/off light it
    * will also reflect the value of the target.
    */
   public void bindToggle(final HardwareButton button, final SettableBooleanValue target)
   {
      bind(button, button.pressedAction(), target.toggleAction());

      final HardwareLight backgroundLight = button.backgroundLight();

      if (backgroundLight instanceof OnOffHardwareLight)
         bind(target, (OnOffHardwareLight)button.backgroundLight());
   }

   public Binding bind(final BooleanSupplier source, final BooleanHardwareOutputValue target)
   {
      final BooleanSupplierOutputValueBinding binding = new BooleanSupplierOutputValueBinding(source, target);

      addBinding(binding);

      return binding;
   }

   public Binding bind(final BooleanSupplier source, final OnOffHardwareLight target)
   {
      return bind(source, target.isOn());
   }

   public Binding bind(final BooleanSupplier source, final HardwareControl target)
   {
      return bind(source, (OnOffHardwareLight)target.backgroundLight());
   }

   public Binding bind(final BooleanValue source, final BooleanHardwareOutputValue target)
   {
      source.markInterested();

      final BooleanValueOutputValueBinding binding = new BooleanValueOutputValueBinding(source, target);

      addBinding(binding);

      return binding;
   }

   public Binding bind(final BooleanValue source, final OnOffHardwareLight light)
   {
      return bind(source, light.isOn());
   }

   public Binding bind(final BooleanValue source, final HardwareControl hardwareControl)
   {
      return bind(source, (OnOffHardwareLight)hardwareControl.backgroundLight());
   }

   public Binding bind(final ColorValue sourceColor, final MultiStateHardwareLight light)
   {
      final LightColorOutputBinding binding = new LightColorOutputBinding(sourceColor, light);

      addBinding(binding);

      return binding;
   }

   public boolean isActive()
   {
      return mIsActive;
   }

   public void setIsActive(final boolean isActive)
   {
      if (isActive != mIsActive)
      {
         mIsActive = isActive;

         mLayers.activeLayersChanged();
      }
   }

   public void toggleIsActive()
   {
      setIsActive(!mIsActive);
   }

   public void activate()
   {
      setIsActive(true);
   }

   public void deactivate()
   {
      setIsActive(false);
   }

   private boolean mIsActive;

   private final Layers mLayers;

   final List<Binding> mBindings = new ArrayList<Binding>();

   private final String mName;
}
