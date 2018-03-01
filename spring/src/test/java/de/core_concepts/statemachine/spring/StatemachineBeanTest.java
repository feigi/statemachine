package de.core_concepts.statemachine.spring;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.util.ReflectionUtils;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

/**
 * Tests the class {@link StatemachineBean}
 */
@RunWith(MockitoJUnitRunner.class)
public class StatemachineBeanTest {

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private SmBeanRegistry bf;

    @Spy
    @InjectMocks
    private TestSm testee;

    @Before
    public void setUp() throws Exception {
        TestSm.currentTransaction.set(null);
    }

    @Test
    public void testCreateTransaction_TransactionManagerNull() throws Exception {
        testee = new TestSm();

        testee.createTransaction();

        // No NPE expected
        assertThat(TestSm.currentTransaction.get(), is(nullValue()));
    }

    @Test
    public void testCreateTransaction_CurrentTransactionNotNull() throws Exception {
        // Given
        TestSm.currentTransaction.set(mock(TransactionStatus.class));

        // When
        testee.createTransaction();

        // Then
        verifyNoMoreInteractions(transactionManager);
    }

    @Test
    public void testCreateTransaction() throws Exception {
        when(transactionManager.getTransaction(new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW)))
                .thenReturn(mock(TransactionStatus.class));

        testee.createTransaction();

        assertThat(TestSm.currentTransaction.get(), is(notNullValue()));
    }

    @Test
    public void testCloseTransaction_TransactionManagerNull() throws Exception {
        testee = new TestSm();

        testee.closeTransaction(false);

        // No NPE expected
        assertThat(TestSm.currentTransaction.get(), is(nullValue()));
    }

    @Test
    public void testCloseTransaction_CurrentTransactionNull() throws Exception {
        testee.closeTransaction(false);

        verifyNoMoreInteractions(transactionManager);
    }

    @Test
    public void testCloseTransaction_TransactionCompleted() throws Exception {
        final SimpleTransactionStatus transactionStatus = new SimpleTransactionStatus();
        transactionStatus.setCompleted();
        TestSm.currentTransaction.set(transactionStatus);

        testee.closeTransaction(false);

        assertThat(TestSm.currentTransaction.get(), is(nullValue()));
        verifyNoMoreInteractions(transactionManager);
    }

    @Test
    public void testCloseTransaction_Rollback() throws Exception {
        final SimpleTransactionStatus transactionStatus = new SimpleTransactionStatus();
        TestSm.currentTransaction.set(transactionStatus);

        testee.closeTransaction(true);

        assertThat(TestSm.currentTransaction.get(), is(nullValue()));
        verify(transactionManager).rollback(transactionStatus);
    }

    @Test
    public void testCloseTransaction_Commit() throws Exception {
        final SimpleTransactionStatus transactionStatus = new SimpleTransactionStatus();
        TestSm.currentTransaction.set(transactionStatus);

        testee.closeTransaction(false);

        assertThat(TestSm.currentTransaction.get(), is(nullValue()));
        verify(transactionManager).commit(transactionStatus);
    }

    @Test(expected = RuntimeException.class)
    public void testCloseTransaction_CommitException() throws Exception {
        final SimpleTransactionStatus transactionStatus = new SimpleTransactionStatus();
        TestSm.currentTransaction.set(transactionStatus);
        doThrow(new RuntimeException()).when(transactionManager).commit(any());

        testee.closeTransaction(false);
    }

    @Test(expected = RuntimeException.class)
    public void testCloseTransaction_RollbackException() throws Exception {
        final SimpleTransactionStatus transactionStatus = new SimpleTransactionStatus();
        TestSm.currentTransaction.set(transactionStatus);
        doThrow(new RuntimeException()).when(transactionManager).rollback(any());

        testee.closeTransaction(true);
    }

    @Test
    public void testInit_IsPostConstruct() throws Exception {
        final Method initMethod = ReflectionUtils.findMethod(StatemachineBean.class, "init");

        assertThat(initMethod, is(notNullValue()));
        assertThat(initMethod.getAnnotation(PostConstruct.class), is(notNullValue()));
    }

    @Test
    public void testInit_CallsInitStatemachine() throws Exception {
        testee.init();

        verify(testee).defineStates(any());
        verify(testee).defineTransitions(any());
        verify(testee).defineGenericActions(any());
    }
}