package fi.testee.jdbc;

import fi.testee.spi.ConnectionFactory;
import fi.testee.runtime.TestSetup;

/**
 * Interface for accessing {@link ConnectionFactory connection factories} of a
 * {@link TestSetup}.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public interface ConnectionFactoryManager {
    ConnectionFactory getFactoryFor(TestDataSource desc);
}
