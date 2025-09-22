/*
 ** OJDBC Log Analyzer version 1.0.0
 **
 ** Copyright (c) 2025 Oracle and/or its affiliates.
 ** Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */

package oracle.jdbc.logs.model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A LogError is a LogEntry recognized as an error
 */
public class LogError {

  /**
   * Pattern to extract the CONNECTION_ID from an error, i.e. {@code (CONNECTION_ID=k+5OXkWXQ7KpAof3H0FpKg==)}.
   */
  public static final Pattern CONN_ID_PATTERN = Pattern.compile("CONNECTION_ID=([^,)]*)");

  /**
   * Pattern to extract the TENANT from an error (i.e. {code TENANT=CDB1_PDB1}).
   */
  public static final Pattern TENANT_PATTERN = Pattern.compile("TENANT=([^,]*)");

  /**
   * Pattern to extract {@code ORA-} code and the error message.
   */
  public static final Pattern ERROR_DETAIL_PATTERN = Pattern.compile(".* (ORA-\\d{5}): (.*)");

  /**
   * Pattern to extract the {@code Error Message}, the {@code sql} and the {@code Original SQL} from an error.
   */
  public static final Pattern ERROR_SQL_DETAIL_PATTERN = Pattern.compile(".*Caused by: Error : \\d*,.*, SQL = (.*), Original SQL = (.*), Error Message = (.*)");

  /**
   * RegEx to extract JDBC packet dumps.
   */
  public static final Pattern JDBC_PACKET_DUMP_PATTERN = Pattern.compile("([\\s0-9A-F]+\\|.{8}\\|)");

  /**
   * String Template for the Database Error Messages website, used as follows:
   * {@code DOCUMENTATION_LINK_TEMPLATE.formatted(oraCode)}
   */
  public static final String DOCUMENTATION_LINK_TEMPLATE = "https://docs.oracle.com/en/error-help/db/%s";

  /**
   * DateTimeFormatter to parse {@code MMM dd, yyyy h:mm:ss a} to {@code yyyy-MM-dd'T'HH:mm:ss}
   * (i.e. {@code Jun 20, 2024 10:27:12 PM} to {code 2024-06-20T22:27:12}).
   */
  public static final DateTimeFormatter DEFAULT_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy h:mm:ss a");

  /**
   * DateTimeFormatter to parse {@code yyyy-MM-dd'T'hh:mm:ss.SSS a Z} to {@code yyyy-MM-dd'T'HH:mm:ss.SSSZ}
   * (i.e. {@code 2024-10-21T12:33:18.887 AM +0000} to {code 2024-10-21T00:33:18.887Z}).
   */
  public static final DateTimeFormatter UCP_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss.SSS a Z");

  /**
   * How far back in the log we should be looking for more info (number of lines).
   */
  public static final int LOG_LINE_MAX_REWIND = 50;

  /**
   * The list of all Logs in the log file
   */
  private final List<LogEntry> allLogs;

  /**
   * The list of all traces in the log file
   */
  private final List<LogLine> allTraces;

  /**
   * The logEntry (i.e. multiple consecutive log lines) where this error appears
   */
  private final LogEntry logEntry;

  /**
   * The SQL executed
   */
  private String sql;

  /**
   * The SQL passed to the JDBC driver
   */
  private String originalSql;

  /**
   * The error message
   */
  private String errorMessage;

  /**
   * The {@code ORA-XXXXX} code in the error message.
   */
  private String errorCode;

  /**
   * The time it took to execute the SQL (in ms).
   */
  private int executionTime = -1;

  /**
   * The list of packet dumps available in the logs.
   */
  private List<JDBCPacketDump> packetDumps;

  /**
   * <p>
   *   Creates an instance with the some log information related to this error.
   * </p>
   *
   * @param allLogs {@link List} corresponding {@link LogEntry}
   * @param allTraces {@code List} corresponding {@link LogLine}
   * @param logEntry corresponding {@link LogEntry}
   */
  public LogError(List<LogEntry> allLogs, List<LogLine> allTraces, LogEntry logEntry) {
    this.allLogs = allLogs;
    this.allTraces = allTraces;
    this.logEntry = logEntry;
  }

  /**
   * <p>
   *   Returns the {@link LogEntry} related to this error.
   * </p>
   *
   * @return  the corresponding log entry.
   */
  public LogEntry getLogEntry() {
    return logEntry;
  }

