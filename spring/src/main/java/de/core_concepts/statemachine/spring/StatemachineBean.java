package de.core_concepts.statemachine.spring;


import de.core_concepts.statemachine.AbstractStatemachine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.annotation.PostConstruct;

import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

/**
 * Abstract superclass for all statemachines with spring support. It also defines two Enum values which have to be present in all concrete manifestations
 * of S, CONCLUDED and NONE. The first is used as the statemachines final state and the second is the initial state.
 * <p>
 * Created by zieglerch on 17.06.2016.
 */
@Component
public abstract class StatemachineBean<S, E, O> extends AbstractStatemachine<S, E, O> {

    static final ThreadLocal<TransactionStatus> currentTransaction = new ThreadLocal<>();

    /**
     * Use this to obtain regular Action beans using a component name.
     */
    @Autowired
    protected SmBeanRegistry registry;

    @Autowired(required = false)
    private PlatformTransactionManager transactionManager;

    /**
     * @param stateType  The class object for the state type S.
     * @param eventType  The class object for the event type E.
     * @param objectType The class object for the object type O.
     */
    protected StatemachineBean(Class<S> stateType, Class<E> eventType, Class<O> objectType) {
        super(stateType, eventType, objectType);
    }

    @PostConstruct
    public final void init() {
        initStateMachine();
    }

    @Override
    protected final void createTransaction() {
        if (transactionManager != null && currentTransaction.get() == null) {
            log.debug("Creating new transaction.");
            currentTransaction.set(transactionManager.getTransaction(new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW)));
        }
    }

    @Override
    protected final void closeTransaction(boolean rollback) {
        TransactionStatus transactionStatus = currentTransaction.get();
        if (transactionManager == null || transactionStatus == null) {
            return;
        }
        if (transactionStatus.isCompleted()) {
            log.debug("Transaction is already completed. Removing it.");
            currentTransaction.remove();
            return;
        }
        try {
            if (rollback) {
                log.debug("Rolling back transaction.");
                transactionManager.rollback(transactionStatus);
            } else {
                log.debug("Committing transaction.");
                transactionManager.commit(transactionStatus);
            }
        } catch (RuntimeException e) {
            log.error("Exception during commit or rollback.");
            throw e;
        } finally {
            log.debug("Removing transaction from thread local.");
            currentTransaction.remove();
        }
    }
}
