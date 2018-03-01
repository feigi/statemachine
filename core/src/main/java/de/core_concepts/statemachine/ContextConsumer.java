package de.core_concepts.statemachine;

import java.util.function.Consumer;

/**
 * Created by zieglerch on 18.11.2015.
 * <p>
 * A functional interface used in Action which defines Context<O> as method argument.
 *
 * @param <O> The object type residing on the Context on which the Consumer operates.
 */
@FunctionalInterface
public interface ContextConsumer<O> extends Consumer<Context<O>> {

}
