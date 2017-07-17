package com.dajudge.testee.utils;

import com.dajudge.testee.exceptions.TesteeException;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JdbcUtilsTest {
    @Test(expected = TesteeException.class)
    public void throws_exception() {
        JdbcUtils.execute(() -> {
            throw new SQLException("lolcats");
        }, e -> "Error");
    }

    @Test
    public void testDump() throws UnsupportedEncodingException, SQLException {
        // Given
        final Connection connection = mock(Connection.class);
        final PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(any())).thenReturn(statement);
        final ResultSet resultSet = mockedResultSet(asList("COL1", "COL2"), "A", "B", "C", "D");
        when(statement.executeQuery()).thenReturn(resultSet);

        // when
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final PrintStream stream = new PrintStream(bos, true, "UTF-8");
        JdbcUtils.dumpQuery(stream, connection, "sql");

        // Then
        final String s = bos.toString("UTF-8");
        assertEquals(s, StringUtils.join(asList(
                " COL1 | COL2 ",
                "-------------",
                " A    | B    ",
                " C    | D    ",
                "2 rows",
                ""
        ), System.lineSeparator()));
    }

    private ResultSet mockedResultSet(final List<String> cols, final Object... values) throws SQLException {
        final List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            if (i % cols.size() == 0) {
                rows.add(new HashMap<>());
            }
            rows.get(rows.size() - 1).put(cols.get(i % cols.size()), values[i]);
        }
        final MutableContainer<Integer> count = new MutableContainer<>(-1);
        final ResultSet ret = mock(ResultSet.class);
        when(ret.next()).thenAnswer(call -> {
            count.setObject(count.getObject() + 1);
            return count.getObject() < rows.size();
        });
        when(ret.getObject(anyInt())).thenAnswer(c -> {
            final int colIndex = c.<Integer>getArgument(0) - 1;
            return rows.get(count.getObject()).get(cols.get(colIndex));
        });
        final ResultSetMetaData metadata = mock(ResultSetMetaData.class);
        when(metadata.getColumnCount()).thenReturn(cols.size());
        when(metadata.getColumnName(anyInt())).thenAnswer(c -> {
            final int idx = c.<Integer>getArgument(0) - 1;
            return cols.get(idx);
        });
        when(ret.getMetaData()).thenReturn(metadata);
        return ret;
    }
}
