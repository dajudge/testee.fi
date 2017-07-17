package com.dajudge.testee.utils;

import com.dajudge.testee.exceptions.TesteeException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.lang.Math.max;
import static java.lang.System.identityHashCode;
import static java.lang.reflect.Proxy.newProxyInstance;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.rightPad;

/**
 * Utilities around JDBC.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public final class JdbcUtils {
    private static final Logger LOG = LoggerFactory.getLogger(JdbcUtils.class);

    public static Map<String, Object> mapRowMapper(final ResultSet rs) throws SQLException {
        final int count = rs.getMetaData().getColumnCount();
        final Map<String, Object> ret = new HashMap<>();
        for (int i = 0; i < count; i++) {
            ret.put(rs.getMetaData().getColumnName(i + 1), rs.getObject(i + 1));
        }
        return ret;
    }

    public interface JdbcConsumer<I> {
        void run(I o) throws SQLException;
    }

    public interface JdbcProducer<O> {
        O get() throws SQLException;
    }

    public interface JdbcFunction<I, O> {
        O apply(I i) throws SQLException;
    }

    public static Connection debug(final Connection c) {
        return debug(c, Connection.class);
    }

    public static <T> T debug(final Object o, final Class<T> iface) {
        final String oid = o.getClass().getName() + "@" + identityHashCode(o);
        return (T) newProxyInstance(
                JdbcUtils.class.getClassLoader(),
                new Class<?>[]{iface},
                (proxy, method, args) -> {
                    try {
                        LOG.debug("CALL {} {} {}", oid, method, args == null ? "[]" : Arrays.asList(args));
                        if (method.getReturnType().isInterface()) {
                            return debug(method.invoke(o, args), method.getReturnType());
                        } else {
                            return method.invoke(o, args);
                        }
                    } catch (final InvocationTargetException e) {
                        throw e.getTargetException();
                    }
                }
        );
    }

    public static <O> O execute(
            final JdbcProducer<O> producer,
            final Function<SQLException, String> error
    ) {
        try {
            return producer.get();
        } catch (final SQLException e) {
            throw new TesteeException(error.apply(e), e);
        }
    }

    public static <O> List<O> query(
            final Connection c,
            final String sql,
            final JdbcFunction<ResultSet, O> rowMapper,
            final Object... args
    ) {
        return execute(
                () -> {
                    try (
                            final PreparedStatement s = applyArgs(c.prepareStatement(sql), args);
                            final ResultSet rs = s.executeQuery()
                    ) {
                        final List<O> ret = new ArrayList<>();
                        while (rs.next()) {
                            ret.add(rowMapper.apply(rs));
                        }
                        return ret;
                    }
                },
                e -> ("Failed to execute SQL query: " + sql)
        );
    }

    private static PreparedStatement applyArgs(final PreparedStatement s, final Object[] args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            s.setObject(i + 1, args[i]);
        }
        return s;
    }

    public static void dumpQuery(final PrintStream out, final Connection c, final String sql, final Object... args) {
        dumpMap(query(c, sql, JdbcUtils::mapRowMapper, args), out);
    }

    private static void dumpMap(final List<Map<String, Object>> rows, final PrintStream out) {
        if (!rows.isEmpty()) {
            final List<String> columns = new ArrayList<>(rows.get(0).keySet());
            final List<Integer> widths = calculateWidths(columns, rows);
            final int overallWidth = widths.stream().mapToInt(it -> it).sum()
                    + columns.size() * 2
                    + (columns.size() - 1);
            for (int i = 0; i < columns.size(); i++) {
                out.print((i == 0 ? " " : "| ") + rightPad(columns.get(i), widths.get(i)) + " ");
            }
            out.println();
            out.println(StringUtils.repeat("-", overallWidth));
            rows.forEach(row -> {
                for (int i = 0; i < columns.size(); i++) {
                    final Object value = row.get(columns.get(i));
                    final String unpaddedString = value == null ? "" : value.toString();
                    final String stringValue = rightPad(unpaddedString, widths.get(i));
                    out.print((i == 0 ? " " : "| ") + stringValue + " ");
                }
                out.println();
            });
        }
        out.println(rows.size() + " rows");
    }

    private static List<Integer> calculateWidths(final List<String> columns, final List<Map<String, Object>> rows) {
        return columns.stream().map(it -> calculateWidth(it, rows)).collect(toList());
    }

    private static int calculateWidth(final String col, final List<Map<String, Object>> rows) {
        int ret = col.length();
        for (final Map<String, Object> row : rows) {
            if (row.get(col) != null) {
                ret = max(ret, row.get(col).toString().length());
            }
        }
        return ret;
    }
}
