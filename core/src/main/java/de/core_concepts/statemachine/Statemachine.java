package de.core_concepts.statemachine;


import java.util.List;


/**
 * Created by zieglerch on 26.11.2015.
 */
public interface Statemachine {

    /**
     * In a state which has automatic transitions (transitions without onError or onEvent), calling this method will try to execute an
     * automatic transition, effectively continuing the process. Use this method if the process stopped unintentionally, e.g. due to a
     * transaction rollback, and you want to restart it.
     *
     * @param object the object on which the Statemachine will be based
     */
    void proceed(Object object);

    /**
     * Sends an event to the Statemachine implementation at hand. The implementation of this method is required to check the type of this event against
     * the event enum it has defined. Should the type not match, an IllegalArgumentException is to be thrown.
     *
     * @param event  the object which represents the event to be sent. Must be of the same type as generic type parameter E of
     *               AbstractStatemachine
     * @param object the object on which the Statemachine will be based
     */
    void sendEvent(Object event, Object object);

    /**
     * Sends an event to the Statemachine implementation at hand. The event is given as a String and {@link Statemachine#getEventFromString(String)} is used to
     * determine the actual event object.
     *
     * @param event  a string that uniquely identifies an event object. {@link Statemachine#getEventFromString(String)} must return this object.
     * @param object the object on which the Statemachine will be based.
     */
    void sendEvent(String event, Object object);

    /**
     * Sends an event to the Statemachine implementation at hand. The event is given as a String and {@link Statemachine#getEventFromString(String)} is used to
     * determine the actual event object.
     *
     * @param event     a string that uniquely identifies an event object. {@link Statemachine#getEventFromString(String)} must return this object.
     * @param object    the object on which the Statemachine will be based
     * @param eventData Additional data of the event which is passed to the context of the Statemachine
     */
    void sendEvent(String event, Object object, Object eventData);

    /**
     * Sends an event to the Statemachine implementation at hand. The implementation of this method is required to check the type of this event against
     * the event enum it has defined. Should the type not match, an IllegalArgumentException is to be thrown.
     *
     * @param event     the object which represents the event to be sent. Must be of the same type as generic type parameter E of
     *                  AbstractStatemachine
     * @param object    the object on which the Statemachine will be based
     * @param eventData Additional data of the event which is passed to the context of the Statemachine
     */
    void sendEvent(Object event, Object object, Object eventData);

    /**
     * Statemachine implementations have to override this in order to provide the current state which is assumed to be held on the object
     * put on the Context.
     *
     * @param object The context object.
     * @return Current state of the actual state machine.
     */
    Object getCurrentState(Object object);

    /**
     * Translates a string representing a state into its object value.
     *
     * @param stateName A string representing state object.
     * @return The state object which is represented by the string if conversion was successful. Null if no object could be found.
     */
    Object getStateFromString(String stateName);

    /**
     * Translates a string representing an event into its object value.
     *
     * @param eventName A string representing the event object.
     * @return The event object which is represented by the string if conversion was successful. Null if no object could be found.
     */
    Object getEventFromString(String eventName);

    /**
     * For a given state returns the events that cause a state transition.
     *
     * @param state The state object for which events should be looked up.
     * @return A list with all possible events. An empty list if none were found.
     */
    List<Object> getPossibleEventsForState(Object state);

    /**
     * Checks if a given state has any automatic transitions, i.e. transitions that don't have an event or error.
     *
     * @param state The state object whose transitions should be checked.
     * @return True is the state has any transitions without error and event.
     */
    boolean hasAutomaticTransitions(Object state);
}
