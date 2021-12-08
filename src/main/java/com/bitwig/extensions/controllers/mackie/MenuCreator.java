package com.bitwig.extensions.controllers.mackie;

import com.bitwig.extension.callback.BooleanValueChangedCallback;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.controllers.mackie.configurations.MenuDisplayLayerBuilder;
import com.bitwig.extensions.controllers.mackie.configurations.MenuModeLayerConfiguration;
import com.bitwig.extensions.controllers.mackie.section.MixControl;
import com.bitwig.extensions.controllers.mackie.value.BasicStringValue;
import com.bitwig.extensions.controllers.mackie.value.BooleanValueObject;
import com.bitwig.extensions.controllers.mackie.value.EnumValueSetting;

import java.util.function.IntConsumer;

public class MenuCreator {

   private final Application application;
   private final MixControl mainSection;
   private final ActionSet actionSet;

   public MenuCreator(final Application application, final MixControl mainSection, final ActionSet actionSet) {
      this.application = application;
      this.mainSection = mainSection;
      this.actionSet = actionSet;
   }

   public MenuModeLayerConfiguration createKeyboardMenu(final NotePlayingSetup notePlayingSetup) {
      final MenuModeLayerConfiguration menu = new MenuModeLayerConfiguration("KEYBOARD_MENU", mainSection);
      final MenuDisplayLayerBuilder builder = new MenuDisplayLayerBuilder(menu);
      builder.bindValue("Base.N", notePlayingSetup.getBaseNote());
      builder.bindValue("Scale", notePlayingSetup.getScale());
      builder.bindValue("Octave", notePlayingSetup.getOctaveOffset());
      builder.bindValue("Layout", notePlayingSetup.getLayoutOffset());
      builder.insertEmpty();
      builder.bindValue("Velocity", notePlayingSetup.getVelocity());
      builder.fillRest();
      return menu;
   }

   public MenuModeLayerConfiguration createCyleMenu(final ControllerHost host, final Transport transport) {
      //final HardwareButton loopButton = createButton(BasicNoteOnAssignment.CYCLE);
      final MenuModeLayerConfiguration menu = new MenuModeLayerConfiguration("MARKER_MENU", mainSection);
      final BeatTimeFormatter formatter = host.createBeatTimeFormatter(":", 2, 1, 1, 0);

      final SettableBeatTimeValue cycleStart = transport.arrangerLoopStart();
      final SettableBeatTimeValue cycleLength = transport.arrangerLoopDuration();

      cycleStart.markInterested();
      cycleLength.markInterested();

      menu.addNameBinding(0, new BasicStringValue("Cycle"));
      menu.addNameBinding(1, new BasicStringValue("Start"));
      menu.addNameBinding(2, new BasicStringValue("Len"));
      menu.addNameBinding(4, new BasicStringValue("P.IN"));
      menu.addNameBinding(5, new BasicStringValue("P.OUT"));

      menu.addValueBinding(0, transport.isArrangerLoopEnabled(), "< ON >", "<OFF >");
      menu.addValueBinding(1, cycleStart, v -> cycleStart.getFormatted(formatter));
      menu.addValueBinding(2, cycleLength, v -> cycleLength.getFormatted(formatter));
      menu.addValueBinding(4, transport.isPunchInEnabled(), "< ON >", "<OFF >");
      menu.addValueBinding(5, transport.isPunchOutEnabled(), "< ON >", "<OFF >");

      menu.addRingBoolBinding(0, transport.isArrangerLoopEnabled());
      menu.addRingFixedBindingActive(1);
      menu.addRingFixedBindingActive(2);
      menu.addRingBoolBinding(0, transport.isPunchInEnabled());
      menu.addRingBoolBinding(0, transport.isPunchOutEnabled());

      menu.addPressEncoderBinding(0, index -> transport.isArrangerLoopEnabled().toggle(), false);
      menu.addPressEncoderBinding(1, index -> cycleStart.set(transport.getPosition().get()), false);
      menu.addPressEncoderBinding(4, index -> transport.isPunchInEnabled().toggle(), false);
      menu.addPressEncoderBinding(5, index -> transport.isPunchOutEnabled().toggle(), false);
      menu.addPressEncoderBinding(2, index -> {
         final double startTime = cycleStart.get();
         final double diff = transport.getPosition().get() - startTime;
         if (diff > 0) {
            cycleLength.set(diff);
         }
      }, false);
      menu.addEncoderIncBinding(1, cycleStart, 1, 0.25);
      menu.addEncoderIncBinding(2, cycleLength, 1, 0.25);

      menu.addRingFixedBinding(3);
      menu.addRingFixedBinding(6);
      menu.addRingFixedBinding(7);
      return menu;
   }

   public MenuModeLayerConfiguration createGrooveMenu(final Groove groove) {
      final MenuModeLayerConfiguration menu = new MenuModeLayerConfiguration("GROOVE_MENU", mainSection);
      final MenuDisplayLayerBuilder builder = new MenuDisplayLayerBuilder(menu);
      builder.bindBool("Groove", groove.getEnabled());
      builder.bindValue("Shf.Rt", groove.getShuffleRate().value(), 2);
      builder.bindValue("Sfl.Am", groove.getShuffleAmount().value(), 1, 0.5);
      builder.insertEmpty();
      builder.bindValue("Acc.Rt", groove.getAccentRate().value(), 3);
      builder.bindValue("Acc.Am", groove.getAccentAmount().value(), 1, 1.0);
      builder.bindValue("Acc.Ph", groove.getAccentPhase().value(), 1, 0.5);
      builder.fillRest();

      return menu;
   }

