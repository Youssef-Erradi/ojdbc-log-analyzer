/*
 ** OJDBC Log Analyzer version 1.0.0
 **
 ** Copyright (c) 2025 Oracle and/or its affiliates.
 ** Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */

package oracle.jdbc.logs.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
  private String lines;              //The actual log.

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
    this.logFile = logFile;
    this.beginLine = beginLine;
    this.endLine = endLine;
    this.beginPosition = beginPosition;
  }

  private BufferedReader getBufferedReader() throws IOException {
    if (logFile.startsWith("http://") || logFile.startsWith("https://")) {
      URL url = new URL(logFile);
      return new BufferedReader(new InputStreamReader(url.openStream()));
    } else {
      return new BufferedReader(new FileReader(logFile));
    }
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
    try (final BufferedReader reader = getBufferedReader()) {

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
      try (final BufferedReader reader = getBufferedReader()) {

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
}
