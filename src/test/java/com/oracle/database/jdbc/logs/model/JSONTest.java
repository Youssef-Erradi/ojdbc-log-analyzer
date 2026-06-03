package com.oracle.database.jdbc.logs.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JSONTest {

  @Test
  void jdbcExecutedQueryToJSONString() {
    final var query = new JDBCExecutedQuery(
      "2026-06-03T12:00:00",
      """
      SELECT *
      FROM table
      WHERE id = 17001
      """,
      42,
      "conn-1",
      null
    );

    assertEquals(
      """
      {"timestamp":"2026-06-03T12:00:00","sql":"SELECT *\\nFROM table\\nWHERE id = 17001\\n","executionTime":42,"connectionId":"conn-1","tenant":null}
      """.strip(),
      query.toJSONString()
    );
  }

  @Test
  void jdbcConnectionEventToJSONString() {
    final var event = new JDBCConnectionEvent(
      "2026-06-03T12:00:00",
      JDBCConnectionEvent.Event.CONNECTION_OPENED,
      "socket=\"127.0.0.1\""
    );

    assertEquals(
      """
      {"timestamp":"2026-06-03T12:00:00","event":"CONNECTION_OPENED","details":"socket=\\\"127.0.0.1\\\""}
      """.strip(),
      event.toJSONString()
    );
  }

  @Test
  void jdbcPacketDumpToJSONString() {
    final var packetDump = new JDBCPacketDump("log-line", "AA BB |......|");

    assertEquals(
      """
      {"log":"log-line","formattedPacket":"AA BB |......|"}
      """.strip(),
      packetDump.toJSONString()
    );
  }

  @Test
  void jdbcTraceToJSONString() {
    final var trace = new JDBCTrace("2026-06-03T12:00:00", "oracle.jdbc.Foo.bar");

    assertEquals(
      """
      {"timestamp":"2026-06-03T12:00:00","executedMethod":"oracle.jdbc.Foo.bar"}
      """.strip(),
      trace.toJSONString()
    );
  }

  @Test
  void rdbmsErrorToJSONString() {
    final var error = new RDBMSError("ORA-17001: \"Internal error\"", "https://docs.oracle.com/en/error-help/db/ora-17001/");

    assertEquals(
      """
      {"errorMessage":"ORA-17001: \\\"Internal error\\\"","documentationLink":"https://docs.oracle.com/en/error-help/db/ora-17001/"}
      """.strip(),
      error.toJSONString()
    );
  }

  @Test
  void rdbmsPacketDumpToJSONString() {
    final var packetDump = new RDBMSPacketDump("2026-06-03 12:00:00", "00 01 02");

    assertEquals(
      """
      {"timestamp":"2026-06-03 12:00:00","formattedPacket":"00 01 02"}
      """.strip(),
      packetDump.toJSONString()
    );
  }

  @Test
  void logEntryToJSONString() {
    final var logEntry = new LogEntry("/tmp/log\"file.trc", 10, 11, 100);

    assertEquals(
      """
      {"logFile":"/tmp/log\\\"file.trc","beginLine":10,"endLine":11}
      """.strip(),
      logEntry.toJSONString()
    );
  }

  @Test
  void jdbcStatsToJSONString() {
    final var stats = new JDBCStats(
      100L,
      10L,
      "2026-06-03T12:00:00",
      "2026-06-03T12:00:05",
      Duration.ofSeconds(5),
      1L,
      2L,
      3.5,
      4L,
      5L,
      6L,
      7L,
      8L,
      9L,
      10L
    );

    assertEquals(
      """
      {"fileSize":100,"lineCount":10,"startTime":"2026-06-03T12:00:00","endTime":"2026-06-03T12:00:05","duration":"PT5S","errorCount":1,"queryCount":2,"averageQueryTime":3.500000,"openedConnectionCount":4,"closedConnectionCount":5,"roundTripCount":8,"sentPacketCount":6,"receivedPacketCount":7,"bytesConsumed":9,"bytesProduced":10}
      """.strip(),
      stats.toJSONString()
    );
  }

  @Test
  void jdbcLogComparisonNestedRecordsToJSONString() {
    final var summary = new JDBCLogComparison.Summary(
      "reference.log",
      "current.log",
      100,
      200,
      10,
      20,
      null,
      "2026-06-03T12:00:00 to 2026-06-03T12:00:10",
      Duration.ofSeconds(10),
      "2026-06-03T12:01:00 to 2026-06-03T12:01:20",
      Duration.ofSeconds(20)
    );

    final var performance = new JDBCLogComparison.Performance(
      10,
      20,
      100.0,
      1.5,
      2.5,
      null
    );

    final var error = new JDBCLogComparison.Error(1, 2, 100.0);
    final var network = new JDBCLogComparison.Network(10, 20, null, 30, 40, 33.33);

    assertEquals(
      """
      {"referenceLogFileName":"reference.log","currentLogFileName":"current.log","referenceLogFileSize":100,"currentLogFileSize":200,"referenceLogFileLineCount":10,"currentLogFileLineCount":20,"lineCountDelta":null,"referenceLogFileTimespan":"2026-06-03T12:00:00 to 2026-06-03T12:00:10","referenceLogFileDuration":"PT10S","currentLogFileTimespan":"2026-06-03T12:01:00 to 2026-06-03T12:01:20","currentLogFileDuration":"PT20S"}
      """.strip(),
      summary.toJSONString()
    );

    assertEquals(
      """
      {"referenceQueryCount":10,"currentQueryCount":20,"queryCountDelta":100.0,"referenceAverageQueryTime":1.500000,"currentAverageQueryTime":2.500000,"averageQueryTimeDelta":null}
      """.strip(),
      performance.toJSONString()
    );

    assertEquals(
      """
      {"referenceErrorCount":1,"currentErrorCount":2,"totalErrorsDelta":100.0}
      """.strip(),
      error.toJSONString()
    );

    assertEquals(
      """
      {"referenceBytesConsumed":10,"currentBytesConsumed":20,"bytesConsumedDelta":null,"referenceBytesProduced":30,"currentBytesProduced":40,"bytesProducedDelta":33.33}
      """.strip(),
      network.toJSONString()
    );
  }

  @Test
  void jdbcLogComparisonToJSONString() {
    final var comparison = new JDBCLogComparison(
      new JDBCLogComparison.Summary(
        "reference.log",
        "current.log",
        100,
        200,
        10,
        20,
        100.0,
        "t1",
        Duration.ofSeconds(10),
        "t2",
        Duration.ofSeconds(20)
      ),
      new JDBCLogComparison.Performance(10, 20, 100.0, 1.0, 2.0, 100.0),
      new JDBCLogComparison.Error(1, 2, 100.0),
      new JDBCLogComparison.Network(10, 20, 100.0, 30, 40, 33.33)
    );

    assertEquals(
      """
      {"summary":{"referenceLogFileName":"reference.log","currentLogFileName":"current.log","referenceLogFileSize":100,"currentLogFileSize":200,"referenceLogFileLineCount":10,"currentLogFileLineCount":20,"lineCountDelta":100.0,"referenceLogFileTimespan":"t1","referenceLogFileDuration":"PT10S","currentLogFileTimespan":"t2","currentLogFileDuration":"PT20S"},"performance":{"referenceQueryCount":10,"currentQueryCount":20,"queryCountDelta":100.0,"referenceAverageQueryTime":1.000000,"currentAverageQueryTime":2.000000,"averageQueryTimeDelta":100.0},"error":{"referenceErrorCount":1,"currentErrorCount":2,"totalErrorsDelta":100.0},"network":{"referenceBytesConsumed":10,"currentBytesConsumed":20,"bytesConsumedDelta":100.0,"referenceBytesProduced":30,"currentBytesProduced":40,"bytesProducedDelta":33.33}}
      """.strip(),
      comparison.toJSONString()
    );
  }

  @Test
  void logErrorToJSONString() {
    final String errorLine = "SEVERE: CONNECTION_ID=abc123,TENANT=tenant1, details ORA-17001: Internal error";
    final var entry = new LogEntry("/tmp/ojdbc.log", 1, 1, 0, errorLine);
    final var logError = new LogError(List.of(entry), List.of(), entry);

    assertEquals(
      """
      {"logEntry": {"logFile":"/tmp/ojdbc.log","beginLine":1,"endLine":1},"sql":null,"originalSql":null,"errorMessage":"ORA-17001: Internal error","packetDumps":[],"tenant":"tenant1","logLines":"SEVERE: CONNECTION_ID=abc123,TENANT=tenant1, details ORA-17001: Internal error","documentationLink":"https://docs.oracle.com/en/error-help/db/ORA-17001","sqlExecutionTime":-1,"nearestTrace":null,"connectionId":"abc123"}
      """.strip(),
      logError.toJSONString()
    );
  }
}
