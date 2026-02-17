package com.oracle.database.jdbc.logs.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.oracle.database.jdbc.logs.analyzer.Utils.*;

/**
 * <p>
 *   A parser for Oracle JDBC log files.
 * </p>
 * <p>
 *   This class provides a way to parse Oracle JDBC log files and extract log entries.
 *   It uses a {@link LogEntryProcessor} to process each log entry as it is parsed.
 * </p>
 *
 * @see LogEntryProcessor
 */
public class LogParser {

  /**
   * Interface for processing individual log entries during log parsing.
   * <p>
   *   Implementations of this interface can be passed to a log parser to receive callbacks when
   *   new log lines with their associated traces are parsed. The processor can then perform
   *   custom logic on each parsed {@link LogEntry}, such as filtering, collecting, or otherwise handling log data.
   * </p>
   */
  public interface LogEntryProcessor {
    /**
     * A method invoked after the parser parsed a log line with the corresponding trace.
     *
     * @param entry the current log line being parsed
     * @return boolean if this processor matched this line or not.
     * @throws IOException if an I/O error occurs
     */
    boolean onNewLogEntry(LogEntry entry) throws IOException;
  }

  private final String logLocation;
  private List<LogEntry> logEntries;

  /**
   * Traces start with a timestamp, fully qualified class name, method name. They can't be more than 1 line.
   */
  static final Pattern TRACE_PATTERN = Pattern.compile("^([a-zA-Z]{3}\\s\\d{1,2},\\s\\d{2,4}\\s\\d{1,2}:\\d{1,2}:\\d{1,2}\\s[A-Z]{2})\\s([a-zA-Z0-9.$]*\\s[a-zA-Z0-9.()<>]*$)");

  /**
   * Log entries start with FINEST, FINER, FINE, CONFIG, INFO, WARNING, SEVERE and finish at the next log / trace.
   */
  static final Pattern LOG_PATTERN = Pattern.compile("( UCP )?(FINEST|FINER|FINE|CONFIG|INFO|WARNING|SEVERE)");

  /**
   * <p>
   *   Creates a new {@code LogParser} instance for the specified log location.
   * </p>
   *
   * @param logLocation the URL or path to the Oracle JDBC log file
   */
  public LogParser(String logLocation) {
    this.logLocation = logLocation;
  }

  /**
   * <p>
   *   Parses the log file and invokes the specified {@code LogEntryProcessor} for each log entry.
   * </p>
   *
   * @param logEntryProcessor the processor to invoke for each log entry
   * @throws IOException if an I/O error occurs while parsing the log file
   */
  public void parse(LogEntryProcessor logEntryProcessor) throws IOException {
    logEntries = new ArrayList<>();

    StringBuilder logBuilder = new StringBuilder();
    int lineNumber = 1;
    long positionInFile = 0;
    long lastTracePositionInFile = 0;
    String lastTrace = null;
    int logBeginLineIndex = 0;
    long logBeginPosition = 0;

    try (final BufferedReader reader = getBufferedReader(logLocation)) {
      while (reader.ready()) {
        // a trace is never more than 1 line
        String line = reader.readLine();

        Matcher matcher = TRACE_PATTERN.matcher(line);
        if (matcher.find()) {
          // This is a trace

          if (!logBuilder.isEmpty()) {
            LogEntry logEntry = new LogEntry(logLocation,
                logBeginLineIndex,
                lineNumber - 1,
                logBeginPosition,
                logBuilder.toString(),
                lastTracePositionInFile,
                lastTrace);

            logEntryProcessor.onNewLogEntry(logEntry);
            logEntries.add(logEntry);

            logBuilder.setLength(0);
          }

          lastTrace = line;
          lastTracePositionInFile = positionInFile;

        } else {

          matcher = LOG_PATTERN.matcher(line);
          if (matcher.find()) {
            // This is the beginning of a log entry

            if (!logBuilder.isEmpty()) {
              LogEntry logEntry = new LogEntry(logLocation,
                  logBeginLineIndex,
                  lineNumber - 1,
                  logBeginPosition,
                  logBuilder.toString(),
                  lastTracePositionInFile,
                  lastTrace);

              logEntryProcessor.onNewLogEntry(logEntry);
              logEntries.add(logEntry);

              logBuilder.setLength(0);
            }

            logBeginLineIndex = lineNumber;
            logBeginPosition = positionInFile;
          }

          logBuilder.append(line).append("\n");
        }

        positionInFile += (line.length() + 1);
        lineNumber++;
      }
    }

    if (!logBuilder.isEmpty()) {
      String log = logBuilder.toString();

      LogEntry logEntry = new LogEntry(logLocation,
          logBeginLineIndex,
          -1,
          logBeginPosition,
          log,
          lastTracePositionInFile,
          lastTrace);

      logEntryProcessor.onNewLogEntry(logEntry);
      logEntries.add(logEntry);

      logBuilder.setLength(0);
    }
  }

  /**
   * <p>
   *   Returns the list of log entries parsed from the log file.
   * </p>
   *
   * @return the list of log entries
   */
  public List<LogEntry> getLogEntries() {
    return this.logEntries;
  }

}
