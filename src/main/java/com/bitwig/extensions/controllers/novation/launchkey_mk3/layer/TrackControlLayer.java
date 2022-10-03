package com.bitwig.extensions.controllers.novation.launchkey_mk3.layer;

import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.ColorLookup;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.LaunchkeyMk3Extension;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.RgbState;
import com.bitwig.extensions.controllers.novation.launchkey_mk3.control.RgbCcButton;
import com.bitwig.extensions.framework.Layer;

public class TrackControlLayer extends Layer {

   private final Layer armLayer;
   private final Layer selectLayer;
   private Mode mode = Mode.SELECT;
   private final int[] trackColors = new int[8];
   private final boolean[] selectedInEditor = new boolean[8];
   private boolean trackDown = false;
   private String deviceName = "";

   private enum Mode {
      ARM,
      SELECT
   }

   public TrackControlLayer(final LaunchkeyMk3Extension driver) {
      super(driver.getLayers(), "TRACK_LAYER");

      final TrackBank trackBank = driver.getTrackBank();
      final CursorTrack cursorTrack = driver.getCursorTrack();
      final RgbCcButton[] trackButtons = driver.getHwControl().getTrackButtons();
      final RgbCcButton armSelectButton = driver.getHwControl().getArmSelectButton();
      armLayer = new Layer(driver.getLayers(), "TRACK_ARM_LAYER");
      selectLayer = new Layer(driver.getLayers(), "TRACK_SELECT_LAYER");
      final PinnableCursorDevice primaryDevice = driver.getPrimaryDevice();
      primaryDevice.name().markInterested();

      primaryDevice.name().addValueObserver(newName -> {
         deviceName = newName;
         if (trackDown) {
            driver.getLcdDisplay().sendText(deviceName, 1);
         }
      });

      for (int i = 0; i < 8; i++) {
         final int index = i;
         final Track track = trackBank.getItemAt(i);
         final RgbCcButton button = trackButtons[i];
         track.arm().markInterested();
         track.exists().markInterested();
         track.name().markInterested();
         track.addIsSelectedInEditorObserver(selected -> selectedInEditor[index] = selected);
         track.color().addValueObserver((r, g, b) -> trackColors[index] = ColorLookup.toColor(r, g, b));
         button.bindIsPressed(armLayer, pressed -> {
            if (pressed) {
               track.arm().toggle();
            }
         }, () -> getArmState(track));
         button.bindIsPressed(selectLayer, pressed -> {
            if (pressed) {
               cursorTrack.selectChannel(track);
               driver.setTransientText(track.name().get(), primaryDevice.name().get());
               trackDown = true;
            } else {
               driver.releaseText();
               trackDown = false;
            }
         }, () -> getSelectState(index, track));
      }

      armSelectButton.bindIsPressed(this, pressed -> {
         if (pressed) {
            if (mode == Mode.ARM) {
               mode = Mode.SELECT;
            } else {
               mode = Mode.ARM;
            }
            setLayer();
         }
      }, () -> mode == Mode.SELECT ? RgbState.DIM_WHITE : RgbState.OFF);
   }

   private RgbState getArmState(final Track track) {
      if (track.exists().get()) {
         if (track.arm().get()) {
            return RgbState.RED;
         }
         return RgbState.RED_LO;
      }
      return RgbState.OFF;
   }

   private RgbState getSelectState(final int index, final Track track) {
      if (track.exists().get()) {
         if (selectedInEditor[index]) {
            return RgbState.WHITE;
         }
         return RgbState.of(trackColors[index]);
      }
      return RgbState.OFF;
   }

   private void setLayer() {
      if (mode == Mode.ARM) {
         selectLayer.setIsActive(false);
         armLayer.setIsActive(true);
      } else {
         armLayer.setIsActive(false);
         selectLayer.setIsActive(true);
      }
   }

   @Override
   protected void onActivate() {
      super.onActivate();
      setLayer();
   }

   @Override
   protected void onDeactivate() {
      super.onDeactivate();
   }

}
