import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RenderDbRestore {
    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException("Usage: RenderDbRestore <jdbcUrl> <username> <password> <dumpPath>");
        }

        String jdbcUrl = args[0];
        String username = args[1];
        String password = args[2];
        Path dumpPath = Path.of(args[3]);

        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            connection.setAutoCommit(false);

            try (Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA IF EXISTS public CASCADE");
                statement.execute("CREATE SCHEMA public");
                statement.execute("GRANT ALL ON SCHEMA public TO " + username);
                statement.execute("GRANT ALL ON SCHEMA public TO public");
            }

            restoreDump(connection, dumpPath);
            connection.commit();
        }
    }

    private static void restoreDump(Connection connection, Path dumpPath) throws IOException, SQLException {
        PGConnection pgConnection = connection.unwrap(PGConnection.class);
        CopyManager copyManager = pgConnection.getCopyAPI();
        List<String> statementLines = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(dumpPath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("\\")) {
                    if (line.startsWith("COPY ")) {
                        executeStatementBuffer(connection, statementLines);
                        restoreCopyBlock(copyManager, line, reader);
                    }
                    continue;
                }

                statementLines.add(line);

                if (line.trim().endsWith(";")) {
                    executeStatementBuffer(connection, statementLines);
                }
            }
        }

        executeStatementBuffer(connection, statementLines);
    }

    private static void restoreCopyBlock(CopyManager copyManager, String copyCommand, BufferedReader reader)
        throws IOException, SQLException {
        StringBuilder data = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            if ("\\.".equals(line)) {
                break;
            }

            data.append(line).append('\n');
        }

        try (Reader copyReader = new StringReader(data.toString())) {
            copyManager.copyIn(copyCommand, copyReader);
        }
    }

    private static void executeStatementBuffer(Connection connection, List<String> statementLines) throws SQLException {
        if (statementLines.isEmpty()) {
            return;
        }

        StringBuilder sqlBuilder = new StringBuilder();
        for (String line : statementLines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("--")) {
                continue;
            }

            sqlBuilder.append(line).append('\n');
        }

        statementLines.clear();
        String sql = sqlBuilder.toString().trim();

        if (sql.isEmpty()) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
