package de.core_concepts.statemachine;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * Tests the GenericActionConfigurer class
 *
 * Created by zieglerch on 18.01.2016.
 */
public class GenericActionConfigurerTest {

  private Map<LifecycleEvent<?>, Action> genericActions = new HashMap<>();
  private GenericActionConfigurer configurer;

  @Before
  public void setUp() throws Exception {
    genericActions.clear();
    configurer = new GenericActionConfigurer(genericActions);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAction_Null() throws Exception {
    configurer.on(LifecycleEvent.SUCCESSFUL_STATE_CHANGE).add();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testHook_Null() throws Exception {
    configurer.execute(mock(Action.class)).add();
  }

  @Test
  public void testAction_Is_InMap() throws Exception {
    Action action = mock(Action.class);
    configurer.execute(action).on(LifecycleEvent.SUCCESSFUL_STATE_CHANGE).add();
    assertNotNull(getGenericActionFor(LifecycleEvent.SUCCESSFUL_STATE_CHANGE));
  }

  private Action getGenericActionFor(LifecycleEvent<?> event) {
    return genericActions.get(event);
  }

  @Test
  public void testFirst_Object_Has_Correct_Values() throws Exception {
    Action action = mock(Action.class);
    configurer
        .on(LifecycleEvent.VALIDATION_ERROR)
        .execute(action)
        .add();

    assertEquals(1, genericActions.size());
    assertEquals(action, getGenericActionFor(LifecycleEvent.VALIDATION_ERROR));
  }

  @Test
  public void testSecond_Object_Has_Correct_Values() throws Exception {
    Action actionStateChange = mock(Action.class);
    Action actionValidationError = mock(Action.class);
    configurer
        .on(LifecycleEvent.VALIDATION_ERROR)
        .execute(actionValidationError)
        .add();

    configurer
        .on(LifecycleEvent.SUCCESSFUL_STATE_CHANGE)
        .execute(actionStateChange)
        .add();

    assertEquals(2, genericActions.size());
    assertEquals(actionValidationError, getGenericActionFor(LifecycleEvent.VALIDATION_ERROR));
    assertEquals(actionStateChange, getGenericActionFor(LifecycleEvent.SUCCESSFUL_STATE_CHANGE));
  }

  @Test
  public void testDefining_Action_Twice_Overwrites() throws Exception {
    Action action = mock(Action.class);
    Action overwritingAction = mock(Action.class);
    configurer
        .on(LifecycleEvent.VALIDATION_ERROR)
        .execute(action)
        .add();

    configurer
        .on(LifecycleEvent.VALIDATION_ERROR)
        .execute(overwritingAction)
        .add();

    assertEquals(1, genericActions.size());
    assertEquals(overwritingAction, getGenericActionFor(LifecycleEvent.VALIDATION_ERROR));
  }
}