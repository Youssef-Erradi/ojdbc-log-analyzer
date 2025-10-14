/*
 ** OJDBC Log Analyzer version 1.0.0
 **
 ** Copyright (c) 2025 Oracle and/or its affiliates.
 ** Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */

package com.oracle.database.jdbc.logs.model;

/**
 * A LogLine is a single line in a LogFile.
 */
public class LogLine {

  /**
   * The line number of this log line
   */
  private final int lineNumber;

  /**
   * The offset in bytes of this log line
   */
  private final long positionInFile;

  /**
   * <p>
   *   Creates a {@link LogLine} instance.
   * </p>
   *
   * @param lineNumber line number
   * @param positionInFile The offset in bytes of this log line.
   */
  public LogLine(int lineNumber, long positionInFile) {
    this.lineNumber = lineNumber;
    this.positionInFile = positionInFile;
  }

  /**
   * <p>
   *   Returns the line number in the log file.
   * </p>
   *
   * @return Line number of this log line.
   */
  public int getLineNumber() {
    return this.lineNumber;
  }

  /**
   * <p>
   *   Returns the log line position in log file.
   * </p>
   *
   * @return offset in bytes of this log line
   */

  public long getPositionInFile() {
    return this.positionInFile;
  }
}
