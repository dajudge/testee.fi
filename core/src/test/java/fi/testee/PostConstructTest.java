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

import fi.testee.interceptor.TestInterceptor;
import fi.testee.runtime.TestRuntime;
import fi.testee.runtime.TestSetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;

import static org.junit.Assert.assertTrue;

public class PostConstructTest {
    private TestSetup.TestContext context;
    private TestSetup testSetup;
    private TestBean root;

    @Before
    public void setup() {
        TestInterceptor.INVOCATIONS.clear();
        testSetup = new TestSetup(TestBean.class, TestRuntime.instance());
        root = new TestBean();
        context = testSetup.prepareTestInstance("myInstance", root);
    }

    @Test
    public void postConstruct_is_invoked() {
        assertTrue(root.getSessionBean().isPostConstructed());
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

    @Singleton
    public static class SessionBean {
        private boolean postConstructed;

        @PostConstruct
        private void postConstruct() {
            postConstructed = true;
        }

        public boolean isPostConstructed() {
            return postConstructed;
        }
    }

    public static class TestBean {
        @EJB
        private SessionBean sessionBean;

        public SessionBean getSessionBean() {
            return sessionBean;
        }
    }
}