  /**
   * <p>
   *   Returns the connection id of the error
   * </p>
   *
   * @return connection id in the log file, can be {@code null}.
   * @throws IOException If an error occurs while reading the log file.
   */
  public String getConnectionId() throws IOException {
    Matcher matcher = CONN_ID_PATTERN.matcher(logEntry.getFirstLine());
    if (matcher.find()) {
      return matcher.group(1);
    }

    return null;
  }

  /**
   * <p>
   *   Returns the connection tenant of the error
   * </p>
   *
   * @return tenant reported in the log file, can be {@code null}.
   * @throws IOException If an error occurs while reading the log file.
   */
  public String getTenant() throws IOException {
    Matcher matcher = TENANT_PATTERN.matcher(logEntry.getFirstLine());
    if (matcher.find()) {
      return matcher.group(1);
    }

    return null;
  }

  private void getErrorInfo() throws IOException {
    final Matcher errorDetailMatcher = ERROR_DETAIL_PATTERN.matcher(logEntry.getLines());
    if (errorDetailMatcher.find()) {
      errorCode = errorDetailMatcher.group(1);
      errorMessage = errorCode + ": " + errorDetailMatcher.group(2);
      final Matcher extractSQLDetails = ERROR_SQL_DETAIL_PATTERN.matcher(logEntry.getLines());
      if (extractSQLDetails.find()) {
        sql = extractSQLDetails.group(1);
        originalSql = extractSQLDetails.group(2);
        errorMessage = extractSQLDetails.group(3);
      }
    }
  }

  /**
   * <p>
   *   Returns the SQL that corresponds to this error.
   * </p>
   *
   * @return the SQL statement.
   * @throws IOException If an error occurs while reading the log file.
   */
  public String getSql() throws IOException {
    if (this.sql == null) {
      getErrorInfo();
    }

    return this.sql;
  }

  /**
   * <p>
   *   Returns the original SQL that corresponds to this error.
   * </p>
   * @return the original SQL statement before the driver's parsing.
   * @throws IOException If an error occurs while reading the log file.
   */
  public String getOriginalSql() throws IOException {
    if (this.originalSql == null) {
      getErrorInfo();
    }

    return this.originalSql;
  }

  /**
   * <p>
   *   Returns the error message as reported in the log file.
   * </p>
   *
   * @return The corresponding error message.
   * @throws IOException If an error occurs while reading the log file.
   */
  public String getErrorMessage() throws IOException {
    if (this.errorMessage == null) {
      getErrorInfo();
    }

    return this.errorMessage;
  }


  /**
   * <p>
   *   Returns the {@code ORA} error code.
   * </p>
   *
   * @return error code with {@code ORA-} prefix.
   * @throws IOException If an error occurs while reading the log file.
   */
  public String getErrorCode() throws IOException {
    if (this.errorCode == null) {
      getErrorInfo();
    }

    return this.errorCode;
  }

  /**
   * <p>
   *   Returns the String representation of the URL to the Database Error Messages platform
   *   (for example: <a href="https://docs.oracle.com/en/error-help/db/ora-17003">https://docs.oracle.com/en/error-help/db/ora-17003</a>)
   * </p>
   *
   * @return {@link String} representation the documentation link.
   * @throws IOException If an error occurs while reading the log file.
   */
  public String getDocumentationLink() throws IOException {
    return getErrorCode() == null ? null :
      DOCUMENTATION_LINK_TEMPLATE.formatted(getErrorCode());
  }

  /**
   * <p>
   *   Returns the time it took to execute the query that corresponds to this error.
   * </p>
   *
   * @return query execution time in {@code ms}
   * @throws IOException If an error occurs while reading the log file.
   */
  public int getSQLExecutionTime() throws IOException {
    if (this.executionTime != -1) {
      return this.executionTime;
    }

    int i = 0;
    // Find the log entry in the list;
    while (allLogs.get(i) != getLogEntry()) {
      i++;
    }

    for (int j = i - 1; j > (i-LOG_LINE_MAX_REWIND) && j > 0; j--) {
      Pattern sqlPattern = Pattern.compile("CONNECTION_ID=" + getConnectionId() + "(.*),sql=" + getOriginalSql() + ", time=(\\d*)ms");
      Matcher matcher = sqlPattern.matcher(allLogs.get(j).getLines());
      if (matcher.find()) {
        try {
          executionTime = Integer.parseInt(matcher.group(2));
        } catch (NumberFormatException e) {
          // Invalid time format.
        }

        break;
      }
    }

    return executionTime;
  }

