package fi.testee.services;

import org.jboss.weld.transaction.spi.TransactionServices;

import javax.transaction.Synchronization;
import javax.transaction.UserTransaction;

/**
 * Transaction management in Weld.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class TransactionServicesImpl implements TransactionServices {
    @Override
    public void registerSynchronization(final Synchronization synchronization) {
        // TODO figure out what to do here
    }

    @Override
    public boolean isTransactionActive() {
        // TODO figure out what to do here
        return false;
    }

    @Override
    public UserTransaction getUserTransaction() {
        // TODO figure out what to do here
        return null;
    }

    @Override
    public void cleanup() {
        // Nothing to clean up
    }
}
