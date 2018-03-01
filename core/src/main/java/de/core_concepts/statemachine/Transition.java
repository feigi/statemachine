package de.core_concepts.statemachine;


import java.util.Optional;
import java.util.function.Predicate;

/**
 * Created by zieglerch on 24.11.2015.
 * <p>
 * This class represents a transition to a state. It can either listen to an event or an error and it can also define a guard which returns
 * true or false depending on whether the transition can be traversed. Usually a transition also has an action to be executed when being
 * traversed.
 *
 * @param <S> The type defining the states the Statemachine knows of.
 * @param <E> The type defining the events the Statemachine listens to.
 * @param <O> The object type which is put on the context for actions to work with.
 */
public class Transition<S, E, O> {

    private State<S, E, O> toState;
    private Optional<E> event;
    private Optional<Class<? extends RuntimeException>> error;
    private Optional<Action<O>> action;
    private Predicate<Context<O>> guard;

    @java.beans.ConstructorProperties({"toState", "event", "error", "action", "guard"})
    Transition(State<S, E, O> toState, Optional<E> event, Optional<Class<? extends RuntimeException>> error, Optional<Action<O>> action, Predicate<Context<O>> guard) {
        this.toState = toState;
        this.event = event;
        this.error = error;
        this.action = action;
        this.guard = guard;
    }

    boolean isErrorTransition() {
        return !event.isPresent() && error.isPresent();
    }

    boolean isAutomaticTransition() {
        return !event.isPresent() && !error.isPresent();
    }

    public State<S, E, O> getToState() {
        return this.toState;
    }

    public Optional<E> getEvent() {
        return this.event;
    }

    public Optional<Class<? extends RuntimeException>> getError() {
        return this.error;
    }

    public Optional<Action<O>> getAction() {
        return this.action;
    }

    public Predicate<Context<O>> getGuard() {
        return this.guard;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Transition)) return false;
        final Transition other = (Transition) o;
        final Object this$toState = this.getToState();
        final Object other$toState = other.getToState();
        if (this$toState == null ? other$toState != null : !this$toState.equals(other$toState)) return false;
        final Object this$event = this.getEvent();
        final Object other$event = other.getEvent();
        if (this$event == null ? other$event != null : !this$event.equals(other$event)) return false;
        final Object this$error = this.getError();
        final Object other$error = other.getError();
        if (this$error == null ? other$error != null : !this$error.equals(other$error)) return false;
        final Object this$action = this.getAction();
        final Object other$action = other.getAction();
        if (this$action == null ? other$action != null : !this$action.equals(other$action)) return false;
        final Object this$guard = this.getGuard();
        final Object other$guard = other.getGuard();
        if (this$guard == null ? other$guard != null : !this$guard.equals(other$guard)) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $toState = this.getToState();
        result = result * PRIME + ($toState == null ? 43 : $toState.hashCode());
        final Object $event = this.getEvent();
        result = result * PRIME + ($event == null ? 43 : $event.hashCode());
        final Object $error = this.getError();
        result = result * PRIME + ($error == null ? 43 : $error.hashCode());
        final Object $action = this.getAction();
        result = result * PRIME + ($action == null ? 43 : $action.hashCode());
        final Object $guard = this.getGuard();
        result = result * PRIME + ($guard == null ? 43 : $guard.hashCode());
        return result;
    }

    public String toString() {
        return "Transition(toState=" + this.getToState() + ", event=" + this.getEvent() + ", error=" + this.getError() + ", action=" + this.getAction() + ", guard=" + this.getGuard() + ")";
    }
}
