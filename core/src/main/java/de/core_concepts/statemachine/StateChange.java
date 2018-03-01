package de.core_concepts.statemachine;

public class StateChange<S> {

    final S fromState;

    final S toState;

    public StateChange(S fromState, S toState) {
        this.fromState = fromState;
        this.toState = toState;
    }

    public S getFromState() {
        return fromState;
    }

    public S getToState() {
        return toState;
    }
}
