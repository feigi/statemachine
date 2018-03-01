package de.core_concepts.statemachine.spring.testing;

import de.core_concepts.statemachine.Action;
import de.core_concepts.statemachine.Error;
import de.core_concepts.statemachine.ValidationException;
import de.core_concepts.statemachine.spring.SmBeanRegistry;
import de.core_concepts.statemachine.spring.StatemachineBean;
import org.junit.Before;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.exceptions.Reporter;
import org.mockito.internal.invocation.InvocationsFinder;
import org.mockito.internal.stubbing.defaultanswers.ReturnsSmartNulls;
import org.mockito.internal.util.MockUtil;
import org.mockito.internal.verification.api.VerificationData;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.verification.VerificationMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Super class for {@link StatemachineBean} tests.
 *
 * @param <T> The type of the statemachine to be tested, i.e. the test subject.
 * @param <O> The type of the object the statemachine operates on.
 * @param <U> The type of the predicate class used in the test subject.
 */
public abstract class StatemachineTest<T extends StatemachineBean, O, U> {

    protected T testee;
    protected U predicate;
    protected SmMocker smMocker;
    protected SmTestRunner smTestRunner;
    protected O testObject;
    private Logger LOG = LoggerFactory.getLogger(StatemachineTest.class);
    private Class<T> smClass;
    private SmBeanRegistry bf;
    private Class<U> predicateClass;

    /**
     * All action mocks requested by the statemachine during the init call.
     */
    private Set<Action> allMockedActions;

    /**
     * All action mocks that explicitly mocked using SmMocker#mockAction. These are the actions that need to be verified in the test.
     */
    private List<Action> explicitelyMockedActions;

    /**
     * All action mocks which were explicitely mocked using {@link SmMocker#mockParamAction(String, Object...)}. These are tested during the
     * initialization phase of the state machine because we need to make sure the arguments passed in are correct.
     */
    private List<ParameterizedAction> parameterizedActions;

    public StatemachineTest(Class<T> smClass, Class<U> predicateClass) {
        this.smClass = smClass;
        this.predicateClass = predicateClass;
    }

    @Before
    public void setUp() throws Exception {
        allMockedActions = new HashSet<>();
        explicitelyMockedActions = new ArrayList<>();
        parameterizedActions = new ArrayList<>();
        smMocker = new SmMocker();
        smTestRunner = new SmTestRunner();
        O testObject = newTestObject();
        this.testObject = spy(testObject);
    }

    /**
     * Override this if you need a subclass of TestObject for your tests.
     *
     * @return A instance of type TestObject.
     */
    protected abstract O newTestObject();

    /*protected void testAllStatesDefinedInParameters(Object[][] parameters, Enum[] enumValues) throws Exception {
        smMocker.init();

        List<Enum> expectedStates = Stream.of(parameters)
                .map(element -> (Enum) element[0])
                .collect(toList());

        final List<Enum> undefinedStates = Stream.of(enumValues)
                .map(Enum::name)
                .map(testee::getStateFromString).filter(Objects::nonNull) // This filters out all states that are not used by the statemachine
                .filter(anEnum -> !expectedStates.contains(anEnum)) // This removes all states that were defined in the test
                .collect(toList());

        assertThat(format("States %s not defined in parameters. For the given test, check if the event is supposed to be possible in the those "
                + "states and add them to the parameters array.", undefinedStates), undefinedStates, hasSize(0));
    }*/

    /**
     * Returns all Action mocks requested by the statemachine. Optionally filtered.
     *
     * @param without All actions that
     */
    protected Object[] allActionMocksExcept(String... without) {
        return allMockedActions.stream()
                .filter(actionMock -> Stream.of(without)
                        .noneMatch(name -> name.equals(mockNameFrom(actionMock))))
                .collect(toList()).toArray();
    }

    /**
     * Verifies that all actions mocked by using {@link SmMocker#mockAction} and {@link SmMocker#mockValidator} are executed in the exact same testObject they
     * were mocked.
     */
    protected void verifyActionsInTestObject() {
        if (explicitelyMockedActions.size() > 0) {
            InOrder inTestObject = inOrder(explicitelyMockedActions.toArray());
            explicitelyMockedActions.forEach(action -> inTestObject.verify(action).execute(Matchers.any()));
        }
    }

