package com.dajudge.testee.services;

import com.dajudge.testee.exceptions.TesteeException;
import com.dajudge.testee.persistence.PersistenceUnitDiscovery;
import com.dajudge.testee.persistence.PersistenceUnitInfoImpl;
import org.jboss.weld.injection.spi.JpaInjectionServices;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;
import org.jboss.weld.injection.spi.helpers.SimpleResourceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.InjectionPoint;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.spi.PersistenceProvider;
import java.util.HashMap;
import java.util.Map;

import static com.dajudge.testee.utils.ProxyUtils.lazy;

/**
 * Injection of JPA persistence units. Thread safe.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class JpaInjectionServicesImpl implements JpaInjectionServices {
    private static final Logger LOG = LoggerFactory.getLogger(JpaInjectionServicesImpl.class);
    private PersistenceUnitDiscovery persistenceUnitDiscovery;
    private Map<String, EntityManager> entityManagers = new HashMap<>();

    public JpaInjectionServicesImpl(final PersistenceUnitDiscovery persistenceUnitDiscovery) {
        this.persistenceUnitDiscovery = persistenceUnitDiscovery;
    }

    @Override
    public ResourceReferenceFactory<EntityManager> registerPersistenceContextInjectionPoint(
            final InjectionPoint injectionPoint
    ) {
        final PersistenceContext persistenceContext = injectionPoint
                .getAnnotated()
                .getAnnotation(PersistenceContext.class);
        final String unitName = persistenceContext.unitName();
        LOG.info("Creating persistence context for unit '{}'", unitName);
        final PersistenceUnitInfoImpl unit = getUnitByName(unitName);
        if (unit == null) {
            return null;
        }
        final EntityManager entityManager = lazy(
                () -> getEntityManager(unit),
                EntityManager.class
        );
        return () -> new SimpleResourceReference<>(entityManager);
    }

    private PersistenceUnitInfoImpl getUnitByName(String unitName) {
        return persistenceUnitDiscovery.findByUnitName(unitName);
    }

    private synchronized EntityManager getEntityManager(final PersistenceUnitInfoImpl unit) {
        if (entityManagers.containsKey(unit.getPersistenceUnitName())) {
            return entityManagers.get(unit.getPersistenceUnitName());
        }
        final String providerClassName = unit.getPersistenceProviderClassName();
        try {
            final PersistenceProvider provider = (PersistenceProvider) Class.forName(providerClassName).newInstance();
            final EntityManagerFactory emf = provider.createContainerEntityManagerFactory(unit, new HashMap());
            final EntityManager entityManager = emf.createEntityManager();
            entityManagers.put(unit.getPersistenceUnitName(), entityManager);
            return entityManager;
        } catch (final IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            throw new TesteeException("Failed to load persistence provider class " + providerClassName, e);
        }
    }

    @Override
    public ResourceReferenceFactory<EntityManagerFactory> registerPersistenceUnitInjectionPoint(
            final InjectionPoint injectionPoint
    ) {
        throw new UnsupportedOperationException("This is not implemented, yet");
    }

    @Override
    public EntityManager resolvePersistenceContext(
            final InjectionPoint injectionPoint
    ) {
        throw new UnsupportedOperationException("This is not implemented, yet");
    }

    @Override
    public EntityManagerFactory resolvePersistenceUnit(
            final InjectionPoint injectionPoint
    ) {
        throw new UnsupportedOperationException("This is not implemented, yet");
    }

    @Override
    public void cleanup() {
        entityManagers.values().forEach(it -> it.close());
    }
}
