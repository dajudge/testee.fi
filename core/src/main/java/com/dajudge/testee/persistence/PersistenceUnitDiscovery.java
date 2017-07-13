package com.dajudge.testee.persistence;

import com.dajudge.testee.classpath.ClasspathResource;
import com.dajudge.testee.classpath.JavaArchive;
import com.dajudge.testee.deployment.BeanArchiveDiscovery;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static javax.xml.xpath.XPathConstants.NODESET;
import static javax.xml.xpath.XPathConstants.STRING;

/**
 * Discovers persistence units on the classpath. Thread safe.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class PersistenceUnitDiscovery {
    private static final DocumentBuilder BUILDER = createDocumentBuilder();
    private Map<String, PersistenceUnitInfoImpl> units;
    private BeanArchiveDiscovery beanArchiveDiscovery;

    public PersistenceUnitDiscovery(final BeanArchiveDiscovery beanArchiveDiscovery) {
        this.beanArchiveDiscovery = beanArchiveDiscovery;
    }

    private static DocumentBuilder createDocumentBuilder() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Failed to initialize " + DocumentBuilder.class.getName(), e);
        }
    }

    private Collection<PersistenceUnitInfoImpl> unitsFrom(
            final JavaArchive archive,
            final ClasspathResource xml
    ) {
        try {
            final Document doc = BUILDER.parse(new ByteArrayInputStream(xml.getBytes()));
            final Element root = doc.getDocumentElement();
            final NodeList units = (NodeList) xpath().evaluate("//persistence/persistence-unit", root, NODESET);

            final Collection<PersistenceUnitInfoImpl> ret = new HashSet<>();
            for (int i = 0; i < units.getLength(); i++) {
                final PersistenceUnitInfoImpl info = addUnit(archive, (Element) units.item(i));
                ret.add(info);
            }
            return ret;
        } catch (final XPathExpressionException | SAXException | IOException e) {
            throw new RuntimeException("Failed to read persistence.xml", e);
        }
    }

    private XPath xpath() {
        return XPathFactory.newInstance().newXPath();
    }

    private PersistenceUnitInfoImpl addUnit(
            final JavaArchive archive,
            final Element unit
    ) throws XPathExpressionException {
        final XPath xpath = xpath();
        final String name = unit.getAttribute("name");
        final String transactionType = unit.getAttribute("transaction-type");
        final String provider = (String) xpath.evaluate("provider/text()", unit, STRING);
        final String dataSource = (String) xpath.evaluate("jta-data-source/text()", unit, STRING);
        final NodeList props = (NodeList) xpath.evaluate("properties/property", unit, NODESET);
        final Properties properties = new Properties();
        for (int i = 0; i < props.getLength(); i++) {
            final Element prop = (Element) props.item(i);
            final String propName = prop.getAttribute("name");
            final String propValue = prop.getAttribute("value");
            properties.put(propName, propValue);
        }
        return new PersistenceUnitInfoImpl(
                provider,
                name,
                PersistenceUnitTransactionType.valueOf(transactionType),
                properties
        );
    }

    public synchronized PersistenceUnitInfoImpl findByUnitName(final String unitName) {
        if (units == null) {
            units = discover();
        }
        return units.get(unitName);
    }

    private Map<String, PersistenceUnitInfoImpl> discover() {
        return beanArchiveDiscovery.getBeanArchives().stream()
                .map(it -> new ImmutablePair<>(it, it.findResource("META-INF/persistence.xml")))
                .filter(it -> it.getRight() != null)
                .map(it -> unitsFrom(it.getLeft(), it.getRight()))
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(
                        it -> it.getPersistenceUnitName(),
                        it -> it
                ));
    }
}
