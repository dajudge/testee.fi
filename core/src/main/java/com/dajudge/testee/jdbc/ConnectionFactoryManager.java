package com.dajudge.testee.jdbc;

import com.dajudge.testee.spi.ConnectionFactory;

/**
 * Interface for accessing {@link com.dajudge.testee.spi.ConnectionFactory connection factories} of a
 * {@link com.dajudge.testee.runtime.TestSetup}.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public interface ConnectionFactoryManager {
    ConnectionFactory getFactoryFor(TestDataSource desc);
}
