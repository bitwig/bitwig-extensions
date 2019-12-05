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
import com.bitwig.extension.controller.api.BooleanHardwareProperty;
import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.ColorValue;
import com.bitwig.extension.controller.api.ContinuousHardwareControl;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareAction;
import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.HardwareControl;
import com.bitwig.extension.controller.api.HardwareLight;
import com.bitwig.extension.controller.api.HardwareTextDisplay;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extension.controller.api.RelativeHardwarControlBindable;
import com.bitwig.extension.controller.api.RelativeHardwareControl;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.StringHardwareProperty;
import com.bitwig.extension.controller.api.StringValue;

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

   public HardwareActionBindable getToggleAction()
   {
      return mToggleAction;
   }

   public HardwareActionBindable getActivateAction()
   {
      return mActivateAction;
   }

   public HardwareActionBindable getDeactivateAction()
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
      final HardwareActionBindable stepBackwardsAction,
      final HardwareActionBindable stepForwardsAction)
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
      return bind(actionOwner, source,
         getLayers().getControllerExtension().getHost().createAction(target, null));
   }

   public Binding bind(final Object actionOwner, final HardwareAction source, final DoubleConsumer target)
   {
      return bind(actionOwner, source,
         getLayers().getControllerExtension().getHost().createAction(target, null));
   }

   public Binding bindPressed(final HardwareButton button, final Runnable pressedRunnable)
   {
      return bind(button, button.pressedAction(), pressedRunnable);
   }

   public Binding bindPressed(final HardwareButton button, final DoubleConsumer pressedPressureConsumer)
   {
      return bind(button, button.pressedAction(), pressedPressureConsumer);
   }

   public Binding bindPressed(final HardwareButton button, final HardwareActionBindable target)
   {
      return bind(button, button.pressedAction(), target);
   }

   public Binding bindReleased(final HardwareButton button, final Runnable releasedRunnable)
   {
      return bind(button, button.releasedAction(), releasedRunnable);
   }

   public Binding bindReleased(final HardwareButton button, final DoubleConsumer releasedPressureConsumer)
   {
      return bind(button, button.releasedAction(), releasedPressureConsumer);
   }

   public Binding bindReleased(final HardwareButton button, final HardwareActionBindable target)
   {
      return bind(button, button.releasedAction(), target);
   }

   public void bindIsPressed(final HardwareButton button, final SettableBooleanValue target)
   {
      bind(button, button.pressedAction(), target.setToTrueAction());
      bind(button, button.releasedAction(), target.setToFalseAction());
      bind(button.isPressed(), button);
   }

   public void bindIsPressed(final HardwareButton button, final Consumer<Boolean> target)
   {
      bind(button, button.pressedAction(), () -> target.accept(true));
      bind(button, button.releasedAction(), () -> target.accept(false));
      bind(button.isPressed(), button);
   }

   public void bindIsPressed(final HardwareButton button, final Layer layer)
   {
      bind(button, button.pressedAction(), () -> layer.setIsActive(true));
      bind(button, button.releasedAction(), () -> layer.setIsActive(false));
      bind(button.isPressed(), button);
   }

   public void bindPressed(final ContinuousHardwareControl control, final HardwareActionBindable target)
   {
      bind(control, control.hardwareButton().pressedAction(), target);
   }

   public void bindPressed(final ContinuousHardwareControl control, final Runnable target)
   {
      bind(control, control.hardwareButton().pressedAction(), target);
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
      final HardwareActionBindable pressedAction,
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

   public Binding bindInverted(final BooleanSupplier source, final BooleanHardwareProperty target)
   {
      if (source instanceof BooleanValue)
         ((BooleanValue)source).markInterested();

      return bind(() -> !source.getAsBoolean(), target);
   }

   public Binding bind(final BooleanSupplier source, final BooleanHardwareProperty target)
   {
      if (source instanceof BooleanValue)
         ((BooleanValue)source).markInterested();

      final BooleanSupplierToPropertyBinding binding = new BooleanSupplierToPropertyBinding(source, target);

      addBinding(binding);

      return binding;
   }

   public Binding bind(final BooleanSupplier source, final OnOffHardwareLight target)
   {
      return bind(source, target.isOn());
   }

   public Binding bindInverted(final BooleanSupplier source, final OnOffHardwareLight target)
   {
      return bindInverted(source, target.isOn());
   }

   public Binding bind(final BooleanSupplier source, final HardwareControl target)
   {
      return bind(source, (OnOffHardwareLight)target.backgroundLight());
   }

   public Binding bind(final BooleanValue source, final BooleanHardwareProperty target)
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

   public Binding bindLightState(final Supplier<InternalHardwareLightState> supplier, final MultiStateHardwareLight light)
   {
      final InternalLightStateBinding binding = new InternalLightStateBinding(supplier, light);
      addBinding(binding);
      return binding;
   }

   public Binding bind(final Supplier<Color> sourceColor, final HardwareControl target)
   {
      return bind(sourceColor, (MultiStateHardwareLight)target.backgroundLight());
   }

   public Binding bind(final Supplier<String> source, final StringHardwareProperty target)
   {
      if (source instanceof StringValue)
         ((StringValue)source).markInterested();

      final StringSupplierToPropertyBinding binding = new StringSupplierToPropertyBinding(source, target);

      addBinding(binding);

      return binding;
   }

   public void bind(final Supplier<String> source, final HardwareTextDisplay textDisplay, final int line)
   {
      bind(source, textDisplay.line(line).text());
   }

   public void bind(final Supplier<String> source, final HardwareTextDisplay textDisplay)
   {
      bind(source, textDisplay, 0);
   }

   public final boolean isActive()
   {
      return mIsActive;
   }

   public final void setIsActive(final boolean isActive)
   {
      if (isActive != mIsActive)
      {
         if (isActive && mLayerGroup != null)
         {
            for (final Layer other : mLayerGroup.getLayers())
            {
               if (other != this)
               {
                  other.doSetIsActive(false);
               }
            }
         }

         doSetIsActive(isActive);

         mLayers.activeLayersChanged();
      }
   }

   private final void doSetIsActive(final boolean isActive)
   {
      if (isActive != mIsActive)
      {
         mIsActive = isActive;

         if (isActive)
            onActivate();
         else
            onDeactivate();
      }
   }

   public final void toggleIsActive()
   {
      if (mIsActive && mLayerGroup != null)
         return;

      setIsActive(!mIsActive);
   }

   public final void activate()
   {
      setIsActive(true);
   }

   public final void deactivate()
   {
      setIsActive(false);
   }

   protected void onActivate()
   {
      /* reserved for subclasses */
   }

   protected void onDeactivate()
   {
      /* reserved for subclasses */
   }

   void setLayerGroup(final LayerGroup layerGroup)
   {
      assert layerGroup != null;
      assert mLayerGroup == null;

      mLayerGroup = layerGroup;
   }

   public boolean shouldReplaceBindingsInLayersBelow()
   {
      return mShouldReplaceBindingsInLayersBelow;
   }

   public void setShouldReplaceBindingsInLayersBelow(final boolean value)
   {
      mShouldReplaceBindingsInLayersBelow = value;
   }

   private boolean mIsActive;

   private final Layers mLayers;

   final List<Binding> mBindings = new ArrayList<>();

   private final String mName;

   private final HardwareActionBindable mToggleAction, mActivateAction, mDeactivateAction;

   private LayerGroup mLayerGroup;

   private boolean mShouldReplaceBindingsInLayersBelow = true;
}
