package de.core_concepts.statemachine;

import org.apache.commons.lang.Validate;

import java.util.Map;

/**
 * Created by zieglerch on 11.01.2016.
 *
 * Builder class for Actions that are executed during statemachine lifecycle phases.
 *
 * @param <O> The object type residing on the Context on which the Actions operate.
 */
public class GenericActionConfigurer<O> {

  private Action<O> action;
  private LifecycleEvent hook;
  private Map<LifecycleEvent<?>, Action<O>> genericActions;

  GenericActionConfigurer(Map<LifecycleEvent<?>, Action<O>> genericActions) {
    Validate.notNull(genericActions);
    this.genericActions = genericActions;
  }

  public GenericActionConfigurer<O> on(LifecycleEvent hook) {
    this.hook = hook;
    return this;
  }

  public GenericActionConfigurer<O> execute(Action<O> action) {
    this.action = action;
    return this;
  }

  public void add() {
    Validate.notNull(action);
    Validate.notNull(hook);
    genericActions.put(hook, action);
    clearValues();
  }

  private void clearValues() {
    action = null;
    hook = null;
  }
}
