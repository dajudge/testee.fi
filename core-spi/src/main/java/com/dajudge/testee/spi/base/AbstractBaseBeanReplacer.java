package com.dajudge.testee.spi.base;

import com.dajudge.testee.spi.BeanModifier;
import com.dajudge.testee.spi.SessionBeanFactory;

import javax.enterprise.inject.spi.Bean;
import java.util.Collection;

/**
 * Base class for simple replacing {@link com.dajudge.testee.spi.BeanModifier} implementations.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public abstract class AbstractBaseBeanReplacer implements BeanModifier {

    private BeanReplacementManager replacementManager;

    protected AbstractBaseBeanReplacer(final Object testSetupClass) {
        replacementManager = new BeanReplacementManager(createMocksFor(testSetupClass));
    }

    @Override
    public <T> void modifyCdiBean(final Bean<T> cdiBean) {
        replacementManager.instrumentCdiBean(cdiBean);
    }

    @Override
    public <T> SessionBeanFactory<T> modifySessionBean(final SessionBeanFactory<T> sessionBean) {
        return replacementManager.wrapSessionBean(sessionBean);
    }

    protected abstract Collection<Object> createMocksFor(final Object testClassInstance);
}
