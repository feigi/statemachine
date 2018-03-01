package de.core_concepts.statemachine;

import org.junit.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests class ChainedAction
 * <p>
 * Created by zieglerch on 05.04.2016.
 */
public class ChainedActionTest {

    @Test
    public void testActions_Executed_Sequentially_WithFirstDo() throws Exception {
        Action firstAction = mock(Action.class);
        Action secondAction = mock(Action.class);
        Action thirdAction = mock(Action.class);
        Context context = mock(Context.class);

        ChainedAction chainedAction = ChainedAction.firstDo(firstAction)
                .thenDo(secondAction)
                .thenDo(thirdAction);
        chainedAction.execute(context);

        InOrder inOrder = inOrder(firstAction, secondAction, thirdAction);

        inOrder.verify(firstAction).execute(context);
        inOrder.verify(secondAction).execute(context);
        inOrder.verify(thirdAction).execute(context);
    }

    @Test
    public void testActions_Executed_Sequentially_WithOf() throws Exception {
        ContextConsumer firstAction = mock(ContextConsumer.class);
        Action secondAction = mock(Action.class);
        Action thirdAction = mock(Action.class);
        Context context = mock(Context.class);

        ChainedAction chainedAction = ChainedAction.of(firstAction)
                .thenDo(secondAction)
                .thenDo(thirdAction);
        chainedAction.execute(context);

        InOrder inOrder = inOrder(firstAction, secondAction, thirdAction);

        inOrder.verify(firstAction).accept(context);
        inOrder.verify(secondAction).execute(context);
        inOrder.verify(thirdAction).execute(context);
    }

    @Test
    public void testActions_Ensure_ChainedActions_Aint_Overlapping() throws Exception {
        ContextConsumer firstActionA = mock(ContextConsumer.class);
        ContextConsumer firstActionB = mock(ContextConsumer.class);
        Action commonAction = mock(Action.class);
        Action thirdActionA = mock(Action.class);
        Action thirdActionB = mock(Action.class);
        Context contextA = mock(Context.class);
        Context contextB = mock(Context.class);

        ChainedAction chainedActionA = ChainedAction.of(firstActionA)
                .thenDo(commonAction)
                .thenDo(thirdActionA);
        chainedActionA.execute(contextA);

        ChainedAction chainedActionB = ChainedAction.of(firstActionB)
                .thenDo(commonAction)
                .thenDo(thirdActionB);
        chainedActionB.execute(contextB);

        InOrder inOrder = inOrder(firstActionA, commonAction, thirdActionA, firstActionB, commonAction, thirdActionB);

        inOrder.verify(firstActionA).accept(contextA);
        inOrder.verify(commonAction).execute(contextA);
        inOrder.verify(thirdActionA).execute(contextA);
        inOrder.verify(firstActionB).accept(contextB);
        inOrder.verify(commonAction).execute(contextB);
        inOrder.verify(thirdActionB).execute(contextB);
    }
}