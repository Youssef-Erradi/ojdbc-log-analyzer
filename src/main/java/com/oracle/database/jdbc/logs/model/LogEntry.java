/*
 ** OJDBC Log Analyzer version 1.0.0
 **
 ** Copyright (c) 2025 Oracle and/or its affiliates.
 ** Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */

package com.oracle.database.jdbc.logs.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.oracle.database.jdbc.logs.analyzer.Utils.getBufferedReader;

/**
 * A LogEntry is a collection of consecutive log lines in a log file.
 * It corresponds to 1 call to the logger (which may be more than 1 line).
 */
public class LogEntry {

  /**
   * The log file where we found this log
   */
  private final String logFile;

  /**
   * The line in the log file where this log started
   */
  private final int beginLine;

  /**
   * The line in the log file where this log ended
   */
  private final int endLine;

  /**
   * The offset in bytes where to find this log in the file.
   */
  private final long beginPosition;

  /**
   * The offset of the last trace line before parsing this log entry.
   */
  private final long lastTraceLineOffset;

  /**
   * The index of the last trace line before parsing this log entry.
   */
  private String lastTraceLine;

  /**
   * The thread ID where this log happened.
   */
  private Integer threadId;

  /**
   * The fist line of this log.
   */
  private String firstLine;

  /**
   * The actual log with stacktrace.
   */
  private String lines;

  /**
   * RegEx to extract the thread id from the log line.
   */
  public static final Pattern THREAD_ID_PATTERN = Pattern.compile("^(FINEST|FINER|FINE|CONFIG|INFO|WARNING|SEVERE):\\s[A-Z]:thread-(\\d)+");

  /**
   * <p>
   *   Creates a log entry instance.
   * </p>
   * @param logFile the log file location.
   * @param beginLine number of the beginning line
   * @param endLine number of the end  line.
   * @param beginPosition the beginning offset.
   */
  public LogEntry(String logFile, int beginLine, int endLine, long beginPosition) {
    this(logFile, beginLine, endLine, beginPosition, -1);
  }

  /**
   * <p>
   *   Constructs a new {@code LogEntry} instance representing a collection of consecutive
   *   log lines in a log file with specified log content.
   * </p>
   * <p>
   *   The offset for the last trace line before this entry is set to {@code -1}.
   * </p>
   *
   * @param logFile            the absolute or relative path of the log file in which this entry was found
   * @param beginLine          the line number in the log file where this log entry starts (inclusive)
   * @param endLine            the line number in the log file where this log entry ends (inclusive)
   * @param beginPosition      the byte offset within the log file where this log entry begins
   * @param lines              the full log text for this entry, potentially including stack traces
   */
  public LogEntry(String logFile, int beginLine, int endLine, long beginPosition, String lines) {
    this(logFile, beginLine, endLine, beginPosition, -1);
    this.lines = lines;
  }

  /**
   * <p>
   *   Creates a new {@code LogEntry} instance representing a collection of consecutive log lines in a log file.
   * </p>
   *
   * @param logFile            the absolute or relative path of the log file in which this entry was found
   * @param beginLine          the line number in the log file where this log entry starts (inclusive)
   * @param endLine            the line number in the log file where this log entry ends (inclusive)
   * @param beginPosition      the byte offset within the log file where this log entry begins
   * @param lines              the full log text for this entry, potentially including stack traces
   * @param lastTraceLineOffset the byte offset of the last trace line before this log entry.
   */
  public LogEntry(String logFile, int beginLine, int endLine, long beginPosition, String lines, long lastTraceLineOffset) {
    this(logFile, beginLine, endLine, beginPosition, lastTraceLineOffset);
    this.lines = lines;
  }

  /**
   * <p>
   *   Creates a new {@code LogEntry} instance representing a collection of consecutive log lines in a log file.
   * </p>
   *
   * @param logFile            the absolute or relative path of the log file in which this entry was found
   * @param beginLine          the line number in the log file where this log entry starts (inclusive)
   * @param endLine            the line number in the log file where this log entry ends (inclusive)
   * @param beginPosition      the byte offset within the log file where this log entry begins
   * @param lines              the full log text for this entry, potentially including stack traces
   * @param lastTraceLineOffset the byte offset of the last trace line before this log entry.
   * @param lastTraceLine      the content of the last trace line before this entry.
   */
  public LogEntry(String logFile, int beginLine, int endLine, long beginPosition, String lines, long lastTraceLineOffset, String lastTraceLine) {
    this(logFile, beginLine, endLine, beginPosition, lastTraceLineOffset);
    this.lines = lines;
    this.lastTraceLine = lastTraceLine;
  }

