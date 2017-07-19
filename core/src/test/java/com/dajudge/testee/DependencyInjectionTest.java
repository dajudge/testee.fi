package com.dajudge.testee;

import com.dajudge.testee.jdbc.PlaygroundConnectionFactory;
import com.dajudge.testee.jdbc.TestDataSource;
import com.dajudge.testee.runtime.TestRuntime;
import com.dajudge.testee.runtime.TestSetup;
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

    @Before
    public void setup() {
        final TestSetup testSetup = new TestSetup(TestBean.class, TestRuntime.instance());
        root = new TestBean();
        testSetup.prepareTestInstance("myInstance", root).run();
    }

    @Test
    public void cdi_in_root_via_inject() {
        assertNotNull(root.cdiInRootViaInject);
    }

    @Test
    public void ejb_in_root_via_ejb() {
        assertNotNull(root.ejbInRootViaEjb);
    }

    @Test
    public void ejb_in_cdi_via_inject() {
        assertNotNull(root.cdiInRootViaInject.ejbInCdiViaInject);
    }

    @Test
    public void ejb_in_ejb_via_inject() {
        assertNotNull(root.ejbInRootViaEjb.ejbInEjbViaInject);
    }

    @Test
    public void ejb_in_ejb_via_ejb() {
        assertNotNull(root.ejbInRootViaEjb.ejbInEjbViaEjb);
    }

    @Test
    public void resource_in_ejb() {
        assertNotNull(root.ejbInRootViaEjb.resourceInEjb);
    }

    @Test
    public void resource_in_root() {
        assertNotNull(root.resourceInRoot);
    }

    @Test
    public void resource_in_cdi() {
        assertNotNull(root.ejbInRootViaEjb.resourceInEjb);
    }

    @Test
    public void ciruclal_ejb_reference() {
        assertNotNull(root.ejbInRootViaEjb.ejbInEjbViaEjb.ciruclar);
    }


    @Stateless
    public static class SessionBean2 {
        @EJB
        private SessionBean2 ciruclar;
    }

    @Stateless
    public static class SessionBean1 {
        @Inject
        private SessionBean2 ejbInEjbViaInject;
        @EJB
        private SessionBean2 ejbInEjbViaEjb;
        @Resource(mappedName = "testds")
        private DataSource resourceInEjb;
    }

    public static class ExampleBean1 {
        @Inject
        private SessionBean1 ejbInCdiViaInject;
        @Resource(mappedName = "testds")
        private DataSource resourceInCdi;
    }

    public static abstract class BaseTestBean {
        @EJB
        protected SessionBean1 ejbInRootViaEjb;
        @Resource(mappedName = "testds")
        protected DataSource resourceInRoot;
    }

    @TestDataSource(name = "testds", factory = PlaygroundConnectionFactory.class)
    public static class TestBean extends BaseTestBean {
        @Inject
        private ExampleBean1 cdiInRootViaInject;
    }

}
