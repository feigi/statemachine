package de.core_concepts.statemachine;


import java.beans.ConstructorProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by zieglerch on 24.11.2015.
 * <p>
 * This class represents a state with all its (configured) attributes in the statemachine.
 *
 * @param <S> The type defining the states the Statemachine knows of.
 * @param <E> The type defining the events the Statemachine listens to.
 * @param <O> The object type which is put on the context for actions to work with.
 */
public class State<S, E, O> {

    private final S id;
    private final Optional<Action<O>> onEntryAction;
    private final Optional<Action<O>> exitValidator;
    private final List<Transition<S, E, O>> transitions = new ArrayList<>();

    @ConstructorProperties({"id", "onEntryAction", "exitValidator"})
    State(S id, Optional<Action<O>> onEntryAction, Optional<Action<O>> exitValidator) {
        this.id = id;
        this.onEntryAction = onEntryAction;
        this.exitValidator = exitValidator;
    }

    State<S, E, O> addTransition(Transition<S, E, O> transition) {
        transitions.add(transition);
        return this;
    }

    public S getId() {
        return this.id;
    }

    public Optional<Action<O>> getOnEntryAction() {
        return this.onEntryAction;
    }

    public Optional<Action<O>> getExitValidator() {
        return this.exitValidator;
    }

    public List<Transition<S, E, O>> getTransitions() {
        return this.transitions;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof State)) return false;
        final State other = (State) o;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final Object this$onEntryAction = this.getOnEntryAction();
        final Object other$onEntryAction = other.getOnEntryAction();
        if (this$onEntryAction == null ? other$onEntryAction != null : !this$onEntryAction.equals(other$onEntryAction))
            return false;
        final Object this$exitValidator = this.getExitValidator();
        final Object other$exitValidator = other.getExitValidator();
        if (this$exitValidator == null ? other$exitValidator != null : !this$exitValidator.equals(other$exitValidator))
            return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final Object $onEntryAction = this.getOnEntryAction();
        result = result * PRIME + ($onEntryAction == null ? 43 : $onEntryAction.hashCode());
        final Object $exitValidator = this.getExitValidator();
        result = result * PRIME + ($exitValidator == null ? 43 : $exitValidator.hashCode());
        return result;
    }

    public String toString() {
        return "State(id=" + this.getId() + ", onEntryAction=" + this.getOnEntryAction() + ", exitValidator=" + this.getExitValidator() + ")";
    }
}
