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
package fi.testee.services;

import fi.testee.exceptions.TestEEfiException;
import fi.testee.jpa.PersistenceUnitDiscovery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceProperty;
import javax.persistence.SynchronizationType;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;
import java.lang.annotation.Annotation;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JpaInjectionServicesTest {

    private static final String UNIT_NAME = "lolcats";
    @Mock
    private EntityTransaction transaction;
    @Mock
    private EntityManager entityManager;
    @Mock
    private EntityManagerFactory factory;
    @Mock
    private MyPersistenceUnitInfo info;
    @Mock
    private PersistenceUnitDiscovery discovery;

    private JpaInjectionServicesImpl subject;

    @Before
    public void setup() {
        subject = new JpaInjectionServicesImpl(discovery);
        when(discovery.findByUnitName(UNIT_NAME)).thenReturn(info);
        when(info.getFactory()).thenReturn(factory);
        when(info.getPersistenceProviderClassName()).thenReturn(MyProvider.class.getName());
        when(factory.createEntityManager()).thenReturn(entityManager);
        when(entityManager.getTransaction()).thenReturn(transaction);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void not_supports_resolvePersistenceUnit() {
        subject.resolvePersistenceUnit(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void not_supports_resolvePersistenceContext() {
        subject.resolvePersistenceContext((InjectionPoint)null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void not_supports_registerPersistenceUnitInjectionPoint() {
        subject.registerPersistenceUnitInjectionPoint(null);
    }

    @Test
    public void creates_entity_manager_lazy() {
        final EntityManager em = getEntityManager();
        assertNotNull(em);
        verify(factory, never()).createEntityManager();
        em.isOpen();
        verify(factory).createEntityManager();
    }

    @Test(expected = IllegalStateException.class)
    public void fails_when_unit_is_unknown() {
        getEntityManager(UNIT_NAME + "_not_known");
    }

    @Test
    public void caches_entityManagers() {
        getEntityManager().isOpen();
        getEntityManager().isOpen();
        verify(factory, times(1)).createEntityManager();
    }

    @Test
    public void autostarts_transaction() {
        getEntityManager().isOpen();
        verify(transaction).begin();
    }

    @Test(expected = TestEEfiException.class)
    public void handles_instantiation_failure() {
        when(info.getPersistenceProviderClassName()).thenReturn("lolcats");
        getEntityManager().isOpen();
    }

    @Test(expected = TestEEfiException.class)
    public void prevents_transaction_access() {
        getEntityManager().getTransaction();
    }

    @Test(expected = TestEEfiException.class)
    public void prevents_close() {
        getEntityManager().close();
    }

    @Test
    public void not_flushes_when_not_open() {
        tryFlush(false, true);
        verify(entityManager, never()).flush();
    }

    @Test
    public void not_flushes_when_not_in_transaction() {
        tryFlush(true, false);
        verify(entityManager, never()).flush();
    }

    @Test
    public void flushes_when_transaction_active() {
        tryFlush(true, true);
        verify(entityManager).flush();
    }

    @Test
    public void not_closes_when_not_open() {
        when(entityManager.isOpen()).thenReturn(false);
        getEntityManager().isOpen();
        subject.cleanup();
        verify(entityManager, never()).close();
    }

    @Test
    public void closes_when_open() {
        when(entityManager.isOpen()).thenReturn(true);
        getEntityManager().isOpen();
        subject.cleanup();
        verify(entityManager).close();
    }

    private void tryFlush(boolean open, boolean txActive) {
        getEntityManager().isOpen();
        when(entityManager.isOpen()).thenReturn(open);
        when(transaction.isActive()).thenReturn(txActive);
        subject.flush();
    }

    private EntityManager getEntityManager() {
        return getEntityManager(UNIT_NAME);
    }

    private EntityManager getEntityManager(String unitName) {
        return subject
                .registerPersistenceContextInjectionPoint(persistenceContext(unitName))
                .createResource()
                .getInstance();
    }

    private InjectionPoint persistenceContext(String unitName) {
        final InjectionPoint ip = mock(InjectionPoint.class);
        final Annotated annotated = mock(Annotated.class);
        when(ip.getAnnotated()).thenReturn(annotated);
        final PersistenceContext annotation = new PersistenceContext() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public String name() {
                return null;
            }

            @Override
            public String unitName() {
                return unitName;
            }

            @Override
            public PersistenceContextType type() {
                return null;
            }

            @Override
            public SynchronizationType synchronization() {
                return null;
            }

            @Override
            public PersistenceProperty[] properties() {
                return new PersistenceProperty[0];
            }
        };
        when(annotated.getAnnotation(PersistenceContext.class)).thenReturn(annotation);
        return ip;
    }

    public interface MyPersistenceUnitInfo extends PersistenceUnitInfo {
        EntityManagerFactory getFactory();
    }

    public static class MyProvider implements PersistenceProvider {

        @Override
        public EntityManagerFactory createEntityManagerFactory(String emName, Map map) {
            return null;
        }

        @Override
        public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map map) {
            return ((MyPersistenceUnitInfo) info).getFactory();
        }

        @Override
        public void generateSchema(PersistenceUnitInfo info, Map map) {

        }

        @Override
        public boolean generateSchema(String persistenceUnitName, Map map) {
            return false;
        }

        @Override
        public ProviderUtil getProviderUtil() {
            return null;
        }
    }
}
