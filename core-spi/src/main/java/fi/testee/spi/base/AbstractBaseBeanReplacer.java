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
package fi.testee.spi.base;

import fi.testee.spi.BeanModifier;
import fi.testee.spi.SessionBeanFactory;

import javax.enterprise.inject.spi.Bean;
import java.util.Collection;

/**
 * Base class for simple replacing {@link BeanModifier} implementations.
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
