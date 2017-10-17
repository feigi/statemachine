[![Build status][travis-image]][travis-url]

# Documentation

Following I will explain the concepts of this statemachine implementation and how you create your own statemachine. 
 To get started you first have to subclass AbstractStatemachine (if you are using Spring use StatemachineBean. But more 
 on that below). In order to do that, first you have to specify the two generic types S and E. S is the type that
 represents a state and E is the type that represents an event. You can use any Object here, however, the equals method 
 of two arbitrary states or events must return false if they do not represent the same state / event. Furthermore, you have to 
 implement its abstract methods:
* `void defineStates(StateConfigurer<S, E> state)`: Used to make states known to the statemachine and configure them.
* `void defineTransitions(TransitionConfigurer<S, E> transition)`: Used to configure transitions between states.
* `Enum<?> getStateFromString(String stateName)`: Used to translate a string into a state object.
* `Enum<?> getEventFromString(String eventName)`: Used to translate a string into an event object.

```Java
public class Sm1 extends AbstractStatemachine<Sm1.Sm1State, Sm1.Sm1Event, SomeObject> {

  public enum Sm1State {
    START, STATEN1, STATE2, END
  }

  public enum Sm1Event {
    EVENT1, EVENT2
  }

  public Sm1() {
    super(Sm1State.class, Sm1Event.class, SomeObject.class);
  }

  @Override
  public void defineStates(StateConfigurer<Sm1States, Sm1Events> state) {...}

  @Override
  public void defineTransitions(TransitionConfigurer<Sm1State, Sm1Event> transition) {...}
 
  @Override
  public Enum<Sm1State> getStateFromString(String stateName) {...}

  @Override
  public Enum<Sm1Event> getEventFromString(String eventName) {...}
}
```
 
Configuring states and transitions is done with a fluid API. Let's see how states are configured first.
In the simplest case, a state only has an id. That is the state object itself.

```Java
state.withId(INITIAL).add();
```

Additionally it is possible to add a so-called `onEntryAction`. This is logic that is called when a transition comes to 
an end by entering a new state. It is irrelevant what the previous state was. This action is called no matter were we 
come from. One important note: The state has to actually change in order for this action to be called. Reflexive 
transitions don't cause it to be called.

```Java
 state.withId(SOME_STATE)
    .onEntryAction(...)
    .add();
```

Lastly a state can have an `exitValidator. That is a special kind of action that, by contract, has to throw a 
 ValidationException with an Error. Use this if you, for instance, want to break out of a normal flow in case of an error. 

```Java
state.withId(SOME_STATE)
    .exitValidator(Action.of(context -> {
      System.out.println("Executing exit validation on SOME_STATE");
      if (!ok) {
		throw new ValidationException(...)
	  };
    })).add();
```

## Configuration of Transitions

The configuration of Transitions is a bit more complex. A transition must have at least one `from` state as well as one 
`to` state. Usually a transition is triggered by an event. In order to define the event that triggers a particular 
transition use onEvent. If you don't add an event it is a so-called automatic transitions. That means it will trigger
immediately after the from state was reached. If logic should be executed during a transition, add the action like
below:

```Java
 transition.from(INITIAL)
    .to(SOME_STATE)
    .onEvent(AN_EVENT)
    .action(...)
    .add();
```

### Syntactic Sugar - fromAll und toSelf

Cases exist in which you want to define the same transitions for all (or most) states. For this you can use the method
`TransitionConfigurer#fromAll`. The given transition will be configured for all states added to the `StateConfigurer`.
If you want to exclude some states, use `TransitionConfigurer#excluding` which accepts a vararg.

Lastly there is a shorthand for defining reflexive transitions, that is, transitions, that don't actually change the 
state but can be used to trigger some logic. So, instead of `from(X).to(X)` you can simply write `from(X).toSelf()`. 

Examples:

```Java
transition.fromAll()
    .excluding(INITIAL, SOME_STATE1, SOME_STATE2)
    .to(TO_STATE)
    .action(...)
    .add();

transition.fromAll()
    .toSelf()
    .action(...)
    .add();
```

Often, you will want to determine the from-state dynamically at runtime. There are two ways to achieve that: 
1.	using the `when` guard, or, 
2.	using the `onError` trigger.

### To-state Determined by Guard

The `when` guard takes a `Predicate` which gets the current transitions `Context` and returns true if the transition 
can be traversed, or false if not. Typically it is used in conjunction with the `exitValidation` of the from-state. 
The `exitValidation` validates something and throws a `ValidationException` if the validation failed. Using the `when` 
guard you can then fork to different to states based on the outcome of the validation.

Example:

```Java

// Traversed when exitValidation of SOME_STATE does not throw a ValidationException
transition.from(SOME_STATE)
    .to(TO_STATE1)
    .action(...)
    .when(context -> !context.hasValidationError()) // <- Guard
    .add();

// Traversed when exitValidation of SOME_STATE throws a ValidationException of type SOME_ERROR1
transition.from(SOME_STATE)
    .to(TO_STATE2)
    .action(...)
    .when(context -> context.getValidationError().map(e -> SOME_ERROR1.equals(e))) // <- Guard
    .add();

// Traversed when exitValidation of SOME_STATE throws a ValidationException of type SOME_ERROR2
transition.from(SOME_STATE)
    .to(TO_STATE3)
    .action(...)
    .when(context -> context.getValidationError().map(e -> SOME_ERROR2.equals(e))) // <- Guard
    .add();
```

