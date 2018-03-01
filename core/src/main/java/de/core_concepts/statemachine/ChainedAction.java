package de.core_concepts.statemachine;


import org.apache.commons.lang.Validate;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by zieglerch on 30.11.2015.
 * <p>
 * A subclass of Action, that wraps several actions and executes them sequentially.
 *
 * @param <O> The object type residing on the Context on which the Actions operate.
 */
public class ChainedAction<O> extends Action<O> {

    private List<Action<O>> actionChain = new LinkedList<>();

    private ChainedAction(Action<O> firstAction) {
        super();
        actionChain.add(firstAction);
    }

    /**
     * @param workPackage The first action to be executed. The ContextConsumer is being wrapped into an Action. Mandatory
     */
    public static <O> ChainedAction<O> firstDo(ContextConsumer<O> workPackage) {
        Validate.notNull(workPackage);
        Action firstAction = new Action<>(workPackage);
        return new ChainedAction<>(firstAction);
    }

    /**
     * @param firstAction The first action to be executed. Also the head of the linked list. Mandatory
     */
    public static <O> ChainedAction<O> firstDo(Action<O> firstAction) {
        Validate.notNull(firstAction);
        return new ChainedAction<>(firstAction);
    }

    public static <O> ChainedAction<O> of(ContextConsumer<O> workPackage) {
        return ChainedAction.firstDo(workPackage);
    }

    /**
     * @param nextWorkPackage Another action to be executed. The ContextConsumer is being wrapped into an Action.
     */
    public ChainedAction<O> thenDo(ContextConsumer<O> nextWorkPackage) {
        Action<O> nextAction = new Action<>(nextWorkPackage);
        return thenDo(nextAction);
    }

    /**
     * @param nextAction Another action to be executed.
     */
    public ChainedAction<O> thenDo(Action<O> nextAction) {
        actionChain.add(nextAction);
        return this;
    }

    @Override
    public void execute(Context<O> context) {
        actionChain.forEach(action -> action.execute(context));
    }
}
