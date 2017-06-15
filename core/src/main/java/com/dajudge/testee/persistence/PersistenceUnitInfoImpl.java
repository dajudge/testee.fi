package com.dajudge.testee.persistence;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import java.net.URL;
import java.util.List;
import java.util.Properties;

/**
 * Implementation of {@link PersistenceUnitInfo}.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class PersistenceUnitInfoImpl implements PersistenceUnitInfo {
    private final String provider;
    private final String name;
    private final PersistenceUnitTransactionType transactionType;
    private final Properties properties;

    /**
     * Constructor.
     *
     * @param provider        provider class name.
     * @param name            the persistence unit name.
     * @param transactionType the transaction type.
     * @param properties      the properties.
     */
    public PersistenceUnitInfoImpl(
            final String provider,
            final String name,
            final PersistenceUnitTransactionType transactionType,
            final Properties properties
    ) {
        this.provider = provider;
        this.name = name;
        this.transactionType = transactionType;
        this.properties = properties;
    }

    @Override
    public String getPersistenceUnitName() {
        return name;
    }

    @Override
    public String getPersistenceProviderClassName() {
        return provider;
    }

    @Override
    public PersistenceUnitTransactionType getTransactionType() {
        return transactionType;
    }

    @Override
    public DataSource getJtaDataSource() {
        return null;
    }

    @Override
    public DataSource getNonJtaDataSource() {
        return null;
    }

    @Override
    public List<String> getMappingFileNames() {
        return null;
    }

    @Override
    public List<URL> getJarFileUrls() {
        return null;
    }

    @Override
    public URL getPersistenceUnitRootUrl() {
        return null;
    }

    @Override
    public List<String> getManagedClassNames() {
        return null;
    }

    @Override
    public boolean excludeUnlistedClasses() {
        return false;
    }

    @Override
    public SharedCacheMode getSharedCacheMode() {
        return null;
    }

    @Override
    public ValidationMode getValidationMode() {
        return null;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    @Override
    public String getPersistenceXMLSchemaVersion() {
        return null;
    }

    @Override
    public ClassLoader getClassLoader() {
        return null;
    }

    @Override
    public void addTransformer(final ClassTransformer transformer) {

    }

    @Override
    public ClassLoader getNewTempClassLoader() {
        return null;
    }
}