Note, that `Context#getValidationError` is a shorthand for `Context#getDataFor(LifecycleEvent.VALIDATION_ERROR)`, but 
more on that later.

An important note regarding `when` guards is, you have to make sure, that at any time, there can only be one guard that
returns true for a specific `from` state. That includes transitions without a `when` statement, because they implicitly
have a guard returning true. 


### To-State Determined by Exception

Of course, during a transition exceptions can occur and you might want to react to that by chosing a different to-state.
This is possible using the trigger `onError`. Consider the following example:

```Java
transition.from(SOME_STATE)
    .to(NORMAL_TO_STATE)
	.action(...)
    .add();

transition.from(SOME_STATE)
    .to(EXCEPTION_TO_STATE)
    .action(...)
    .onError(RuntimeException.class)
    .add();
```

The statemachine leaves the SOME_STATE trying to go to NORMAL_TO_STATE. During that transition it executes different 
actions (exitValidation, transition action, onEntryAction). If during any of those actions an unhandled exception occurs
it searches for transitions with `onError` trigger and matches the class. If the thrown exception if is an `instanceof`
the `onError` exception, it traverses this transition instead. 

Important:
*	The `exitValidation` is only executed once!
*	`when` guards always have to catch their exceptions themselves and return true or false!
 
## Configuration of Generic Actions

Generic Actions are actions that are bound to a specific step / event during the lifecycle of a transition. 
Examples for such lifecycle events are SUCCESSFUL_STATE_CHANGE and VALIDATION_ERROR (list down below). This means, you
can for instane define actions that are executed whenever a state is changed successfully, or when a ValidationException 
is thrown. For this, you have to implement the optional method `AbstractStatemachine#defineGenericActions()`. 

Example:

```Java
protected void defineGenericActions(GenericActionConfigurer genericActionConfigurer) {
  genericActionConfigurer.on(LifecycleEvent.VALIDATION_ERROR)
      .execute(context -> System.out.println("Do stuff that has to be done")).add();
}
```

For such `LifecycleEvents` the statemachine can set data on the `Context` (e.g. an error object for the `LifecycleEvent` 
VALIDATION_ERROR). The date is set on a map with the `LifecycleEvent` object as key and can be retrieved from the 
`Context` as follows:

```Java
private void doExecute(Context context) {
  context.getDataFor(LifecycleEvent.VALIDATION_ERROR)
      .ifPresent(error -> LOG.error(error));
}
```

Following we list and describe all existing `LifecycleEvents` in the order of their occurrence.

Event | Type | Description
EVENT_RECEIVED | Object | `Statemachine#sendEvent` was called. The objekt passed to `sendEvent` is saved on the `Context`.
UNKNOWN_EVENT | Object | `Statemachine#sendEvent` was called but the event passed was not known. The unknown event is set on the `Context`.
VALIDATION_ERROR | Error | An `exitValidation` threw a `ValidationException`. The error contained in this exception is saved on the `Context`.
SUCCESSFUL_STATE_CHANGE | Void | A state transition is complete. Only called if the state actually changes.

Also note the confinience method `Context#getMostRecentData`, which returns the data from the last `LifecycleEvent` that
actually put data on the context. For instance, if both EVENT_RECEIVED and VALIDATION_ERROR put data on the context, 
this method will return the data put on the context by VALIDATION_ERROR, as it comes after EVENT_RECEIVED in the lifecycle. 
 
## Actions, ChainedActions and ExitValidators

Essentially an `Action` is a wrapper for a `ContextConsumer`, which itself is a sub-interface of the functional interfaces 
`Consumer` for the generic type `Context`. This enables the usage of lambda expressions. A simple action can be created
in different fashions:
*	For very simple cases the factory method `Action#of()` can we used with a lambda expression or a function pointer.
*	In most cases you will want to write a dedicated and reusable sublass. For this you simply subclass `Action` and call
    `setWorkPackage` in the constructor passing in a lambda expression or a function pointer.

```Java
// Using the factory method
Action myAction = Action.of(context -> System.out.println("Doing some stuff"));
Action myAction2 = Action.of(this::methodThatDoesStuff);
 
// As subclass
public class MyAction extends Action {

  public MyAction() {
    super.setWorkPackage(this::doExecute);
  }

  private void doExecute(Context context) {
	// Do some stuff
  }
}
```

If you want several actions to be executed in a row, you have to use the class `ChainedAction`, to link actions together.
A `ChainedAction` saves all the actions in a list and executes them in the order they were added:

```Java
Action myAction = ChainedAction.firstDo(new MyAction())
                               .thenDo(context -> System.out.println("Do stuff that has to be done"));
```

## Using Spring with StatemachineBean

TBD

### The SmBeanFactory

### Transaction Support

### Testing


[travis-image]: https://travis-ci.org/feigi/statemachine.svg?branch=master
[travis-url]: https://travis-ci.org/feigi/statemachine
[codeclimate-image]: https://codeclimate.com/github/feigi/statemachine/badges/gpa.svg
[codeclimate-url]: https://codeclimate.com/github/feigi/statemachine
[testcoverage-image]: https://codeclimate.com/github/feigi/statemachine/badges/coverage.svg
[testcoverage-url]: https://codeclimate.com/github/feigi/statemachine