package de.core_concepts.statemachine;


import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static de.core_concepts.statemachine.LifecycleEvent.*;
import static java.lang.String.format;

/**
 * This is the abstract implementation of a statemachine. Subclass this, to create your own statemachines.
 * <p>
 * Sublcasses have to implement several methods:
 * <p>
 * void defineStates(StateConfigurer<S, E, O> state)
 * void defineTransitions(TransitionConfigurer<S, E, O> transition)
 * protected S getCurrentState(O object);
 * protected S getFinalState();
 * protected S getInitialState();
 * <p>
 * By using StateConfigurer, respectively TransitionConfigurer you define the states and transitions which are allowed. Actions which
 * should be executed during certain phases in the transition process can be registered by using the optional GenericActionConfigurer.
 *
 * @param <S> The enum type defining the states the Statemachine knows of. It is crucial that within all possible objects of type S there can not be
 *            two for which Object#equals returns true.
 * @param <E> The enum type defining the events the Statemachine listens to.
 * @param <O> The object type which is put on the context for actions to work with.
 */
public abstract class AbstractStatemachine<S, E, O> implements Statemachine {


    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final Class<S> stateType;
    protected final Class<E> eventType;
    protected final ThreadLocal<Context<O>> context = new ThreadLocal<>();
    private final Class<O> objectType;
    private final Map<S, State<S, E, O>> states = new HashMap<>();
    private Map<LifecycleEvent<?>, Action<O>> genericActions = new HashMap<>();

    /**
     * @param stateType  The class object for the state type S.
     * @param eventType  The class object for the event type E.
     * @param objectType The class object for the object type O.
     */
    public AbstractStatemachine(Class<S> stateType, Class<E> eventType, Class<O> objectType) {
        this.stateType = stateType;
        this.eventType = eventType;
        this.objectType = objectType;
    }

    /**
     * Hook method to be implemented by concrete state machine. Used to define states.
     *
     * @param state This class is supposed to be used by the implementor to define states.
     */
    protected abstract void defineStates(StateConfigurer<S, E, O> state);

    /**
     * Hook method to be implemented by concrete state machine. Used to define transitions.
     *
     * @param transition This class is supposed to be used by the implementor to define transitions.
     */
    protected abstract void defineTransitions(TransitionConfigurer<S, E, O> transition);

    /**
     * Hook method to be implemented by concrete state machine. Used to define generic actions which are executed in defined state machine
     * lifecycle states.
     *
     * @param genericActionConfigurer This class is supposed to be used by the implementor to define generic actions.
     */

    protected void defineGenericActions(GenericActionConfigurer<O> genericActionConfigurer) {
    }

    /**
     * Must be called by implementor to trigger the initialisation of the state machine.
     */
    protected final void initStateMachine() {
        S initialState = getInitialState();
        S finalState = getFinalState();
        states.put(initialState, new State<>(initialState, Optional.empty(), Optional.empty()));
        states.put(finalState, new State<>(finalState, Optional.empty(), Optional.empty()));

        defineStates(new StateConfigurer<>(states));
        defineTransitions(new TransitionConfigurer<>(states, initialState, finalState));
        defineGenericActions(new GenericActionConfigurer<>(genericActions));
    }

    Context<O> getContext() {
        return context.get();
    }

    protected State<S, E, O> getState(S state) {
        State<S, E, O> theState = states.get(state);
        if (theState == null) {
            throw new IllegalStateException("The state " + state + " is not configured for Statemachine " + getClass().getSimpleName() + ". Either"
                    + " it was not defined, or the wrong Statemachine is handling the event.");
        }
        return theState;
    }

    private void validateObjectType(Object object) {
        Validate.isTrue(objectType.isInstance(object), format("The object passed must be of type %s but was %s", objectType,
                object.getClass()));
    }

    private void validateStateType(Object state) {
        Validate.isTrue(stateType.isInstance(state), format("Expected object of type %s, but got %s", stateType, state.getClass()));
    }

    @Override
    public void proceed(Object object) {
        validateObjectType(object);
        context.set(new Context<>((O) object));
        State<S, E, O> currentState = getCurrentState();
        executeTransition(currentState, getAllAutomaticTransitions(currentState), false);
    }

