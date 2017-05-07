package de.core_concepts.statemachine;

import org.junit.Before;
import org.junit.Test;
import org.omg.CORBA.Object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

/**
 * Tests the Context class
 *
 * Created by zieglerch on 11.02.2016.
 */
public class ContextTest {

  private Context context;

  @Before
  public void setUp() throws Exception {
    context = new Context(mock(TestObject.class));
  }

  @Test
  public void testGetMostRecentData_NoData() throws Exception {
    assertFalse(context.getMostRecentData().isPresent());
  }

  /**
   * Data stored under EVENT_RECEIVED should be returned if no other event data is stored.
   */
  @Test
  public void testGetMostRecentData_Test_Order_EVENT_RECEIVED() throws Exception {
    Object data = mock(Object.class);
    context.setDataFor(LifecycleEvent.EVENT_RECEIVED, data);

    assertEquals(data, context.getMostRecentData().get());
  }

  /**
   * Data stored under VALIDATION_ERROR should be returned if no SUCCESSFUL_STATE_CHANGE data is stored.
   */
  @Test
  public void testGetMostRecentData_Order_VALIDATION_ERROR() throws Exception {
    Object data = mock(Object.class);
    context.setDataFor(LifecycleEvent.EVENT_RECEIVED, mock(Object.class));
    context.setDataFor(LifecycleEvent.VALIDATION_ERROR, data);

    assertEquals(data, context.getMostRecentData().get());
  }

  /**
   * Data stored under SUCCESSFUL_STATE_CHANGE be returned if present.
   */
  @Test
  public void testGetMostRecentData_Order_SUCCESSFULL_STATE_CHANGE() throws Exception {
    Object data = mock(Object.class);
    context.setDataFor(LifecycleEvent.EVENT_RECEIVED, mock(Object.class));
    context.setDataFor(LifecycleEvent.VALIDATION_ERROR, mock(Object.class));
    context.setDataFor(LifecycleEvent.SUCCESSFUL_STATE_CHANGE, data);

    assertEquals(data, context.getMostRecentData().get());
  }
}