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
package fi.testee;

import fi.testee.runtime.TestRuntime;
import fi.testee.runtime.TestSetup;
import org.junit.Test;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.inject.Inject;
import java.util.function.Supplier;

import static org.junit.Assert.assertTrue;

public class LifecycleTest {
    private TestSetup.TestInstance context;
    private TestSetup testSetup;
    private TestBean root;

    public void setup() {
        testSetup = new TestSetup(TestBean.class, TestRuntime.instance()).init();
        root = new TestBean();
        context = testSetup.prepareTestInstance("myInstance", root, null);
    }

    @Test
    public void lifecycle_works_with_ejb() {
        assertLifecycle(() -> root.getSessionBean());
    }

    @Test
    public void lifecycle_works_with_cdi() {
        assertLifecycle(() -> root.getManagedBean());
    }

    @Test
    public void lifecycle_works_with_testInstance() {
        assertLifecycle(() -> root);
    }

    private void assertLifecycle(final Supplier<AbstractBaseBean> beanSupplier) {
        setup();
        try {
            assertTrue(beanSupplier.get().isPostConstructed());
        } finally {
            shutdown();
        }
        assertTrue(beanSupplier.get().isPreDestroyed());
    }

    public void shutdown() {
        if (context != null) {
            context.shutdown();
        }
        if (testSetup != null) {
            testSetup.shutdown();
        }
    }

    public static abstract class AbstractBaseBean {
        private boolean postConstructed;
        private boolean preDestroyed;

        @PostConstruct
        private void postConstruct() {
            postConstructed = true;
        }

        @PreDestroy
        public void preDestroy() {
            preDestroyed = true;
        }

        public boolean isPostConstructed() {
            return postConstructed;
        }

        public boolean isPreDestroyed() {
            return preDestroyed;
        }
    }

    @Singleton
    public static class SessionBean extends AbstractBaseBean {

    }

    public static class ManagedBean extends AbstractBaseBean {

    }

    public static class TestBean extends AbstractBaseBean {
        @EJB
        private SessionBean sessionBean;
        @Inject
        private ManagedBean managedBean;

        public ManagedBean getManagedBean() {
            return managedBean;
        }

        public SessionBean getSessionBean() {
            return sessionBean;
        }
    }
}
