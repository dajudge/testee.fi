package com.dajudge.testee.ejb;

import org.jboss.weld.injection.spi.ResourceReferenceFactory;

import java.lang.reflect.Type;

/**
 * Management of a session bean's lifecycle.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
interface SessionBeanContainer {
    ResourceReferenceFactory<Object> get();
}
