package com.dajudge.testee.services;

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

    }

    @Override
    public boolean isTransactionActive() {
        return false;
    }

    @Override
    public UserTransaction getUserTransaction() {
        return null;
    }

    @Override
    public void cleanup() {

    }
}