    /**
     * Verifies that there are no more interactions on any action mock with the exception of actions with bean names returned by
     * {@link #excludeFromVerifyNoMoreInteractions()}. Additionally calls {@link #verifyActionsInTestObject()}.
     */
    protected void verifyNoMoreInteractions() {
        Mockito.verifyNoMoreInteractions(allActionMocksExcept(excludeFromVerifyNoMoreInteractions()));
        verifyNoMoreInteractionsOnObject();
    }

    /**
     * Subclass this in order to verify there were no more interactions with certain methods of the object your statemachine is working on.
     * Use the {@link VerificationMode} {@link NoMoreInvocationsOnMethod} for this purpose.
     * Example: verify(yourTestObjectMock, new NoMoreInvocationsOnMethod()).theMethodYouWantToCheck(any());
     */
    protected void verifyNoMoreInteractionsOnObject() {
    }

    /**
     * @return
     */
    protected String[] excludeFromVerifyNoMoreInteractions() {
        return new String[0];
    }

    private String mockNameFrom(Object mock) {
        return new MockUtil().getMockName(mock).toString();
    }

    /**
     * A mockito VerificationMode that checks on the method level whether there are unverified invocations. For example, if you don't verify
     * a call to mockA#methodB but it was invoked, the verification fails.
     */
    private static class NoMoreInvocationsOnMethod implements VerificationMode {

        @Override
        public void verify(VerificationData data) {
            new InvocationsFinder().findInvocations(data.getAllInvocations(), data.getWanted()).stream()
                    .filter(invocation -> !invocation.isVerified())
                    .findAny()
                    .ifPresent(invocation -> new Reporter().noMoreInteractionsWanted(invocation, (List) data.getAllInvocations()));
        }
    }

    /**
     * This class makes mocking for statemachine tests more readable. Use it to mock actions (exitValidator, onEntryAction, action) and
     * predicates to simulate certain routes through the statemachine. If not specifically mocked, all calls to SmBeanFactory#getAction will
     * return a mocked Action, whereas all calls to a subclass of CommonPredicate will call the actual code. If this code calls beanFactory
     * for a bean, we return (context) -> true by default. Use SmMocker#mockPredicate to override this.
     */
    protected class SmMocker {

        public SmMocker() throws IllegalAccessException, InstantiationException {
            bf = mock(SmBeanRegistry.class);

            // If not specified otherwise always call the real method
            predicate = spy(predicateClass.newInstance());

            SmBeanRegistry beanFactory = mock(SmBeanRegistry.class);
            // If a predicate is implemented as a bean, we always return context -> true! Override with 'withPredicate' if needed.
            when(beanFactory.getPredicate(Matchers.any())).thenReturn(testObjectContext -> true);
            ReflectionTestUtils.setField(predicate, "registry", beanFactory);
            mockCommon();
        }

        /**
         * Default mocks. For all beanNames that are not specifically mocked we return 'empty' instances, i.e. Actions that do nothing and
         * Predicates that return true.
         */
        private void mockCommon() {
            when(bf.getAction(Matchers.any())).then(invocation -> createActionMock(invocation.getArgumentAt(0, String.class)));
        }

        private Action<O> createActionMock(String name) {
            Action<O> mock = mock(Action.class, name);
            allMockedActions.add(mock);
            return mock;
        }

        /**
         * Mocks a method invocation given the methodName as String.
         *
         * @param mock        The mock.
         * @param returnValue The value which should be returned by the method.
         * @param methodName  A method on the mock given as String.
         * @param args        The arguments passed to the method.
         */
        private void mockByString(Object mock, Object returnValue, String methodName, Object... args) {
            when(ReflectionTestUtils.invokeMethod(mock, methodName, args)).thenReturn(returnValue);
        }

        /**
         * Use this method to mock actions. This method creates the mock itself.
         *
         * @param beanName The beanName of the Action to be mocked.
         * @return Returns itself.
         */
        public SmMocker mockAction(String beanName) {
            doMockAction(mock(Action.class, beanName));
            return this;
        }

        /**
         * Use this method to mock actions.
         *
         * @param actionMock The mock object, i.e. bean instance. IMPORTANT: The mocked action must have the action's beanName set as name.
         *                   Create the mock like thisF: mock(Action.class, "SOME_ACTION")
         * @return Returns itself.
         */
        public SmMocker mockAction(Action actionMock) {
            doMockAction(actionMock);
            return this;
        }

