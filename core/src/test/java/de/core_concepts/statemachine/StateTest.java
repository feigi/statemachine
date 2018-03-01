package de.core_concepts.statemachine;

import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

/**
 * Tests the clas State
 * <p>
 * Created by zieglerch on 04.08.2016.
 */
public class StateTest {

    /**
     * Tests that a transition is properly added.
     */
    @Test
    public void addTransition_AddsTransition() throws Exception {
        State<TestState, TestEvent, TestObject> fromState = new State<>(TestState.STATE1, Optional.empty(), Optional.empty());
        State<TestState, TestEvent, TestObject> toState = new State<>(TestState.STATE2, Optional.empty(), Optional.empty());
        // Kein Diamond-Operator bei der Instanziierung wegen eines Compilerbugs in Java 8_u20
        Transition<TestState, TestEvent, TestObject> transition =
                new Transition<TestState, TestEvent, TestObject>(toState, Optional.empty(), Optional.empty(), Optional.empty(), context -> true);
        fromState.addTransition(transition);

        assertThat(fromState.getTransitions(), hasSize(1));
        assertThat(fromState.getTransitions().get(0), equalTo(transition));
    }
}