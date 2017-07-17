package com.dajudge.testee.utils;

import com.dajudge.testee.exceptions.TesteeException;
import com.dajudge.testee.utils.UrlUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UrlUtilsTest {
    @Test
    public void toUrl_succeeds_with_valid_url() {
        assertEquals("http://www.example.org", UrlUtils.toUrl("http://www.example.org").toString());
    }

    @Test(expected = TesteeException.class)
    public void toUrl_fails_on_invalid_url() {
        UrlUtils.toUrl("lolcats");
    }
}
