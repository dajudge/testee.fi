package fi.testee.spi;

import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;

public interface SessionBeanFactory<T> {
    EjbDescriptor<T> getDescriptor();

    ResourceReferenceFactory<T> getResourceReferenceFactory();
}
