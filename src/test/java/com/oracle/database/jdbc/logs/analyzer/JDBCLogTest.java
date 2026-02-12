package com.oracle.database.jdbc.logs.analyzer;

import com.oracle.database.jdbc.logs.model.JDBCConnectionEvent;
import com.oracle.database.jdbc.logs.model.JDBCExecutedQuery;
import com.oracle.database.jdbc.logs.model.JDBCLogComparison;
import com.oracle.database.jdbc.logs.model.JDBCStats;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class JDBCLogTest {

  private static JDBCLog jdbcLog;

  @BeforeAll
  static void setup(){
    final var filepath = JDBCLogTest.class.getClassLoader().getResource("ojdbc-2.log").getPath();
    jdbcLog = new JDBCLog(filepath);
  }

  @Test
  void getLogErrorsTest() {
    final var expectedErrorMessages = """
      ORA-01918: user 'TKPJSPICP01' does not exist
      ORA-01940: cannot drop a user who is currently connected
      ORA-01940: cannot drop a user who is currently connected
      """
      .lines()
      .toList();

    final var actualErrorMessages = jdbcLog.getLogErrors()
      .stream()
      .map(logError -> {
        var errorMessage = "";
        try {
          errorMessage = logError.getErrorMessage();
        } catch (IOException ignored) {
          // shouldn't occur
        }
        return errorMessage;
      })
      .toList();

    assertEquals(expectedErrorMessages, actualErrorMessages,
      "Should report one 'ORA-01918' error followed by two 'ORA-01940' errors.");
  }

  @Test
  void getStatsTest() {
    final var expectedStats = new JDBCStats(1093201L, 7773, "2024-06-20T21:44:31",
        "2024-06-20T21:44:36", Duration.ofSeconds(5), 3,
        37, 12, 15,
        4, 180, 263,
        180, 159387, 155951);

    final var actualStats = jdbcLog.getStats();

    assertEquals(expectedStats, actualStats, "The statistics should yield the same values");
  }

  @Test
  void getQueriesTest() {
    final var expectedQueries = """
      drop directory TEST_DIR
      drop user tkpjspicp01 cascade
      create user tkpjspicp01 identified by tkpjspicp01 default tablespace system quota unlimited on system
      grant connect, resource, unlimited tablespace,ALTER SESSION to tkpjspicp01
      grant create view, create session, create synonym,create type, create sequence, CREATE TABLE,create procedure, select any table to tkpjspicp01
      grant create database link to tkpjspicp01
      grant CREATE CLUSTER, CREATE OPERATOR, CREATE TRIGGER, CREATE INDEXTYPE to tkpjspicp01
      grant create any directory,drop any directory to tkpjspicp01
      grant select on emp to public
      grant select on dept to public
      create directory TEST_DIR as '/test/b/1465033565/oracle/work'
      ALTER SESSION SET TIME_ZONE = 'PST8PDT'
      SELECT DBTIMEZONE FROM DUAL
      select 'Ok' from dual
      select 'Ok Json Abs Path' from dual
      select 'Ok Json No User No Password' from dual
      select 'Ok Json Multiple' from dual
      select 'Ok Https' from dual
      select 'Ok Basic Auth' from dual
      select 'Ok credentials from URL' from dual
      select 'Ok credentials from properties' from dual
      select 'Ok credentials from datasource' from dual
      select 'Ok remote config filtering' from dual
      drop user tkpjspicp01 cascade
      select sid,serial# from v$session where username='TKPJSPICP01'
      alter system kill session '20,28752' immediate
      alter system kill session '43,27039' immediate
      alter system kill session '48,23778' immediate
      alter system kill session '49,14999' immediate
      alter system kill session '52,57878' immediate
      alter system kill session '53,6901' immediate
      alter system kill session '278,37365' immediate
      alter system kill session '279,58579' immediate
      alter system kill session '282,11318' immediate
      alter system kill session '283,30053' immediate
      alter system kill session '289,23780' immediate
      drop user tkpjspicp01 cascade
      """.lines()
      .toList();

    final var actualQueries = jdbcLog.getQueries()
      .stream()
      .map(JDBCExecutedQuery::sql)
      .toList();

    assertEquals(expectedQueries, actualQueries, "Should report 37 executed queries");
  }

  @Test
  void getConnectionEvents() {
    final var expectedConnectionEvents = """
      CONNECTION_OPENED
      CONNECTION_OPENED
      CONNECTION_OPENED
      CONNECTION_CLOSED
      CONNECTION_CLOSED
      CONNECTION_CLOSED
      CONNECTION_OPENED
      CONNECTION_OPENED
      CONNECTION_OPENED
      CONNECTION_OPENED
      CONNECTION_OPENED
      CONNECTION_OPENED
      CONNECTION_OPENED
      CONNECTION_OPENED
      CONNECTION_OPENED
      CONNECTION_OPENED
      CONNECTION_OPENED
      CONNECTION_OPENED
      CONNECTION_CLOSED
      """
      .strip()
      .lines()
      .map(JDBCConnectionEvent.Event::valueOf)
      .toList();

    final var actualConnectionEvents = jdbcLog.getConnectionEvents()
      .stream()
      .map(JDBCConnectionEvent::event)
      .toList();

    assertEquals(expectedConnectionEvents, actualConnectionEvents,
      "Should report 15 connection opened and 4 connection closed events");
  }

  @Test
  void compareToTest() {
    final var ojdbc1 = JDBCLogTest.class.getClassLoader().getResource("ojdbc.log").getPath();
    final var ojdbc2 = JDBCLogTest.class.getClassLoader().getResource("ojdbc-2.log").getPath();

    final var expectedComparisonResults = new JDBCLogComparison(
      new JDBCLogComparison.Summary(ojdbc2,ojdbc1, 1093201L, 1362671L, 7773, 25796, 231.87, "2024-06-20T21:44:31 to 2024-06-20T21:44:36", Duration.ofSeconds(5), "2024-06-20T22:27:11 to 2024-06-20T22:28:14", Duration.ofSeconds(63)),
      new JDBCLogComparison.Performance(37, 17, -54.05, 12, 20, 66.67),
      new JDBCLogComparison.Error(3, 10, 233.33),
      new JDBCLogComparison.Network(159387, 55197, -65.37, 155951, 3285597, 2006.81)
    );

    final var actualComparisonResults = jdbcLog.compareTo(ojdbc1);

    assertEquals(expectedComparisonResults, actualComparisonResults,
      "JDBCLogComparison results should be the same");
  }
}