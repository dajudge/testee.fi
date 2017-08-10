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
import fi.testee.interceptor.UseInterceptor;
import fi.testee.jdbc.PlaygroundConnectionFactory;
import fi.testee.jdbc.TestDataSource;
import org.junit.Test;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.sql.DataSource;

import static fi.testee.interceptor.TestInterceptor.Type.AROUND_INVOKE;
import static fi.testee.interceptor.TestInterceptor.Type.POST_CONSTRUCT;
import static fi.testee.interceptor.TestInterceptor.Type.PRE_DESTROY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DependencyInjectionTest extends BaseDependencyInjectionTest<DependencyInjectionTest.TestBean> {
    public DependencyInjectionTest() {
        super(TestBean.class);
    }

    @Test
    public void cdi_in_root_via_inject() {
        runTest(() -> {
            assertNotNull(root.getCdiInRootViaInject());
            ensureInterception(ExampleBean1.class, null, POST_CONSTRUCT);
        }, () -> {
            ensureInterception(ExampleBean1.class, null, PRE_DESTROY);
        });
    }

    @Test
    public void ejb_in_root_via_ejb() {
        runTest(() -> {
            assertNotNull(root.getEjbInRootViaEjb());
            ensureInterception(ExampleBean1.class, null, POST_CONSTRUCT);
        }, () -> {
            ensureInterception(ExampleBean1.class, null, PRE_DESTROY);
        });
    }

    @Test
    public void ejb_in_cdi_via_inject() {
        runTest(() -> {
            assertNotNull(root.getCdiInRootViaInject().getEjbInCdiViaInject());
            ensureInterception(ExampleBean1.class, null, POST_CONSTRUCT);
            ensureInterception(ExampleBean1.class, "getEjbInCdiViaInject", AROUND_INVOKE);
        }, () -> {
            ensureInterception(ExampleBean1.class, null, PRE_DESTROY);
        });
    }

    @Test
    public void ejb_in_ejb_via_inject() {
        runTest(() -> {
            assertNotNull(root.getEjbInRootViaEjb().getEjbInEjbViaInject());
            ensureInterception(ExampleBean1.class, null, POST_CONSTRUCT); // TODO can't this happen lazily?
            ensureInterception(SessionBean1.class, "getEjbInEjbViaInject", AROUND_INVOKE);
        }, () -> {
            ensureInterception(ExampleBean1.class, null, PRE_DESTROY);
        });

    }

    @Test
    public void ejbIface_in_root_via_inject() {
        runTest(() -> {
            assertEquals("Hello, world", root.ejbIfaceViaInject.test());
            ensureInterception(ExampleBean1.class, null, POST_CONSTRUCT); // TODO can't this happen lazily?
            ensureInterception(SessionBean1.class, "test", AROUND_INVOKE);
        }, () -> {
            ensureInterception(ExampleBean1.class, null, PRE_DESTROY);
        });

    }

    @Test
    public void ejb_in_ejb_via_ejb() {
        runTest(() -> {
            assertNotNull(root.getEjbInRootViaEjb().getEjbInEjbViaEjb());
            ensureInterception(ExampleBean1.class, null, POST_CONSTRUCT); // TODO can't this happen lazily?
            ensureInterception(SessionBean1.class, "getEjbInEjbViaEjb", AROUND_INVOKE);
        }, () -> {
            ensureInterception(ExampleBean1.class, null, PRE_DESTROY);
        });
    }

    @Test
    public void resource_in_ejb() {
        runTest(() -> {
            assertNotNull(root.getEjbInRootViaEjb().getResourceInEjb());
            ensureInterception(ExampleBean1.class, null, POST_CONSTRUCT); // TODO can't this happen lazily?
            ensureInterception(SessionBean1.class, "getResourceInEjb", AROUND_INVOKE);
        }, () -> {
            ensureInterception(ExampleBean1.class, null, PRE_DESTROY);
        });
    }

    @Test
    public void resource_in_root() {
        runTest(() -> {
            assertNotNull(root.getResourceInRoot());
            ensureInterception(ExampleBean1.class, null, POST_CONSTRUCT); // TODO can't this happen lazily?
        }, () -> {
            ensureInterception(ExampleBean1.class, null, PRE_DESTROY);
        });
    }

    @Test
    public void resource_in_cdi() {
        runTest(() -> {
            assertNotNull(root.getCdiInRootViaInject().getResourceInCdi());
            ensureInterception(ExampleBean1.class, null, POST_CONSTRUCT);
            ensureInterception(ExampleBean1.class, "getResourceInCdi", AROUND_INVOKE);
        }, () -> {
            ensureInterception(ExampleBean1.class, null, PRE_DESTROY);
        });
    }

    @Test
    public void bean_from_different_archive() {
        runTest(() -> {
            assertNotNull(root.getBeanFromDifferentArchive());
            ensureInterception(ExampleBean1.class, null, POST_CONSTRUCT);
        }, () -> {
            ensureInterception(ExampleBean1.class, null, PRE_DESTROY);
        });
    }

    @Override
    TestBean instance() {
        return new TestBean();
    }

    @Stateless
    @UseInterceptor
    public static class SessionBean2 {
        @PostConstruct
        public void postConstruct() {
        }

        @PreDestroy
        public void preDestroy() {
        }
    }

    public interface SessionBeanInterface {

        String test();
    }

    @Stateless
    @UseInterceptor
    public static class SessionBean1 implements SessionBeanInterface {
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

        @Override
        public String test() {
            return "Hello, world";
        }
    }

    @UseInterceptor
    public static class ExampleBean1 {
        @Inject
        private SessionBean1 ejbInCdiViaInject;
        @Resource(mappedName = "testds")
        private DataSource resourceInCdi;

        @PostConstruct
        public void postConstruct() {
        }

        @PreDestroy
        public void preDestroy() {
        }

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
        @Inject
        private SessionBeanInterface ejbIfaceViaInject;

        public ExampleBean1 getCdiInRootViaInject() {
            return cdiInRootViaInject;
        }

        public BeanArchiveDiscovery getBeanFromDifferentArchive() {
            return beanFromDifferentArchive;
        }
    }

}
