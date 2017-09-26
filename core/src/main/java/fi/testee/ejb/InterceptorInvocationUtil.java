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
package fi.testee.ejb;

import fi.testee.exceptions.TestEEfiException;
import fi.testee.spi.ReleaseCallbackHandler;
import fi.testee.spi.Releaser;
import org.jboss.weld.context.CreationalContextImpl;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.interceptor.InvocationContext;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class InterceptorInvocationUtil {

    public static InterceptorChain getInterceptorChain(
            final ContextFactory contextFactory
    ) {
        return new InterceptorChain() {
            @Override
            public <T> Object invoke(
                    final Object target,
                    final Method method,
                    final Object[] args,
                    final ChainEnd<T> next,
                    final InterceptionType interceptionType,
                    final List<Interceptor<?>> interceptors
            ) throws Throwable {
                return processInterceptorChain(
                        interceptors,
                        target,
                        method,
                        args,
                        contextFactory,
                        next,
                        interceptionType
                );
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static <T> Object processInterceptorChain(
            final List<Interceptor<?>> chain,
            final Object target,
            final Method method,
            final Object[] args,
            final ContextFactory contextFactory,
            final InterceptorChain.ChainEnd<T> next,
            final InterceptionType interceptionType
    ) throws Throwable {
        if (chain.isEmpty()) {
            return next.invoke();
        }
        final Interceptor<Object> it = (Interceptor<Object>) chain.remove(0);
        final Releaser releaser = new Releaser();
        try {
            return intercept(
                    it,
                    it.create(contextFactory.create(it, releaser)),
                    target,
                    method,
                    args,
                    () -> processInterceptorChain(chain, target, method, args, contextFactory, next, interceptionType),
                    interceptionType
            );
        } finally {
            releaser.release();
        }
    }

    private static <T> Object intercept(
            final Interceptor<T> it,
            final T instance,
            final Object target,
            final Method method,
            final Object[] args,
            final Proceed proceed,
            final InterceptionType interceptionType
    ) throws Exception {
        return it.intercept(
                interceptionType,
                instance,
                context(target, interceptionType == InterceptionType.AROUND_INVOKE ? method : null, args, proceed)
        );
    }

    private static InvocationContext context(
            final Object target,
            final Method method,
            final Object[] args,
            final Proceed proceed
    ) {
        return new InvocationContext() {
            @Override
            public Object getTarget() {
                return target;
            }

            @Override
            public Object getTimer() {
                return null;
            }

            @Override
            public Method getMethod() {
                return method;
            }

            @Override
            public Constructor<?> getConstructor() {
                return null;
            }

            @Override
            public Object[] getParameters() {
                return args;
            }

            @Override
            public void setParameters(final Object[] params) {
                throw new UnsupportedOperationException("Changing parameters is not supported, yet");
            }

            @Override
            public Map<String, Object> getContextData() {
                return new HashMap<>();
            }

            @Override
            public Object proceed() throws Exception {
                try {
                    return proceed.run();
                } catch (final Exception e) {
                    throw e;
                } catch (final Throwable throwable) {
                    throw new TestEEfiException("Failed to process interceptor chain", throwable);
                }
            }
        };
    }

    private interface Proceed {
        Object run() throws Throwable;
    }

    public interface ContextFactory {
        <T> CreationalContextImpl<T> create(Contextual<T> ctx, ReleaseCallbackHandler releaser);
    }

    public static interface InterceptorChain {
        interface ChainEnd<T> {
            T invoke() throws Throwable;
        }

        <T> Object invoke(
                Object target,
                Method method,
                Object[] args,
                ChainEnd<T> next,
                InterceptionType interceptionType,
                List<Interceptor<?>> interceptors
        ) throws Throwable;
    }
}
