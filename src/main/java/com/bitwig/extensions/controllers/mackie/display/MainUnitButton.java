package com.bitwig.extensions.controllers.mackie.display;

import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extensions.controllers.mackie.BasicNoteOnAssignment;
import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;
import com.bitwig.extensions.controllers.mackie.NoteAssignment;
import com.bitwig.extensions.controllers.mackie.definition.ControllerConfig;
import com.bitwig.extensions.controllers.mackie.definition.SimulationLayout;
import com.bitwig.extensions.framework.Layer;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class MainUnitButton {

   private final OnOffHardwareLight led;
   private final HardwareButton button;
   private final NoteAssignment assignment;

   public static MainUnitButton assignToggle(final MackieMcuProExtension driver, final BasicNoteOnAssignment assignment,
                                             final Layer layer, final SettableBooleanValue value,
                                             final ControllerConfig controllerConfig) {
      final MainUnitButton button = new MainUnitButton(driver, assignment, controllerConfig.getSimulationLayout());
      button.bindToggle(layer, value);
      return button;
   }

   public static MainUnitButton assignIsPressed(final MackieMcuProExtension driver,
                                                final BasicNoteOnAssignment assignment, final Layer layer,
                                                final Consumer<Boolean> target, final ControllerConfig config) {
      final MainUnitButton button = new MainUnitButton(driver, assignment, config.getSimulationLayout());
      button.bindIsPressed(layer, target);

      return button;
   }

   public MainUnitButton(final MackieMcuProExtension driver, final BasicNoteOnAssignment assignment,
                         final SimulationLayout layout) {
      this(driver, assignment, layout, false);
   }

   public MainUnitButton(final MackieMcuProExtension driver, final BasicNoteOnAssignment assignment,
                         final SimulationLayout layout, final boolean reverseLed) {
      final NoteAssignment actualAssignment = driver.get(assignment);
      this.assignment = actualAssignment;

      final int noteNr = actualAssignment.getNoteNo();
      button = driver.getSurface().createHardwareButton(actualAssignment + "_BUTTON");
      actualAssignment.holdActionAssign(driver.getMidiIn(), button);

      led = driver.getSurface().createOnOffHardwareLight(actualAssignment + "_BUTTON_LED");
      button.setBackgroundLight(led);
      if (reverseLed) {
         led.onUpdateHardware(() -> driver.sendLedUpdate(noteNr, led.isOn().currentValue() ? 0 : 127));
      } else {
         led.onUpdateHardware(() -> driver.sendLedUpdate(noteNr, led.isOn().currentValue() ? 127 : 0));
      }
      layout.layout(assignment, button);
   }

   public NoteAssignment getAssignment() {
      return assignment;
   }

   public HardwareButton getButton() {
      return button;
   }

   public void setLed(final boolean onOff) {
      led.isOn().setValue(onOff);
   }

   public void bindLight(final Layer layer, final BooleanSupplier stateProvider) {
      layer.bind(stateProvider, led);
   }

   public void bindIsPressed(final Layer layer, final Consumer<Boolean> target) {
      layer.bindIsPressed(button, target);
   }

   public void bindPressed(final Layer layer, final HardwareActionBindable action) {
      layer.bindPressed(button, action);
   }

   public void bindPressedState(final Layer layer) {
      layer.bind(button.isPressed(), led);
   }


   public void bindPressed(final Layer layer, final Runnable pressedAction) {
      layer.bindPressed(button, pressedAction);
   }

   public void bindReleased(final Layer layer, final Runnable pressedAction) {
      layer.bindReleased(button, pressedAction);
   }

   public void bindPressed(final Layer layer, final SettableBooleanValue value) {
      layer.bindPressed(button, value);
      layer.bind(value, led);
   }

   public void bindToggle(final Layer layer, final SettableBooleanValue value) {
      layer.bindPressed(button, value::toggle);
      layer.bind(value, led);
   }

   public MainUnitButton activateHoldState() {
      button.isPressed().addValueObserver(v -> led.isOn().setValue(v));
      return this;
   }

   public String getName() {
      return button.getName();
   }

   public void bindLigthPressed(final Layer mainLayer) {
   }
}
