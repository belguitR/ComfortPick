package com.comfortpick.application.service

import org.postgresql.PGConnection
import org.postgresql.copy.CopyManager
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.StringReader
import java.sql.SQLException
import javax.sql.DataSource

@Service
class DatabaseImportService(
    private val dataSource: DataSource,
) {

    fun importSqlDump(dump: String) {
        dataSource.connection.use { connection ->
            connection.autoCommit = false

            connection.createStatement().use { statement ->
                statement.execute("DROP SCHEMA IF EXISTS public CASCADE")
                statement.execute("CREATE SCHEMA public")
                statement.execute("GRANT ALL ON SCHEMA public TO public")
            }

            val copyManager = connection.unwrap(PGConnection::class.java).copyAPI
            val statementLines = mutableListOf<String>()

            BufferedReader(StringReader(dump)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    if (line.startsWith("COPY ")) {
                        executeStatementBuffer(connection, statementLines)
                        restoreCopyBlock(copyManager, line, reader)
                        line = reader.readLine()
                        continue
                    }

                    if (line.startsWith("\\")) {
                        line = reader.readLine()
                        continue
                    }

                    statementLines += line
                    if (line.trim().endsWith(";")) {
                        executeStatementBuffer(connection, statementLines)
                    }

                    line = reader.readLine()
                }
            }

            executeStatementBuffer(connection, statementLines)
            connection.commit()
        }
    }

    private fun restoreCopyBlock(copyManager: CopyManager, copyCommand: String, reader: BufferedReader) {
        val data = StringBuilder()
        var line = reader.readLine()

        while (line != null && line != "\\.") {
            data.append(line).append('\n')
            line = reader.readLine()
        }

        StringReader(data.toString()).use { copyReader ->
            copyManager.copyIn(copyCommand, copyReader)
        }
    }

    private fun executeStatementBuffer(connection: java.sql.Connection, statementLines: MutableList<String>) {
        if (statementLines.isEmpty()) {
            return
        }

        val sql = buildString {
            statementLines.forEach { line ->
                val trimmed = line.trim()
                if (!trimmed.startsWith("--")) {
                    appendLine(line)
                }
            }
        }.trim()

        statementLines.clear()

        if (sql.isBlank()) {
            return
        }

        if (sql.contains("set_config('search_path', '', false)", ignoreCase = true)) {
            return
        }

        try {
            connection.createStatement().use { statement ->
                statement.execute(sql)
            }
        } catch (exception: SQLException) {
            val preview = sql.replace("\n", " ").take(220)
            throw IllegalStateException("Failed SQL statement: $preview", exception)
        }
    }
}
