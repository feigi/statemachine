package de.core_concepts.statemachine;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static de.core_concepts.statemachine.TestEvent.EVENT1;
import static de.core_concepts.statemachine.TestEvent.EVENT2;
import static de.core_concepts.statemachine.TestState.*;
import static java.util.Collections.emptyList;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class AbstractStatemachineTest {

    private TestObject testObject;

    @Before
    public void setUp() throws Exception {
        testObject = new TestObject();
        testObject.setCurrentState(STATE1);
    }

    @Test
    public void contextLoads() {
    }

    @Test
    public void testStateTransition_OnEvent() throws Exception {
        TestStatemachine testGF = new TestStatemachine();

        testGF.sendEvent(EVENT1, testObject);
        assertTargetState(STATE2, testObject);
    }

    @Test
    public void testStateTransition_NoTransitions() throws Exception {
        TestStatemachine testGF = new TestStatemachine() {
            @Override
            public void defineTransitions(TransitionConfigurer<TestState, TestEvent, TestObject> transition) {
                // None
            }
        };

        testGF.sendEvent(EVENT1, testObject);

        assertNoTransition(testObject);
    }

    @Test
    public void testStateTransition_Wrong_Event() throws Exception {
        Action exitValidator = mock(Action.class);
        Action transitAction = mock(Action.class);
        Action onEntryAction = mock(Action.class);
        TestStatemachine testGF = new TestStatemachine() {
            @Override
            public void defineStates(StateConfigurer<TestState, TestEvent, TestObject> state) {
                state.withId(STATE1).exitValidator(exitValidator).add();
                state.withId(STATE2).onEntryAction(onEntryAction).add();
            }

            @Override
            public void defineTransitions(TransitionConfigurer<TestState, TestEvent, TestObject> transition) {
                transition.from(STATE1).to(STATE2).onEvent(EVENT1).action(transitAction).add();
            }
        };

        testGF.sendEvent(EVENT2, testObject);

        assertNoTransition(testObject);
        verifyNoMoreInteractions(exitValidator, transitAction, onEntryAction);
    }

    @Test
    public void testStateTransition_Transit_Action_Executed() throws Exception {
        Action transitAction = mock(Action.class);
        TestStatemachine testGF = new TestStatemachine() {

            @Override
            public void defineTransitions(TransitionConfigurer<TestState, TestEvent, TestObject> transition) {
                transition.from(STATE1).to(STATE2).onEvent(EVENT1).action(transitAction).add();
            }
        };

        testGF.sendEvent(EVENT1, testObject);
        verify(transitAction).execute(any());
        verifyNoMoreInteractions(transitAction);
    }

    @Test
    public void testStateTransition_Transit_Action_Chain_Executed() throws Exception {
        ContextConsumer step1 = mock(ContextConsumer.class);
        ContextConsumer step2 = mock(ContextConsumer.class);
        Action transitAction = spy(ChainedAction.firstDo(step1).thenDo(step2));
        TestStatemachine testGF = new TestStatemachine() {

            @Override
            public void defineTransitions(TransitionConfigurer<TestState, TestEvent, TestObject> transition) {
                transition.from(STATE1).to(STATE2).onEvent(EVENT1).action(transitAction).add();
            }
        };

        testGF.sendEvent(EVENT1, testObject);

        verify(transitAction).execute(any());
        verify(step1).accept(any());
        verify(step2).accept(any());
        verifyNoMoreInteractions(transitAction, step1, step2);
    }

    /**
     * Given we have states / transitions A -> B -> C, where A -> B is triggered on event and transition B -> C has no event and no error
     * assigned, test whether the exitValidation of B is executed.
     */
    @Test
    public void testStateTransition_Transit_Action_Executed_On_AutoTransit() throws Exception {
        Action transitAction = mock(Action.class);
        TestStatemachine testGF = new TestStatemachine() {

            @Override
            public void defineStates(StateConfigurer<TestState, TestEvent, TestObject> state) {
                state.withId(STATE1).add();
                state.withId(STATE2).add();
                state.withId(STATE3).add();
            }

            @Override
            public void defineTransitions(TransitionConfigurer<TestState, TestEvent, TestObject> transition) {
                transition.from(STATE1).to(STATE2).onEvent(EVENT1).add();
                transition.from(STATE2).to(STATE3).action(transitAction).add();
            }
        };

        testGF.sendEvent(EVENT1, testObject);
        verify(transitAction).execute(any());
        verifyNoMoreInteractions(transitAction);
    }

    @Test
    public void testStateTransition_ExitValidation_Executed() throws Exception {
        Action exitValidator = mock(Action.class);
        TestStatemachine testGF = new TestStatemachine() {

            @Override
            public void defineStates(StateConfigurer<TestState, TestEvent, TestObject> state) {
                state.withId(STATE1).exitValidator(exitValidator).add();
                state.withId(STATE2).add();
            }
        };

        testGF.sendEvent(EVENT1, testObject);

        verify(exitValidator).execute(any());
        verifyNoMoreInteractions(exitValidator);
    }

    /**
     * Given we have states / transitions A -> B -> C, where A -> B is triggered on event and transition B -> C has no event and no error
     * assigned, test whether the exitValidation of B is executed.
     */
    @Test
    public void testStateTransition_ExitValidation_Executed_On_AutoTransit() throws Exception {
        Action exitValidator = mock(Action.class);
        TestStatemachine testGF = new TestStatemachine() {

            @Override
            public void defineStates(StateConfigurer<TestState, TestEvent, TestObject> state) {
                state.withId(STATE1).add();
                state.withId(STATE2)
                        .exitValidator(exitValidator)
                        .add();
                state.withId(STATE3).add();
            }

            @Override
            public void defineTransitions(TransitionConfigurer<TestState, TestEvent, TestObject> transition) {
                transition.from(STATE1).to(STATE2).onEvent(EVENT1).add();
                transition.from(STATE2).to(STATE3).add();
            }
        };

        testGF.sendEvent(EVENT1, testObject);
        verify(exitValidator).execute(any());
        verifyNoMoreInteractions(exitValidator);
    }

    @Test
    public void testStateTransition_ExitValidation_Executed_Only_Once_If_OnError_Transition() throws Exception {
        Action exitValidator = mock(Action.class);
        Action throwingAction = mock(Action.class);
        doThrow(RuntimeException.class).when(throwingAction).execute(any());
        TestStatemachine testGF = new TestStatemachine() {

            @Override
            public void defineStates(StateConfigurer<TestState, TestEvent, TestObject> state) {
                state.withId(STATE1)
                        .exitValidator(exitValidator).add();
                state.withId(STATE2).add();
                state.withId(STATE3).add();
            }

            @Override
            public void defineTransitions(TransitionConfigurer<TestState, TestEvent, TestObject> transition) {
                transition.from(STATE1).to(STATE2)
                        .onEvent(EVENT1)
                        .action(throwingAction)
                        .add();
                transition.from(STATE1).to(STATE3)
                        .onError(RuntimeException.class).add();
            }
        };

        testGF.sendEvent(EVENT1, testObject);
        verify(exitValidator).execute(any());
        verifyNoMoreInteractions(exitValidator);
    }

    @Test
    public void testTechnicalException_During_ExitValidation_Has_Validation_Error() throws Exception {
        Action exitValidator = mock(Action.class);
        doThrow(RuntimeException.class).when(exitValidator).execute(any());
        TestStatemachine testGF = new TestStatemachine() {

            @Override
            public void defineStates(StateConfigurer<TestState, TestEvent, TestObject> state) {
                state.withId(STATE1).exitValidator(exitValidator).add();
                state.withId(STATE2).add();
            }

            @Override
            public void defineGenericActions(GenericActionConfigurer genericActionConfigurer) {
                genericActionConfigurer.on(LifecycleEvent.VALIDATION_ERROR)
                        .execute(new Action<>(context -> assertTrue(context.hasValidationError())))
                        .add();
            }
        };

        testGF.sendEvent(EVENT1, testObject);
    }

    @Test
    public void testNo_OnError_Transition_If_TechnicalException_During_ExitValidation() throws Exception {
        Action exitValidator = mock(Action.class);
        Action onEntryAction = mock(Action.class);
        doThrow(RuntimeException.class).when(exitValidator).execute(any());
        TestStatemachine testGF = new TestStatemachine() {

            @Override
            public void defineStates(StateConfigurer<TestState, TestEvent, TestObject> state) {
                state.withId(STATE1).exitValidator(exitValidator).add();
                state.withId(STATE2).add();
                state.withId(STATE2B).onEntryAction(onEntryAction).add();
            }

            @Override
            public void defineTransitions(TransitionConfigurer<TestState, TestEvent, TestObject> transition) {
                transition.from(STATE1).to(STATE2).onEvent(EVENT1).add();
                transition.from(STATE1).to(STATE2B).onError(RuntimeException.class).add();
            }
        };

        testGF.sendEvent(EVENT1, testObject);

        verify(onEntryAction).execute(any());
        verifyNoMoreInteractions(onEntryAction);
    }

    @Test
    public void testStateTransition_OnEntryAction_Executed() throws Exception {
        Action onEntryAction = mock(Action.class);
        TestStatemachine testGF = new TestStatemachine() {

            @Override
            public void defineStates(StateConfigurer<TestState, TestEvent, TestObject> state) {
                state.withId(STATE1).add();
                state.withId(STATE2).onEntryAction(onEntryAction).add();
            }
        };

        testGF.sendEvent(EVENT1, testObject);

        verify(onEntryAction).execute(any());
        verifyNoMoreInteractions(onEntryAction);
    }

    /**
     * Given we have states / transitions A -> B -> C, where A -> B is triggered on event and transition B -> C has no event and no error
     * assigned, test whether the onEntryAction of C is executed.
     */
    @Test
    public void testStateTransition_OnEntryAction_Executed_On_AutoTransit() throws Exception {
        Action onEntryAction = mock(Action.class);
        TestStatemachine testGF = new TestStatemachine() {

            @Override
            public void defineStates(StateConfigurer<TestState, TestEvent, TestObject> state) {
                state.withId(STATE1).add();
                state.withId(STATE2).add();
                state.withId(STATE3).onEntryAction(onEntryAction).add();
            }

            @Override
            public void defineTransitions(TransitionConfigurer<TestState, TestEvent, TestObject> transition) {
                transition.from(STATE1).to(STATE2).onEvent(EVENT1).add();
                transition.from(STATE2).to(STATE3).add();
            }
        };

        testGF.sendEvent(EVENT1, testObject);

        verify(onEntryAction).execute(any());
        verifyNoMoreInteractions(onEntryAction);
    }

    /**
     * Test assures that OnEntryAction is not executed if the transition is reflexive
     */
    @Test
    public void testStateTransition_OnEntryAction_Not_Executed_If_Reflexive() throws Exception {
        Action onEntryAction = mock(Action.class);
        TestStatemachine testGF = new TestStatemachine() {

            @Override
            public void defineStates(StateConfigurer<TestState, TestEvent, TestObject> state) {
                state.withId(STATE1).onEntryAction(onEntryAction).add();
            }

            @Override
            public void defineTransitions(TransitionConfigurer<TestState, TestEvent, TestObject> transition) {
                transition.from(STATE1).toSelf().onEvent(EVENT1).add();
            }
        };

        testGF.sendEvent(EVENT1, testObject);

        verifyNoMoreInteractions(onEntryAction);
    }

    @Test
    public void testStateTransition_NoTransition_If_Guard_False() throws Exception {
        Predicate guard = mock(Predicate.class);
        TestStatemachine testGF = new TestStatemachine() {

            @Override
            public void defineTransitions(TransitionConfigurer<TestState, TestEvent, TestObject> transition) {
                transition.from(STATE1).to(STATE2).onEvent(EVENT1).when(guard).add();
            }
        };

        testGF.sendEvent(EVENT1, testObject);

        verify(guard).test(any());
        verifyNoMoreInteractions(guard);
        assertNoTransition(testObject);
    }

    @Test
    public void testStateTransition_Transition_If_Guard_True() throws Exception {
        Predicate<Context<TestObject>> guard = mock(Predicate.class);
        doReturn(true).when(guard).test(any());
        TestStatemachine testGF = new TestStatemachine() {

            @Override
            public void defineTransitions(TransitionConfigurer<TestState, TestEvent, TestObject> transition) {
                transition.from(STATE1).to(STATE2).onEvent(EVENT1).when(guard).add();
            }
        };

        testGF.sendEvent(EVENT1, testObject);

        verify(guard).test(any());
        verifyNoMoreInteractions(guard);
        assertTargetState(STATE2, testObject);
    }

    @Test
    public void testStateTransition_Multiple_To_States_No_Passing_Guard() throws Exception {
        Predicate<Context> guard2A = mock(Predicate.class);
        Predicate<Context> guard2B = mock(Predicate.class);
        TestStatemachine testGF = multipleToStatesGF(guard2A, guard2B);

        testGF.sendEvent(EVENT1, testObject);

        verify(guard2A).test(any());
        verify(guard2B).test(any());
        verifyNoMoreInteractions(guard2A, guard2B);
        assertNoTransition(testObject);
    }

    @Test(expected = TransitionException.class)
    public void testStateTransition_Multiple_To_States_Multiple_Passing_Guards() throws Exception {
        Predicate<Context> guard2A = mock(Predicate.class);
        Predicate<Context> guard2B = mock(Predicate.class);
        doReturn(true).when(guard2A).test(any());
        doReturn(true).when(guard2B).test(any());
        TestStatemachine testGF = multipleToStatesGF(guard2A, guard2B);

        testGF.sendEvent(EVENT1, testObject);
    }

    @Test
    public void testStateTransition_Multiple_To_States_One_Passing_Guard() throws Exception {
        Predicate<Context> guard2A = mock(Predicate.class);
        Predicate<Context> guard2B = mock(Predicate.class);
        doReturn(true).when(guard2B).test(any());
        TestStatemachine testGF = multipleToStatesGF(guard2A, guard2B);

        testGF.sendEvent(EVENT1, testObject);

        verify(guard2A).test(any());
        verify(guard2B).test(any());
        verifyNoMoreInteractions(guard2A, guard2B);
        assertTargetState(STATE2B, testObject);
    }

    private TestStatemachine multipleToStatesGF(final Predicate guard2A, final Predicate guard2B) {
        return new TestStatemachine() {

            @Override
            public void defineStates(StateConfigurer<TestState, TestEvent, TestObject> state) {
                state.withId(TestState.STATE1).add();
                state.withId(TestState.STATE2A).add();
                state.withId(TestState.STATE2B).add();
            }

            @Override
            public void defineTransitions(TransitionConfigurer<TestState, TestEvent, TestObject> transition) {
                transition.from(TestState.STATE1).to(TestState.STATE2A).onEvent(TestEvent.EVENT1).when(guard2A).add();
                transition.from(TestState.STATE1).to(TestState.STATE2B).onEvent(TestEvent.EVENT1).when(guard2B).add();
            }
        };
    }

    @Test
    public void testValidationResultResetAfterTransition() throws Exception {
        TestStatemachine testGF = new TestStatemachine();
        // Simulate a previous transition with validation error has taken place.
        testGF.context.set(new Context(mock(TestObject.class)));
        testGF.getContext().setDataFor(LifecycleEvent.VALIDATION_ERROR, mock(Error.class));

        testGF.sendEvent(EVENT1, testObject);

        assertTargetState(STATE2, testObject);
        assertFalse(testGF.getContext().getValidationError().isPresent());
    }

    @Test
    public void testStateTransition_Transition_To_ErrorState_If_Action_Throws_Exception() throws Exception {
        Action onEntryAction = mock(Action.class);
        Action transitionAction = mock(Action.class);
        Action exceptionThrowingAction = mock(Action.class);
        doThrow(RuntimeException.class).when(exceptionThrowingAction).execute(any());
        TestStatemachine testGF = new TestStatemachine() {

            @Override
            public void defineStates(StateConfigurer<TestState, TestEvent, TestObject> state) {
                state.withId(STATE1).add();
                state.withId(STATE2A).add();
                state.withId(STATE2B).onEntryAction(onEntryAction).add();
            }

            @Override
            public void defineTransitions(TransitionConfigurer<TestState, TestEvent, TestObject> transition) {
                transition.from(STATE1).to(STATE2A).onEvent(EVENT1).action(exceptionThrowingAction).add();
                transition.from(STATE1).to(STATE2B).onError(RuntimeException.class).action(transitionAction).add();
            }
        };

        testGF.sendEvent(EVENT1, testObject);

        verify(exceptionThrowingAction).execute(any());
        verify(transitionAction).execute(any());
        verify(onEntryAction).execute(any());
        assertTargetState(STATE2B, testObject);
    }

    @Test
    public void testStateTransition_Transition_To_ErrorState_If_OnEntryAction_Throws_Exception() throws Exception {
        Action exceptionThrowingOnEntryAction = mock(Action.class);
        Action transitionActionA = mock(Action.class);
        Action transitionActionB = mock(Action.class);
        doThrow(RuntimeException.class).when(exceptionThrowingOnEntryAction).execute(any());
        TestStatemachine testGF = new TestStatemachine() {

            @Override
            public void defineStates(StateConfigurer<TestState, TestEvent, TestObject> state) {
                state.withId(STATE1).add();
                state.withId(STATE2A).onEntryAction(exceptionThrowingOnEntryAction).add();
                state.withId(STATE2B).add();
            }

            @Override
            public void defineTransitions(TransitionConfigurer<TestState, TestEvent, TestObject> transition) {
                transition.from(STATE1).to(STATE2A).onEvent(EVENT1).action(transitionActionA).add();
                transition.from(STATE1).to(STATE2B).onError(RuntimeException.class).action(transitionActionB).add();
            }
        };

        testGF.sendEvent(EVENT1, testObject);

        verify(transitionActionA).execute(any());
        verify(exceptionThrowingOnEntryAction).execute(any());
        verify(transitionActionB).execute(any());
        assertTargetState(STATE2B, testObject);
    }

    private void assertTargetState(TestState targetState, TestObject testObject) {
        assertEquals(targetState, testObject.getCurrentState());
    }

    @Test
    public void testStateTransition_NoTransition_If_Action_Throws_Exception_But_Non_Passing_Guard() throws Exception {
        Predicate<Context<TestObject>> guard = mock(Predicate.class);
        Action action = mock(Action.class);
        Action exceptionThrowingAction = mock(Action.class);
        doThrow(RuntimeException.class).when(exceptionThrowingAction).execute(any());
        TestStatemachine testGF = new TestStatemachine() {

            @Override
            public void defineStates(StateConfigurer<TestState, TestEvent, TestObject> state) {
                state.withId(STATE1).add();
                state.withId(STATE2A).add();
                state.withId(STATE2B).add();
            }

            @Override
            public void defineTransitions(TransitionConfigurer<TestState, TestEvent, TestObject> transition) {
                transition.from(STATE1).to(STATE2A).onEvent(EVENT1).action(exceptionThrowingAction).add();
                transition.from(STATE1).to(STATE2B).onError(RuntimeException.class).when(guard).action(action).add();
            }
        };

        testGF.sendEvent(EVENT1, testObject);

        verify(exceptionThrowingAction).execute(any());
        verify(guard).test(any());
        assertNoTransition(testObject);
    }

    @Test
    public void testStateTransition_NoTransition_If_Action_Throws_Exception_But_No_Error_State_Defined() throws Exception {
        Action exceptionThrowingAction = mock(Action.class);
        doThrow(RuntimeException.class).when(exceptionThrowingAction).execute(any());
        TestStatemachine testGF = new TestStatemachine() {

            @Override
            public void defineTransitions(TransitionConfigurer<TestState, TestEvent, TestObject> transition) {
                transition.from(STATE1).to(STATE2).onEvent(EVENT1).action(exceptionThrowingAction).add();
            }
        };

        testGF.sendEvent(EVENT1, testObject);

        verify(exceptionThrowingAction).execute(any());
        assertNoTransition(testObject);
    }

    private void assertNoTransition(TestObject testObject) {
        assertTargetState(STATE1, testObject);
    }

    @Test
    public void testStateTransition_Another_Exception_After_Transition_To_ErrorState() throws Exception {
        Action exceptionThrowingAction = mock(Action.class);
        Action exceptionThrowingAction2 = mock(Action.class);
        doThrow(RuntimeException.class).when(exceptionThrowingAction).execute(any());
        doThrow(RuntimeException.class).when(exceptionThrowingAction2).execute(any());
        TestStatemachine testGF = new TestStatemachine() {

            @Override
            public void defineStates(StateConfigurer<TestState, TestEvent, TestObject> state) {
                state.withId(TestState.STATE1).add();
                state.withId(TestState.STATE2A).add();
                state.withId(TestState.STATE2B).add();
            }

            @Override
            public void defineTransitions(TransitionConfigurer<TestState, TestEvent, TestObject> transition) {
                transition.from(TestState.STATE1).to(TestState.STATE2A).onEvent(TestEvent.EVENT1).action(exceptionThrowingAction).add();
                transition.from(TestState.STATE1).to(TestState.STATE2B).onError(RuntimeException.class).action(exceptionThrowingAction2).add();
            }
        };

        testGF.sendEvent(EVENT1, testObject);

        verify(exceptionThrowingAction).execute(any());
        verify(exceptionThrowingAction2).execute(any());
        verifyNoMoreInteractions(exceptionThrowingAction);
        assertNoTransition(testObject);
    }

    @Test
    public void testStateTransition_Automatic_Transition_If_Not_On_Event() throws Exception {
        Action action = mock(Action.class);
        TestStatemachine testGF = new TestStatemachine() {

            @Override
            public void defineStates(StateConfigurer<TestState, TestEvent, TestObject> state) {
                state.withId(STATE1).add();
                state.withId(STATE2).add();
                state.withId(STATE3).add();
            }

            @Override
            public void defineTransitions(TransitionConfigurer<TestState, TestEvent, TestObject> transition) {
                transition.from(STATE1).to(STATE2).onEvent(EVENT1).add();
                transition.from(STATE2).to(STATE3).action(action).add();
            }
        };

        testGF.sendEvent(EVENT1, testObject);

        verify(action).execute(any());
        assertTargetState(STATE3, testObject);
    }

    @Test
    public void testStateTransition_Automatic_Transition_Transitivly() throws Exception {
        Action action1 = mock(Action.class);
        Action action2 = mock(Action.class);
        TestStatemachine testGF = new TestStatemachine() {

            @Override
            public void defineStates(StateConfigurer<TestState, TestEvent, TestObject> state) {
                state.withId(STATE1).add();
                state.withId(STATE2).add();
                state.withId(STATE3).add();
                state.withId(STATE4).add();
            }

            @Override
            public void defineTransitions(TransitionConfigurer<TestState, TestEvent, TestObject> transition) {
                transition.from(STATE1).to(STATE2).onEvent(EVENT1).add();
                transition.from(STATE2).to(STATE3).action(action1).add();
                transition.from(STATE3).to(STATE4).action(action2).add();
            }
        };

        testGF.sendEvent(EVENT1, testObject);

        verify(action1).execute(any());
        verify(action2).execute(any());
        assertTargetState(STATE4, testObject);
    }

    @Test
    public void testStateTransition_No_Automatic_Transition_If_On_Event() throws Exception {
        Action action = mock(Action.class);
        TestStatemachine testGF = new TestStatemachine() {

            @Override
            public void defineStates(StateConfigurer<TestState, TestEvent, TestObject> state) {
                state.withId(TestState.STATE1).add();
                state.withId(TestState.STATE2).add();
                state.withId(STATE3).add();
            }

            @Override
            public void defineTransitions(TransitionConfigurer<TestState, TestEvent, TestObject> transition) {
                transition.from(TestState.STATE1).to(TestState.STATE2).onEvent(TestEvent.EVENT1).add();
                transition.from(TestState.STATE2).to(STATE3).onEvent(TestEvent.EVENT2).action(action).add();
            }
        };

        testGF.sendEvent(EVENT1, testObject);

        verifyNoMoreInteractions(action);
        assertTargetState(STATE2, testObject);
    }

    @Test
    public void testGenericAction_EVENT_RECEIVED_Called_And_Data_Set() throws Exception {
        Action eventReceivedAction = lifeCycleEventDataValidatingAction(LifecycleEvent.EVENT_RECEIVED);

        Object eventData = mock(Object.class);

        TestStatemachine testGF = new TestStatemachine() {
            @Override
            public void defineGenericActions(GenericActionConfigurer genericActionConfigurer) {
                genericActionConfigurer.on(LifecycleEvent.EVENT_RECEIVED)
                        .execute(eventReceivedAction)
                        .add();
            }
        };
        testGF.sendEvent(EVENT1, testObject, eventData);

        verify(eventReceivedAction).execute(any());
    }

    private Action lifeCycleEventDataValidatingAction(LifecycleEvent event) {
        return spy(new Action<>(context -> assertTrue("Event data not set on context", context.getDataFor(event).isPresent())));
    }

    @Test
    public void testGenericAction_SUCCESSFUL_STATE_CHANGE_Called_And_Data_Set() throws Exception {
        Action successfulStateChangeAction = lifeCycleEventDataValidatingAction(LifecycleEvent.SUCCESSFUL_STATE_CHANGE);
        TestStatemachine testGF = new TestStatemachine() {
            @Override
            public void defineGenericActions(GenericActionConfigurer genericActionConfigurer) {
                genericActionConfigurer.on(LifecycleEvent.SUCCESSFUL_STATE_CHANGE)
                        .execute(successfulStateChangeAction)
                        .add();
            }
        };
        testGF.sendEvent(EVENT1, testObject);

        verify(successfulStateChangeAction).execute(any());
    }

    @Test
    public void testGenericAction_SUCCESSFUL_STATE_CHANGE_Not_Called_If_Reflexive_Transition() throws Exception {
        Action successfulStateChangeAction = mock(Action.class);
        TestStatemachine testGF = new TestStatemachine() {

            @Override
            public void defineTransitions(TransitionConfigurer<TestState, TestEvent, TestObject> transition) {
                transition.from(STATE1).toSelf().onEvent(EVENT1).add();
            }

            @Override
            public void defineGenericActions(GenericActionConfigurer genericActionConfigurer) {
                genericActionConfigurer.on(LifecycleEvent.SUCCESSFUL_STATE_CHANGE)
                        .execute(successfulStateChangeAction)
                        .add();
            }
        };
        testGF.sendEvent(EVENT1, testObject);

        verifyNoMoreInteractions(successfulStateChangeAction);
    }

    @Test
    public void testGenericAction_VALIDATION_ERROR_Called() throws Exception {
        Action validationErrorAction = lifeCycleEventDataValidatingAction(LifecycleEvent.VALIDATION_ERROR);
        Action exitValidator = mock(Action.class);
        doThrow(new ValidationException(() -> "myError")).when(exitValidator).execute(any());
        TestStatemachine testGF = new TestStatemachine() {

            @Override
            public void defineStates(StateConfigurer<TestState, TestEvent, TestObject> state) {
                state.withId(STATE1).exitValidator(exitValidator).add();
                state.withId(STATE2).add();
            }

            @Override
            public void defineGenericActions(GenericActionConfigurer genericActionConfigurer) {
                genericActionConfigurer.on(LifecycleEvent.VALIDATION_ERROR)
                        .execute(validationErrorAction)
                        .add();
            }
        };
        testGF.sendEvent(EVENT1, testObject);

        verify(validationErrorAction).execute(any());
    }

    @Test
    public void testLifecycleEventData_Cleared_After_Technical_Exception_In_ExitValidator() throws Exception {
        Action throwingAction = mock(Action.class);
        doThrow(RuntimeException.class).when(throwingAction).execute(any());
        TestStatemachine testGF = new TestStatemachine() {
            @Override
            public void defineStates(StateConfigurer<TestState, TestEvent, TestObject> state) {
                state.withId(STATE1).exitValidator(throwingAction).add();
                state.withId(STATE2).add();
            }
        };

        testGF.sendEvent(EVENT1, testObject);

        assertTrue(testGF.getContext().getLifecycleEventData().isEmpty());
    }

    @Test
    public void testLifecycleEventData_Cleared_After_Technichal_Exception_In_Action() throws Exception {
        Action throwingAction = mock(Action.class);
        doThrow(RuntimeException.class).when(throwingAction).execute(any());
        TestStatemachine testGF = new TestStatemachine() {
            @Override
            public void defineTransitions(TransitionConfigurer<TestState, TestEvent, TestObject> transition) {
                transition.from(TestState.STATE1).to(TestState.STATE2).onEvent(TestEvent.EVENT1).action(throwingAction).add();
            }
        };

        testGF.sendEvent(EVENT1, testObject);

        assertTrue(testGF.getContext().getLifecycleEventData().isEmpty());
    }

    @Test
    public void testLifecycleEventData_Cleared_After_Technichal_Exception_In_OnEntryAction() throws Exception {
        Action throwingAction = mock(Action.class);
        doThrow(RuntimeException.class).when(throwingAction).execute(any());
        TestStatemachine testGF = new TestStatemachine() {
            @Override
            public void defineStates(StateConfigurer<TestState, TestEvent, TestObject> state) {
                state.withId(STATE1).add();
                state.withId(STATE2).onEntryAction(throwingAction).add();
            }
        };

        testGF.sendEvent(EVENT1, testObject);

        assertTrue(testGF.getContext().getLifecycleEventData().isEmpty());
    }

    /**
     * GFTest is configured to use TestEvent as Event enum. This test checks the behavior if some other event is sent.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSendEvent_Unknown_Event() throws Exception {
        Statemachine statemachineTest = new TestStatemachine();
        statemachineTest.sendEvent(UnkownEvent.TEST, mock(TestObject.class));
    }

    /**
     * GFTest is configured to use TestEvent as Event enum. This test checks the behavior if some other event is sent.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSendEvent_Wrong_Datatype() throws Exception {
        Statemachine statemachineTest = new TestStatemachine();
        statemachineTest.sendEvent(EVENT1, new Object());
    }

    @Test
    public void testTransition_FromAll_ToSelf_Executes_Action() {
        Action action1 = mock(Action.class);
        TestStatemachine testGF = new TestStatemachine() {

            @Override
            public void defineTransitions(TransitionConfigurer<TestState, TestEvent, TestObject> transition) {
                transition.fromAll().toSelf().onEvent(EVENT1).action(action1).add();
            }
        };

        testGF.sendEvent(EVENT1, testObject);
        testObject.setCurrentState(STATE2);
        testGF.sendEvent(EVENT1, testObject);

        verify(action1, times(2)).execute(any());
    }

    /**
     * Tests if toSelf does not change state because it's a reflexive transition
     */
    @Test
    public void testTransition_ToSelf_Does_Not_Change_State() {
        TestStatemachine testGF = new TestStatemachine() {

            @Override
            public void defineTransitions(TransitionConfigurer<TestState, TestEvent, TestObject> transition) {
                transition.from(STATE1).toSelf().onEvent(EVENT1).add();
            }
        };

        testGF.sendEvent(EVENT1, testObject);

        assertTargetState(STATE1, testObject);
    }

    /**
     * Tests whether fromAll().to() works
     */
    @Test
    public void testTransition_FromAll_With_To_Works_In_Every_State() {
        TestStatemachine testGF = new TestStatemachine() {

            @Override
            public void defineStates(StateConfigurer<TestState, TestEvent, TestObject> state) {
                state.withId(STATE1).add();
                state.withId(STATE2).add();
                state.withId(STATE3).add();
            }

            @Override
            public void defineTransitions(TransitionConfigurer<TestState, TestEvent, TestObject> transition) {
                transition.fromAll().to(STATE3).onEvent(EVENT1).add();
            }
        };

        testGF.sendEvent(EVENT1, testObject);

        assertTargetState(STATE3, testObject);

        testObject.setCurrentState(STATE2);

        testGF.sendEvent(EVENT1, testObject);

        assertTargetState(STATE3, testObject);
    }

    /**
     * Tests that createTransaction is called before the action for the lifecycle action EVENT_RECEIVED is called and a second time before
     * exitValidators are called.
     */
    @Test
    public void testCreateTransaction_CalledBeforeEventReceivedAndBeforeExitValidation() throws Exception {
        Action eventReceivedAction = mock(Action.class);
        Action exitValidator = mock(Action.class);
        TestStatemachine testGF = spy(new TestStatemachine() {
            @Override
            public void defineStates(StateConfigurer<TestState, TestEvent, TestObject> state) {
                state.withId(STATE1).exitValidator(exitValidator).add();
                state.withId(STATE2).add();
            }

            @Override
            public void defineGenericActions(GenericActionConfigurer<TestObject> genericActionConfigurer) {
                genericActionConfigurer.on(LifecycleEvent.EVENT_RECEIVED).execute(eventReceivedAction).add();
            }
        });

        testGF.sendEvent(EVENT1, testObject);

        InOrder inOrder = Mockito.inOrder(testGF, eventReceivedAction, exitValidator);
        inOrder.verify(testGF).createTransaction();
        inOrder.verify(eventReceivedAction).execute(any());
        inOrder.verify(testGF).createTransaction();
        inOrder.verify(exitValidator).execute(any());
    }

    @Test
    public void testCloseTransaction_CalledAfterSuccuessfulStateChange() throws Exception {
        Action successfulStateChangeAction = mock(Action.class);
        TestStatemachine testGF = spy(new TestStatemachine() {
            @Override
            public void defineStates(StateConfigurer<TestState, TestEvent, TestObject> state) {
                state.withId(STATE1).add();
                state.withId(STATE2).add();
            }

            @Override
            public void defineGenericActions(GenericActionConfigurer<TestObject> genericActionConfigurer) {
                genericActionConfigurer.on(LifecycleEvent.SUCCESSFUL_STATE_CHANGE).execute(successfulStateChangeAction).add();
            }
        });

        testGF.sendEvent(EVENT1, testObject);

        InOrder inOrder = Mockito.inOrder(testGF, successfulStateChangeAction);
        inOrder.verify(successfulStateChangeAction).execute(any());
        inOrder.verify(testGF, times(2)).closeTransaction(false);
    }

    @Test
    public void testCloseTransaction_CalledAfterExceptionInExitValidator() throws Exception {
        Action throwingExitValidator = mock(Action.class);
        doThrow(new RuntimeException()).when(throwingExitValidator).execute(any());
        TestStatemachine testGF = spy(new TestStatemachine() {
            @Override
            public void defineStates(StateConfigurer<TestState, TestEvent, TestObject> state) {
                state.withId(STATE1).exitValidator(throwingExitValidator).add();
                state.withId(STATE2).add();
            }
        });

        testGF.sendEvent(EVENT1, testObject);

        InOrder inOrder = Mockito.inOrder(testGF, throwingExitValidator);
        inOrder.verify(throwingExitValidator).execute(any());
        inOrder.verify(testGF).closeTransaction(true);
    }

    @Test
    public void testCloseTransaction_CalledAfterExceptionInAction() throws Exception {
        Action throwingAction = mock(Action.class);
        doThrow(new RuntimeException()).when(throwingAction).execute(any());
        TestStatemachine testGF = spy(new TestStatemachine() {
            @Override
            public void defineTransitions(TransitionConfigurer<TestState, TestEvent, TestObject> transition) {
                transition.from(STATE1).to(STATE2).onEvent(EVENT1).action(throwingAction).add();
            }
        });

        testGF.sendEvent(EVENT1, testObject);

        InOrder inOrder = Mockito.inOrder(testGF, throwingAction);
        inOrder.verify(throwingAction).execute(any());
        inOrder.verify(testGF).closeTransaction(true);
    }

    @Test
    public void testCloseTransaction_CalledAfterExceptionInOnEntryAction() throws Exception {
        Action throwingOnEntryAction = mock(Action.class);
        doThrow(new RuntimeException()).when(throwingOnEntryAction).execute(any());
        TestStatemachine testGF = spy(new TestStatemachine() {
            @Override
            public void defineStates(StateConfigurer<TestState, TestEvent, TestObject> state) {
                state.withId(STATE1).add();
                state.withId(STATE2).onEntryAction(throwingOnEntryAction).add();
            }
        });

        testGF.sendEvent(EVENT1, testObject);

        InOrder inOrder = Mockito.inOrder(testGF, throwingOnEntryAction);
        inOrder.verify(throwingOnEntryAction).execute(any());
        inOrder.verify(testGF).closeTransaction(true);
    }

    /**
     * Tests that a TransitionException during a transition will pass the statemachine border as it is a configuration error.
     */
    @Test(expected = TransitionException.class)
    public void testTransitionExceptionDuringTransitionNotCaught() throws Exception {
        Action throwingOnEntryAction = mock(Action.class);
        doThrow(new RuntimeException()).when(throwingOnEntryAction).execute(any());
        TestStatemachine testGF = new TestStatemachine() {
            @Override
            public void defineStates(StateConfigurer<TestState, TestEvent, TestObject> state) {
                super.defineStates(state);
                state.withId(STATE3).add();
            }

            @Override
            public void defineTransitions(TransitionConfigurer<TestState, TestEvent, TestObject> transition) {
                transition.from(STATE1).to(STATE2).onEvent(EVENT1).add();
                transition.from(STATE1).to(STATE3).onEvent(EVENT1).add();
            }
        };

        testGF.sendEvent(EVENT1, testObject);
    }

    /**
     * Tests the case where a state is not in the state map.
     */
    @Test(expected = IllegalStateException.class)
    public void testGetState_ReturnsNull() throws Exception {
        testObject.setCurrentState(STATE3);
        TestStatemachine testGF = new TestStatemachine();

        testGF.proceed(testObject);
    }

    @Test
    public void testProceed_ContinuesAutomaticTransition() throws Exception {
        TestStatemachine testGF = new TestStatemachine() {
            @Override
            public void defineStates(StateConfigurer<TestState, TestEvent, TestObject> state) {
                super.defineStates(state);
                state.withId(STATE3).add();
            }

            @Override
            public void defineTransitions(TransitionConfigurer<TestState, TestEvent, TestObject> transition) {
                transition.from(STATE1).to(STATE2).add();
                transition.from(STATE1).to(STATE3).onEvent(EVENT1).add();
            }
        };

        testGF.proceed(testObject);

        assertTargetState(STATE2, testObject);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetPossibleEventsForState_WrongStateType() throws Exception {
        TestStatemachine testGF = new TestStatemachine();

        testGF.getPossibleEventsForState(new Object());
    }

    @Test
    public void testGetPossibleEventsForState_UnknownState() throws Exception {
        TestStatemachine testGF = new TestStatemachine();
        testObject.setCurrentState(STATE3);

        List<Object> possibleEventsForState = testGF.getPossibleEventsForState(STATE3);

        assertEquals(possibleEventsForState, emptyList());
    }

    @Test
    public void testNoTransition() throws Exception {
        TestStatemachine testGF = new TestStatemachine() {
            @Override
            public void defineStates(StateConfigurer<TestState, TestEvent, TestObject> state) {
                super.defineStates(state);
                state.withId(STATE3).add();
            }

            @Override
            public void defineTransitions(TransitionConfigurer<TestState, TestEvent, TestObject> transition) {
                transition.from(STATE1).to(STATE2).onEvent(EVENT1).add();
                transition.from(STATE1).to(STATE3).onEvent(EVENT2).add();
            }
        };

        List<Object> possibleEventsForState = testGF.getPossibleEventsForState(STATE1);

        assertEquals(possibleEventsForState, Arrays.asList(EVENT1, EVENT2));
    }

    enum UnkownEvent {
        TEST
    }
}
