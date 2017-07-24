package com.dajudge.testee.liqiubase;

import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;

/**
 * {@link ResourceAccessorFactory} for a simple {@link liquibase.resource.ClassLoaderResourceAccessor}.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class ClassLoaderResourceAccessorFactory implements ResourceAccessorFactory {
    @Override
    public ResourceAccessor create() {
        return new ClassLoaderResourceAccessor();
    }
}
