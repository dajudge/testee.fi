package fi.testee;

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

import static org.junit.Assert.assertNotNull;

public class DependencyInjectionTest {
    private TestBean root;
    private Runnable shutdown;

    @Before
    public void setup() {
        final TestSetup testSetup = new TestSetup(TestBean.class, TestRuntime.instance());
        root = new TestBean();
        shutdown = testSetup.prepareTestInstance("myInstance", root);
    }

    @After
    public void shutdown() {
        shutdown.run();
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
    }

    @Test
    public void ejb_in_ejb_via_inject() {
        assertNotNull(root.getEjbInRootViaEjb().getEjbInEjbViaInject());
    }

    @Test
    public void ejb_in_ejb_via_ejb() {
        assertNotNull(root.getEjbInRootViaEjb().getEjbInEjbViaEjb());
    }

    @Test
    public void resource_in_ejb() {
        assertNotNull(root.getEjbInRootViaEjb().getResourceInEjb());
    }

    @Test
    public void resource_in_root() {
        assertNotNull(root.getResourceInRoot());
    }

    @Test
    public void resource_in_cdi() {
        assertNotNull(root.getEjbInRootViaEjb().getResourceInEjb());
    }

    @Test
    public void circular_ejb_reference() {
        assertNotNull(root.getEjbInRootViaEjb().getEjbInEjbViaEjb().getCircular());
    }


    @Stateless
    public static class SessionBean2 {
        @EJB
        private SessionBean2 circular;

        public SessionBean2 getCircular() {
            return circular;
        }
    }

    @Stateless
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

        public ExampleBean1 getCdiInRootViaInject() {
            return cdiInRootViaInject;
        }
    }

}
