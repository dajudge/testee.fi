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

import fi.testee.spi.ResourceProvider;

import javax.ejb.EJBContext;
import javax.enterprise.inject.spi.InjectionPoint;

public class EjbResourceProvider implements ResourceProvider {
    private EJBContext ejbContext = new EJBContextImpl();

    @Override
    public Object resolve(final InjectionPoint injectionPoint) {
        if(injectionPoint.getAnnotated().getBaseType() == EJBContext.class) {
            return ejbContext;
        }
        return null;
    }

    @Override
    public Object resolve(String jndiName, String mappedName) {
        return null;
    }

    @Override
    public void shutdown(boolean rollback) {

    }

}
