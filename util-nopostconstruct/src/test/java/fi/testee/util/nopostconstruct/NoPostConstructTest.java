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
package fi.testee.util.nopostconstruct;

import fi.testee.runtime.TestRuntime;
import fi.testee.runtime.TestSetup;
import fi.testee.util.nopostconstruct.annotation.NoPostConstructFor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.inject.Inject;

import static org.junit.Assert.assertFalse;

public class NoPostConstructTest {

    private TestSetup testSetup;
    private TestBean root;
    private TestSetup.TestInstance context;

    @Before
    public void setup() throws NoSuchMethodException {
        testSetup = new TestSetup(TestBean.class, TestRuntime.instance()).init();
        root = new TestBean();
        context = testSetup.prepareTestInstance("myInstance", root, getClass().getMethod("annotatedMethod"));
    }

    @NoPostConstructFor({SessionBean.class, ManagedBean.class})
    public void annotatedMethod() {
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
    public void works_for_ejbs() {
        assertFalse(root.getSessionBean().isPostConstructed());
    }

    @Test
    public void works_for_cdi() {
        assertFalse(root.getManagedBean().isPostConstructed());
    }

    public abstract static class BaseBean {
        private boolean postConstructed;

        @PostConstruct
        public void postConstruct() {
            postConstructed = true;
        }

        public boolean isPostConstructed() {
            return postConstructed;
        }
    }

    @Singleton
    public static class SessionBean extends BaseBean {

    }

    public static class ManagedBean extends BaseBean {

    }

    public static class TestBean {
        @EJB
        private SessionBean sessionBean;
        @Inject
        private ManagedBean managedBean;

        public SessionBean getSessionBean() {
            return sessionBean;
        }

        public ManagedBean getManagedBean() {
            return managedBean;
        }
    }
}
