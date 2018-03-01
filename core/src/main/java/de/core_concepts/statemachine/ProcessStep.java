package de.core_concepts.statemachine;

/**
 * Created by zieglerch on 18.11.2015.
 */
@FunctionalInterface
public interface ProcessStep {

    void execute(Context context);
}
