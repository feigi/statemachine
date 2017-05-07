package de.core_concepts.statemachine;


import java.util.Optional;
import java.util.function.Predicate;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * Created by zieglerch on 24.11.2015.
 *
 * This class represents a transition to a state. It can either listen to an event or an error and it can also define a guard which returns
 * true or false depending on whether the transition can be traversed. Usually a transition also has an action to be executed when being
 * traversed.
 *
 * @param <S> The type defining the states the Statemachine knows of.
 * @param <E> The type defining the events the Statemachine listens to.
 * @param <O> The object type which is put on the context for actions to work with.
 */
@Value
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class Transition<S, E, O> {

  private State<S, E, O> toState;
  private Optional<E> event;
  private Optional<Class<? extends RuntimeException>> error;
  private Optional<Action<O>> action;
  private Predicate<Context<O>> guard;

  boolean isErrorTransition() {
    return !event.isPresent() && error.isPresent();
  }

  boolean isAutomaticTransition() {
    return !event.isPresent() && !error.isPresent();
  }
}
