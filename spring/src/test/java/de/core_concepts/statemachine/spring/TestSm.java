package de.core_concepts.statemachine.spring;

import de.core_concepts.statemachine.*;

import static de.core_concepts.statemachine.spring.TestEvent.EVENT1;
import static de.core_concepts.statemachine.spring.TestState.STATE1;
import static de.core_concepts.statemachine.spring.TestState.STATE2;

/**
 * Statemachine implementation for AbstractStatemachineTest.
 */
public class TestSm extends StatemachineBean<TestState, TestEvent, TestObject> {

    public TestSm() {
        super(TestState.class, TestEvent.class, TestObject.class);
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
        genericActionConfigurer.on(LifecycleEvent.SUCCESSFUL_STATE_CHANGE)
                .execute(Action.of(context -> context.getDataFor(LifecycleEvent.SUCCESSFUL_STATE_CHANGE)
                        .ifPresent(stateChange -> setCurrentState(context.getObject(), (TestState) stateChange.getToState()))))
                .add();
    }

    @Override
    public Object getCurrentState(Object object) {
        return ((TestObject) object).getState();
    }

    @Override
    public void setCurrentState(TestObject object, TestState state) {
        object.setState(state);
    }

    @Override
    public TestState getStateFromString(String stateName) {
        return TestState.valueOf(stateName);
    }

    @Override
    public TestEvent getEventFromString(String eventName) {
        return TestEvent.valueOf(eventName);
    }

    @Override
    protected TestState getFinalState() {
        return TestState.START;
    }

    @Override
    protected TestState getInitialState() {
        return TestState.END;
    }
}
