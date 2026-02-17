/*
 ** OJDBC Log Analyzer version 1.0.0
 **
 ** Copyright (c) 2025 Oracle and/or its affiliates.
 ** Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */

package com.oracle.database.jdbc.logs.analyzer;

import com.oracle.database.jdbc.logs.model.JDBCConnectionEvent;
import com.oracle.database.jdbc.logs.model.JDBCExecutedQuery;
import com.oracle.database.jdbc.logs.model.JDBCLogComparison;
import com.oracle.database.jdbc.logs.model.JDBCStats;
import com.oracle.database.jdbc.logs.model.LogEntry;
import com.oracle.database.jdbc.logs.model.LogError;
import com.oracle.database.jdbc.logs.model.LogParser;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.oracle.database.jdbc.logs.analyzer.Utils.*;

/**
 * <p>
 *   This class is responsible for parsing Oracle JDBC thin log files.
 * </p>
 * <p>
 *   It allows to access, query and analyze data stored in an Oracle JDBC log
 *   file, the extracted info are: Errors, Stats, Queries and Connection Events.
 * </p>
 *
 * @see #getLogErrors()
 * @see #getStats()
 * @see #getQueries()
 * @see #getConnectionEvents()
 * @see #compareTo(String)
 */
public class JDBCLog {

  /**
   * Keyword to determine if the log file is formatted by {@code UCPFormatter}.
   */
  private static final String UCP = " UCP ";

  // Boolean (not primitive type 'boolean') to know whether the field has been initialized or not.
  private Boolean isUCPFormatted;
  private final String logLocation;
  private final LogParser parser;
  private final CombinedCollector combinedCollector = new CombinedCollector();

  /**
   * <p>
   *   Creates an instance capable of parsing the Oracle JDBC log file.
   * </p>
   *
   * @param logLocation URL or path to the Oracle JDBC log file.
   * @throws IllegalArgumentException If {@code logLocation} is null or empty.
   */
  public JDBCLog(String logLocation) {
    Utils.requireNonBlank(logLocation, "logLocation cannot be null or blank.");
    this.logLocation = logLocation;

    parser = new LogParser(logLocation);

    try {
      parser.parse(combinedCollector);
    } catch (IOException e) {
      //unable to parse
      e.printStackTrace();
    }
  }

  /**
   * <p>
   *   Collector for log errors.
   * </p>
   */
  public static class ErrorsCollector implements LogParser.LogEntryProcessor {
    /**
     * Pattern to extract Exception with {@code ORA-}.
     */
    static final Pattern EXCEPTION_PATTERN = Pattern.compile("^(java|oracle).*Exception: ORA-", Pattern.MULTILINE);

    List<LogEntry> errors;

    /**
     * <p>
     *   Constructs a new {@code ErrorsCollector} instance for collecting error entries from log data.
     * </p>
     */
    public ErrorsCollector() {
      errors = new ArrayList<>();
    }


    @Override
    public boolean onNewLogEntry(LogEntry entry) throws IOException {
      Matcher matcher = EXCEPTION_PATTERN.matcher(entry.getLines());

      if (matcher.find()) {
        errors.add(entry);

        return true;
      }

      return false;
    }

    /**
     * <p>
     *   Retrieve the log entries from the log file.
     * </p>
     *
     * @return {@link List} of {@link LogEntry}
     */
    public List<LogEntry> getErrors() {
      return errors;
    }

    /**
     * <p>
     *   Retrieve the number of errors from the log file.
     * </p>
     *
     * @return Error count.
     */
    public int getErrorsCount() {
      return errors.size();
    }
  }

  /**
   * <p>
   *   Collector for log statistics.
   * </p>
   */
  public class StatsCollector implements LogParser.LogEntryProcessor {
    private final Pattern writtenBytesPattern;
    private final Pattern receivedBytesPattern;

    private long receivedPacketCount;
    private long sentPacketCount;
    private long bytesConsumed;
    private long bytesProduced;
    private String startTime;
    private String endTime;

    private Duration zonedDiff;
    private Duration localDiff;

    private JDBCStats stats;

