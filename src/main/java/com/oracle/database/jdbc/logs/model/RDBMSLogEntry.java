/*
 ** OJDBC Log Analyzer version 1.0.0
 **
 ** Copyright (c) 2025 Oracle and/or its affiliates.
 ** Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */

package com.oracle.database.jdbc.logs.model;

/**
 * A collection of consecutive log lines in an RDBMS log file.
 * It corresponds to 1 call to the logger (which may be more than 1 line).
 */
public class RDBMSLogEntry extends LogEntry {

  /**
   * <p>
   *   Creates a {@link RDBMSLogEntry} instance.
   * </p>
   * @param logFile Oracle JDBC thin log file.
   * @param beginLine the start line.
   * @param endLine the end line (inclusive).
   * @param beginPosition start position in the log file.
   */

  public RDBMSLogEntry(String logFile, int beginLine, int endLine, long beginPosition) {
    super(logFile, beginLine, endLine, beginPosition);
  }

}