    @Override
    public void sendEvent(Object event, Object object) {
        sendEvent(event, object, null);
    }

    @Override
    public void sendEvent(String event, Object object) {
        sendEvent(getEventFromString(event), object, null);
    }

    @Override
    public void sendEvent(String event, Object object, Object eventData) {
        sendEvent(getEventFromString(event), object, eventData);
    }

    @Override
    public void sendEvent(Object event, Object object, Object eventData) {
        validateObjectType(object);
        log.info("Received event {}\nfor object {}\nwith eventData {}", event, object, eventData);
        Validate.isTrue(eventType.isAssignableFrom(event.getClass()),
                format("Expected Enum of type %s, but got %s", eventType, event.getClass()));
        E validEvent = eventType.cast(event);

        context.set(new Context<>((O) object));

        createTransaction();
        try {
            getContext().setDataFor(LifecycleEvent.EVENT_RECEIVED, eventData);
            executeGenericAction(LifecycleEvent.EVENT_RECEIVED);
            sendEvent(validEvent);
        } catch (RuntimeException e) {
            handleException(e, null);
        } finally {
            closeTransaction(false);
        }
    }

    private void handleException(RuntimeException e, Transition<S, E, O> transitionToBeExecuted) {
        log.error("Exception during statemachine transition.", e);
        closeTransaction(true);

        // Clear data potentially written during the transition that caused the exception
        getContext().clearData();
        if (e instanceof TransitionException) {
            // Configuration exception, so we rethrow it.
            throw e;
        } else if (transitionToBeExecuted != null && transitionToBeExecuted.isErrorTransition()) {
            log.error("An exception occurred while executing onError transition. Stopping transition in order to avoid recursion loop", e);
            raiseProcessingErrorEvent();
        } else {
            if (getAllTransitionsForException(getCurrentState(), e.getClass()).size() > 0) {
                handleTechnicalException(e);
            } else {
                log.warn("No error state defined.");
                raiseProcessingErrorEvent();
            }
        }
    }

    private void raiseProcessingErrorEvent() {
        executeGenericAction(LifecycleEvent.PROCESSING_ERROR);
        getContext().clearData();
    }


