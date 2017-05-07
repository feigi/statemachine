package de.core_concepts.statemachine;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

/**
 * Created by zieglerch on 24.11.2015.
 *
 * This class represents a state with all its (configured) attributes in the statemachine.
 *
 * @param <S> The type defining the states the Statemachine knows of.
 * @param <E> The type defining the events the Statemachine listens to.
 * @param <O> The object type which is put on the context for actions to work with.
 *
 */
@Value
@ToString(exclude = "transitions")
@EqualsAndHashCode(exclude = "transitions")
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class State<S, E, O> {

  private final S id;
  private final Optional<Action<O>> onEntryAction;
  private final Optional<Action<O>> exitValidator;
  private final List<Transition<S, E, O>> transitions = new ArrayList<>();

  State<S, E, O> addTransition(Transition<S, E, O> transition) {
    transitions.add(transition);
    return this;
  }
}
