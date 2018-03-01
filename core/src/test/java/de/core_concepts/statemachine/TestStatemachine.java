package de.core_concepts.statemachine;


import static de.core_concepts.statemachine.TestEvent.EVENT1;
import static de.core_concepts.statemachine.TestState.*;

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
                        .ifPresent(stateChange -> context.getObject().setCurrentState(((TestState) stateChange.getToState())))))
                .add();
    }

    @Override
    protected void setCurrentState(TestObject object, TestState state) {
        object.setCurrentState(state);
    }

    @Override
    public TestEvent getEventFromString(String eventName) {
        return null;
    }

    @Override
    public TestState getCurrentState(Object object) {
        return ((TestObject) object).getCurrentState();
    }

    @Override
    public TestState getStateFromString(String StateName) {
        return null;
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
