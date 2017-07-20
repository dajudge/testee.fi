package com.dajudge.testee.junit5;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(TestEE.class)
public class ExtensionTest {

    @Inject
    private CdiBean bean;

    @Test
    public void injects() {
        assertNotNull(bean);
    }

    static class CdiBean {

    }
}
