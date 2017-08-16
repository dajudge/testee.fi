/*
 * Copyright (C) 2017 Alex Stockinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fi.testee.hibernate;

import fi.testee.spi.PersistenceUnitPropertyContributor;
import org.hibernate.cfg.AvailableSettings;
import org.jboss.weld.transaction.spi.TransactionServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import java.util.Properties;

public class PropertyContributor implements PersistenceUnitPropertyContributor {
    private static final Logger LOG = LoggerFactory.getLogger(PropertyContributor.class);

    @Resource(mappedName = "testeefi/setup/transactionServices")
    private TransactionServices transactionServices;

    @Override
    public void contribute(final Properties properties, final String provider) {
        if (!provider.equals(org.hibernate.jpa.HibernatePersistenceProvider.class.getName())) {
            return;
        }
        properties.put(
                AvailableSettings.SCANNER_ARCHIVE_INTERPRETER,
                new TestEEfiArchiveDescriptorFactory()
        );
        // TODO find out why eclipselink allows TX access by default while Hibernate doesn't
        properties.put(
                AvailableSettings.ALLOW_JTA_TRANSACTION_ACCESS,
                true
        );
        properties.put(
                AvailableSettings.JTA_PLATFORM,
                new TestEEfiJtaPlatform(transactionManager(), transactionServices)
        );
    }

    private TransactionManager transactionManager() {
        return new TransactionManager() {
            private int status = Status.STATUS_NO_TRANSACTION;
            @Override
            public void begin() throws NotSupportedException, SystemException {
                status = Status.STATUS_ACTIVE;
            }

            @Override
            public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
                status = Status.STATUS_NO_TRANSACTION;
            }

            @Override
            public void rollback() throws IllegalStateException, SecurityException, SystemException {
                status = Status.STATUS_NO_TRANSACTION;
            }

            @Override
            public void setRollbackOnly() throws IllegalStateException, SystemException {
                throw new AssertionError("setRollbackOnly");
            }

            @Override
            public int getStatus() throws SystemException {
                return status;
            }

            @Override
            public Transaction getTransaction() throws SystemException {
                return transaction();
            }

            @Override
            public void setTransactionTimeout(int seconds) throws SystemException {
                throw new AssertionError("setTransactionTimeout");
            }

            @Override
            public Transaction suspend() throws SystemException {
                throw new AssertionError("Suspend");
            }

            @Override
            public void resume(Transaction tobj) throws InvalidTransactionException, IllegalStateException, SystemException {
                throw new AssertionError("Resume");
            }
        };
    }

    private Transaction transaction() {
        return new Transaction() {
            @Override
            public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, SystemException {
                throw new AssertionError("commit");
            }

            @Override
            public void rollback() throws IllegalStateException, SystemException {
                throw new AssertionError("rollback");
            }

            @Override
            public void setRollbackOnly() throws IllegalStateException, SystemException {
                throw new AssertionError("setRollbackOnly");
            }

            @Override
            public int getStatus() throws SystemException {
                throw new AssertionError("getStatus");
            }

            @Override
            public boolean enlistResource(final XAResource xaRes) throws RollbackException, IllegalStateException, SystemException {
                throw new AssertionError("enlistResource");
            }

            @Override
            public boolean delistResource(final XAResource xaRes, final int flag) throws IllegalStateException, SystemException {
                throw new AssertionError("delistResource");
            }

            @Override
            public void registerSynchronization(final Synchronization sync) throws RollbackException, IllegalStateException, SystemException {
                LOG.info("Transaction.registerSynchronization: {}", sync);
            }
        };
    }
}
