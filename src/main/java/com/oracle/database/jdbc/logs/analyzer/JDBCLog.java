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
import com.oracle.database.jdbc.logs.model.LogLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.oracle.database.jdbc.logs.analyzer.Utils.getBufferedReader;
import static com.oracle.database.jdbc.logs.analyzer.Utils.getFileSize;

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
   * Traces start with a timestamp, fully qualified class name, method name. They can't be more than 1 line.
   */
  static final Pattern TRACE_PATTERN = Pattern.compile("^([a-zA-Z]{3}\\s\\d{1,2},\\s\\d{2,4}\\s\\d{1,2}:\\d{1,2}:\\d{1,2}\\s[A-Z]{2})\\s([a-zA-Z0-9.$]*\\s[a-zA-Z0-9.()<>]*$)");

  /**
   * Log entries start with FINEST, FINER, FINE, CONFIG, INFO, WARNING, SEVERE and finish at the next log / trace.
   */
  static final Pattern LOG_PATTERN = Pattern.compile("( UCP )?(FINEST|FINER|FINE|CONFIG|INFO|WARNING|SEVERE)");

  /**
   * Pattern to extract Exception with {@code ORA-}.
   */
  static final Pattern EXCEPTION_PATTERN = Pattern.compile("^(java|oracle).*Exception: ORA-", Pattern.MULTILINE);

  /**
   * Keyword to determine if the log file is formatted by {@code UCPFormatter}.
   */
  static final String UCP = " UCP ";

  /**
   * Pattern to extract sent payload size lines.
   */
  private static final Pattern WRITTEN_BYTES_PATTERN = Pattern.compile("(\\d+|\\d{1,3}(?:,\\d{3})*) bytes written to the Socket");
  /**
   * Pattern to extract received payload size lines.
   */
  private static final Pattern RECEIVED_BYTES_PATTERN = Pattern.compile("(\\d+|\\d{1,3}(?:,\\d{3})*) bytes$");

  /**
   * Pattern that marks query execution log entries.
   */
  private static final Pattern QUERIES_PATTERN = Pattern.compile("[\\s|.]endCurrentSql");
  /**
   * Pattern to split SQL text and execution time.
   */
  private static final Pattern SQL_AND_TIME_PATTERN = Pattern.compile("sql=([\\S\\s]*), time=(.*)", Pattern.MULTILINE);
  /**
   * Pattern to capture connection id and tenant from query logs.
   */
  private static final Pattern CONNECTION_ID_AND_TENANT_PATTERN = Pattern.compile("CONNECTION_ID=(.*),TENANT=(.*),SQL=", Pattern.MULTILINE);

  /**
   * Pattern that marks connection open events.
   */
  private static final Pattern OPENED_CONNECTIONS_PATTERN = Pattern.compile(" oracle.jdbc.driver.T4CConnection[. ]logon\\s.*Session Attributes:");
  /**
   * Pattern that marks connection close events.
   */
  private static final Pattern CLOSED_CONNECTIONS_PATTERN = Pattern.compile(" oracle.jdbc.driver.T4CConnection[. ]logoff$");
  /**
   * Signature used to detect multi-line logon records.
   */
  private static final String LOGON_SIGNATURE = "oracle.jdbc.driver.T4CConnection logon";

  /**
   * Pattern for default (non-UCP) timestamp prefix.
   */
  private static final Pattern DEFAULT_TIMESTAMP_PREFIX = Pattern.compile("^([A-Za-z]{3}\\s+\\d{1,2},\\s+\\d{2,4}\\s+\\d{1,2}:\\d{1,2}:\\d{1,2}\\s+[AP]M)\\b");

  /**
   * Source log file location.
   */
  private final String logLocation;

  /**
   * Indicates whether parsing has already been completed.
   */
  private boolean parsed;

  /**
   * Parsed executed SQL queries.
   */
  private List<JDBCExecutedQuery> queries;

  /**
   * Parsed connection events.
   */
  private List<JDBCConnectionEvent> connectionEvents;

  /**
   * Parsed errors mapped to their log entries.
   */
  private List<LogError> logErrors;

  /**
   * Computed statistics for the parsed log file.
   */
  private JDBCStats stats;

  /**
   * Mutable state used only during a single parse run.
   */
  private static final class ParseState {
    /**
     * Whether the log format has been detected as UCP.
     */
    private Boolean isUCPFormatted;

    /**
     * Parsed log entry ranges.
     */
    private final List<LogEntry> logEntries = new ArrayList<>();
    /**
     * Parsed trace line markers.
     */
    private final List<LogLine> traceLines = new ArrayList<>();
    /**
     * Parsed query events.
     */
    private final List<JDBCExecutedQuery> queries = new ArrayList<>();
    /**
     * Parsed connection open/close events.
     */
    private final List<JDBCConnectionEvent> connectionEvents = new ArrayList<>();
    /**
     * Line numbers where matching exceptions were detected.
     */
    private final List<Integer> errorLines = new ArrayList<>();

    /**
     * Total number of matching errors.
     */
    private long errorsCount;
    /**
     * Number of received packet lines.
     */
    private long receivedPacketCount;
    /**
     * Number of sent packet lines.
     */
    private long sentPacketCount;
    /**
     * Total consumed bytes parsed from received packet lines.
     */
    private long bytesConsumed;
    /**
     * Total produced bytes parsed from sent packet lines.
     */
    private long bytesProduced;
    /**
     * Total number of processed lines.
     */
    private long linesCount;

    /**
     * Earliest default-format timestamp found.
     */
    private LocalDateTime localStart;
    /**
     * Latest default-format timestamp found.
     */
    private LocalDateTime localEnd;
    /**
     * Earliest UCP-format timestamp found.
     */
    private ZonedDateTime zonedStart;
    /**
     * Latest UCP-format timestamp found.
     */
    private ZonedDateTime zonedEnd;

    /**
     * Current 1-based line number while reading.
     */
    private int lineNumber = 1;
    /**
     * Current byte-like cursor position in file.
     */
    private long positionInFile;
    /**
     * Begin line of the current in-progress log entry.
     */
    private int currentLogBeginLine = -1;
    /**
     * Begin position of the current in-progress log entry.
     */
    private long currentLogBeginPosition = -1;
    /**
     * Whether a log entry is currently open.
     */
    private boolean inLogEntry;

    /**
     * Timestamp associated with the in-progress query block.
     */
    private String queryTimestamp;
    /**
     * Buffer for a multi-line query block.
     */
    private StringBuilder queryContent;
    /**
     * Timestamp associated with the in-progress open-connection event.
     */
    private String openTimestamp;
    /**
     * Buffer for open-connection event details.
     */
    private StringBuilder openDetails;
    /**
     * Indicates the parser is waiting for cookie detail line.
     */
    private boolean waitingForCookie;
    /**
     * Indicates the parser has seen trailing logon and still waits for cookie detail line.
     */
    private boolean waitingForCookieAfterLogon;
    /**
     * Cached pending line for multi-line logon detection.
     */
    private String pendingLogonLine;
    /**
     * Cached timestamp for deferred close-event confirmation.
     */
    private String pendingClosedTimestamp;
  }

  /**
   * <p>
   *   Creates an instance capable of parsing the Oracle JDBC log file.
   * </p>
   *
   * @param logLocation URL or path to the Oracle JDBC log file.
   * @throws IOException if an error occurs while reading/parsing the log file.
   */
  public JDBCLog(String logLocation) throws IOException {
    Utils.requireNonBlank(logLocation, "logLocation cannot be null or blank.");
    this.logLocation = logLocation;
    parse();
  }

  /**
   * Parses the log file once and caches derived data.
   *
   * @throws IOException if reading the log file fails.
   */
  private void parse() throws IOException {
    if (parsed)
      return;

    final ParseState state = new ParseState();

    try (final BufferedReader reader = getBufferedReader(logLocation)) {
      String line;
      while ((line = reader.readLine()) != null) {
        final int lineLengthWithSeparator = line.length() + 1;
        line = line.strip();

        collectTraceAndLogLines(state, line);
        collectStatsAndErrors(state, line);
        collectQuery(state, line);
        collectConnectionEvent(state, line);

        state.positionInFile += lineLengthWithSeparator;
        state.lineNumber++;
      }
    }

    if (state.inLogEntry) {
      state.logEntries.add(new LogEntry(logLocation, state.currentLogBeginLine, -1, state.currentLogBeginPosition));
    }

    if (state.queryContent != null) {
      appendQuery(state, state.queryTimestamp, state.queryContent.toString());
      state.queryTimestamp = null;
      state.queryContent = null;
    }

    queries = List.copyOf(state.queries);
    connectionEvents = List.copyOf(state.connectionEvents);
    logErrors = List.copyOf(buildLogErrors(state));
    stats = buildStats(state);
    parsed = true;
  }

  /**
   * Collects trace boundaries and builds logical log entries.
   *
   * @param state current parse state.
   * @param line current stripped line.
   */
  private void collectTraceAndLogLines(ParseState state, String line) {
    if (TRACE_PATTERN.matcher(line).find()) {
      state.traceLines.add(new LogLine(state.lineNumber, state.positionInFile));
      if (state.inLogEntry) {
        state.logEntries.add(new LogEntry(logLocation, state.currentLogBeginLine, state.lineNumber - 1, state.currentLogBeginPosition));
        state.inLogEntry = false;
      }
    }

    if (LOG_PATTERN.matcher(line).find()) {
      if (state.inLogEntry) {
        state.logEntries.add(new LogEntry(logLocation, state.currentLogBeginLine, state.lineNumber - 1, state.currentLogBeginPosition));
      }
      state.currentLogBeginLine = state.lineNumber;
      state.currentLogBeginPosition = state.positionInFile;
      state.inLogEntry = true;
    }
  }

  /**
   * Updates counters, format detection, timestamps, and error markers.
   *
   * @param state current parse state.
   * @param line current stripped line.
   */
  private void collectStatsAndErrors(ParseState state, String line) {
    state.linesCount++;
    if (state.isUCPFormatted == null) {
      if (line.contains(UCP)) {
        state.isUCPFormatted = true;
      } else if (DEFAULT_TIMESTAMP_PREFIX.matcher(line).find()) {
        state.isUCPFormatted = false;
      }
    }

    updateTimeBounds(state, line);

    Matcher matcher = WRITTEN_BYTES_PATTERN.matcher(line);
    if (matcher.find()) {
      state.sentPacketCount++;
      state.bytesProduced += Long.parseLong(matcher.group(1).replace(",", ""));
    } else {
      matcher = RECEIVED_BYTES_PATTERN.matcher(line);
      if (matcher.find()) {
        state.receivedPacketCount++;
        state.bytesConsumed += Long.parseLong(matcher.group(1).replace(",", ""));
      }
    }

    if (EXCEPTION_PATTERN.matcher(line).find()) {
      state.errorsCount++;
      state.errorLines.add(state.lineNumber);
    }
  }

  /**
   * Parses executed SQL queries, including multi-line SQL blocks.
   *
   * @param state current parse state.
   * @param line current stripped line.
   */
  private void collectQuery(ParseState state, String line) {
    if (state.queryContent != null) {
      state.queryContent.append(line);
      if (line.contains(", time=")) {
        appendQuery(state, state.queryTimestamp, state.queryContent.toString());
        state.queryTimestamp = null;
        state.queryContent = null;
      } else {
        state.queryContent.append("\n");
      }
      return;
    }

    if (QUERIES_PATTERN.matcher(line).find()) {
      state.queryTimestamp = parseTimestampForLine(state, line);
      state.queryContent = new StringBuilder();
      state.queryContent.append(line);
      if (line.contains(", time=")) {
        appendQuery(state, state.queryTimestamp, state.queryContent.toString());
        state.queryTimestamp = null;
        state.queryContent = null;
      } else {
        state.queryContent.append("\n");
      }
    }
  }

  /**
   * Parses connection opened/closed events from the current line stream.
   *
   * @param state current parse state.
   * @param line current stripped line.
   */
  private void collectConnectionEvent(ParseState state, String line) {
    if (state.pendingClosedTimestamp != null) {
      if (!line.endsWith(" null")) {
        state.connectionEvents.add(new JDBCConnectionEvent(state.pendingClosedTimestamp, JDBCConnectionEvent.Event.CONNECTION_CLOSED));
      }
      state.pendingClosedTimestamp = null;
      return;
    }

    if (state.openDetails != null) {
      if (!state.waitingForCookie && !state.waitingForCookieAfterLogon) {
        if (!line.isBlank()) {
          state.openDetails.append(line).append(" ");
        } else {
          state.openDetails.append(", ");
          state.waitingForCookie = true;
        }
        return;
      }

      if (state.waitingForCookie) {
        if (line.endsWith("logon")) {
          state.waitingForCookie = false;
          state.waitingForCookieAfterLogon = true;
          return;
        }
      }

      int index = line.indexOf("cookie found?");
      if (index >= 0) {
        state.openDetails.append(line.substring(index));
      } else {
        state.openDetails.append(line);
      }

      state.connectionEvents.add(new JDBCConnectionEvent(
        state.openTimestamp,
        JDBCConnectionEvent.Event.CONNECTION_OPENED,
        state.openDetails.toString()
      ));

      state.openTimestamp = null;
      state.openDetails = null;
      state.waitingForCookie = false;
      state.waitingForCookieAfterLogon = false;
      return;
    }

    if (CLOSED_CONNECTIONS_PATTERN.matcher(line).find()) {
      state.pendingClosedTimestamp = parseTimestampForLine(state, line);
      return;
    }

    if (state.pendingLogonLine != null) {
      String combined = state.pendingLogonLine + "\n" + line;
      if (OPENED_CONNECTIONS_PATTERN.matcher(combined).find()) {
        state.openTimestamp = parseTimestampForLine(state, state.pendingLogonLine);
        state.openDetails = new StringBuilder();
        state.pendingLogonLine = null;
        return;
      }
      state.pendingLogonLine = null;
    }

    if (line.contains(LOGON_SIGNATURE)) {
      state.pendingLogonLine = line;
    }

    if (OPENED_CONNECTIONS_PATTERN.matcher(line).find()) {
      state.openTimestamp = parseTimestampForLine(state, line);
      state.openDetails = new StringBuilder();
      state.waitingForCookie = false;
      state.waitingForCookieAfterLogon = false;
    }
  }

  /**
   * Maps captured error line numbers to corresponding log entries.
   *
   * @param state current parse state.
   * @return parsed log errors.
   */
  private List<LogError> buildLogErrors(ParseState state) {
    List<LogError> errors = new ArrayList<>();
    int logIndex = 0;
    List<LogEntry> logs = state.logEntries;

    for (Integer errorLine : state.errorLines) {
      while (logIndex < logs.size()) {
        LogEntry log = logs.get(logIndex);
        int endLine = log.getEndLine();

        if (endLine != -1 && endLine < errorLine) {
          logIndex++;
          continue;
        }

        if (log.getBeginLine() <= errorLine) {
          errors.add(new LogError(logs, state.traceLines, log));
        }
        break;
      }
    }
    return errors;
  }

  /**
   * Builds aggregated statistics from parse state.
   *
   * @param state current parse state.
   * @return computed JDBC statistics.
   */
  private JDBCStats buildStats(ParseState state) {
    Duration duration = null;
    String startTime = null;
    String endTime = null;

    if (state.isUCPFormatted != null && state.isUCPFormatted && state.zonedStart != null && state.zonedEnd != null) {
      startTime = state.zonedStart.toString();
      endTime = state.zonedEnd.toString();
      duration = Duration.between(state.zonedStart, state.zonedEnd);
    } else if (state.localStart != null && state.localEnd != null) {
      startTime = state.localStart.toString();
      endTime = state.localEnd.toString();
      duration = Duration.between(state.localStart, state.localEnd);
    }

    double averageQueryTime = state.queries.stream()
      .mapToDouble(JDBCExecutedQuery::executionTime)
      .average()
      .orElse(0);

    long openedConnectionCount = state.connectionEvents.stream()
      .filter(event -> event.event() == JDBCConnectionEvent.Event.CONNECTION_OPENED)
      .count();

    long closedConnectionCount = state.connectionEvents.stream()
      .filter(event -> event.event() == JDBCConnectionEvent.Event.CONNECTION_CLOSED)
      .count();

    return new JDBCStats(
      getFileSize(logLocation),
      state.linesCount,
      startTime,
      endTime,
      duration,
      state.errorsCount,
      state.queries.size(),
      averageQueryTime,
      openedConnectionCount,
      closedConnectionCount,
      state.sentPacketCount,
      state.receivedPacketCount,
      state.bytesConsumed,
      state.bytesProduced
    );
  }

  /**
   * Expands log start/end time bounds from the current line when possible.
   *
   * @param state current parse state.
   * @param line current stripped line.
   */
  private void updateTimeBounds(ParseState state, String line) {
    try {
      if (state.isUCPFormatted != null && state.isUCPFormatted) {
        if (!line.contains(UCP)) {
          return;
        }

        ZonedDateTime timestamp = ZonedDateTime.parse(line.split(UCP)[0].strip(), LogError.UCP_TIMESTAMP_FORMATTER);
        if (state.zonedStart == null || timestamp.isBefore(state.zonedStart)) {
          state.zonedStart = timestamp;
        }

        if (state.zonedEnd == null || timestamp.isAfter(state.zonedEnd)) {
          state.zonedEnd = timestamp;
        }
      } else {
        Matcher matcher = DEFAULT_TIMESTAMP_PREFIX.matcher(line);
        if (!matcher.find()) {
          return;
        }

        LocalDateTime timestamp = LocalDateTime.parse(matcher.group(1), LogError.DEFAULT_TIMESTAMP_FORMATTER);
        if (state.localStart == null || timestamp.isBefore(state.localStart)) {
          state.localStart = timestamp;
        }

        if (state.localEnd == null || timestamp.isAfter(state.localEnd)) {
          state.localEnd = timestamp;
        }
      }
    } catch (DateTimeParseException | ArrayIndexOutOfBoundsException ignored) {
      // Ignore non timestamp lines
    }
  }

  /**
   * Extracts and normalizes a timestamp from a single line.
   *
   * @param state current parse state.
   * @param line current stripped line.
   * @return normalized timestamp or {@code null} when not parseable.
   */
  private String parseTimestampForLine(ParseState state, String line) {
    try {
      if (Boolean.TRUE.equals(state.isUCPFormatted) && line.contains(UCP)) {
        return ZonedDateTime.parse(line.split(UCP)[0].strip(), LogError.UCP_TIMESTAMP_FORMATTER).toString();
      }

      Matcher matcher = DEFAULT_TIMESTAMP_PREFIX.matcher(line);
      if (matcher.find()) {
        return LocalDateTime.parse(matcher.group(1), LogError.DEFAULT_TIMESTAMP_FORMATTER).toString();
      }
    } catch (DateTimeParseException | ArrayIndexOutOfBoundsException ignored) {
      // best effort
    }

    return null;
  }

  /**
   * Parses a query block and appends it to the parsed query list.
   *
   * @param state current parse state.
   * @param timestamp query timestamp.
   * @param queryBlock raw query block content.
   */
  private void appendQuery(ParseState state, String timestamp, String queryBlock) {
    String sql = null;
    int executionTime = 0;
    String connectionId = null;
    String tenant = null;

    Matcher sqlAndTimeMatcher = SQL_AND_TIME_PATTERN.matcher(queryBlock);
    if (sqlAndTimeMatcher.find()) {
      sql = sqlAndTimeMatcher.group(1);
      try {
        executionTime = Integer.parseInt(sqlAndTimeMatcher.group(2).replace("ms", "").strip());
      } catch (NumberFormatException ignored) {
        // no-op
      }
    }

    Matcher connectionIdAndTenantMatcher = CONNECTION_ID_AND_TENANT_PATTERN.matcher(queryBlock);
    if (connectionIdAndTenantMatcher.find()) {
      connectionId = connectionIdAndTenantMatcher.group(1);
      tenant = connectionIdAndTenantMatcher.group(2);
    }

    state.queries.add(new JDBCExecutedQuery(timestamp, sql, executionTime, connectionId, tenant));
  }

  /**
   * <p>
   *   Get all the errors reported in the log file.
   * </p>
   *
   * @return List of errors
   * @see LogError
   */
  public List<LogError> getLogErrors() {
    return logErrors;
  }

  /**
   * <p>
   *   Generate statistics from the log file.
   * </p>
   *
   * @return {@link JDBCStats} object
   */
  public JDBCStats getStats() {
    return stats;
  }

  /**
   * <p>
   *   Retrieve the executed SQL statements with the timestamp and the execution time.
   * </p>
   *
   * @return {@link List} of {@link JDBCExecutedQuery}
   */
  public List<JDBCExecutedQuery> getQueries() {
    return queries;
  }

  /**
   * <p>
   *  Retrieve the connection opened/closed events.
   * </p>
   *
   * @return {@link List} of {@link JDBCConnectionEvent}
   */
  public List<JDBCConnectionEvent> getConnectionEvents() {
    return connectionEvents;
  }

  /**
   * <p>
   *   Compare {@code this} log file with another one.
   * </p>
   *
   * @param filepath path to the Oracle JDBC log file.
   * @return {@link JDBCLogComparison} object.
   * @throws IOException if an error occurs while reading the log files.
   */
  public JDBCLogComparison compareTo(final String filepath) throws IOException {
    // this = reference
    // other =  supplied log file
    final JDBCLog other = new JDBCLog(filepath);
    final JDBCStats thisStats = this.getStats();
    final JDBCStats otherStats = other.getStats();

    var summary = new JDBCLogComparison.Summary(
      this.logLocation,
      other.logLocation,

      thisStats.fileSize(),
      otherStats.fileSize(),

      thisStats.lineCount(),
      otherStats.lineCount(),
      JDBCLogComparison.delta(thisStats.lineCount(), otherStats.lineCount()),

      thisStats.timespan(),
      thisStats.duration(),

      otherStats.timespan(),
      otherStats.duration()
    );

    var performance = new JDBCLogComparison.Performance(
      thisStats.queryCount(),
      otherStats.queryCount(),
      JDBCLogComparison.delta(thisStats.queryCount(), otherStats.queryCount()),

      thisStats.averageQueryTime(),
      otherStats.averageQueryTime(),
      JDBCLogComparison.delta(thisStats.averageQueryTime(), otherStats.averageQueryTime())
    );

    final var referenceErrorCount = thisStats.errorCount();
    final var otherErrorCount = otherStats.errorCount();

    var error = new JDBCLogComparison.Error(
      referenceErrorCount,
      otherErrorCount,
      JDBCLogComparison.delta(referenceErrorCount, otherErrorCount)
    );

    final var referenceConsumed = thisStats.bytesConsumed();
    final var otherConsumed = otherStats.bytesConsumed();

    final var referenceProduced = thisStats.bytesProduced();
    final var otherProduced = otherStats.bytesProduced();

    var network = new JDBCLogComparison.Network(
      thisStats.bytesConsumed(),
      otherStats.bytesConsumed(),
      JDBCLogComparison.delta(referenceConsumed, otherConsumed),
      thisStats.bytesProduced(),
      otherStats.bytesProduced(),
      JDBCLogComparison.delta(referenceProduced, otherProduced)
    );

    return new JDBCLogComparison(summary, performance, error, network);
  }

}
