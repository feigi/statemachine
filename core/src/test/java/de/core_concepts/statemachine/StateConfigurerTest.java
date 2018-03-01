package de.core_concepts.statemachine;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static de.core_concepts.statemachine.TestState.STATE1;
import static de.core_concepts.statemachine.TestState.STATE2;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * Tests the StateConfigurer class
 * <p>
 * Created by zieglerch on 08.12.2015.
 */
public class StateConfigurerTest {

    private Map<TestState, State<TestState, TestEvent, TestObject>> states;
    private StateConfigurer<TestState, TestEvent, TestObject> configurer;

    @Before
    public void setUp() throws Exception {
        states = new HashMap<>();
        configurer = new StateConfigurer<>(states);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testId_Null() throws Exception {
        configurer.add();
    }

    @Test
    public void testState_Object_Has_Correct_Values() throws Exception {
        Action action = mock(Action.class);
        Action validator = mock(Action.class);
        configurer
                .withId(STATE1)
                .onEntryAction(action)
                .exitValidator(validator)
                .add();

        State<TestState, TestEvent, TestObject> state = getState(STATE1);
        assertEquals(STATE1, state.getId());
        assertEquals(action, state.getOnEntryAction().get());
        assertEquals(validator, state.getExitValidator().get());
    }

    @Test
    public void testState_Second_Object_Has_Correct_Values() throws Exception {
        Action action = mock(Action.class);
        Action action2 = mock(Action.class);
        Action validator = mock(Action.class);
        Action validator2 = mock(Action.class);
        configurer
                .withId(STATE1)
                .onEntryAction(action)
                .exitValidator(validator)
                .add();

        configurer
                .withId(STATE2)
                .onEntryAction(action2)
                .exitValidator(validator2)
                .add();

        State<TestState, TestEvent, TestObject> state = getState(STATE1);
        State<TestState, TestEvent, TestObject> state2 = getState(STATE2);
        assertEquals(STATE1, state.getId());
        assertEquals(action, state.getOnEntryAction().get());
        assertEquals(validator, state.getExitValidator().get());
        assertEquals(STATE2, state2.getId());
        assertEquals(action2, state2.getOnEntryAction().get());
        assertEquals(validator2, state2.getExitValidator().get());
    }

    @Test
    public void testState_Is_InMap() throws Exception {
        configurer
                .withId(STATE1)
                .add();
        assertNotNull(getState(STATE1));
    }

    @Test
    public void testOptionals_Instantiated() throws Exception {
        configurer
                .withId(STATE1)
                .add();

        State<TestState, TestEvent, TestObject> state = getState(STATE1);
        assertNotNull(state.getExitValidator());
        assertNotNull(state.getOnEntryAction());
    }

    private State<TestState, TestEvent, TestObject> getState(TestState state) {
        return states.get(state);
    }
}