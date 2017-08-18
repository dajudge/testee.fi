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

import org.hibernate.engine.transaction.jta.platform.internal.AbstractJtaPlatform;
import org.jboss.weld.transaction.spi.TransactionServices;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

public class TestEEfiJtaPlatform extends AbstractJtaPlatform {
    private final transient TransactionManager transactionManager;
    private final transient TransactionServices transactionServices;

    public TestEEfiJtaPlatform(
            final TransactionManager transactionManager,
            final TransactionServices transactionServices
    ) {
        this.transactionManager = transactionManager;
        this.transactionServices = transactionServices;
    }

    @Override
    protected TransactionManager locateTransactionManager() {
        return transactionManager;
    }

    @Override
    protected UserTransaction locateUserTransaction() {
        return transactionServices.getUserTransaction();
    }
}