        /**
         * Use this to mock actions returned by {@link ParameterizableActions}.
         *
         * @param methodName The method in {@link ParameterizableActions} which should be mocked.
         * @param args       The arguments which are supposed to be passed to the method above. These are validated
         *                   after initialization of the statemachine.
         * @return Returns itself.
         */
/*        public SmMocker mockParamAction(String methodName, Object... args) {
            parameterizedActions.add(new ParameterizedAction(methodName, args));
            final Action mock = mock(Action.class, methodName);
            addToMockedActions(mock);
            mockByString(parameterizableActions, mock, methodName, args);
            return this;
        }*/

        /**
         * Use this method to mock actions that are used as exitValidator and should throw a ValidationException.
         *
         * @param beanName The beanName of the Validator to be mocked.
         * @param error    The error that should be passed to the constructor of ValidationException.
         * @return Returns itself.
         */
        public SmMocker mockValidator(String beanName, Error error) {
            mockValidator(mock(Action.class, beanName), error);
            return this;
        }

        /**
         * Use this method to mock actions that are used as exitValidator and should throw a ValidationException.
         *
         * @param validator The mock object, i.e. bean instance. IMPORTANT: The mocked action must have the action's beanName set as name.
         *                  Create the mock like this: mock(Action.class, "SOME_ACTION").
         * @param error     The error that should be passed to the constructor of ValidationException.
         * @return Returns itself.
         */
        public SmMocker mockValidator(Action validator, Error error) {
            doMockAction(validator);
            if (error != null) {
                doThrow(new ValidationException(error)).when(validator).execute(Matchers.any());
            }
            return this;
        }

        private void addToMockedActions(Action actionMock) {
            allMockedActions.add(actionMock);
            explicitelyMockedActions.add(actionMock);
        }

        private Action doMockAction(Action actionMock) {
            addToMockedActions(actionMock);
            when(bf.getAction(new MockUtil().getMockName(actionMock).toString())).thenReturn(actionMock);
            return actionMock;
        }

        /**
         * Use this method to mock predicates used in the 'when' clause of a state transition.
         *
         * @param methodName The name of the method to mock.
         * @param result     Boolean that should be return by the mocked predicate.
         * @return Returns itself.
         */
        public SmMocker mockPredicate(String methodName, boolean result) {
            mockByString(predicate, (Predicate) context -> result, methodName);
            return this;
        }

        /**
         * Complete mocking by initializing the statemachine.
         */
        public void init() throws Exception {
            testee = smClass.newInstance();
            try {
                ReflectionTestUtils.setField(testee, "predicate", predicate);
            } catch (IllegalArgumentException e) {
                LOG.debug("Could not inject field predicate because it does not exist. Ignore this if you don't need a predicate in your statemachine");
            }
            ReflectionTestUtils.setField(testee, "registry", bf);
            ReflectionTestUtils.setField(testee, "newAction", parameterizedActions);
            testee.init();
        }

