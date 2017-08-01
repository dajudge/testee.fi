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

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.ws.rs.core.Application;
import java.io.IOException;

/**
 * Synchronized {@link ServletContainer}.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
class JerseyServlet extends ServletContainer {

    public JerseyServlet(final ResourceConfig resourceConfig) {
        super(resourceConfig);
    }

    @Override
    public void service(
            final ServletRequest req,
            final ServletResponse res
    ) throws ServletException, IOException {
        synchronized (this) {
            super.service(req, res);
        }
    }
}
