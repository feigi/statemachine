package de.core_concepts.statemachine;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static de.core_concepts.statemachine.TestEvent.EVENT1;
import static de.core_concepts.statemachine.TestState.*;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * Tests the class TransitionConfigurer
 * <p>
 * Created by zieglerch on 03.12.2015.
 */
public class TransitionConfigurerTest {

    private TransitionConfigurer<TestState, TestEvent, TestObject> configurer;
    private HashMap<TestState, State<TestState, TestEvent, TestObject>> states;

    @Before
    public void setUp() throws Exception {
        states = new HashMap<>();
        states.put(INITIAL, new State<>(INITIAL, Optional.empty(), Optional.empty()));
        states.put(STATE1, new State<>(STATE1, Optional.empty(), Optional.empty()));
        states.put(STATE2, new State<>(STATE2, Optional.empty(), Optional.empty()));
        states.put(STATE3, new State<>(STATE3, Optional.empty(), Optional.empty()));
        states.put(FINAL, new State<>(FINAL, Optional.empty(), Optional.empty()));
        configurer = new TransitionConfigurer<>(states, INITIAL, FINAL);
    }

    /**
     * Tests constructor validation.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInstantiation_StatesNotNull() throws Exception {
        configurer = new TransitionConfigurer<>(null, INITIAL, FINAL);
    }

    /**
     * Tests constructor validation.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInstantiation_StatesEmpty() throws Exception {
        configurer = new TransitionConfigurer<>(Collections.emptyMap(), INITIAL, FINAL);
    }

    /**
     * Tests constructor validation.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInstantiation_InitialNull() throws Exception {
        configurer = new TransitionConfigurer<>(states, null, FINAL);
    }

    /**
     * Tests constructor validation.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInstantiation_FinalNull() throws Exception {
        configurer = new TransitionConfigurer<>(states, INITIAL, null);
    }

    /**
     * Tests if all values are reset after an add() call.
     */
    @Test
    public void testTransition_Second_Object_Has_Correct_Values() throws Exception {
        Action<TestObject> action = mock(Action.class);
        Action<TestObject> action2 = mock(Action.class);
        Predicate<Context<TestObject>> predicate = mock(Predicate.class);
        Predicate<Context<TestObject>> predicate2 = mock(Predicate.class);

        configurer
                .from(STATE1)
                .to(STATE2)
                .action(action)
                .onEvent(EVENT1)
                .when(predicate)
                .add();

        configurer
                .from(STATE2)
                .to(STATE3)
                .action(action2)
                .onError(RuntimeException.class)
                .when(predicate2)
                .add();

        Transition<TestState, TestEvent, TestObject> state1ToState2 = transitionFor(STATE1);
        Transition<TestState, TestEvent, TestObject> state2ToState3 = transitionFor(STATE2);
        assertEquals(STATE2, state1ToState2.getToState().getId());
        assertEquals(action, state1ToState2.getAction().get());
        assertEquals(EVENT1, state1ToState2.getEvent().get());
        assertEquals(predicate, state1ToState2.getGuard());
        assertEquals(STATE3, state2ToState3.getToState().getId());
        assertEquals(action2, state2ToState3.getAction().get());
        assertFalse(state2ToState3.getEvent().isPresent());
        assertEquals(predicate2, state2ToState3.getGuard());
    }

    /**
     * Tests if an Exception is thrown if 'to' is null.
     */
    @Test(expected = ConfigurationException.class)
    public void testTo_Null() throws Exception {
        configurer
                .from(STATE1)
                .add();
    }

    /**
     * Tests that 'to' is not needed if 'toSelf' is used.
     */
    @Test
    public void testTo_Null_But_ToSelf() throws Exception {
        configurer
                .from(STATE1)
                .toSelf()
                .add();
    }

    /**
     * It shouldn't be possible to call to with the initial state.
     */
    @Test(expected = ConfigurationException.class)
    public void testTo_InitialState() throws Exception {
        configurer
                .from(STATE1)
                .to(INITIAL)
                .add();
    }

    /**
     * Tests an exception is thrown if neither 'from' and 'fromAll' is used.
     */
    @Test(expected = ConfigurationException.class)
    public void testFrom_Null() throws Exception {
        configurer
                .to(STATE2)
                .add();
    }

    /**
     * It shouldn't be possible to call from with the final state.
     */
    @Test(expected = ConfigurationException.class)
    public void testFrom_FinalState() throws Exception {
        configurer
                .from(FINAL)
                .to(STATE1)
                .add();
    }

    /**
     * Tests that 'from' is not needed if 'fromAll' is used.
     */
    @Test
    public void testFrom_Null_But_FromAll() throws Exception {
        configurer
                .fromAll()
                .to(STATE2)
                .add();
    }

    /**
     * Tests if an added transition has the correct values.
     */
    @Test
    public void testTransition_Object_Has_Correct_Values() throws Exception {
        Action<TestObject> action = mock(Action.class);
        Predicate<Context<TestObject>> predicate = mock(Predicate.class);
        configurer
                .from(STATE1)
                .to(STATE2)
                .action(action)
                .onEvent(EVENT1)
                .when(predicate)
                .add();

        Transition<TestState, TestEvent, TestObject> transition = transitionFor(STATE1);
        assertEquals(STATE2, transition.getToState().getId());
        assertEquals(action, transition.getAction().get());
        assertEquals(EVENT1, transition.getEvent().get());
        assertEquals(predicate, transition.getGuard());
    }

