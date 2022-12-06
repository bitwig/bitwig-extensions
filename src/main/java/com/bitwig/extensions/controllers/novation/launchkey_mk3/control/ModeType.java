package com.bitwig.extensions.controllers.novation.launchkey_mk3.control;

import com.bitwig.extensions.controllers.novation.launchkey_mk3.LaunchkeyMk3Extension;
import com.bitwig.extensions.framework.Layer;

import java.util.function.Consumer;

public interface ModeType {
   int getId();

   static <T extends ModeType> void bindButton(final T[] elements, final LaunchkeyMk3Extension driver, final String id,
                                               final int modeButtonCc, final Layer layer,
                                               final Consumer<T> handleChange) {
      for (final T type : elements) {
         final ModeButton<T> button = new ModeButton<T>(id, driver, modeButtonCc, type);
         button.bind(layer, handleChange);
      }
   }
}
