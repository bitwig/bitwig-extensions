package com.bitwig.extensions.framework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.AbsoluteHardwarControlBindable;
import com.bitwig.extension.controller.api.AbsoluteHardwareControl;
import com.bitwig.extension.controller.api.BooleanHardwareOutputValue;
import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.ColorValue;
import com.bitwig.extension.controller.api.ControllerHost;
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
      final ControllerHost host = layers.getControllerExtension().getHost();
      mToggleAction = host.createAction(this::toggleIsActive, () -> "Toggle " + name);
      mActivateAction = host.createAction(this::activate, () -> "Activate " + name);
      mDeactivateAction = host.createAction(this::deactivate, () -> "Deactivate " + name);

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

   public TriggerAction getToggleAction()
   {
      return mToggleAction;
   }

   public TriggerAction getActivateAction()
   {
      return mActivateAction;
   }

   public TriggerAction getDeactivateAction()
   {
      return mDeactivateAction;
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

   public AbsoluteHardwareControlBinding bind(
      final AbsoluteHardwareControl source,
      final DoubleConsumer adjustmentConsumer)
   {
      final AbsoluteHardwarControlBindable target = getLayers().getControllerExtension().getHost()
         .createAbsoluteHardwareControlAdjustmentTarget(adjustmentConsumer);

      return bind(source, target);
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
      final TriggerAction stepForwardsAction,
      final TriggerAction stepBackwardsAction)
   {
      final RelativeHardwarControlBindable target = getLayers().getControllerExtension().getHost()
         .createRelativeHardwareControlStepTarget(stepForwardsAction, stepBackwardsAction);

      return bind(source, target);
   }

   public RelativeHardwareControlBinding bind(
      final RelativeHardwareControl source,
      final DoubleConsumer adjustmentConsumer)
   {
      final RelativeHardwarControlBindable target = getLayers().getControllerExtension().getHost()
         .createRelativeHardwareControlAdjustmentTarget(adjustmentConsumer);

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

   public Binding bindReleased(final HardwareButton button, final Runnable releasedRunnable)
   {
      return bind(button, button.releasedAction(), releasedRunnable);
   }

   public Binding bindReleased(final HardwareButton button, final HardwareActionBindable target)
   {
      return bind(button, button.releasedAction(), target);
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

   public void bindToggle(
      final HardwareButton button,
      final Runnable pressedRunnable,
      final BooleanSupplier isLightOnOffSupplier)
   {
      bindPressed(button, pressedRunnable);
      bind(isLightOnOffSupplier, button);
   }

   public void bindToggle(
      final HardwareButton button,
      final TriggerAction pressedAction,
      final BooleanSupplier isLightOnOffSupplier)
   {
      bindPressed(button, pressedAction);
      bind(isLightOnOffSupplier, button);
   }

   public void bindToggle(final HardwareButton button, final Layer layerToToggle)
   {
      bindPressed(button, layerToToggle::toggleIsActive);
      bind(layerToToggle::isActive, button);
   }

   public Binding bind(final BooleanSupplier source, final BooleanHardwareOutputValue target)
   {
      if (source instanceof BooleanValue)
         ((BooleanValue)source).markInterested();

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

      return bind((BooleanSupplier)source, target);
   }

   public Binding bind(final Supplier<Color> sourceColor, final MultiStateHardwareLight light)
   {
      if (sourceColor instanceof ColorValue)
         ((ColorValue)sourceColor).markInterested();

      final LightColorOutputBinding binding = new LightColorOutputBinding(sourceColor, light);

      addBinding(binding);

      return binding;
   }

   public Binding bind(final Supplier<Color> sourceColor, final HardwareControl target)
   {
      return bind(sourceColor, (MultiStateHardwareLight)target.backgroundLight());
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

   private final TriggerAction mToggleAction, mActivateAction, mDeactivateAction;
}
