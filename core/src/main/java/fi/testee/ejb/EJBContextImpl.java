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

import javax.ejb.EJBContext;
import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.TimerService;
import javax.transaction.UserTransaction;
import java.security.Identity;
import java.security.Principal;
import java.util.Map;
import java.util.Properties;

public class EJBContextImpl implements EJBContext {
    @Override
    public EJBHome getEJBHome() throws IllegalStateException {
        return null;
    }

    @Override
    public EJBLocalHome getEJBLocalHome() throws IllegalStateException {
        return null;
    }

    @Override
    public Properties getEnvironment() {
        return null;
    }

    @Override
    public Identity getCallerIdentity() {
        return null;
    }

    @Override
    public Principal getCallerPrincipal() throws IllegalStateException {
        return null;
    }

    @Override
    public boolean isCallerInRole(Identity role) {
        return false;
    }

    @Override
    public boolean isCallerInRole(String roleName) throws IllegalStateException {
        return false;
    }

    @Override
    public UserTransaction getUserTransaction() throws IllegalStateException {
        return null;
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException {

    }

    @Override
    public boolean getRollbackOnly() throws IllegalStateException {
        return false;
    }

    @Override
    public TimerService getTimerService() throws IllegalStateException {
        return null;
    }

    @Override
    public Object lookup(String name) throws IllegalArgumentException {
        return null;
    }

    @Override
    public Map<String, Object> getContextData() {
        return null;
    }
}
