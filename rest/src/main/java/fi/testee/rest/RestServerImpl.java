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
import fi.testee.spi.Releaser;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.common.scopes.SimpleContainerScope;
import org.eclipse.jetty.websocket.common.scopes.WebSocketContainerScope;
import org.eclipse.jetty.websocket.jsr356.server.AnnotatedServerEndpointConfig;
import org.eclipse.jetty.websocket.jsr356.server.ServerContainer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerEndpoint;
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
    private static final Class<? extends Annotation>[] JAX_RS_MANAGED_CLASSES = new Class[]{Path.class, Provider.class};
    private static final Class<? extends Annotation>[] WEBSOCKET_MANAGED_CLASSES = new Class[]{ServerEndpoint.class};

    private static final String PREFIX = RestServerImpl.class.getName();
    static final String ATTR_CLASSES = PREFIX + ".classes";
    static final String ATTR_DI = PREFIX + ".di";

    private final Releaser releaser = new Releaser();
    private final AnnotationScanner annotationScanner;
    private final DependencyInjection dependencyInjection;
    private final Collection<StaticResourceResolver> staticResourceResolvers;

    private Server server;

    public RestServerImpl(
            final AnnotationScanner annotationScanner,
            final DependencyInjection dependencyInjection,
            final Collection<StaticResourceResolver> staticResourceResolvers
    ) {
        this.annotationScanner = annotationScanner;
        this.dependencyInjection = dependencyInjection;
        this.staticResourceResolvers = staticResourceResolvers;
    }

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
            final HandlerCollection handlerCollection = new HandlerCollection();
            server.setHandler(handlerCollection);
            createJerseyHandler(handlerCollection);
            createWebsocketHandler(handlerCollection);
            createStaticHandler(handlerCollection);
            server.start();
        } catch (final Exception e) {
            throw new TestEEfiException("Failed to start Jetty", e);
        }
    }

    private void createStaticHandler(final HandlerCollection handlerCollection) {
        final ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/static");
        context.addServlet(new ServletHolder(new StaticServlet(staticResourceResolvers)), "/*");
        handlerCollection.addHandler(context);
    }


    private void createWebsocketHandler(final HandlerCollection handlerCollection) {
        // https://github.com/jetty-project/embedded-jetty-websocket-examples/blob/master/javax.websocket-example/src/main/java/org/eclipse/jetty/demo/EventServer.java
        try {
            final Set<Class<?>> managedClasses = collectAnnotated(WEBSOCKET_MANAGED_CLASSES);
            final ServletContextHandler context = initContext("/websockets", managedClasses);
            handlerCollection.addHandler(context);
            final ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(context);
            managedClasses.forEach(c -> registerEndpoint(wscontainer, c));
        } catch (final ServletException e) {
            throw new TestEEfiException("Failed to initialize websockets", e);
        }
    }

    private void registerEndpoint(final ServerContainer wscontainer, final Class<?> endpointClass) {
        try {
            final ServerEndpoint annotation = endpointClass.getAnnotation(ServerEndpoint.class);
            final WebSocketContainerScope scope = new SimpleContainerScope(WebSocketPolicy.newServerPolicy());
            final AnnotatedServerEndpointConfig endpointConfig = new AnnotatedServerEndpointConfig(
                    scope,
                    endpointClass,
                    annotation,
                    null
            ) {
                @Override
                public Configurator getConfigurator() {
                    return new DelegatingConfigurator(super.getConfigurator()) {
                        @Override
                        public <T> T getEndpointInstance(
                                final Class<T> endpointClass
                        ) throws InstantiationException {
                            return dependencyInjection.getInstanceOf(endpointClass, releaser);
                        }
                    };
                }
            };
            wscontainer.addEndpoint(endpointConfig);
        } catch (final DeploymentException e) {
            throw new TestEEfiException("Failed to register websocket enpoint", e);
        }
    }

    private ServletContextHandler createJerseyHandler(final HandlerCollection handlerCollection) {
        // http://nikgrozev.com/2014/10/16/rest-with-embedded-jetty-and-jersey-in-a-single-jar-step-by-step/
        final Set<Class<?>> managedClasses = collectAnnotated(JAX_RS_MANAGED_CLASSES);
        final ServletContextHandler context = initContext("/rest", managedClasses);
        handlerCollection.addHandler(context);
        final ResourceConfig config = initConfig(managedClasses);
        annotationScanner.scanFor(ApplicationPath.class).forEach(appClass -> {
            final String path = appClass.getAnnotation(ApplicationPath.class).value();
            final String pathSpec = (path.startsWith("/") ? "" : "/") + path + "/*";
            LOG.debug("Mapping rest application {} to {}", appClass.getName(), pathSpec);
            context.addServlet(new ServletHolder(new JerseyServlet(config)), pathSpec);
        });
        return context;
    }

    private ResourceConfig initConfig(final Set<Class<?>> managedClasses) {
        final ResourceConfig config = new ResourceConfig();
        managedClasses.forEach(config::register);
        return config;
    }

    private ServletContextHandler initContext(final String contextPath, final Set<Class<?>> classes) {
        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath(contextPath);
        context.setAttribute(ATTR_CLASSES, classes);
        context.setAttribute(ATTR_DI, dependencyInjection);
        return context;
    }

    private Set<Class<?>> collectAnnotated(Class<? extends Annotation>[] annotations) {
        return Stream.of(annotations)
                .map(annotationScanner::scanFor)
                .flatMap(Collection::stream)
                .collect(toSet());
    }

    public void shutdown() {
        releaser.release();
        if (server != null) {
            try {
                server.stop();
            } catch (final Exception e) {
                throw new TestEEfiException("Failed to stop Jetty", e);
            }
        }
    }
}