    /**
     *   Constructs a new {@code StatsCollector} instance for collecting statistics from log entries.
     * <p>
     *   This default constructor initializes internal counters and patterns required for
     *   parsing and aggregating statistics such as packet counts, byte counts, and time intervals
     *   from log file entries.
     * </p>
     */
    public StatsCollector() {
      this.writtenBytesPattern = Pattern.compile("(\\d+|\\d{1,3}(?:,\\d{3})*) bytes written to the Socket", Pattern.MULTILINE);
      this.receivedBytesPattern = Pattern.compile("(\\d+|\\d{1,3}(?:,\\d{3})*) bytes$", Pattern.MULTILINE);

      this.receivedPacketCount = 0;
      this.sentPacketCount = 0;
      this.bytesConsumed = 0;
      this.bytesProduced = 0;
      this.startTime = null;
      this.endTime = null;
      this.zonedDiff = null;
      this.localDiff = null;
      this.stats = null;
    }

    @Override
    public boolean onNewLogEntry(LogEntry entry) throws IOException {
      String line = entry.getLastTrace();

      if (isUCPFormatted == null)
        isUCPFormatted = line.contains(UCP);

      Matcher matcherHolder;
      line = line.strip();

      try {
        if (isUCPFormatted) {
          final var zonedDateTime = ZonedDateTime.parse(line.split(UCP)[0].strip(), LogError.UCP_TIMESTAMP_FORMATTER);

          if (startTime == null || zonedDateTime.isBefore(ZonedDateTime.parse(startTime)))
            startTime = zonedDateTime.toString();

          if (endTime == null || zonedDateTime.isAfter(ZonedDateTime.parse(endTime)))
            endTime = zonedDateTime.toString();

          zonedDiff = Duration.between(ZonedDateTime.parse(startTime), ZonedDateTime.parse(endTime));

        } else {
          final String[] lineSegments = line.split(" ");
          final var localDateTime = LocalDateTime.parse(
            String.join(" ", lineSegments[0], lineSegments[1], lineSegments[2], lineSegments[3], lineSegments[4]),
            LogError.DEFAULT_TIMESTAMP_FORMATTER);

          if (startTime == null || localDateTime.isBefore(LocalDateTime.parse(startTime)))
            startTime = localDateTime.toString();

          if (endTime == null || localDateTime.isAfter(LocalDateTime.parse(endTime)))
            endTime = localDateTime.toString();

          localDiff = Duration.between(LocalDateTime.parse(startTime), LocalDateTime.parse(endTime));
        }
      } catch (Exception ignored) {
        // Line doesn't start with timestamp
      }

      line = entry.getLines();

      matcherHolder = writtenBytesPattern.matcher(line);
      if (matcherHolder.find()) {
        sentPacketCount ++;
        final String bytesString = matcherHolder.group(1).replace(",", "");
        bytesProduced += Long.parseLong(bytesString);

        return true;
      } else {

        matcherHolder = receivedBytesPattern.matcher(line);
        if (matcherHolder.find()) {
          receivedPacketCount++;
          final String bytesString = matcherHolder.group(1).replace(",", "");
          bytesConsumed += Long.parseLong(bytesString);

          return true;
        }
      }

      return false;
    }

    /**
     * <p>
     *   Computes and returns JDBC statistics for the processed log file.
     * </p>
     *
     * @param queries      the list of executed JDBC queries, each providing its execution time
     * @param events       the list of JDBC connection events (e.g. connections opened/closed)
     * @param linesCount   the total number of lines in the processed log
     * @param errorsCount  the total number of error lines found in the log
     *
     * @return an instance of {@link JDBCStats} containing all computed statistics
     */
    public JDBCStats getStats(List<JDBCExecutedQuery> queries, List<JDBCConnectionEvent> events, long linesCount, long errorsCount) {
      if (this.stats != null) {
        return this.stats;
      }

      final var averageQueryTime = Math.round(
        queries.stream()
          .mapToInt(JDBCExecutedQuery::executionTime)
          .average()
          .orElse(0)
      );

      final long openedConnectionCount = events.stream()
        .filter(jdbcConnectionEvent -> jdbcConnectionEvent.event() == JDBCConnectionEvent.Event.CONNECTION_OPENED)
        .count();

      final long closedConnectionCount = events.stream()
        .filter(jdbcConnectionEvent -> jdbcConnectionEvent.event() == JDBCConnectionEvent.Event.CONNECTION_CLOSED)
        .count();

      stats = new JDBCStats(getFileSize(logLocation), linesCount, startTime, endTime,
        localDiff != null ? localDiff : zonedDiff, errorsCount, queries.size(),
        averageQueryTime,  openedConnectionCount, closedConnectionCount,
        receivedPacketCount, sentPacketCount, receivedPacketCount, bytesConsumed, bytesProduced);

      return stats;
    }
  }