    @Override
    public List<Object> getPossibleEventsForState(Object state) {
        validateStateType(state);
        State<S, E, O> stateObject = states.get(state);
        if (stateObject == null) {
            return Collections.emptyList();
        }
        return stateObject.getTransitions().stream()
                .filter(transition -> transition.getEvent().isPresent())
                .map(transition -> transition.getEvent().get())
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasAutomaticTransitions(Object state) {
        validateStateType(state);
        return getState((S) state).getTransitions().stream()
                .anyMatch(Transition::isAutomaticTransition);
    }

    /**
     * Sending an event will trigger a state transition if the current status, respectively the transition configuration allows it.
     * <p>
     * In pseudo-code the following happens:
     * <p>
     * Get the current state CURRENT_STATE and If present, execute the exit validator of CURRENT_STATE, get all transitions
     * TRANSITIONS exiting CURRENT_STATE which listen to the given event, get a single TRANSITION in TRANSITIONS which passes its guard.
     * Execute TRANSITION (i.e. execute transition action, execute onEntryAction ...), set new state, find and execute a potentially
     * following
     * automatic transition.
     *
     * @param event The event
     */
    private void sendEvent(E event) {
        State<S, E, O> currentState = getCurrentState();

        List<Transition<S, E, O>> allTransitionsForEvent = getAllTransitionsForEvent(currentState, event);

        executeTransition(currentState, allTransitionsForEvent, false);
    }

    /**
     * Executes the ExitValidator of the given state and handles exceptions
     *
     * @param currentState The state whose ExitValidator is called
     */
    private void executeExitValidator(State<S, E, O> currentState) {
        log.info("Executing ExitValidator of state {}", currentState.getId());
        try {
            currentState.getExitValidator().ifPresent(exitValidator -> exitValidator.execute(getContext()));
        } catch (ValidationException e) {
            handleExitValidatorException(e);
        }
    }

    /**
     * This method handles all exceptions which might occur during the execution of the exitValidator. It sets an Error object on the context
     * and executes genericActions registered with the LifecycleEvent VALIDATION_ERROR. If the exception is a ValidationException (or
     * subclass), it uses the error object within the ValidationException. For all other cases it uses the exception message.
     */
    private void handleExitValidatorException(ValidationException e) {
        Error error = e.getError();
        // If the ValidationException has a causing exception we log an error with the cause, otherwise we log info with only the error
        String logMessage = format("A validation exception occurred while executing exit validation. Error: %s", error);
        if (e.getCause() != null) {
            log.error(logMessage + ", Cause: ", e.getCause());
        } else {
            log.info(logMessage);
        }
        getContext().setDataFor(VALIDATION_ERROR, error);
        executeGenericAction(VALIDATION_ERROR);
    }

    private void handleTechnicalException(RuntimeException e) {
        log.error("An exception occurred while executing transition action or onEntryAction. Trying to transit to error state.", e);
        traverseErrorTransition(e);
    }

    /**
     * This is the heart of the statemachine. This method determines which transition should be executed and executes it. Due to automatic
     * transitions following an event triggered transition this method is recursive.
     *
     * @param currentState      The current state the statemachine is in. This is the 'from' state for potentially following transition.
     * @param validTransitions  A list of transitions that may be executed. Can be empty. The guard (when-clause) determines which transition
     *                          can be executed.
     * @param isErrorTransition A boolean that determines if the trigger for a transition is an exception during a previous transition. In
     *                          this case, the exitValidations are not called a second time.
     */
    private void executeTransition(State<S, E, O> currentState, List<Transition<S, E, O>> validTransitions,
                                   boolean isErrorTransition) { //NOSONAR squid:UnusedPrivateMethod , Aufruf in sendEvent mit Lambda Expression

        if (validTransitions.isEmpty()) {
            return;
        }

        Transition<S, E, O> transitionToBeExecuted = null;

        createTransaction();

        try {
            // If an onError-Transition is to be traversed don't execute the exitValidator again.
            if (!isErrorTransition) {
                executeExitValidator(currentState);
            }

            Optional<Transition<S, E, O>> transitionWhichPassesGuard = getTransitionWhichPassesGuard(validTransitions);
            if (!transitionWhichPassesGuard.isPresent()) {
                closeTransaction(false);
            } else {
                transitionToBeExecuted = transitionWhichPassesGuard.get();

                log.info("Executing transition {}", transitionToBeExecuted);

                // Execute transition action
                transitionToBeExecuted.getAction().ifPresent(action -> action.execute(getContext()));

                // Remember fromState because after SUCCESSFUL_STATE_CHANGE it has most likely changed.
                State<S, E, O> fromState = getCurrentState();
                State<S, E, O> toState = transitionToBeExecuted.getToState();

                // Execute onEntryAction of to-State, execute successful state change action only if transition is not reflexive
                if (!isReflexiveTransition(fromState, toState)) {
                    toState.getOnEntryAction().ifPresent(onEntryAction -> onEntryAction.execute(getContext()));

                    // Call successful state change actions
                    getContext().setDataFor(SUCCESSFUL_STATE_CHANGE, new StateChange<>(fromState.getId(), toState.getId()));
                    executeGenericAction(SUCCESSFUL_STATE_CHANGE);
                }

                // Clean up all shared data between transitions
                getContext().clearData();

                closeTransaction(false);

                // Automatically execute next transition if applicable
                executeTransition(toState, getAllAutomaticTransitions(toState), false);
            }
        } catch (RuntimeException e) {
            handleException(e, transitionToBeExecuted);
        }
    }

    /**
     * This method is called when a new transaction should be created. It is first called right after invocation of sendEvent. For
     * consecutive automatic transitions, following the one triggered by sendEvent it is called before the exitValidators are executed. This
     * means, that for the first transition (the one triggered by sendEvent) it is called <strong>twice</strong>. In this case, the second
     * call must be a no-op.
     */
    protected void createTransaction() {

    }

    /**
     * This method is called when a transaction should be committed or rolled back. A transaction is committed right after the lifecycle
     * event SUCCESSFUL_STATE_CHANGE is called. If <strong>any</strong> action throws a RuntimeException, this method is called with
     * rollback = true.
     */
    protected void closeTransaction(boolean rollback) {

    }

    private boolean isReflexiveTransition(State<S, E, O> fromState, State<S, E, O> toState) {
        return fromState.equals(toState);
    }

    private void executeGenericAction(LifecycleEvent<?> event) {
        Action<O> action = genericActions.get(event);
        if (action != null) {
            action.execute(getContext());
        }
    }

    private void traverseErrorTransition(RuntimeException e) {
        State<S, E, O> currentState = getCurrentState();
        log.info("Trying to traverse error transition for state {} and exception {}", currentState, e.getClass());
        executeTransition(currentState, getAllTransitionsForException(currentState, e.getClass()), true);
    }

    private List<Transition<S, E, O>> getAllTransitions(State<S, E, O> currentState) {
        return currentState.getTransitions();
    }

    private List<Transition<S, E, O>> getAllTransitionsForEvent(State<S, E, O> currentState, E event) {
        Predicate<Transition<S, E, O>> predicate = t -> t.getEvent().map(e -> e.equals(event)).orElse(false);
        List<Transition<S, E, O>> allTransitionsForEvent = getAllTransitionsForPredicate(currentState, predicate);
        if (allTransitionsForEvent.isEmpty()) {
            log.info("No transition from state " + currentState.getId() + " found for event " + event);

            // Call UNKNOWN_EVENT actions
            getContext().setDataFor(UNKNOWN_EVENT, event);
            executeGenericAction(UNKNOWN_EVENT);
        }
        return allTransitionsForEvent;
    }

    /**
     * Gets all transitions from state which don't have an event (onEvent) or error (onError) set and thus are considered automatic
     * transitions.
     */
    private List<Transition<S, E, O>> getAllAutomaticTransitions(State<S, E, O> state) {
        Predicate<Transition<S, E, O>> predicate = Transition::isAutomaticTransition;
        return getAllTransitionsForPredicate(state, predicate);
    }

    /**
     * Gets all transitions from state which have an error property (onError) which is assignable from errorClass
     */
    private List<Transition<S, E, O>> getAllTransitionsForException(State<S, E, O> state, Class<? extends RuntimeException> errorClass) {
        Predicate<Transition<S, E, O>> predicate = t -> t.getError().map(e -> e.isAssignableFrom(errorClass)).orElse(false);
        return getAllTransitionsForPredicate(state, predicate);
    }

    private List<Transition<S, E, O>> getAllTransitionsForPredicate(State<S, E, O> state, Predicate<Transition<S, E, O>> predicate) {
        return getAllTransitions(state).stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    private Optional<Transition<S, E, O>> getTransitionWhichPassesGuard(List<Transition<S, E, O>> transitions) {
        // Find all Transitions with guard that returns true
        List<Transition<S, E, O>> passingTransitions = transitions.stream()
                .filter(transition -> transition.getGuard().test(getContext()))
                .collect(Collectors.toList());

        if (passingTransitions.size() != 1) {
            if (passingTransitions.isEmpty()) {
                log.debug("Trying to transit from " + getCurrentState().getId()
                        + ". No transition guard returned true, thus no transition is taking place.");
                return Optional.empty();
            } else {
                throw new TransitionException("There are multiple possible transitions from " + getCurrentState().getId() +
                        ". Only one guard must return true.");
            }
        }

        return Optional.of(passingTransitions.get(0));
    }

    /**
     * Gets all States known by this statemachine.
     *
     * @return A set containing all states that were added to this statemachine.
     */
    protected Set<S> getStates() {
        return states.keySet();
    }

    private State<S, E, O> getCurrentState() {
        S currentState = getCurrentState(getContext().getObject());
        return getState(currentState);
    }

    /**
     * Statemachine implementations have to override this in order to provide the current state which is assumed to be held on the object
     * put on the Context (type O).
     *
     * @param object The context object.
     * @return Current state of the actual state machine.
     */
    protected abstract S getCurrentState(O object);

    /**
     * Hook method which should return an object which is considered the final state of the statemachine implementation.
     *
     * @return The statemachines final state.
     */
    protected abstract S getFinalState();

    /**
     * Hook method which should return an object which is considered the initial state of the statemachine implementation.
     *
     * @return The statemachines initial state.
     */
    protected abstract S getInitialState();
}
