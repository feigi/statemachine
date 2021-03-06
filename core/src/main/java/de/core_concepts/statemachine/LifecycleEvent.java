package de.core_concepts.statemachine;


import java.util.Arrays;
import java.util.LinkedList;

/**
 * Created by zieglerch on 11.01.2016.
 * <p>
 * This class defines certain lifecycle events which a statemachine can use to define actions which are called when the statemachine reaches
 * a certain lifecycle. If there is data which needs to be shared, it will be saved in a special Map on the Context. The type of this data
 * is defined as constructor argument in this class. For instance a ValidationException causes the VALIDATION_ERROR lifecycle event to be
 * triggered and an Error object to be saved for the callback action to be used.
 *
 * @param <T> The data type returned if Context#getDataFor is called.
 */
public class LifecycleEvent<T> {

    /* Statemachine#sendEvent was called. The data passed to sendEvent will be put on Context. */
    public static final LifecycleEvent<Object> EVENT_RECEIVED = new LifecycleEvent<>(Object.class);
    /* No transition was found on the current state for the given event. The event sent will be put on Context */
    public static final LifecycleEvent<Object> UNKNOWN_EVENT = new LifecycleEvent<>(Object.class);
    /* An exception occurred during exit validation. An Error object will be put on Context */
    public static final LifecycleEvent<Error> VALIDATION_ERROR = new LifecycleEvent<>(Error.class);
    /* State was changed (only if toState != fromState). No data will be put on Context. */
    public static final LifecycleEvent<StateChange> SUCCESSFUL_STATE_CHANGE = new LifecycleEvent<>(StateChange.class);
    /* An error (usually in form of a caught exception) was raised during processing a state change. */
    public static final LifecycleEvent<Object> PROCESSING_ERROR = new LifecycleEvent<>(Object.class);

    /* A list defining the order in which the lifecycle events occur during a state transition */
    protected static final LinkedList<LifecycleEvent<?>> ORDERED_LIFECYCLE_EVENTS =
            new LinkedList<>(Arrays.asList(EVENT_RECEIVED, UNKNOWN_EVENT, VALIDATION_ERROR, SUCCESSFUL_STATE_CHANGE));

    private final Class<T> type;

    public LifecycleEvent(Class<T> type) {
        this.type = type;
    }

    public Class<T> getType() {
        return this.type;
    }
}