  /**
   * <p>
   *   Collector for executed SQL queries.
   * </p>
   */
  public class QueriesCollector implements LogParser.LogEntryProcessor {

    private final Pattern queriesPattern;
    private final Pattern sqlAndTimePattern;
    private final Pattern connectionIdAndTenantPattern;
    private final List<JDBCExecutedQuery> queries;

    /**
     * <p>
     *   Constructs a new {@code QueriesCollector} instance for collecting executed JDBC queries from log entries.
     * </p>
     */
    public QueriesCollector() {
      this.queriesPattern = Pattern.compile("[\\s|.]endCurrentSql");
      this.sqlAndTimePattern = Pattern.compile("sql=([\\S\\s]*), time=(.*)", Pattern.MULTILINE);
      this.connectionIdAndTenantPattern = Pattern.compile("CONNECTION_ID=(.*),TENANT=(.*),SQL=", Pattern.MULTILINE);
      this.queries = new ArrayList<>();
    }

    @Override
    public boolean onNewLogEntry(LogEntry entry) throws IOException {
      String line = entry.getLastTrace();

      if (isUCPFormatted == null)
        isUCPFormatted = line.contains(UCP);

      line = line.strip();
      String timestamp;
      final Matcher queryMatcher = queriesPattern.matcher(line);

      if (queryMatcher.find()) {
        if (isUCPFormatted)
          timestamp = ZonedDateTime.parse(line.split(UCP)[0].strip(), LogError.UCP_TIMESTAMP_FORMATTER).toString();
        else {
          timestamp = line.replace(" oracle.jdbc.driver.ConnectionDiagnosable endCurrentSql", "").strip();
          timestamp = LocalDateTime.parse(timestamp, LogError.DEFAULT_TIMESTAMP_FORMATTER).toString();
        }

        String sql = null;
        int executionTime = 0;
        String connectionId = null;
        String tenant = null;
        final Matcher sqlAndTimeMatcher = sqlAndTimePattern.matcher(entry.getLines());
        if (sqlAndTimeMatcher.find()) {
          sql = sqlAndTimeMatcher.group(1);
          executionTime = Integer.parseInt(
            sqlAndTimeMatcher.group(2)
              .replace("ms","")
              .replace(",","")
              .strip()
          );
        }

        final Matcher connectionIdAndTenantMatcher = connectionIdAndTenantPattern.matcher(line);
        if (connectionIdAndTenantMatcher.find()) {
          connectionId = connectionIdAndTenantMatcher.group(1);
          tenant = connectionIdAndTenantMatcher.group(2);
        }

        queries.add(new JDBCExecutedQuery(timestamp, sql, executionTime, connectionId, tenant));

        return true;
      }

      return false;
    }

    /**
     * <p>
     *   Returns the list of executed queries.
     * </p>
     *
     * @return a {@link List} of {@link JDBCExecutedQuery}.
     */
    public List<JDBCExecutedQuery> getQueries() {
      return this.queries;
    }
  }

  /**
   * <p>
   *   Collector for connection events.
   * </p>
   */
  public class ConnectionEventsCollector implements LogParser.LogEntryProcessor {

    private final Pattern openedConnectionsTracePattern;
    private final Pattern openedConnectionsLogPattern;
    private final Pattern closedConnectionsTracePattern;
    private final List<JDBCConnectionEvent> events;

    /**
     * <p>
     *   Constructs a new {@code ConnectionEventsCollector} instance for collecting JDBC connection events from log entries.
     * </p>
     */
    public ConnectionEventsCollector() {
      this.openedConnectionsTracePattern = Pattern.compile(" oracle.jdbc.driver.T4CConnection[. ]logon$");
      this.openedConnectionsLogPattern = Pattern.compile("\\s.*Session Attributes:");
      this.closedConnectionsTracePattern = Pattern.compile(" oracle.jdbc.driver.T4CConnection[. ]logoff$");
      this.events = new ArrayList<>();
    }


