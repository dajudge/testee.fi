package com.dajudge.testee.utils;

import com.dajudge.testee.exceptions.TesteeException;
import org.apache.commons.lang3.StringUtils;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.lang.Math.max;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.rightPad;

/**
 * Utilities around JDBC.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public final class JdbcUtils {
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

    public static int update(
            final Connection c,
            final String sql,
            final Object... args
    ) {
        return execute(
                () -> {
                    try (final PreparedStatement s = applyArgs(c.prepareStatement(sql), args)) {
                        return s.executeUpdate();
                    }
                },
                e -> ("Failed to execute SQL update: " + sql)
        );
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
            columns.sort(String::compareTo);
            final List<Integer> widths = calculateWidths(columns, rows);
            final int overallWidth = widths.stream().mapToInt(it -> it).sum()
                    + columns.size() * 2
                    + (columns.size() - 1);
            printHeader(out, columns, widths);
            out.println(StringUtils.repeat("-", overallWidth));
            rows.forEach(row -> printRow(out, columns, widths, row));
        }
        out.println(rows.size() + " rows");
    }

    private static void printHeader(final PrintStream out, final List<String> columns, final List<Integer> widths) {
        for (int i = 0; i < columns.size(); i++) {
            out.print((i == 0 ? " " : "| ") + rightPad(columns.get(i), widths.get(i)) + " ");
        }
        out.println();
    }

    private static void printRow(
            final PrintStream out,
            final List<String> columns,
            final List<Integer> widths,
            final Map<String, Object> row
    ) {
        for (int i = 0; i < columns.size(); i++) {
            final Object value = row.get(columns.get(i));
            final String unpaddedString = value == null ? "" : value.toString();
            final String stringValue = rightPad(unpaddedString, widths.get(i));
            out.print((i == 0 ? " " : "| ") + stringValue + " ");
        }
        out.println();
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
