package com.dajudge.testee.classpath;

import com.dajudge.testee.exceptions.TesteeException;
import com.dajudge.testee.utils.UrlUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.dajudge.testee.utils.UrlUtils.toUrl;
import static java.util.Collections.emptyList;

/**
 * A directory based {@link JavaArchive}.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class DirectoryJavaArchive extends AbstractBaseJavaArchive {
    private File file;

    /**
     * Constructor.
     *
     * @param file the directory.
     */
    public DirectoryJavaArchive(final File file) {
        super(toUrl(file));
        this.file = file;
    }

    @Override
    public URL getURL() {
        return toUrl(file);
    }

    @Override
    protected <T> T iterate(final Callback<T> cb) {
        return iterate(emptyList(), file, cb);
    }

    private static <T> T iterate(final List<String> prefix, final File file, final Callback<T> cb) {
        try {
            final T result = cb.item(() -> new FileInputStream(file), StringUtils.join(prefix, "/") + "/" + file.getName());
            if (result != null) {
                return result;
            }
            for (final File child : file.listFiles()) {
                if (child.isDirectory()) {
                    final T childResult = iterate(prefix(prefix, child.getName()), child, cb);
                    if (childResult != null) {
                        return childResult;
                    }
                } else {
                    final T childResult = cb.item(() -> new FileInputStream(child), StringUtils.join(prefix, "/") + "/" + child.getName());
                    if (childResult != null) {
                        return childResult;
                    }
                }
            }
        } catch (final IOException e) {
            throw new TesteeException("Error iterating classpath", e);
        }
        return null;
    }

    private static List<String> prefix(final List<String> prefix, final String name) {
        final List<String> ret = new ArrayList<>(prefix);
        ret.add(name);
        return ret;
    }

    @Override
    public String toString() {
        return "DirectoryClasspathEntry{" +
                "file=" + file +
                '}';
    }
}