    @Override
    public boolean onNewLogEntry(LogEntry entry) throws IOException {
      String traceLine = entry.getLastTrace();

      if (isUCPFormatted == null)
        isUCPFormatted = traceLine.contains(UCP);

      traceLine = traceLine.strip();
      Matcher matcherHolder;
      String timestamp;

      matcherHolder = closedConnectionsTracePattern.matcher(traceLine);
      if (matcherHolder.find()) {
        final var logLine = entry.getLines().strip();
        if (!logLine.endsWith(" null")) {
          if (isUCPFormatted)
            timestamp = ZonedDateTime.parse(traceLine.split(UCP)[0].strip(), LogError.UCP_TIMESTAMP_FORMATTER).toString();
          else {
            timestamp = traceLine.split(closedConnectionsTracePattern.pattern())[0];
            timestamp = LocalDateTime.parse(timestamp, LogError.DEFAULT_TIMESTAMP_FORMATTER).toString();
          }
          events.add(new JDBCConnectionEvent(timestamp, JDBCConnectionEvent.Event.CONNECTION_CLOSED));

          return true;
        }
      } else {

        matcherHolder = openedConnectionsTracePattern.matcher(traceLine);
        if (matcherHolder.find()) {
          Matcher logMatcher = openedConnectionsLogPattern.matcher(entry.getLines());
          if (logMatcher.find()) {
            if (isUCPFormatted)
              timestamp = ZonedDateTime.parse(traceLine.split(UCP)[0].strip(), LogError.UCP_TIMESTAMP_FORMATTER).toString();
            else {
              timestamp = traceLine.split(openedConnectionsTracePattern.pattern().substring(0, openedConnectionsTracePattern.pattern().length() - 1))[0];
              timestamp = LocalDateTime.parse(timestamp, LogError.DEFAULT_TIMESTAMP_FORMATTER).toString();
            }

            events.add(new JDBCConnectionEvent(timestamp, JDBCConnectionEvent.Event.CONNECTION_OPENED, entry.getLines()));

            return true;
          }
        }
      }

      return false;
    }

    /**
     * <p>
     *   Retrieve the connection events from the log file.
     * </p>
     *
     * @return {@link List} of {@link JDBCConnectionEvent}
     */
    public List<JDBCConnectionEvent> getEvents() {
      return this.events;
    }
  }

  /**
   * <p>
   *   Collector that combines all other collectors.
   * </p>
   *
   * @see StatsCollector
   * @see QueriesCollector
   * @see ConnectionEventsCollector
   */
  public class CombinedCollector implements LogParser.LogEntryProcessor {

    private long linesCount;
    private List<LogError> logErrors;

    private final ErrorsCollector errorsCollector;
    private final StatsCollector statsCollector;
    private final QueriesCollector queriesCollector;
    private final ConnectionEventsCollector connectionEventsCollector;

    /**
     * Constructs a new {@code CombinedCollector} instance with initialized internal collectors
     * for errors, statistics, executed queries, and connection events.
     * <p>
     * This default constructor sets up a {@code CombinedCollector} so that it is ready to
     * process and aggregate data from log entries through its associated {@link ErrorsCollector},
     * {@link StatsCollector}, {@link QueriesCollector}, and {@link ConnectionEventsCollector} components.
     * </p>
     */
    public CombinedCollector() {
      this.errorsCollector = new ErrorsCollector();
      this.statsCollector = new StatsCollector();
      this.queriesCollector = new QueriesCollector();
      this.connectionEventsCollector = new ConnectionEventsCollector();
      this.linesCount = 0;
      this.logErrors = null;
    }

    @Override
    public boolean onNewLogEntry(LogEntry entry) throws IOException {
      if (entry.getEndLine() == -1) {
        // This is the last logEntry
        linesCount = entry.getBeginLine() + entry.getLines().lines().count();
      }

      return ( errorsCollector.onNewLogEntry(entry)
        || queriesCollector.onNewLogEntry(entry)
        || connectionEventsCollector.onNewLogEntry(entry)
        || statsCollector.onNewLogEntry(entry));
    }

    /**
     * <p>
     *   Returns a list of {@link LogError} objects constructed from the collected {@link LogEntry} errors.
     * </p>
     *
     * @param allLogs the complete list of {@link LogEntry} instances, which will be passed to each
     *                {@link LogError} constructor
     * @return a list of {@link LogError} objects representing the collected error log entries
     */
    public List<LogError> getLogErrors(List<LogEntry> allLogs) {
      if (this.logErrors != null) {
        return this.logErrors;
      }

      this.logErrors = errorsCollector.getErrors()
        .stream()
        .map(logEntry -> new LogError(allLogs, logEntry))
        .toList();

      return this.logErrors;
    }

