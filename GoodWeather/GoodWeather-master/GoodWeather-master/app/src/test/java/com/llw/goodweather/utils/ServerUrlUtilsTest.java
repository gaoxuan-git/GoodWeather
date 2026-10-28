package com.llw.goodweather.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ServerUrlUtilsTest {

    @Test
    public void normalizeAddsSchemeAndTrailingSlash() {
        assertEquals("http://10.110.61.220:5000/",
                ServerUrlUtils.normalize("10.110.61.220:5000"));
    }

    @Test
    public void normalizeKeepsHttps() {
        assertEquals("https://weather.example.com/",
                ServerUrlUtils.normalize("https://weather.example.com"));
    }

    @Test
    public void normalizeRejectsUnsupportedOrIncompleteUrls() {
        assertNull(ServerUrlUtils.normalize(""));
        assertNull(ServerUrlUtils.normalize("ftp://10.0.0.1:5000"));
        assertNull(ServerUrlUtils.normalize("http://"));
    }
}
