package com.dajudge.testee.mockito;

import com.dajudge.testee.exceptions.TesteeException;
import com.dajudge.testee.spi.BeanModifier;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import javax.enterprise.inject.spi.Bean;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * A {@link BeanModifier} for integrating Mockito with TestEE.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class MockitoBeanModifier implements BeanModifier {
    private MockManager mockManager;

    MockitoBeanModifier(final Object testSetupClass) {
        mockManager = new MockManager(createMocksFor(testSetupClass));
    }

    @Override
    public <T> void initializeForBean(final Bean<T> bean) {
        mockManager.wrapProducerFor(bean);
    }

    private static Collection<Object> createMocksFor(final Object testClassInstance) {
        MockitoAnnotations.initMocks(testClassInstance);
        return FieldUtils.getAllFieldsList(testClassInstance.getClass()).stream()
                .filter(it -> it.getAnnotation(Mock.class) != null || it.getAnnotation(Spy.class) != null)
                .map(it -> {
                    it.setAccessible(true);
                    try {
                        return it.get(testClassInstance);
                    } catch (final IllegalAccessException e) {
                        throw new TesteeException("Could not extract mock from " + testClassInstance, e);
                    }
                })
                .collect(Collectors.toSet());
    }
}