  /**
   * <p>
   *   Returns the formatted packet dumps related to this error.
   * </p>
   *
   * @return String {@link List} of packet dump lines as ot appears in the log file.
   * @throws IOException if an error occurs while reading the log file.
   */
  public List<JDBCPacketDump> getPacketDumps() throws IOException {
    if (this.packetDumps != null) {
      return this.packetDumps;
    }

    packetDumps = new ArrayList<>();

    int i = 0;
    // Find the log entry in the list;
    while (allLogs.get(i) != getLogEntry()) {
      i++;
    }

    for (int j = i - 1; j > (i-LOG_LINE_MAX_REWIND) && j > 0; j--) {
      Pattern packetDumpPattern = Pattern.compile("(.*) CONNECTION_ID=" + getConnectionId() + ",TENANT=" + getTenant() + ",SQL=" + getSql() + "((.*)\n)+" + "(^\\s([0-9A-F]{2}\\s){8}\\s+\\|(.){8}\\|$)+", Pattern.MULTILINE);
      //Pattern packetDumpPattern = Pattern.compile("[\\s0-9A-F]+\\|.{8}\\|", Pattern.MULTILINE);
      Matcher matcher = packetDumpPattern.matcher(allLogs.get(j).getLines());
      if (matcher.find()) {
        final List<String> lines = allLogs.get(j)
          .getLines()
          .lines()
          .filter(line -> !line.isBlank())
          .toList();

        final String log = lines.stream()
          .filter(line -> !JDBC_PACKET_DUMP_PATTERN.matcher(line).find())
          .collect(Collectors.joining(System.lineSeparator()));

        final String packet = lines.stream()
          .filter(line -> JDBC_PACKET_DUMP_PATTERN.matcher(line).find())
          .map(String::strip)
          .collect(Collectors.joining("\n"));

        packetDumps.add(new JDBCPacketDump(log, packet));
      }
    }

    Collections.reverse(packetDumps);

    return packetDumps;
  }

  /**
   * <p>
   *   Returns nearest {@link JDBCTrace}, which gives the timestamp and the FQN
   *   of the class and the method that threw the error.
   * </p>
   *
   * @return {@link JDBCTrace} right before this error was thrown.
   * @throws IOException if an error occurs while reading the log file.
   */
  public JDBCTrace getNearestTrace() throws IOException {
    JDBCTrace trace = null;

    if (allTraces == null || allTraces.isEmpty())
      return trace;

    LogLine nearestTrace = null;
    for (LogLine logLine : allTraces) {
      if (logLine.getLineNumber() < getLogEntry().getBeginLine()) {
        nearestTrace = logLine;
      } else {
        break;
      }
    }

    if (nearestTrace != null) {
      try (FileReader fileReader = new FileReader(getLogEntry().getLogFile());
           BufferedReader reader = new BufferedReader(fileReader)) {

        reader.skip(nearestTrace.getPositionInFile());

        final String traceLine = reader.readLine();
        final String[] traceSegments = traceLine.split(" ");

        final String lastExecutedMethod = traceSegments[traceSegments.length - 2] + " " + traceSegments[traceSegments.length - 1];
        final String stringDate = traceLine.replace(lastExecutedMethod, "").strip();
        final String formattedTimestamp = LocalDateTime.parse(stringDate, DEFAULT_TIMESTAMP_FORMATTER).toString();

        trace = new JDBCTrace(formattedTimestamp, lastExecutedMethod);
      }
    }

    return trace;
  }

  /**
   * <p>
   *   Return the log lines (i.e stacktrace) of this error.
   * </p>
   *
   * @return the corresponding stacktrace lines.
   * @throws IOException if an error occurs while reading the log file.
   */
  public String getLogLines() throws IOException {
    return logEntry.getLines();
  }

  public String toString() {
    try {
      return getErrorMessage() + "\n  SQL = " + getSql() + "\n  Original SQL = " + getOriginalSql();
    } catch (IOException e) {
      return null;
    }
  }
}
