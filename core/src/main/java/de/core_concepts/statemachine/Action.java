package de.core_concepts.statemachine;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Created by zieglerch on 18.11.2015.
 *
 * This implements some action which can be executed during the
 * transition or on entry of another state.
 * It can either be used directly with a lambda expression or it can
 * be subclassed. Subclasses should just call workPackage in their
 * constructor with a function pointer:
 *
 * super.setWorkPackage(this::doExecute)
 *
 * @param <O> The object type residing on the Context on which the Action operates.
 */
@Getter
@Setter
@NoArgsConstructor
public class Action<O> {

  private ContextConsumer<O> workPackage;

  Action(ContextConsumer<O> workPackage) {
    this.workPackage = workPackage;
  }

  public static <O> Action<O> of(ContextConsumer<O> workPackage) {
    return new Action<>(workPackage);
  }

  public void execute(Context<O> context) {
    if (workPackage != null) {
      workPackage.accept(context);
    }
  }
}