    /**
     * Tests that an onEvent call after onError causes an exception.
     */
    @Test(expected = ConfigurationException.class)
    public void testOnEvent_After_OnError() throws Exception {
        configurer.from(STATE1)
                .to(STATE2)
                .onError(RuntimeException.class)
                .onEvent(EVENT1)
                .add();
    }

    /**
     * Tests that an 'onError' call after 'onEvent' causes an exception.
     */
    @Test(expected = ConfigurationException.class)
    public void testNo_Event_If_Error() throws Exception {
        configurer.from(STATE1)
                .to(STATE2)
                .onEvent(EVENT1)
                .onError(RuntimeException.class)
                .add();
    }

    /**
     * Tests that all optional fields are pre-initialized with empty Optionals.
     */
    @Test
    public void testOptionals_Instantiated() throws Exception {
        configurer.from(STATE1)
                .to(STATE2)
                .add();

        Transition<TestState, TestEvent, TestObject> transition = transitionFor(STATE1);
        assertNotNull(transition.getAction());
        assertNotNull(transition.getError());
        assertNotNull(transition.getEvent());
    }

    /**
     * Tests that 'from' with multiple states correctly adds transitions from all given states.
     */
    @Test
    public void testFrom_MultipleStates() throws Exception {
        Action action = mock(Action.class);
        Predicate<Context<TestObject>> guard = mock(Predicate.class);

        configurer.from(STATE1, STATE2)
                .to(STATE3)
                .when(guard)
                .onEvent(EVENT1)
                .action(action)
                .add();

        Stream.of(STATE1, STATE2)
                .forEach(state -> {
                    Transition<TestState, TestEvent, TestObject> transition = transitionFor(state);
                    assertEquals(STATE3, transition.getToState().getId());
                    assertEquals(action, transition.getAction().get());
                    assertEquals(guard, transition.getGuard());
                    assertEquals(EVENT1, transition.getEvent().get());
                });
    }

    /**
     * Tests that 'from' with multiple states can not contain the final state.
     */
    @Test(expected = ConfigurationException.class)
    public void testFrom_MultipleStates_ContainsFinalState() throws Exception {
        configurer.from(STATE1, FINAL)
                .to(STATE3)
                .add();
    }

    /**
     * Tests that 'fromAll' adds a transition outgoing from all existing states.
     */
    @Test
    public void testFromAll_Adds_Transitions_To_All_States() throws Exception {
        Action action = mock(Action.class);
        Predicate<Context<TestObject>> guard = mock(Predicate.class);

        configurer.fromAll()
                .to(STATE1)
                .when(guard)
                .onEvent(EVENT1)
                .action(action)
                .add();

        states.values().stream()
                .filter(state -> !asList(INITIAL, FINAL).contains(state.getId()))
                .forEach(state -> {
                    Transition<TestState, TestEvent, TestObject> transition = transitionFor(state.getId());
                    assertEquals(STATE1, transition.getToState().getId());
                    assertEquals(action, transition.getAction().get());
                    assertEquals(guard, transition.getGuard());
                    assertEquals(EVENT1, transition.getEvent().get());
                });
    }

    /**
     * Tests that 'fromAll' excludes the initial and final state.
     */
    @Test
    public void testFromAll_ExcludesInitialAndFinalStates() throws Exception {
        configurer.fromAll()
                .to(STATE1)
                .add();

        assertThat(states.get(INITIAL).getTransitions(), empty());
        assertThat(states.get(FINAL).getTransitions(), empty());
    }

    /**
     * Tests that 'toSelf' adds a reflexive transition.
     */
    @Test
    public void testToSelf_Adds_Reflexive_Transition() throws Exception {
        configurer.from(STATE1)
                .toSelf()
                .add();

        Transition<TestState, TestEvent, TestObject> transition = transitionFor(STATE1);
        assertEquals(STATE1, transition.getToState().getId());
    }

    /**
     * Tests that fromInitial adds a transition from the INITIAL state.
     */
    @Test
    public void testFromInitial() throws Exception {
        configurer.fromInitial()
                .to(STATE1)
                .add();

        Transition<TestState, TestEvent, TestObject> transition = transitionFor(INITIAL);
        assertNotNull(transition);
        assertEquals(STATE1, transition.getToState().getId());
    }

    /**
     * Tests that toFinal adds a transition to the FINAL state.
     */
    @Test
    public void testToFinal() throws Exception {
        configurer.from(STATE1)
                .toFinal()
                .add();

        Transition<TestState, TestEvent, TestObject> transition = transitionFor(STATE1);
        assertNotNull(transition);
        assertEquals(FINAL, transition.getToState().getId());
    }

    private Transition<TestState, TestEvent, TestObject> transitionFor(TestState state) {
        return states.get(state).getTransitions().get(0);
    }
}