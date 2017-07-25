package fi.testee.mockito;

import fi.testee.runtime.TestRuntime;
import fi.testee.runtime.TestSetup;
import org.junit.Test;
import org.mockito.Mock;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.inject.Inject;

import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

public class InjectMockTest {

    @Test
    public void cdiMock_in_cdiBean_via_inject() {
        test(it -> assertEquals("lolcats", it.getCdiBean().getCdiMockInCdiViaInject().doIt()));
    }
    @Test
    public void ejbMock_in_cdiBean_via_ejb() {
        test(it -> assertEquals("lolcats", it.getCdiBean().getEjbMockInCdiViaEjb().doIt()));
    }
    @Test
    public void ejbMock_in_cdiBean_via_inject() {
        test(it -> assertEquals("lolcats", it.getCdiBean().getEjbMockInCdiViaInject().doIt()));
    }
    @Test
    public void cdiMock_in_ejb_via_inject() {
        test(it -> assertEquals("lolcats", it.getEjb().getCdiMockInEjbViaInject().doIt()));
    }
    @Test
    public void ejbMock_in_ejb_via_ejb() {
        test(it -> assertEquals("lolcats", it.getEjb().getEjbMockInEjbViaEjb().doIt()));
    }
    @Test
    public void ejbMock_in_ejb_via_inject() {
        test(it -> assertEquals("lolcats", it.getEjb().getEjbMockInEjbViaInject().doIt()));
    }

    private void test(Consumer<TestBean> test) {
        // Given
        final TestSetup testSetup = new TestSetup(TestBean.class, TestRuntime.instance());
        final TestBean testClassInstance = new TestBean();

        // When
        final Runnable cleanup = testSetup.prepareTestInstance("myInstance", testClassInstance);
        when(testClassInstance.cdiMock.doIt()).thenReturn("lolcats");
        when(testClassInstance.ejbMock.doIt()).thenReturn("lolcats");

        try {
            // Then
            test.accept(testClassInstance);
        } finally {
            cleanup.run();
        }
    }

    @Singleton
    public static class ExampleSessionBean1 {
        @Inject
        public ExampleBean2 cdiMockInEjbViaInject;
        @EJB
        public ExampleSessionBean2 ejbMockInEjbViaEjb;
        @Inject
        public ExampleSessionBean2 ejbMockInEjbViaInject;

        public ExampleBean2 getCdiMockInEjbViaInject() {
            return cdiMockInEjbViaInject;
        }

        public ExampleSessionBean2 getEjbMockInEjbViaEjb() {
            return ejbMockInEjbViaEjb;
        }

        public ExampleSessionBean2 getEjbMockInEjbViaInject() {
            return ejbMockInEjbViaInject;
        }
    }

    @Singleton
    public static class ExampleSessionBean2 {
        public String doIt() {
            return "I am IronMan";
        }
    }

    public static class ExampleBean2 {
        public String doIt() {
            return "I am IronMan";
        }
    }

    public static class ExampleBean1 {
        @Inject
        public ExampleBean2 cdiMockInCdiViaInject;
        @EJB
        public ExampleSessionBean2 ejbMockInCdiViaEjb;
        @Inject
        public ExampleSessionBean2 ejbMockInCdiViaInject;

        public ExampleBean2 getCdiMockInCdiViaInject() {
            return cdiMockInCdiViaInject;
        }

        public ExampleSessionBean2 getEjbMockInCdiViaEjb() {
            return ejbMockInCdiViaEjb;
        }

        public ExampleSessionBean2 getEjbMockInCdiViaInject() {
            return ejbMockInCdiViaInject;
        }
    }

    public static class TestBean {
        @Inject
        public ExampleBean1 cdiBean;
        @EJB
        public ExampleSessionBean1 ejb;
        @Mock
        public ExampleBean2 cdiMock;
        @Mock
        public ExampleSessionBean2 ejbMock;

        public ExampleBean1 getCdiBean() {
            return cdiBean;
        }

        public ExampleSessionBean1 getEjb() {
            return ejb;
        }

        public ExampleBean2 getCdiMock() {
            return cdiMock;
        }

        public ExampleSessionBean2 getEjbMock() {
            return ejbMock;
        }
    }
}
