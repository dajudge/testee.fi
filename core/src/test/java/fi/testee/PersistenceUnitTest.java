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

import javax.ejb.Singleton;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static org.junit.Assert.assertNotNull;

public class PersistenceUnitTest {
    private final TestSetup testSetup = new TestSetup(TestBean.class, TestRuntime.instance()).init();
    private final TestBean testClassInstance = new TestBean();

    @Test
    public void injects_in_root() {
        check(() -> assertNotNull(testClassInstance.getEntityManager()));
    }

    @Test
    public void injects_in_cdi() {
        check(() -> assertNotNull(testClassInstance.getManagedBean().getEntityManager()));
    }

    @Test
    public void injects_in_ejb() {
        check(() -> assertNotNull(testClassInstance.getSessionBean().getEntityManager()));
    }

    private void check(Runnable r) {
        TestSetup.TestInstance context = testSetup.prepareTestInstance(
                "myInstance",
                testClassInstance,
                null
        );
        try {
            r.run();
        } finally {
            context.shutdown();
        }
    }

    @Singleton
    public static class SessionBean {
        @Inject
        private EntityManager entityManager;

        public EntityManager getEntityManager() {
            return entityManager;
        }
    }

    public static class EntityManagerProducer {
        @PersistenceContext(unitName = "testUnit")
        @Produces
        public EntityManager entityManager;
    }

    public static class ManagedBean {
        @Inject
        private EntityManager entityManager;

        public EntityManager getEntityManager() {
            return entityManager;
        }
    }

    public static class TestBean {
        @Inject
        private EntityManager entityManager;
        @Inject
        private ManagedBean managedBean;
        @Inject
        private SessionBean sessionBean;

        public EntityManager getEntityManager() {
            return entityManager;
        }

        public ManagedBean getManagedBean() {
            return managedBean;
        }

        public SessionBean getSessionBean() {
            return sessionBean;
        }
    }
}
