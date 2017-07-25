package fi.testee.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProxyUtilsTest {
    @Mock
    private Function mock;

    @Test
    public void proxying_works() {
        when(mock.apply("a")).thenReturn("b");
        final Function trace = ProxyUtils.trace(mock, Function.class);
        assertEquals("b", trace.apply("a"));
    }

    static class MyException extends RuntimeException {

    }

    @Test(expected = MyException.class)
    public void proxying_relays_exceptions() {
        when(mock.apply("a")).thenThrow(new MyException());
        final Function trace = ProxyUtils.trace(mock, Function.class);
        trace.apply("a");
    }
}
