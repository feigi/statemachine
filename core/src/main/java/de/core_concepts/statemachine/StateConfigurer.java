package de.core_concepts.statemachine;


import org.apache.commons.lang.Validate;

import java.util.Map;
import java.util.Optional;

/**
 * Created by zieglerch on 27.11.2015.
 *
 * A builder class to configure states. It expects an empty map in which
 * the instantiated states are put.
 *
 * @param <S> An enum type defining states.
 * @param <E> An enum type defining events.
 * @param <O> The object type which is put on the context for actions to work with.
 */
public class StateConfigurer<S, E, O> {

  private S id;
  private Action<O> onEntryAction;
  private Action<O> exitValidator;

  private final Map<S, State<S, E, O>> states;

  StateConfigurer(Map<S, State<S, E, O>> states) {
    this.states = states;
  }

  /**
   * @param id The id of the state.
   *           Mandatory
   */
  public StateConfigurer<S, E, O> withId(S id) {
    this.id = id;
    return this;
  }

  /**
   * Optionally you can add an action which will be executed on entry
   * of this state. It will be executed no matter from which state
   * the transitions takes place. It is the last action being
   * executed in a state transition.
   *
   * @param onEntryAction The action to be executed upon entry of this state
   */
  public StateConfigurer<S, E, O> onEntryAction(Action<O> onEntryAction) {
    this.onEntryAction = onEntryAction;
    return this;
  }

  /**
   * Optionally you can add a validator which will be executed upon leaving
   * a state. It should set a validation result on the context which guards
   * then can evaluate.
   *
   * @param validator The validator to execute.
   */
  public StateConfigurer<S, E, O> exitValidator(Action<O> validator) {
    this.exitValidator = validator;
    return this;
  }

  /**
   * Add the state to the state machine model
   *
   * @return The state added
   */
  public State<S, E, O> add() {
    Validate.notNull(id);
    State<S, E, O> state = new State<>(id, Optional.ofNullable(onEntryAction), Optional.ofNullable(exitValidator));
    states.put(id, state);
    clearValues();
    return state;
  }

  private void clearValues() {
    this.id = null;
    this.onEntryAction = null;
    this.exitValidator = null;
  }
}

