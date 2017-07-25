package fi.testee.services;

import fi.testee.exceptions.TestEEfiException;
import fi.testee.jpa.PersistenceUnitDiscovery;
import fi.testee.utils.ProxyUtils;
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
import javax.persistence.spi.PersistenceUnitInfo;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Injection of JPA persistence units. Thread safe.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class JpaInjectionServicesImpl implements JpaInjectionServices {
    private static final Logger LOG = LoggerFactory.getLogger(JpaInjectionServicesImpl.class);
    public static final Supplier<RuntimeException> NOT_SUPPORTED = () -> new UnsupportedOperationException("This is not implemented, yet");
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
        return registerPersistenceContextInjectionPoint(unitName);
    }

    public ResourceReferenceFactory<EntityManager> registerPersistenceContextInjectionPoint(final String unitName) {
        LOG.debug("Creating persistence context for unit '{}'", unitName);
        final PersistenceUnitInfo unit = getUnitByName(unitName);
        if (unit == null) {
            throw new IllegalStateException("Unknown persistence unit: " + unitName);
        }
        final EntityManager entityManager = ProxyUtils.lazy(
                () -> getEntityManager(unit),
                EntityManager.class
        );
        return () -> new SimpleResourceReference<>(safeguard(entityManager));
    }

    private EntityManager safeguard(final EntityManager entityManager) {
        return (EntityManager) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{EntityManager.class},
                (proxy, method, args) -> {
                    try {
                        if ("close".equals(method.getName())) {
                            throw new TestEEfiException("Naughty developer. "
                                    + "One does not simply close the entity manager.");
                        } else if ("getTransaction".equals(method.getName())) {
                            throw new TestEEfiException("Naughty developer. "
                                    + "One does not simply access the transaction.");
                        }
                        return method.invoke(entityManager, args);
                    } catch (final InvocationTargetException e) {
                        throw e.getTargetException();
                    }
                }
        );
    }

    private PersistenceUnitInfo getUnitByName(String unitName) {
        return persistenceUnitDiscovery.findByUnitName(unitName);
    }

    private synchronized EntityManager getEntityManager(final PersistenceUnitInfo unit) {
        if (entityManagers.containsKey(unit.getPersistenceUnitName())) {
            return entityManagers.get(unit.getPersistenceUnitName());
        }
        final String providerClassName = unit.getPersistenceProviderClassName();
        try {
            final PersistenceProvider provider = (PersistenceProvider) Class.forName(providerClassName).newInstance();
            final EntityManagerFactory emf = provider.createContainerEntityManagerFactory(unit, new HashMap());
            final EntityManager entityManager = emf.createEntityManager();
            entityManagers.put(unit.getPersistenceUnitName(), entityManager);
            entityManager.getTransaction().begin();
            return entityManager;
        } catch (final IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            throw new TestEEfiException("Failed to load persistence provider class " + providerClassName, e);
        }
    }

    @Override
    public ResourceReferenceFactory<EntityManagerFactory> registerPersistenceUnitInjectionPoint(
            final InjectionPoint injectionPoint
    ) {
        throw NOT_SUPPORTED.get();
    }

    @Override
    public EntityManager resolvePersistenceContext(
            final InjectionPoint injectionPoint
    ) {
        throw NOT_SUPPORTED.get();
    }

    @Override
    public EntityManagerFactory resolvePersistenceUnit(
            final InjectionPoint injectionPoint
    ) {
        throw NOT_SUPPORTED.get();
    }

    @Override
    public void cleanup() {
        entityManagers.values().forEach(it -> {
            if (it.isOpen()) {
                it.close();
            }
        });
    }

    public void flush() {
        entityManagers.values().forEach(it -> {
            if (it.isOpen() && it.getTransaction().isActive()) {
                it.flush();
            }
        });
    }
}