    /**
     * <p>
     *   Retrieve the statistics from the log file.
     * </p>
     *
     * @return {@link List} of {@link JDBCStats}
     */
    public JDBCStats getStats() {
      return statsCollector.getStats(
        queriesCollector.getQueries(),
        connectionEventsCollector.getEvents(),
        linesCount,
        errorsCollector.getErrorsCount());
    }

    /**
     * <p>
     *   Retrieve the executed queries from the log file.
     * </p>
     *
     * @return {@link List} of {@link JDBCExecutedQuery}
     */
    public List<JDBCExecutedQuery> getQueries() {
      return queriesCollector.getQueries();
    }

    /**
     * <p>
     *   Returns the list of collected JDBC connection events.
     * </p>
     *
     * <p>
     *   This method delegates to {@code connectionEventsCollector.getEvents()} to retrieve
     *   all {@link JDBCConnectionEvent} instances that have been collected so far.
     * </p>
     *
     * @return a {@link List} of {@link JDBCConnectionEvent} of recorded connection events.
     */
    public List<JDBCConnectionEvent> getConnectionEvents() {
      return connectionEventsCollector.getEvents();
    }
  }

  /**
   * <p>
   *   Retrieve the log errors from the log file.
   * </p>
   *
   * @return {@link List} of {@link LogError}
   */
  public List<LogError> getLogErrors() {
    return combinedCollector.getLogErrors(parser.getLogEntries());
  }

  /**
   * <p>
   *   Generate statistics from the log file.
   * </p>
   *
   * @return {@link JDBCStats} object
   */
  public JDBCStats getStats() {
    return combinedCollector.getStats();
  }

  /**
   * <p>
   *   Retrieve the executed SQL statements with the timestamp and the execution time.
   * </p>
   *
   * @return {@link List} of {@link JDBCExecutedQuery}
   */
  public List<JDBCExecutedQuery> getQueries() {
    return combinedCollector.getQueries();
  }

  /**
   * <p>
   *  Retrieve the connection opened/closed events.
   * </p>
   *
   * @return {@link List} of {@link JDBCConnectionEvent}
   */
  public List<JDBCConnectionEvent> getConnectionEvents() {
    return combinedCollector.getConnectionEvents();
  }

  /**
   * <p>
   *   Compare {@code this} log file with another one.
   * </p>
   *
   * @param filepath path to the Oracle JDBC log file.
   * @return {@link JDBCLogComparison} object.
   */
  public JDBCLogComparison compareTo(final String filepath) {
    // this = reference
    // other =  supplied log file
    final JDBCLog other = new JDBCLog(filepath);

    var summary = new JDBCLogComparison.Summary(
      this.logLocation,
      other.logLocation,

      this.getStats().fileSize(),
      other.getStats().fileSize(),

      this.getStats().lineCount(),
      other.getStats().lineCount(),
      JDBCLogComparison.delta(this.getStats().lineCount(), other.getStats().lineCount()),

      this.getStats().timespan(),
      this.getStats().duration(),

      other.getStats().timespan(),
      other.getStats().duration()
    );

    var performance = new JDBCLogComparison.Performance(
      this.getStats().queryCount(),
      other.getStats().queryCount(),
      JDBCLogComparison.delta(this.getStats().queryCount(), other.getStats().queryCount()),

      this.getStats().averageQueryTime(),
      other.getStats().averageQueryTime(),
      JDBCLogComparison.delta(this.getStats().averageQueryTime(), other.getStats().averageQueryTime())
    );

    final var referenceErrorCount = this.getStats().errorCount();
    final var otherErrorCount = other.getStats().errorCount();

    var error = new JDBCLogComparison.Error(
      referenceErrorCount,
      otherErrorCount,
      JDBCLogComparison.delta(referenceErrorCount, otherErrorCount)
    );

    final var referenceConsumed = this.getStats().bytesConsumed();
    final var otherConsumed = other.getStats().bytesConsumed();

    final var referenceProduced = this.getStats().bytesProduced();
    final var otherProduced = other.getStats().bytesProduced();

    var network = new JDBCLogComparison.Network(
      getStats().bytesConsumed(),
      other.getStats().bytesConsumed(),
      JDBCLogComparison.delta(referenceConsumed, otherConsumed),
      getStats().bytesProduced(),
      other.getStats().bytesProduced(),
      JDBCLogComparison.delta(referenceProduced, otherProduced)
    );

    return new JDBCLogComparison(summary, performance, error, network);
  }

}
