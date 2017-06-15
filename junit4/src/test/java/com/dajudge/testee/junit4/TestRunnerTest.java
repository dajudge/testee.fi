package com.dajudge.testee.junit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import static org.junit.Assert.assertNotNull;

@RunWith(TestEE.class)
public class TestRunnerTest {
    @Inject
    private ExampleBean exampleBean;

    @Test
    public void dependency_injection_works() {
        assertNotNull(exampleBean);
    }

    static class ExampleBean {

    }
}
