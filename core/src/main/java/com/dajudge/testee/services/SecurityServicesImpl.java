package com.dajudge.testee.services;

import org.jboss.weld.security.spi.SecurityServices;

import java.security.Principal;

/**
 * Implementation of {@link SecurityServices}.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class SecurityServicesImpl implements SecurityServices {
    @Override
    public Principal getPrincipal() {
        return null;
    }

    @Override
    public void cleanup() {

    }
}
