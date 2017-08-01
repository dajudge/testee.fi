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
package fi.testee.rest;

import fi.testee.exceptions.TestEEfiException;
import fi.testee.spi.AnnotationScanner;
import fi.testee.spi.DependencyInjection;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

/**
 * Implementation of the {@link RestServer}.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
class RestServerImpl implements RestServer {
    private static final Logger LOG = LoggerFactory.getLogger(RestServerImpl.class);
    @SuppressWarnings("unchecked")
    private static final Class<? extends Annotation>[] MANAGED_CLASSES = new Class[]{Path.class, Provider.class};

    private static final String PREFIX = RestServerImpl.class.getName();
    static final String ATTR_CLASSES = PREFIX + ".classes";
    static final String ATTR_DI = PREFIX + ".di";


    private Server server;

    @Resource(mappedName = "testeefi/setup/annotationScanner")
    private AnnotationScanner annotationScanner;
    @Resource(mappedName = "testeefi/instance/dependencyInjection")
    private DependencyInjection dependencyInjection;

    @Override
    public int getPort() {
        return ((ServerConnector) getServer().getConnectors()[0]).getLocalPort();
    }

    private Server getServer() {
        if (server == null) {
            createServer();
        }
        return server;
    }

    private void createServer() {

        try {
            server = new Server(0);
            server.setHandler(createJerseyHandler());
            server.start();
        } catch (final Exception e) {
            throw new TestEEfiException("Failed to start Jetty", e);
        }
    }

    private ServletContextHandler createJerseyHandler() {
        // http://nikgrozev.com/2014/10/16/rest-with-embedded-jetty-and-jersey-in-a-single-jar-step-by-step/
        final Set<Class<?>> managedClasses = getManagedClasses();
        final ServletContextHandler context = initContext("/rest", managedClasses);
        final ResourceConfig config = initConfig(managedClasses);
        annotationScanner.scanFor(ApplicationPath.class).forEach(appClass -> {
            final String path = appClass.getAnnotation(ApplicationPath.class).value();
            LOG.info("Mapping rest application {} to {}", appClass.getName(), path);
            context.addServlet(new ServletHolder(new JerseyServlet(config)), path + "/*");
        });
        return context;
    }

    private ResourceConfig initConfig(final Set<Class<?>> managedClasses) {
        final ResourceConfig config = new ResourceConfig();
        managedClasses.forEach(config::register);
        return config;
    }

    private ServletContextHandler initContext(final String contextPath, final Set<Class<?>> classes) {
        final ServletContextHandler context = new ServletContextHandler();
        context.setContextPath(contextPath);
        context.setAttribute(ATTR_CLASSES, classes);
        context.setAttribute(ATTR_DI, dependencyInjection);
        return context;
    }

    private Set<Class<?>> getManagedClasses() {
        return Stream.of(MANAGED_CLASSES)
                .map(annotationScanner::scanFor)
                .flatMap(Collection::stream)
                .collect(toSet());
    }

    @PostConstruct
    public void lolcats() {
        server = null;
    }

    @PreDestroy
    public void shutdown() {
        if (server != null) {
            try {
                server.stop();
            } catch (final Exception e) {
                throw new TestEEfiException("Failed to stop Jetty", e);
            }
        }
    }
}
