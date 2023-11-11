package com.bitwig.extensions.controllers.arturia.minilab3;

import java.util.List;
import java.util.Optional;

public class EncoderStateMaschine {
   public enum State {
      INITIAL, //
      HOLD, // ;
      HOLD_SHIFT, //
      SHIFT, //
      SHIFT_HOLD
   }

   public enum Event {
      SHIFT_DOWN,
      SHIFT_UP,
      ENCODER_DOWN,
      ENCODER_UP,
      TURN,
      SHIFT_TURN
   }

   private State state = State.INITIAL;
   private boolean turnAction = false;
   private long eventTime = 0;

   private List<Transition> transitions = List.of(//
      new Transition(State.INITIAL, Event.SHIFT_DOWN, State.SHIFT),//
      new Transition(State.INITIAL, Event.ENCODER_DOWN, State.HOLD, true),//
      new Transition(State.HOLD, Event.ENCODER_UP, State.INITIAL, true),//
      new Transition(State.HOLD, Event.SHIFT_DOWN, State.HOLD_SHIFT),//
      new Transition(State.HOLD_SHIFT, Event.SHIFT_UP, State.HOLD),//
      new Transition(State.SHIFT, Event.SHIFT_UP, State.INITIAL), //
      new Transition(State.SHIFT, Event.ENCODER_DOWN, State.SHIFT_HOLD), //
      new Transition(State.SHIFT_HOLD, Event.ENCODER_UP, State.SHIFT), //
      new Transition(State.SHIFT_HOLD, Event.ENCODER_UP, State.SHIFT),//
      new Transition(State.SHIFT_HOLD, Event.SHIFT_UP, State.HOLD) //
   );

   private record Transition(State state, Event event, State result, boolean resetTurn) {
      public Transition(State state, Event event, State result) {
         this(state, event, result, false);
      }
   }

   public void doTransition(Event event) {
      //MiniLab3Extension.println(" EVENT > %s", event);
      eventTime = System.currentTimeMillis();
      Optional<Transition> result = transitions.stream()
         .filter(t -> t.state == state)
         .filter(t -> t.event == event)
         .findFirst();
      if (result.isPresent()) {
         this.state = result.get().result;
         if (result.get().resetTurn) {
            turnAction = false;
         }
         //MiniLab3Extension.println(" RESULT = %s", this);
      }
   }

   public long getTimeSinceLastEvent() {
      return System.currentTimeMillis() - eventTime;
   }

   public State getState() {
      return state;
   }

   public void notifyTurn() {
      if (state != State.INITIAL) {
         turnAction = true;
      }
   }

   public boolean isTurnAction() {
      return turnAction;
   }

   @Override
   public String toString() {
      return state.toString() + " <" + (turnAction ? "*" : "-") + ">";
   }
}
