package fi.testee.liqiubase;

import liquibase.resource.ResourceAccessor;

/**
 * Interface for classes creating {@link ResourceAccessorFactory} instances.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public interface ResourceAccessorFactory {
    /**
     * Creates a new instance of the {@link ResourceAccessor}.
     *
     * @return the {@link ResourceAccessor} instance.
     */
    ResourceAccessor create();
}
