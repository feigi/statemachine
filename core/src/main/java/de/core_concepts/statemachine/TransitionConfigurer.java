package de.core_concepts.statemachine;


import org.apache.commons.lang.Validate;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;

/**
 * Created by zieglerch on 27.11.2015.
 * <p>
 * A builder class to configure transitions. Used by AbstractGF implementations to define transitions and their actions/guards and so on. It
 * expects a map of states prefilled with all known states.
 *
 * @param <S> An enum type defining states.
 * @param <E> An enum type defining events.
 * @param <O> The object type which is put on the context for actions to work with.
 */
public class TransitionConfigurer<S, E, O> {

    private final Predicate<Context<O>> defaultGuard = context -> true;
    private final Map<S, State<S, E, O>> states;
    private final S initalState;
    private final S finalState;
    private State<S, E, O> to;
    private Set<State<S, E, O>> from;
    private E event;
    private Class<? extends RuntimeException> error;
    private Predicate<Context<O>> guard = defaultGuard;
    private Action<O> action;
    private boolean fromAll = false;
    private Set<S> excluding = new HashSet<>();
    private boolean toSelf = false;

    TransitionConfigurer(Map<S, State<S, E, O>> states, S initalState, S finalState) {
        Validate.notNull(initalState, "initialState can not be null.");
        Validate.notNull(finalState, "finalState can not be null.");
        Validate.notNull(states, "states can not be null.");
        Validate.notEmpty(states, "states must not be empty.");
        this.states = states;
        this.initalState = initalState;
        this.finalState = finalState;
    }

    /**
     * @param from The state to transit from. This class will call addTransition on this state if add() is called. Mandatory
     */
    public TransitionConfigurer<S, E, O> from(S... from) {
        Validate.notNull(from);
        this.from = Stream.of(from).map(this::getState).collect(toSet());
        return this;
    }

    /**
     * States that the transition should take place from the initial state. Per definition the initial state can only have outgoing
     * transitions but no incoming.
     */
    public TransitionConfigurer<S, E, O> fromInitial() {
        this.from = singleton(getState(initalState));
        return this;
    }

    /**
     * @param to The state to transit to. Mandatory
     */
    public TransitionConfigurer<S, E, O> to(S to) {
        Validate.notNull(to);
        this.to = getState(to);
        return this;
    }

    /**
     * States that the transition should end in the final state. Per definition the final state can only have incoming transitions but no
     * outgoing.
     */
    public TransitionConfigurer<S, E, O> toFinal() {
        this.to = getState(finalState);
        return this;
    }

    private State<S, E, O> getState(S stateEnum) {
        State<S, E, O> state = states.get(stateEnum);
        if (state == null) {
            throw new ConfigurationException("State with name " + stateEnum + " not found. Apparently it hasn't been configured.");
        }
        return state;
    }

    /**
     * Traverse this transition if the following event was sent. Only either onError or onEvent is valid. Calling this after a call to onError
     * will delete the onError value.
     *
     * @param event The event to 'listen' on.
     */
    public TransitionConfigurer<S, E, O> onEvent(E event) {
        this.event = event;
        return this;
    }

    /**
     * Only traverse this transition if another transition did throw the following exception. Only either onError or onEvent is valid. Calling
     * this after a call to onEvent will delete the onEvent value.
     *
     * @param error The Exception to 'listen' on.
     */
    public TransitionConfigurer<S, E, O> onError(Class<? extends RuntimeException> error) {
        this.error = error;
        return this;
    }

    /**
     * Only traverse the transition if the following guard returns true.
     *
     * @param guard The guard which decides wh ether the transition may be executed.
     */
    public TransitionConfigurer<S, E, O> when(Predicate<Context<O>> guard) {
        Validate.notNull(guard);
        this.guard = guard;
        return this;
    }

    /**
     * @param action The action which should be executed upon transition
     */
    public TransitionConfigurer<S, E, O> action(Action<O> action) {
        this.action = action;
        return this;
    }

    /**
     * States whether a transition should apply to all states. The initial and final states are excluded from this!
     */
    public TransitionConfigurer<S, E, O> fromAll() {
        this.fromAll = true;
        return this;
    }

    /**
     * If using fromAll, use this to exclude some states
     *
     * @param excluding The states to be excluded
     */
    public TransitionConfigurer<S, E, O> excluding(S... excluding) {
        this.excluding = new HashSet<>(Arrays.asList(excluding));
        return this;
    }

    /**
     * States whether a transition should be reflexive
     */
    public TransitionConfigurer<S, E, O> toSelf() {
        this.toSelf = true;
        return this;
    }

    /**
     * Add the transition to the state machine model
     */
    public void add() {

        if (fromAll) {
            from = states.values().stream()
                    .filter(state -> !excluding.contains(state.getId()))
                    .filter(state -> !initalState.equals(state.getId()) && !finalState.equals(state.getId()))
                    .collect(toSet());
        }

        validateConfiguration();

        from.forEach(fromState -> fromState.addTransition(newTransition(toSelf ? fromState : to, event, error, action, guard)));
        clearValues();
    }

    private void validateConfiguration() {
        shouldBeUsedConfigurationException(fromAll, toSelf, from, to);
        transitToInitialOrFinalStateConfigurationException(from, to, finalState, initalState);
        notSupportedConfigurationException(this.event, this.error);
    }

    private void notSupportedConfigurationException(E event, Class<? extends RuntimeException> error) {
        if (event != null && error != null) {
            throw new ConfigurationException("Both onError and onEvent were configured. This is not supported.");
        }
    }

    private void shouldBeUsedConfigurationException(boolean fromAll, boolean toSelf, Set<State<S, E, O>> from, State<S, E, O> to) {
        if (!(fromAll || from != null)) {
            throw new ConfigurationException("Either 'from', 'fromAll' or 'fromInitial' must be used.");
        }

        if (!(toSelf || to != null)) {
            throw new ConfigurationException("Either 'to', 'toSelf' or 'toFinal' must be used.");
        }
    }

    private void transitToInitialOrFinalStateConfigurationException(Set<State<S, E, O>> from, State<S, E, O> to, S finalState,
                                                                    S initalState) {
        boolean fromFinalFound = from.stream().anyMatch(fromState -> finalState.equals(fromState.getId()));
        if (fromFinalFound) {
            throw new ConfigurationException("Can not transit from final state.");
        }

        if (to != null && initalState.equals(to.getId())) {
            throw new ConfigurationException("Can not transit to the initial state.");
        }
    }

    private Transition<S, E, O> newTransition(State<S, E, O> toState, E event, Class<? extends RuntimeException> error, Action<O> action,
                                              Predicate<Context<O>> guard) {
        return new Transition<>(toState, Optional.ofNullable(event), Optional.ofNullable(error), Optional.ofNullable(action), guard);
    }

    private void clearValues() {
        this.to = null;
        this.from = null;
        this.event = null;
        this.error = null;
        this.action = null;
        this.guard = defaultGuard;
        this.fromAll = false;
        this.excluding.clear();
        this.toSelf = false;
    }
}
