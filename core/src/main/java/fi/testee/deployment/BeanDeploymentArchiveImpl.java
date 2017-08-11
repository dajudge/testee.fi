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
package fi.testee.deployment;

import fi.testee.classpath.ClasspathResource;
import fi.testee.classpath.JavaArchive;
import fi.testee.exceptions.TestEEfiException;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.xml.BeansXmlHandler;
import org.jboss.weld.xml.XmlSchema;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.function.Supplier;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static org.jboss.weld.bootstrap.spi.BeansXml.EMPTY_BEANS_XML;

/**
 * Implementation of a {@link BeanDeploymentArchive} (or short <i>BDA</i>), which basically links a {@link JavaArchive}
 * to a {@link DeploymentImpl deployment}.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class BeanDeploymentArchiveImpl implements BeanDeploymentArchive {
    private final ServiceRegistry serviceRegistry;
    private final BeanArchive beanArchive;
    private final Supplier<Collection<BeanDeploymentArchive>> archivesSupplier;
    private final BeansXml beansXml;

    public BeanDeploymentArchiveImpl(
            final ServiceRegistry serviceRegistry,
            final BeanArchive beanArchive,
            final Supplier<Collection<BeanDeploymentArchive>> archivesSupplier
    ) {
        this.serviceRegistry = serviceRegistry;
        this.beanArchive = beanArchive;
        this.archivesSupplier = archivesSupplier;

        final ClasspathResource resource = beanArchive.getClasspathEntry().findResource("META-INF/beans.xml");
        beansXml = resource == null ? EMPTY_BEANS_XML : readBeansXml(beanArchive, resource);
    }

    private BeansXml readBeansXml(BeanArchive beanArchive, ClasspathResource resource) {
        try {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(true);
            factory.setNamespaceAware(true);
            final SAXParser parser = factory.newSAXParser();
            final InputSource source = new InputSource(new ByteArrayInputStream(resource.getBytes()));
            if (source.getByteStream().available() == 0) {
                // The file is just acting as a marker file
                return EMPTY_BEANS_XML;
            }
            final BeansXmlHandler handler = new BeansXmlHandler(beanArchive.getClasspathEntry().getURL());
            parser.setProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
            parser.setProperty("http://java.sun.com/xml/jaxp/properties/schemaSource", loadXsds());
            parser.parse(source, handler);
            return handler.createBeansXml();
        } catch (final SAXException | ParserConfigurationException | IOException e) {
            throw new TestEEfiException(
                    "Failed to parse META-INF/beans.xml in " + beanArchive.getClasspathEntry().getURL(),
                    e
            );
        }
    }

    private static InputSource[] loadXsds() {
        return stream(XmlSchema.values())
                .map(schema -> loadXsd(schema.getFileName(), schema.getClassLoader()))
                .filter(Objects::nonNull)
                .collect(toSet())
                .toArray(new InputSource[]{});
    }


    private static InputSource loadXsd(final String name, final ClassLoader classLoader) {
        final InputStream in = classLoader.getResourceAsStream(name);
        if (in == null) {
            return null;
        } else {
            return new InputSource(in);
        }
    }

    @Override
    public Collection<BeanDeploymentArchive> getBeanDeploymentArchives() {
        return archivesSupplier.get();
    }

    @Override
    public Collection<String> getBeanClasses() {
        return beanArchive.getBeanClasses();
    }

    @Override
    public BeansXml getBeansXml() {
        return beansXml;
    }

    @Override
    public Collection<EjbDescriptor<?>> getEjbs() {
        return new HashSet<>(beanArchive.getEjbs());
    }

    @Override
    public ServiceRegistry getServices() {
        return serviceRegistry;
    }

    @Override
    public String getId() {
        return beanArchive.getClasspathEntry().getURL().toExternalForm();
    }
}