  /**
   * <p>
   *   Creates a log entry instance.
   * </p>
   * @param logFile the log file location.
   * @param beginLine number of the beginning line
   * @param endLine number of the end  line.
   * @param beginPosition the beginning offset.
   * @param lastTraceLineOffset the offset of the last trace before this log entry
   */
  public LogEntry(String logFile, int beginLine, int endLine, long beginPosition, long lastTraceLineOffset) {
    this.logFile = logFile;
    this.beginLine = beginLine;
    this.endLine = endLine;
    this.beginPosition = beginPosition;
    this.lastTraceLineOffset = lastTraceLineOffset;
  }

  /**
   * <p>
   *   Returns the absolute path of the log file.
   * </p>
   *
   * @return absolute path of the log file.
   */
  public String getLogFile() {
    return this.logFile;
  }

  /**
   * <p>
   *   Return the number of this log entry's begin line.
   * </p>
   *
   * @return Returns begin line number
   */
  public int getBeginLine() {
    return this.beginLine;
  }

  /**
   * <p>
   *   Returns the number of this log entry's last line.
   * </p>
   *
   * @return the last line of this log entry.
   * @see #getFirstLine()
   * @see #getLines()
   */
  public int getEndLine() {
    return endLine;
  }

  /**
   * <p>
   *   Returns the log that corresponds to this log entry.
   * </p>
   *
   * @return lines of this log entry.
   * @throws IOException if an error occurs while reading the log file.
   * @see #getFirstLine()
   * @see #getEndLine()
   */
  public String getLines() throws IOException {
    if (lines != null)
      return lines;

    StringBuilder result = new StringBuilder();
    try (final BufferedReader reader = getBufferedReader(logFile)) {

      if (reader.ready())
        reader.skip(beginPosition);

      if (getEndLine() != -1) {
        // The log ends at getEndLine()
        int linesToRead = getEndLine() - getBeginLine() + 1;

        while (reader.ready() && linesToRead > 0) {
          String line = reader.readLine();
          result.append(line).append("\n");
          linesToRead--;
        }
      } else {
        // The log ends at the end of the file
        while (reader.ready() ) {
          String line = reader.readLine();
          result.append(line).append("\n");
        }
      }
    }

    lines = result.toString();

    return lines;
  }

  /**
   * <p>
   *   Returns the last trace line preceding this log entry from the log file.
   * </p>
   *
   * @return the last trace line as a {@link String}, or {@code null} if there is no trace line offset.
   * @throws IOException if an error occurs while reading the log file.
   */
  public String getLastTrace() throws IOException {
    if (lastTraceLine != null) {
      return lastTraceLine;
    }

    if (lastTraceLineOffset == -1)
      return null;

    try (final BufferedReader reader = getBufferedReader(logFile)) {
      if (reader.ready())
        reader.skip(lastTraceLineOffset);

      lastTraceLine = reader.readLine();
    }

    return lastTraceLine;
  }

  /**
   * <p>
   *   Returns the first line of this log entry.
   * </p>
   *
   * @return the first line of the log entry.
   * @throws IOException if an error occurs while reading the log file.
   * @see #getLines()
   */
  public String getFirstLine() throws IOException {
    if (this.firstLine != null) {
      return this.firstLine;
    }

    if (this.endLine == this.beginLine) {
      // This log is only a single line
      this.firstLine = getLines();
    } else {
      try (final BufferedReader reader = getBufferedReader(logFile)) {

        if (reader.ready())
          reader.skip(beginPosition);

        this.firstLine = reader.readLine();
      }
    }

    return this.firstLine;
  }

  /**
   * <p>
   *   Returns the thread id that logged this entry.
   * </p>
   *
   * @return the thread id reported in the log entry.
   * @throws IOException if an error occurs while reading the log file.
   */
  public int getThreadId() throws IOException {
    if (this.threadId != null) {
      return this.threadId;
    }

    Matcher matcher = THREAD_ID_PATTERN.matcher(getFirstLine());
    if (matcher.find()) {
      this.threadId = Integer.valueOf(matcher.group(2));
    }

    return this.threadId;
  }

  public String toString() {
    return "beginLine: " + beginLine + ", endLine: " + endLine;
  }

  /**
   * <p>
   *   Returns a JSON string representation of this object.
   * </p>
   *
   * @return a JSON-formatted {@link String} representing the current state of this object
   */
  public String toJSONString() {
    return """
      {"logFile": "%s","beginLine": %d,"endLine": %d}
      """.formatted(logFile, beginLine, endLine)
      .strip();
  }

}
