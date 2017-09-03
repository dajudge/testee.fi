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
package fi.testee.mockito;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Interceptor
@UseInterceptor
@Priority(Interceptor.Priority.APPLICATION)
public class TestInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(TestInterceptor.class);

    @AroundInvoke
    public Object logMethodEntry(final InvocationContext invocationContext) throws Exception {
        LOG.info("AroundInvoke: {} {}", invocationContext.getTarget(), invocationContext.getMethod());
        return invocationContext.proceed();
    }
}
