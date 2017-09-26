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
package fi.testee.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static fi.testee.interceptor.TestInterceptor.Type.AROUND_INVOKE;
import static fi.testee.interceptor.TestInterceptor.Type.POST_CONSTRUCT;
import static fi.testee.interceptor.TestInterceptor.Type.PRE_DESTROY;

@UseInterceptor
@Priority(5)
@Interceptor
public class TestInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(TestInterceptor.class);

    public enum Type {
        AROUND_INVOKE,
        POST_CONSTRUCT,
        PRE_DESTROY
    }

    public static class Invocation {
        public final Object target;
        public final Method method;
        public final Type type;
        public final StackTraceElement[] stacktrace;

        public Invocation(final Object target, final Method method, final Type type) {
            this.target = target;
            this.method = method;
            this.type = type;
            stacktrace = Thread.currentThread().getStackTrace();
        }
    }

    public static final List<Invocation> INVOCATIONS = new ArrayList<>();

    @PostConstruct
    public Object logPostConstruct(final InvocationContext invocationContext) throws Exception {
        LOG.info("PostConstruct: {} {}", invocationContext.getTarget(), invocationContext.getMethod());
        INVOCATIONS.add(new Invocation(invocationContext.getTarget(), invocationContext.getMethod(), POST_CONSTRUCT));
        return invocationContext.proceed();
    }

    @PreDestroy
    public Object logPreDestroy(final InvocationContext invocationContext) throws Exception {
        LOG.info("PreDestroy: {} {}", invocationContext.getTarget(), invocationContext.getMethod());
        INVOCATIONS.add(new Invocation(invocationContext.getTarget(), invocationContext.getMethod(), PRE_DESTROY));
        return invocationContext.proceed();
    }

    @AroundInvoke
    public Object logMethodEntry(final InvocationContext invocationContext) throws Exception {
        LOG.info("AroundInvoke: {} {}", invocationContext.getTarget(), invocationContext.getMethod());
        if (invocationContext.getMethod().getDeclaringClass() != Object.class) {
            INVOCATIONS.add(new Invocation(invocationContext.getTarget(), invocationContext.getMethod(), AROUND_INVOKE));
        }
        return invocationContext.proceed();
    }
}
