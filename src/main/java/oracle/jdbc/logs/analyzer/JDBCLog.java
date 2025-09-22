/*
 ** OJDBC Log Analyzer version 1.0.0
 **
 ** Copyright (c) 2025 Oracle and/or its affiliates.
 ** Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */

package oracle.jdbc.logs.analyzer;

import oracle.jdbc.logs.model.*;

import java.net.URL;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static oracle.jdbc.logs.analyzer.Utils.*;

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
  static final Pattern EXCEPTION_PATTERN = Pattern.compile("^(.*)\\..*Exception: ORA-");

  /**
   * Keyword to determine if the log file is formatted by {@code UCPFormatter}.
   */
  static final String UCP = " UCP ";

  // Boolean to know whether the field has been initialized or not.
  private Boolean isUCPFormatted;

  private final String logLocation;
  private List<LogEntry> logEntries;
  private List<LogLine> traceLines;
  private List<LogLine> logLines;
  private JDBCStats stats;
  private List<JDBCExecutedQuery> executedQueries;
  private List<JDBCConnectionEvent> connectionEvents;

  /**
   * <p>
   *   Creates an instance capable of parsing the Oracle JDBC log file.
   * </p>
   *
   * @param logLocation URL or path to the Oracle JDBC log file.
   * @throws IllegalArgumentException If {@code logLocation} is null or empty.
   */
  public JDBCLog(String logLocation) throws IllegalArgumentException {
    Utils.requireNonBlank(logLocation, "logLocation cannot be null nor blank.");
    this.logLocation = logLocation;
  }

  private List<LogLine> extractTraceLines() throws IOException  {
    List<LogLine> tracesLines = new ArrayList<>();

    try (final BufferedReader reader = getBufferedReader(logLocation)) {
      int lineNumber = 1;
      long positionInFile = 0;
      while (reader.ready()) {
        // a trace is never more than 1 line
        String line = reader.readLine();

        Matcher matcher = TRACE_PATTERN.matcher(line);
        if (matcher.find()) {
          tracesLines.add(new LogLine(lineNumber, positionInFile));
        }

        positionInFile += (line.length() + 1);
        lineNumber++;
      }
    }

    return tracesLines;
  }

  private List<LogLine> extractLogLines() throws IOException  {
    List<LogLine> logLines = new ArrayList<>();

    try (final BufferedReader reader = getBufferedReader(logLocation)) {
      int lineNumber = 1;
      long positionInFile = 0;
      while (reader.ready()) {
        String line = reader.readLine();

        Matcher matcher = LOG_PATTERN.matcher(line);
        if (matcher.find()) {
          logLines.add(new LogLine(lineNumber, positionInFile));
        }

        positionInFile += (line.length() + 1);
        lineNumber++;
      }
    }

    return logLines;
  }

  private List<LogEntry> parse() throws IOException {
    if (logEntries != null) {
      return logEntries;
    }

    // Differentiate between trace lines and log lines
    // traces can only be 1 line but logs can be multiple lines.
    traceLines = extractTraceLines();
    logLines = extractLogLines();

    logEntries = new ArrayList<>(logLines.size());
    int traceLineIndex = 0, logLineIndex = 0, logBeginLineIndex = 0;
    long logBeginPosition = 0;
    boolean inLogEntry = false;
    for (int i = 0; i < (traceLines.size() + logLines.size()); i++) {
      if (traceLineIndex < traceLines.size() && traceLines.get(traceLineIndex).getLineNumber() < logLines.get(logLineIndex).getLineNumber()) {
        if (inLogEntry) {
          logEntries.add(new LogEntry(logLocation, logBeginLineIndex, traceLines.get(traceLineIndex).getLineNumber() - 1, logBeginPosition));
        }
        inLogEntry = false;
        traceLineIndex++;
      } else {
        if (inLogEntry) {
          logEntries.add(new LogEntry(logLocation, logBeginLineIndex, logLines.get(logLineIndex).getLineNumber() - 1, logBeginPosition));
        }
        logBeginLineIndex = logLines.get(logLineIndex).getLineNumber();
        logBeginPosition = logLines.get(logLineIndex).getPositionInFile();
        inLogEntry = true;
        logLineIndex++;
      }
    }

    if (inLogEntry) {
      logEntries.add(new LogEntry(logLocation, logBeginLineIndex, -1, logBeginPosition));
    }

    return logEntries;
  }

  /**
   * <p>
   *   Get all the errors reported in the log file.
   * </p>
   *
   * @return List of errors
   * @throws IOException if an error occurs while reading the log file.
   * @see LogError
   */
  public List<LogError> getLogErrors() throws IOException {
    List<Integer> errorLines = new ArrayList<>();

    try (final BufferedReader reader = getBufferedReader(logLocation)) {
      int lineNumber = 1;
      while (reader.ready()) {
        Matcher matcher = EXCEPTION_PATTERN.matcher(reader.readLine());
        if (matcher.find()) {
          errorLines.add(lineNumber);
        }
        lineNumber++;
      }
    }

    List<LogError> result = new ArrayList<>();
    if (!errorLines.isEmpty()) {
      // We found some errors, we should associate them with logs.
      List<LogEntry> logs = parse();

      for (Integer errorLine : errorLines) {
        for (LogEntry log : logs) {
          if (log.getBeginLine() <= errorLine && (log.getEndLine() >= errorLine || log.getEndLine() == -1)) {
            // This is the entire log for this error
            result.add(new LogError(logs, this.traceLines, log));
            break;
          }
        }
      }
    }

    return result;
  }

  /**
   * <p>
   *   Generate statistics from the log file.
   * </p>
   *
   * @return {@link JDBCStats} object
   * @throws IOException if an error occurs while reading the log file.
   */
  public JDBCStats getStats() throws IOException {
    if (stats != null)
      return stats;

    final Pattern writtenBytesPattern = Pattern.compile("(\\d+|\\d{1,3}(?:,\\d{3})*) bytes written to the Socket");
    final Pattern receivedBytesPattern = Pattern.compile("(\\d+|\\d{1,3}(?:,\\d{3})*) bytes$");

    long errorsCount = 0;
    long receivedPacketCount = 0;
    long sentPacketCount = 0;
    long bytesConsumed = 0;
    long bytesProduced = 0;
    long linesCount = 0;
    String startTime = null;
    String endTime = null;

    Duration zonedDiff = null;
    Duration localDiff = null;

    String line;
    try (final BufferedReader bufferedReader = getBufferedReader(logLocation)) {
      while( (line = bufferedReader.readLine()) != null) {

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

        linesCount++;

        matcherHolder = writtenBytesPattern.matcher(line);
        if (matcherHolder.find()) {
          sentPacketCount ++;
          final String bytesString = matcherHolder.group(1).replace(",", "");
          bytesProduced += Long.parseLong(bytesString);
          continue;
        }

        matcherHolder = receivedBytesPattern.matcher(line);
        if (matcherHolder.find()) {
          receivedPacketCount++;
          final String bytesString = matcherHolder.group(1).replace(",", "");
          bytesConsumed += Long.parseLong(bytesString);
          continue;
        }

        if (EXCEPTION_PATTERN.matcher(line).find()) {
          errorsCount++;
        }

      }
    }

    final var averageQueryTime = getQueries().stream()
      .mapToDouble(JDBCExecutedQuery::executionTime)
      .average()
      .orElse(0);

    final long openedConnectionCount = getConnectionEvents().stream()
      .filter(jdbcConnectionEvent -> jdbcConnectionEvent.event() == JDBCConnectionEvent.Event.CONNECTION_OPENED)
      .count();

    final long closedConnectionCount = getConnectionEvents().stream()
      .filter(jdbcConnectionEvent -> jdbcConnectionEvent.event() == JDBCConnectionEvent.Event.CONNECTION_CLOSED)
      .count();

    stats = new JDBCStats(getFileSize(logLocation), linesCount, startTime, endTime,
      localDiff != null ? localDiff : zonedDiff, errorsCount, getQueries().size(),
      averageQueryTime,  openedConnectionCount, closedConnectionCount,
      receivedPacketCount, sentPacketCount, bytesConsumed, bytesProduced);

    return stats;
  }

  /**
   * <p>
   *   Retrieve the executed SQL statements with the timestamp and the execution time.
   * </p>
   *
   * @return {@link List} of {@link JDBCExecutedQuery}
   * @throws IOException if an error occurs while reading the log file.
   */
  public List<JDBCExecutedQuery> getQueries() throws IOException {
    if (executedQueries != null && !executedQueries.isEmpty())
      return executedQueries;

    final Pattern queriesPattern = Pattern.compile("[\\s|.]endCurrentSql");
    final Pattern sqlAndTimePattern = Pattern.compile("sql=([\\S\\s]*), time=(.*)", Pattern.MULTILINE);
    final List<JDBCExecutedQuery> queries = new ArrayList<>();

    String line;
    StringBuilder multilineSql;
    try (final BufferedReader bufferedReader = getBufferedReader(logLocation)) {
      while ((line = bufferedReader.readLine()) != null) {
        if (isUCPFormatted == null)
          isUCPFormatted = line.contains(UCP);

        line = line.strip();
        String timestamp;
        multilineSql = new StringBuilder();
        final Matcher queryMatcher = queriesPattern.matcher(line);

        if (queryMatcher.find()) {
          if (isUCPFormatted)
            timestamp = ZonedDateTime.parse(line.split(UCP)[0].strip(), LogError.UCP_TIMESTAMP_FORMATTER).toString();
          else {
            timestamp = line.replace(" oracle.jdbc.driver.ConnectionDiagnosable endCurrentSql", "").strip();
            timestamp = LocalDateTime.parse(timestamp, LogError.DEFAULT_TIMESTAMP_FORMATTER).toString();
          }

          // Multiline SQL Queries, for example:
          // ... sql=update /*+ index(orders, order_pk) */
          // orders
          // set order_status = ?
          // where order_id = ?, time=14ms
          while (!line.contains(", time=")) {
            multilineSql.append(line)
              .append("\n");
            line = bufferedReader.readLine();
          }

          // Add the last line (that has the `, time=` string).
          multilineSql.append(line);

          final Matcher sqlAndTimeMatcher = sqlAndTimePattern.matcher(multilineSql.toString());
          if (sqlAndTimeMatcher.find())
            queries.add(new JDBCExecutedQuery(timestamp, sqlAndTimeMatcher.group(1),
              Integer.parseInt(sqlAndTimeMatcher.group(2).replace("ms","").strip())));
        }
      }
    }

    executedQueries = queries;
    return executedQueries;
  }

  /**
   * <p>
   *  Retrieve the connection opened/closed events.
   * </p>
   *
   * @return {@link List} of {@link JDBCConnectionEvent}
   * @throws IOException if an error occurs while reading the log file.
   */
  public List<JDBCConnectionEvent> getConnectionEvents() throws IOException {
    if (connectionEvents!= null && !connectionEvents.isEmpty())
      return connectionEvents;
    final Pattern openedConnectionsPattern = Pattern.compile(" oracle.jdbc.driver.T4CConnection[. ]logon\\s.*Session Attributes:");
    final Pattern closedConnectionsPattern = Pattern.compile(" oracle.jdbc.driver.T4CConnection[. ]logoff$");
    final List<JDBCConnectionEvent> events = new ArrayList<>();

    String line;
    try (final BufferedReader bufferedReader = getBufferedReader(logLocation)) {
      while ((line = bufferedReader.readLine()) != null) {
        if (isUCPFormatted == null)
          isUCPFormatted = line.contains(UCP);

        line = line.strip();
        Matcher matcherHolder;
        String timestamp;

        matcherHolder = closedConnectionsPattern.matcher(line);
        if (matcherHolder.find()) {
          if (!line.contains("ORA-17909") && !line.contains("CONNECTION_ID")) {
            if (isUCPFormatted)
              timestamp = ZonedDateTime.parse(line.split(UCP)[0].strip(), LogError.UCP_TIMESTAMP_FORMATTER).toString();
            else {
              timestamp = line.split(closedConnectionsPattern.pattern())[0];
              timestamp = LocalDateTime.parse(timestamp, LogError.DEFAULT_TIMESTAMP_FORMATTER).toString();
            }
            events.add(new JDBCConnectionEvent(timestamp, JDBCConnectionEvent.Event.CONNECTION_CLOSED));
          }
          continue;
        }

        if (line.contains("oracle.jdbc.driver.T4CConnection logon"))
          line += "\n" + bufferedReader.readLine();

        matcherHolder = openedConnectionsPattern.matcher(line);
        if (matcherHolder.find()) {
          if (isUCPFormatted)
            timestamp = ZonedDateTime.parse(line.split(UCP)[0].strip(), LogError.UCP_TIMESTAMP_FORMATTER).toString();
          else {
            timestamp = line.split(openedConnectionsPattern.pattern().substring(0, openedConnectionsPattern.pattern().length()-1))[0];
            timestamp = LocalDateTime.parse(timestamp, LogError.DEFAULT_TIMESTAMP_FORMATTER).toString();
          }
          StringBuilder details = new StringBuilder();
          String temp = bufferedReader.readLine();
          while (!temp.isBlank()) {
            details.append(temp).append(" ");
            temp = bufferedReader.readLine();
          }
          details.append(", ");
          temp = bufferedReader.readLine();
          if (temp.endsWith("logon"))
            temp = bufferedReader.readLine();
          details.append(temp.substring(temp.indexOf("cookie found?")));
          events.add(new JDBCConnectionEvent(timestamp, JDBCConnectionEvent.Event.CONNECTION_OPENED, details.toString()));
        }

      }
    }

    connectionEvents = events;
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
      JDBCLogComparison.delta(this.getStats().averageQueryTimeAsDouble(), other.getStats().averageQueryTimeAsDouble())
    );

    final var referenceErrorCount = this.getStats().errorCount();
    final var otherErrorCount = other.getStats().errorCount();

    var error = new JDBCLogComparison.Error(
      referenceErrorCount,
      otherErrorCount,
      JDBCLogComparison.delta(referenceErrorCount, otherErrorCount)
    );

    final var referenceConsumed = this.getStats().bytesConsumedAsDouble();
    final var otherConsumed = other.getStats().bytesConsumedAsDouble();

    final var referenceProduced = this.getStats().bytesProducedAsDouble();
    final var otherProduced = other.getStats().bytesProducedAsDouble();

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
