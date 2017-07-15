package com.dajudge.testee.utils;

import com.dajudge.testee.exceptions.TesteeException;
import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.split;

/**
 * Utilities around URLs.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public final class UrlUtils {
    private static final String COMPOSITE_PREFIX = "file:/:testeeComposite/";
    public static final String COMPOSITE_SEPARATOR_CHAR = "|";

    private UrlUtils() {
    }

    /**
     * URL from string throwing a {@link TesteeException} instead of checked ones.
     *
     * @param string the string to convert to a URL.
     * @return the URL.
     */
    public static URL toUrl(final String string) {
        try {
            return new URL(string);
        } catch (final MalformedURLException e) {
            throw new TesteeException("Malformed URL", e);
        }
    }

    /**
     * Builds a composite URL from which the original URLs can be recovered via {@link #splitCompositeUrl}.
     *
     * @param urls the urls to compose to a single URL.
     * @return the composite URL.
     */
    public static URL createCompositeUrl(final List<URL> urls) {
        final List<String> strings = urls.stream().map(Object::toString).collect(toList());
        final StringBuilder sep = new StringBuilder("|");
        while (contains(strings, sep.toString())) {
            sep.append(COMPOSITE_SEPARATOR_CHAR);
        }
        final String suffixes = strings.stream().collect(Collectors.joining(sep.toString()));
        return UrlUtils.toUrl(COMPOSITE_PREFIX + sep.length() + "/" + suffixes);
    }

    /**
     * Splits a composite URL created with {@link #createCompositeUrl(List)} into its original parts.
     *
     * @param composite the composite URL to split.
     * @return the original URLs.
     */
    public static List<URL> splitCompositeUrl(final URL composite) {
        final String remainder = composite.toString().substring(COMPOSITE_PREFIX.length());
        final int sepIdx = remainder.indexOf('/');
        final int sepLen = Integer.parseInt(remainder.substring(0, sepIdx));
        final String separator = StringUtils.repeat(COMPOSITE_SEPARATOR_CHAR, sepLen);
        final String composed = remainder.substring(sepIdx + 1);
        return Arrays.stream(split(composed, separator))
                .map(UrlUtils::toUrl)
                .collect(toList());
    }

    private static boolean contains(final Collection<String> strings, final String s) {
        for (final String string : strings) {
            if (string.contains(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Indicates if a given URL is a composite URL created via {@link #createCompositeUrl(List)}.
     *
     * @param url the URL to check.
     * @return {@code true} if the URL is a composite URL, {@code false} otherwise.
     */
    public static boolean isCompositeURL(final URL url) {
        return url.toString().startsWith(COMPOSITE_PREFIX);
    }
}
