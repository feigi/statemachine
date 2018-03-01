package de.core_concepts.statemachine.spring;

import de.core_concepts.statemachine.Action;
import de.core_concepts.statemachine.Context;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * This class holds all spring beans of type Action and offers a getter to retrieve an action by their bean name.
 * <p>
 * Created by zieglerch on 14.01.2016.
 */
@Component
public class SmBeanRegistry<O> {

    /**
     * A map of all Actions with key = Bean-name
     */
    @Autowired(required = false)
    private Map<String, Action<O>> actions = new HashMap<>();

    /**
     * A map of all Actions with key = Bean-name
     */
    @Autowired(required = false)
    private Map<String, Predicate<Context<O>>> predicates = new HashMap<>();

    /**
     * Returns an Action that has been registered as spring bean by their bean name.
     *
     * @param beanName The bean name of the bean to get.
     * @return The bean with the given bean name.
     * @throws NoSuchBeanDefinitionException In case no bean with the given name was found.
     */
    public Action<O> getAction(String beanName) {
        return getBean(actions, beanName);
    }

    /**
     * Returns a Predicate that has been registered as spring bean by their bean name.
     *
     * @param beanName The bean name of the bean to get.
     * @return The bean with the given bean name.
     * @throws NoSuchBeanDefinitionException In case no bean with the given name was found.
     */
    public Predicate<Context<O>> getPredicate(String beanName) {
        return getBean(predicates, beanName);
    }

    private <T> T getBean(Map<String, T> beanMap, String beanName) {
        T bean = beanMap.get(beanName);
        if (bean == null) {
            throw new NoSuchBeanDefinitionException(beanName);
        }
        return bean;
    }
}
