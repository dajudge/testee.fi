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

import fi.testee.deployment.BeanArchiveDiscovery;
import fi.testee.interceptor.TestInterceptor;
import fi.testee.interceptor.UseInterceptor;
import fi.testee.jdbc.PlaygroundConnectionFactory;
import fi.testee.jdbc.TestDataSource;
import fi.testee.runtime.TestRuntime;
import fi.testee.runtime.TestSetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.sql.DataSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DependencyInjectionTest {
    private TestBean root;
    private TestSetup.TestContext context;
    private TestSetup testSetup;

    @Before
    public void setup() {
        TestInterceptor.INVOCATIONS.clear();
        testSetup = new TestSetup(TestBean.class, TestRuntime.instance());
        root = new TestBean();
        context = testSetup.prepareTestInstance("myInstance", root);
    }

    @After
    public void shutdown() {
        if (context != null) {
            context.shutdown();
        }
        if (testSetup != null) {
            testSetup.shutdown();
        }
    }

    @Test
    public void cdi_in_root_via_inject() {
        assertNotNull(root.getCdiInRootViaInject());
    }

    @Test
    public void ejb_in_root_via_ejb() {
        assertNotNull(root.getEjbInRootViaEjb());
    }

    @Test
    public void ejb_in_cdi_via_inject() {
        assertNotNull(root.getCdiInRootViaInject().getEjbInCdiViaInject());
        ensureInterception(ExampleBean1.class, "getEjbInCdiViaInject");
    }

    @Test
    public void ejb_in_ejb_via_inject() {
        assertNotNull(root.getEjbInRootViaEjb().getEjbInEjbViaInject());
        ensureInterception(SessionBean1.class, "getEjbInEjbViaInject");
    }

    @Test
    public void ejb_in_ejb_via_ejb() {
        assertNotNull(root.getEjbInRootViaEjb().getEjbInEjbViaEjb());
        ensureInterception(SessionBean1.class, "getEjbInEjbViaEjb");
    }

    @Test
    public void resource_in_ejb() {
        assertNotNull(root.getEjbInRootViaEjb().getResourceInEjb());
        ensureInterception(SessionBean1.class, "getResourceInEjb");
    }

    @Test
    public void resource_in_root() {
        assertNotNull(root.getResourceInRoot());
    }

    @Test
    public void resource_in_cdi() {
        assertNotNull(root.getCdiInRootViaInject().getResourceInCdi());
        ensureInterception(ExampleBean1.class, "getResourceInCdi");
    }

    @Test
    public void circular_ejb_reference() {
        assertNotNull(root.getEjbInRootViaEjb().getEjbInEjbViaEjb().getCircular());
        ensureInterception(SessionBean1.class, "getEjbInEjbViaEjb");
        ensureInterception(SessionBean2.class, "getCircular");
    }

    @Test
    public void bean_from_different_archive() {
        assertNotNull(root.getBeanFromDifferentArchive());
    }

    private void ensureInterception(final Class<?> target, final String methodName) {
        assertFalse(TestInterceptor.INVOCATIONS.isEmpty());
        final TestInterceptor.Invocation invocation = TestInterceptor.INVOCATIONS.remove(0);
        assertTrue(target.isAssignableFrom(invocation.target.getClass()));
        assertEquals(methodName, invocation.method.getName());
    }

    @Stateless
    @UseInterceptor
    public static class SessionBean2 {
        @EJB
        private SessionBean2 circular;

        public SessionBean2 getCircular() {
            return circular;
        }
    }

    @Stateless
    @UseInterceptor
    public static class SessionBean1 {
        @Inject
        private SessionBean2 ejbInEjbViaInject;
        @EJB
        private SessionBean2 ejbInEjbViaEjb;
        @Resource(mappedName = "testds")
        private DataSource resourceInEjb;

        public SessionBean2 getEjbInEjbViaInject() {
            return ejbInEjbViaInject;
        }

        public SessionBean2 getEjbInEjbViaEjb() {
            return ejbInEjbViaEjb;
        }

        public DataSource getResourceInEjb() {
            return resourceInEjb;
        }
    }

    @UseInterceptor
    public static class ExampleBean1 {
        @Inject
        private SessionBean1 ejbInCdiViaInject;
        @Resource(mappedName = "testds")
        private DataSource resourceInCdi;

        public SessionBean1 getEjbInCdiViaInject() {
            return ejbInCdiViaInject;
        }

        public DataSource getResourceInCdi() {
            return resourceInCdi;
        }
    }

    @UseInterceptor
    public static abstract class BaseTestBean {
        @EJB
        private SessionBean1 ejbInRootViaEjb;
        @Resource(mappedName = "testds")
        private DataSource resourceInRoot;

        public SessionBean1 getEjbInRootViaEjb() {
            return ejbInRootViaEjb;
        }

        public DataSource getResourceInRoot() {
            return resourceInRoot;
        }
    }

    @TestDataSource(name = "testds", factory = PlaygroundConnectionFactory.class)
    public static class TestBean extends BaseTestBean {
        @Inject
        private ExampleBean1 cdiInRootViaInject;
        @Inject
        private BeanArchiveDiscovery beanFromDifferentArchive;

        public ExampleBean1 getCdiInRootViaInject() {
            return cdiInRootViaInject;
        }

        public BeanArchiveDiscovery getBeanFromDifferentArchive() {
            return beanFromDifferentArchive;
        }
    }

}