   public MenuModeLayerConfiguration createTempoMenu(final Transport transport, final IntConsumer tempoHandler) {
      final MenuModeLayerConfiguration menu = new MenuModeLayerConfiguration("TEMP_MENU", mainSection);
      final MenuDisplayLayerBuilder builder = new MenuDisplayLayerBuilder(menu);
      builder.bindInc("TEMPO", transport.tempo().value(), tempoHandler);
      builder.bindAction("<TAP>", "", transport::tapTempo);
      builder.fillRest();
      return menu;
   }

   public MenuModeLayerConfiguration createClickMenu(final Transport transport,
                                                     final BooleanValueChangedCallback clickHandler) {
      final MenuModeLayerConfiguration menu = new MenuModeLayerConfiguration("MARKER_MENU", mainSection);
      final MenuDisplayLayerBuilder builder = new MenuDisplayLayerBuilder(menu);
      final BooleanValueObject transportClick = new BooleanValueObject();
      builder.bindBool("METRO", transport.isMetronomeEnabled());
      builder.bindBool("Pre ->", transport.isMetronomeAudibleDuringPreRoll());
      builder.bindEnum("ROLL", new EnumValueSetting(transport.preRoll()).add("none", "None", 0)
         .add("one_bar", "1bar", 3)
         .add("two_bars", "2bar", 6)
         .add("four_bars", "4bar", 11));
      builder.bindBool("M.TICK", transport.isMetronomeTickPlaybackEnabled());
      builder.bindValue("M.VOL", transport.metronomeVolume(), 0.05, 0.6);
      builder.insertEmpty();
      builder.bindBool("T.CLCK", transportClick);
      transportClick.addValueObserver(clickHandler);
      builder.fillRest();
      return menu;
   }

   public MenuModeLayerConfiguration creatClipMenuSection() {
      final MenuModeLayerConfiguration menu = new MenuModeLayerConfiguration("CLIP_MENU_MENU", mainSection);
      final MenuDisplayLayerBuilder builder = new MenuDisplayLayerBuilder(menu);

      builder.bindAction("<DBL>", "double", () -> actionSet.executeClip(ActionSet.ActionType.DOUBLE));
      builder.bindAction("<REV>", "reverse", () -> actionSet.executeClip(ActionSet.ActionType.REVERSE));
      builder.bindAction("<SCL>", "50%", () -> actionSet.executeClip(ActionSet.ActionType.SCALE50));
      builder.bindAction("<SCL>", "200%", () -> actionSet.executeClip(ActionSet.ActionType.SCALE200));
      builder.bindAction("<TrP>", "-semi", () -> actionSet.executeClip(ActionSet.ActionType.TRANSDOWN));
      builder.bindAction("<Trp>", "+semi", () -> actionSet.executeClip(ActionSet.ActionType.TRANSUP));
      builder.bindAction("<Oct>", "-oct", () -> actionSet.executeClip(ActionSet.ActionType.OCTDOWN));
      builder.bindAction("<Oct>", "+oct", () -> actionSet.executeClip(ActionSet.ActionType.OCTUP));
      return menu;
   }

   public MenuModeLayerConfiguration createQuantizeSection(final Transport transport, final FocusClipView followClip) {
      final MenuModeLayerConfiguration menu = new MenuModeLayerConfiguration("QUANTIZE_MENU", mainSection);
      final MenuDisplayLayerBuilder builder = new MenuDisplayLayerBuilder(menu);
      builder.bindEnum("REC.Q", new EnumValueSetting(application.recordQuantizationGrid()).add("OFF", 0)
         .add("1/32", 3)
         .add("1/16", 6)
         .add("1/8", 8)
         .add("1/4", 11));
      builder.bindBool("LEN.Q", application.recordQuantizeNoteLength());
      builder.bindEnum("CLIP.Q", new EnumValueSetting(transport.defaultLaunchQuantization()).add("none", 0)
         .add("8", 2)
         .add("4", 4)
         .add("1", 6)
         .add("1/2", 7)//
         .add("1/4", 8)
         .add("1/8", 9)
         .add("1/16", 11));
      builder.insertEmpty();
//      quantizationValue.add("1/16", 0.25).add("1/8", 0.5).add("1/4", 1.0).select(0);
//      builder.bindValueSet("QUANT", quantizationValue);
      builder.insertEmpty();
      builder.bindAction("<Do.Q>", "100%", () -> {
         actionSet.focusEditor();
         followClip.getFocusClip().quantize(1.0);
      });
      builder.bindAction("<Do.Q>", ">DLG", () -> {
         actionSet.focusEditor();
         application.selectAll();
         actionSet.execute(ActionSet.ActionType.QUANTIZE);
      });

      builder.fillRest();
      return menu;
   }

}