        /**
         * An Mockito Answer that returns a new mock of type Action for every invocation that returns Action and uses
         * ReturnsSmartNulls for all other cases.
         */
        private class ActionMockAnswer implements Answer {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                if (invocation.getMethod().getReturnType().equals(Action.class)) {
                    final Action mock = mock(Action.class);
                    allMockedActions.add(mock);
                    return mock;
                }
                return new ReturnsSmartNulls().answer(invocation);
            }
        }
    }

    /**
     * This class is another abstraction to SmMocker and covers common test setup, running and assertions. It uses the SmMocker. Normally it
     * is not necessary to use SmMocker directly.
     */
    protected class SmTestRunner {

        private Enum fromState;
        private boolean isPossible = true;
        private Enum event;
        private Enum targetState;
        private boolean strictVerification = true;
        private Runnable customVerification = () -> {
        };

        private void testInit() throws Exception {
            // Given
            smMocker.init();

//            parameterizedActions.forEach(parameterizedAction -> verifyByString(parameterizedAction.getMethodName(),
//                    parameterizedAction.getArguments()));
        }

        /**
         * Verifies a method invocation with the methodName given as String and the arguments given by an Object[].
         *
         * @param methodName The name of the method to verify.
         * @param args       The arguments the method should have been called with.
         */
        private void verifyByString(String methodName, Object... args) {
            final List<? extends Class<?>> paramTypesList = Stream.of(args).map(Object::getClass).collect(toList());
            final Class<?>[] paramTypes = paramTypesList.toArray(new Class<?>[paramTypesList.size()]);
//            final Method method = ReflectionUtils.findMethod(ParameterizableActions.class, methodName, paramTypes);
//            ReflectionUtils.invokeMethod(method, Mockito.verify(parameterizableActions), args);
        }

        /**
         * Generic test method that tests sending an event to a given state and, if the event is possible at this state, asserts the actions
         * which should be called and the target state.
         *
         * @param fromState   The state the testObject is in. Mandatory.
         * @param isPossible  Whether the event should be possible in the given state.
         * @param event       The event to be sent. Mandatory.
         * @param targetState The expected state at the end of the transition. Optional. If null, it won't be asserted.
         */
        private void testTransition(Enum fromState, boolean isPossible, Enum event, Enum targetState) throws Exception {
            // Given
            setFromState(fromState);

            // When
            testee.sendEvent(event, testObject);

            // Then
            if (isPossible) {
                verifyActionsInTestObject();
            }

            customVerification.run();

            if (strictVerification || !isPossible) {
                verifyNoMoreInteractions();
            }

            if (targetState != null) {
                assertSmState(targetState);
            }
        }

        private void assertSmState(Object targetState) {
            assertThat(testee.getCurrentState(testObject), is(targetState));
        }

        /**
         * Sets the statemachine status a test should start with.
         */
        private void setFromState(Object state) {
            ReflectionTestUtils.invokeMethod(testee, "setCurrentState", testObject, state);
        }

        /**
         * The state the testObject is in.
         */
        public SmTestRunner fromState(Enum fromState) {
            this.fromState = fromState;
            return this;
        }

        /**
         * Whether or not a configured transition should be possible. Use this for cases where you want to check that an event is NOT handled
         * by a state.
         */
        public SmTestRunner isPossible(boolean isPossible) {
            this.isPossible = isPossible;
            return this;
        }

        /**
         * The event that should be sent to the statemachine.
         */
        public SmTestRunner onEvent(Enum event) {
            this.event = event;
            return this;
        }

        /**
         * An action which should be invoked during the test. TestObject is relevant!
         */
        public SmTestRunner withAction(String actionBeanName) {
            smMocker.mockAction(actionBeanName);
            return this;
        }

        /**
         * An action which should be invoked during the test. TestObject is relevant!
         */
        public SmTestRunner withAction(Action mockedAction) {
            smMocker.mockAction(mockedAction);
            return this;
        }

/*        public SmTestRunner withParamAction(String methodName, Object... params) {
            smMocker.mockParamAction(methodName, params);
            return this;
        }*/

        /**
         * A predicate which is passed during the test. Configure its return value with this method.
         */
        public SmTestRunner withPredicate(String methodName, boolean result) {
            smMocker.mockPredicate(methodName, result);
            return this;
        }

        /**
         * A validator that should be invoked during the test. Use this if the validator should report an error.
         */
        public SmTestRunner withValidator(String beanName, Error error) {
            smMocker.mockValidator(beanName, error);
            return this;
        }

        /**
         * The expected target state of the test.
         */
        public SmTestRunner withTargetState(Enum targetState) {
            this.targetState = targetState;
            return this;
        }

        /**
         * Default is true. If true, no other actions than the given ones must be executed (i.e. verifyNoMoreInteractions is called). Set this
         * to false, if you only want to validate that your actions where called, but you don't care if others were called as well.
         */
        public SmTestRunner strictVerification(boolean strictVerification) {
            this.strictVerification = strictVerification;
            return this;
        }

        /**
         * Use this if you need to do custom verification before the actions and testObject/anschluss are verified for "no more interactions".
         */
        public SmTestRunner verify(Runnable customVerification) {
            this.customVerification = customVerification;
            return this;
        }

        public void run() throws Exception {
            testInit();
            testTransition(fromState, isPossible, event, targetState);
        }
    }

    private class ParameterizedAction {

        private final String methodName;
        private final Object[] arguments;

        public ParameterizedAction(String methodName, Object[] arguments) {
            this.methodName = methodName;
            this.arguments = arguments;
        }

        public String getMethodName() {
            return methodName;
        }

        public Object[] getArguments() {
            return arguments;
        }
    }
}
