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
package fi.testee.jpa;

import fi.testee.spi.PersistenceUnitPropertyContributor;
import fi.testee.classpath.ClasspathResource;
import fi.testee.classpath.JavaArchive;
import fi.testee.deployment.BeanArchiveDiscovery;
import fi.testee.exceptions.TestEEfiException;
import fi.testee.utils.ProxyUtils;
import fi.testee.utils.UrlUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static javax.xml.xpath.XPathConstants.NODESET;
import static javax.xml.xpath.XPathConstants.STRING;

/**
 * Discovers persistence units on the classpath. Thread safe.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class PersistenceUnitDiscovery {
    private static final Logger LOG = LoggerFactory.getLogger(PersistenceUnitDiscovery.class);
    private static final DocumentBuilder BUILDER = createDocumentBuilder();

    private final PersistenceUnitPropertyContributor contributor;
    private final BeanArchiveDiscovery beanArchiveDiscovery;
    private final ResourceInjectionServices resourceInjectionServices;

    private Map<String, ? extends PersistenceUnitInfo> units;

    public PersistenceUnitDiscovery(
            final BeanArchiveDiscovery beanArchiveDiscovery,
            final ResourceInjectionServices resourceInjectionServices,
            final PersistenceUnitPropertyContributor contributor
    ) {
        this.beanArchiveDiscovery = beanArchiveDiscovery;
        this.resourceInjectionServices = resourceInjectionServices;
        this.contributor = contributor;
    }

    private static DocumentBuilder createDocumentBuilder() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new TestEEfiException("Failed to initialize " + DocumentBuilder.class.getName(), e);
        }
    }

    private Collection<PersistenceUnitInfoImpl> unitsFrom(
            final JavaArchive archive,
            final ClasspathResource xml
    ) {
        try {
            final Document doc = BUILDER.parse(new ByteArrayInputStream(xml.getBytes()));
            final Element root = doc.getDocumentElement();
            final NodeList unitNodes = (NodeList) xpath().evaluate(
                    "//persistence/persistence-unit",
                    root,
                    NODESET
            );
            final Collection<PersistenceUnitInfoImpl> ret = new HashSet<>();
            for (int i = 0; i < unitNodes.getLength(); i++) {
                ret.add(createUnitInfo(archive, (Element) unitNodes.item(i)));
            }
            return ret;
        } catch (final XPathExpressionException | SAXException | IOException e) {
            throw new TestEEfiException("Failed to read persistence.xml", e);
        }
    }

    private XPath xpath() {
        return XPathFactory.newInstance().newXPath();
    }

    private PersistenceUnitInfoImpl createUnitInfo(
            final JavaArchive archive,
            final Element unit
    ) throws XPathExpressionException {
        final XPath xpath = xpath();
        final String name = unit.getAttribute("name");
        LOG.debug("Initializing persistence unit info for {}", name);
        final String transactionType = unit.getAttribute("transaction-type");
        final String provider = stringTag(unit, xpath, "provider");
        final String jtaDataSourceName = stringTag(unit, xpath, "jta-data-source");
        final String excludeUnlistedClassesString = stringTag(unit, xpath, "exclude-unlisted-classes");
        final NodeList props = (NodeList) xpath.evaluate("properties/property", unit, NODESET);
        final Properties properties = new Properties();
        for (int i = 0; i < props.getLength(); i++) {
            final Element prop = (Element) props.item(i);
            final String propName = prop.getAttribute("name");
            final String propValue = prop.getAttribute("value");
            properties.put(propName, propValue);
        }
        contributor.contribute(properties, provider);
        final List<URL> jarFileUrls = collectStringListElements(unit, xpath, "jar-file").stream()
                .map(UrlUtils::toUrl)
                .collect(toList());
        final List<String> mappingFileNames = collectStringListElements(unit, xpath, "mapping-file");
        final List<String> managedClassNames = collectStringListElements(unit, xpath, "class");
        boolean excludeUnlistedClasses = excludeUnlistedClassesString != null
                && Boolean.parseBoolean(excludeUnlistedClassesString);
        return new PersistenceUnitInfoImpl(
                archive.getURL(),
                provider,
                name + "/" + UUID.randomUUID(),
                name,
                PersistenceUnitTransactionType.valueOf(transactionType),
                ProxyUtils.lazy(() -> resolveDataSource(jtaDataSourceName), DataSource.class),
                properties,
                jarFileUrls,
                mappingFileNames,
                managedClassNames,
                excludeUnlistedClasses,
                getClass().getClassLoader()
        );
    }

    private String stringTag(
            final Element element,
            final XPath xpath,
            final String tagName
    ) throws XPathExpressionException {
        return (String) xpath.evaluate(tagName + "/text()", element, STRING);
    }

    private List<String> collectStringListElements(
            final Element unit,
            final XPath xpath,
            final String elementName
    ) throws XPathExpressionException {
        final NodeList nodes = (NodeList) xpath.evaluate(elementName, unit, NODESET);
        final List<String> strings = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            strings.add(nodes.item(i).getTextContent());
        }
        return strings;
    }

    private DataSource resolveDataSource(final String dataSourceName) {
        final Object result = resourceInjectionServices.registerResourceInjectionPoint(null, dataSourceName)
                .<DataSource>createResource()
                .getInstance();
        if (!(result instanceof DataSource)) {
            throw new TestEEfiException("The resolved container managed resource is not a DataSource: " + result);
        }
        return (DataSource) result;
    }

    public synchronized PersistenceUnitInfo findByUnitName(final String unitName) {
        if (units == null) {
            units = discover();
        }
        return units.get(unitName);
    }

    private Map<String, PersistenceUnitInfoImpl> discover() {
        return beanArchiveDiscovery.getBeanArchives().stream()
                .map(it -> new ImmutablePair<>(it, it.getClasspathEntry().findResource("META-INF/persistence.xml")))
                .filter(it -> it.getRight() != null)
                .map(it -> unitsFrom(it.getLeft().getClasspathEntry(), it.getRight()))
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(
                        PersistenceUnitInfoImpl::getActualPersistenceUnitName,
                        it -> it
                ));
    }
}
