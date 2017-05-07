package de.core_concepts.statemachine;


import static de.core_concepts.statemachine.TestEvent.EVENT1;
import static de.core_concepts.statemachine.TestState.FINAL;
import static de.core_concepts.statemachine.TestState.INITIAL;
import static de.core_concepts.statemachine.TestState.STATE1;
import static de.core_concepts.statemachine.TestState.STATE2;

/**
 * Statemachine Implementation for AbstractStatemachineTest.
 */
public class TestStatemachine extends AbstractStatemachine<TestState, TestEvent, TestObject> {

  public TestStatemachine() {
    super(TestState.class, TestEvent.class, TestObject.class);
    initStateMachine();
  }

  @Override
  public void defineStates(StateConfigurer<TestState, TestEvent, TestObject> state) {
    state.withId(STATE1).add();
    state.withId(STATE2).add();
  }

  @Override
  public void defineTransitions(TransitionConfigurer<TestState, TestEvent, TestObject> transition) {
    transition.from(STATE1).to(STATE2).onEvent(EVENT1).add();
  }

  @Override
  public void defineGenericActions(GenericActionConfigurer<TestObject> genericActionConfigurer) {
    genericActionConfigurer
        .on(LifecycleEvent.SUCCESSFUL_STATE_CHANGE)
        .execute(Action.of(context -> context.getDataFor(LifecycleEvent.SUCCESSFUL_STATE_CHANGE)
            .map(o -> (TestState) o)
            .ifPresent(testState -> context.getObject().setCurrentState(testState))))
        .add();
  }

  @Override
  public Enum<?> getEventFromString(String eventName) {
    return null;
  }

  @Override
  public Enum<?> getStateFromString(String StateName) {
    return null;
  }

  @Override
  protected TestState getCurrentState(TestObject object) {
    return object.getCurrentState();
  }

  @Override
  protected TestState getFinalState() {
    return FINAL;
  }

  @Override
  protected TestState getInitialState() {
    return INITIAL;
  }
}
