package com.bitwig.extensions.controllers.mackie;

import com.bitwig.extension.controller.api.Action;
import com.bitwig.extension.controller.api.Application;

import java.util.HashMap;
import java.util.Map;

public class ActionSet {
   private final Action inspectAction;
   private final Application application;
   private final Action zoomToFitAction;
   private final Action clipLauncherAction;
   private final Map<ActionType, Action> actionMap = new HashMap<>();

   private final Action detailAction;
   private final Action deviceAction;


   public enum ActionType {
      QUANTIZE,
      DOUBLE,
      REVERSE,
      SCALE200,
      SCALE50,
      TRANSUP,
      TRANSDOWN,
      OCTUP,
      OCTDOWN,
      SAVE,
      SELECT_ALL,
      UNSELECT_ALL
   }

   public ActionSet(final Application application) {
      this.application = application;
      inspectAction = application.getAction("focus_or_toggle_inspector");
      detailAction = application.getAction("focus_or_toggle_detail_editor");
      deviceAction = application.getAction("focus_or_toggle_device_panel");
      zoomToFitAction = application.getAction("Zoom to Fit");
      clipLauncherAction = application.getAction("focus_or_toggle_clip_launcher");
      actionMap.put(ActionType.QUANTIZE, application.getAction("quantize"));
      actionMap.put(ActionType.DOUBLE, application.getAction("double_content"));
      actionMap.put(ActionType.REVERSE, application.getAction("reverse"));
      actionMap.put(ActionType.SCALE200, application.getAction("scale_time_double"));
      actionMap.put(ActionType.SCALE50, application.getAction("scale_time_half"));
      actionMap.put(ActionType.TRANSUP, application.getAction("transpose_semitone_up"));
      actionMap.put(ActionType.TRANSDOWN, application.getAction("transpose_semitone_down"));
      actionMap.put(ActionType.OCTUP, application.getAction("transpose_octave_up"));
      actionMap.put(ActionType.OCTDOWN, application.getAction("transpose_octave_down"));
      actionMap.put(ActionType.SAVE, application.getAction("Save"));
      actionMap.put(ActionType.SELECT_ALL, application.getAction("Select All"));
      actionMap.put(ActionType.UNSELECT_ALL, application.getAction("Unselect All"));
   }

   public void execute(final ActionType type) {
      final Action action = actionMap.get(type);
      if (action != null) {
         action.invoke();
      }
   }

   public void executeClip(final ActionType type) {
      final Action action = actionMap.get(type);
      if (action != null) {
         focusEditor();
//         clipLauncherAction.invoke();
         action.invoke();
         zoomToFitAction.invoke();
      }
   }

   public void zoomToFitEditor() {
      focusEditor();
      zoomToFitAction.invoke();
   }

   public void focusDevice() {
      detailAction.invoke();
      deviceAction.invoke();
   }

   public void focusEditor() {
      deviceAction.invoke();
      detailAction.invoke();
      zoomToFitAction.invoke();
   }

}
