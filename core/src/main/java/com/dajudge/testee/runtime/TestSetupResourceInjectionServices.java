package com.dajudge.testee.runtime;

import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.jboss.weld.injection.spi.ResourceReference;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;

import javax.annotation.Resource;
import javax.enterprise.inject.spi.InjectionPoint;
import java.util.Map;

public class TestSetupResourceInjectionServices implements ResourceInjectionServices {
    private Map<String, Object> params;

    public TestSetupResourceInjectionServices(final Map<String, Object> params) {
        this.params = params;
    }

    @Override
    public ResourceReferenceFactory<Object> registerResourceInjectionPoint(final InjectionPoint injectionPoint) {
        final Resource resource = injectionPoint.getAnnotated().getAnnotation(Resource.class);
        return () -> {
            if (!params.containsKey(resource.mappedName())) {
                throw new IllegalArgumentException("Unresolved @Resource: " + injectionPoint);
            }
            return new ResourceReference<Object>() {
                @Override
                public Object getInstance() {
                    return params.get(resource.mappedName());
                }

                @Override
                public void release() {

                }
            };
        };
    }

    @Override
    public ResourceReferenceFactory<Object> registerResourceInjectionPoint(final String jndiName, final String mappedName) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Object resolveResource(InjectionPoint injectionPoint) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Object resolveResource(String jndiName, String mappedName) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void cleanup() {

    }
}
