package com.bitwig.extensions.controllers.mackie.display;

import com.bitwig.extension.controller.api.HardwareActionBindable;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.OnOffHardwareLight;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extensions.controllers.mackie.BasicNoteOnAssignment;
import com.bitwig.extensions.controllers.mackie.MackieMcuProExtension;
import com.bitwig.extensions.controllers.mackie.NoteAssignment;
import com.bitwig.extensions.framework.Layer;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class MainUnitButton {

   private final OnOffHardwareLight led;
   private final HardwareButton button;
   private final NoteAssignment assignment;

   public static MainUnitButton assignToggle(final MackieMcuProExtension driver, final BasicNoteOnAssignment assignment,
                                             final Layer layer, final SettableBooleanValue value) {
      final MainUnitButton button = new MainUnitButton(driver, assignment);
      button.bindToggle(layer, value);
      return button;
   }

   public static MainUnitButton assignIsPressed(final MackieMcuProExtension driver,
                                                final BasicNoteOnAssignment assignment, final Layer layer,
                                                final Consumer<Boolean> target) {
      final MainUnitButton button = new MainUnitButton(driver, assignment);
      button.bindIsPressed(layer, target);
      return button;
   }

   public MainUnitButton(final MackieMcuProExtension driver, final BasicNoteOnAssignment assignment) {
      this(driver, assignment, false);
   }

   public MainUnitButton(final MackieMcuProExtension driver, final BasicNoteOnAssignment assignment,
                         final boolean reverseLed) {
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
}
