/*
 ** OJDBC Log Analyzer version 1.0.0
 **
 ** Copyright (c) 2025 Oracle and/or its affiliates.
 ** Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */

package com.oracle.database.jdbc.logs.model;

import java.time.Duration;

/**
 * <p>
 *   A POJO representing a comparison of different aspects such as summary, performance, error, and network.
 * </p>
 *
 * <p>
 *   This record encapsulates summary, performance metrics, error details, and network information.
 * </p>
 *
 * @param summary {@link Summary Summary} a brief comparison overview or the log files.
 * @param performance {@link Performance Performance} query-related metrics.
 * @param error {@link Error Error} error comparison between the log files, if any occurred.
 * @param network {@link Network Network} network-related information at the time of logging.
 */
public record JDBCLogComparison(Summary summary, Performance performance, Error error, Network network) {
  private static final String DELTA_PERCENTAGE_TEMPLATE = "%s%.2f%%";

  /**
   * <p>
   *   Returns the difference between {@code referenceValue} and {@code comparedValue} in {@code %}.
   *   for example: {@code +50.00%} or {@code -67.00%}
   * </p>
   * @param referenceValue reference value.
   * @param comparedValue compared value.
   * @return The difference between {@code referenceValue} and {@code comparedValue} in {@code %}.
   */
  public static String delta(double referenceValue, double comparedValue) {
    final var percentage = ((comparedValue - referenceValue) / referenceValue) * 100 ;
    return DELTA_PERCENTAGE_TEMPLATE.formatted(percentage > 0 ? "+" : "", percentage);
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
        {"summary":%s,"performance":%s,"error":%s,"network":%s}
        """.formatted(summary.toJSONString(), performance.toJSONString(), error.toJSONString(), network.toJSONString())
      .strip();
  }

  /**
   * <p>
   *   Represents a summary of log file comparisons, including file names, sizes,
   *   line counts, durations, and timespans.
   * </p>
   *
   * <p>
   *   This record encapsulates information about a reference log file and a current log file,
   *   providing details such as their names, sizes, line counts, durations, and timespans,
   *   as well as the delta in line counts between them.
   * </p>
   *
   * @param referenceLogFileName       the name of the reference log file
   * @param currentLogFileName         the name of the current log file
   * @param referenceLogFileSize       the size of the reference log file (e.g., in bytes or human-readable format)
   * @param currentLogFileSize         the size of the current log file (e.g., in bytes or human-readable format)
   * @param referenceLogFileLineCount  the number of lines in the reference log file
   * @param currentLogFileLineCount    the number of lines in the current log file
   * @param lineCountDelta             the difference in line counts between current and reference log files
   * @param referenceLogFileTimespan   the timespan covered by the reference log file
   * @param referenceLogFileDuration   the duration of the reference log file
   * @param currentLogFileTimespan     the timespan covered by the current log file
   * @param currentLogFileDuration     the duration of the current log file
   */
  public record Summary(
    String referenceLogFileName,
    String currentLogFileName,

    String referenceLogFileSize,
    String currentLogFileSize,

    long referenceLogFileLineCount,
    long currentLogFileLineCount,
    String lineCountDelta,

    String referenceLogFileTimespan,
    Duration referenceLogFileDuration,

    String currentLogFileTimespan,
    Duration currentLogFileDuration
  ) {
    /**
     * <p>
     *   Returns a JSON string representation of this object.
     * </p>
     *
     * @return a JSON-formatted {@link String} representing the current state of this object
     */
    public String toJSONString() {
      return """
        {"referenceLogFileName":"%s","currentLogFileName":"%s","referenceLogFileSize":"%s","currentLogFileSize":"%s","referenceLogFileLineCount":%d,"currentLogFileLineCount":%d,"lineCountDelta":"%s","referenceLogFileTimespan":"%s","referenceLogFileDuration":"%s","currentLogFileTimespan":"%s","currentLogFileDuration":"%s"}
        """.formatted(
          referenceLogFileName,
          currentLogFileName,
          referenceLogFileSize,
          currentLogFileSize,
          referenceLogFileLineCount,
          currentLogFileLineCount,
          lineCountDelta,
          referenceLogFileTimespan,
          referenceLogFileDuration,
          currentLogFileTimespan,
          currentLogFileDuration)
        .strip();
    }
  }

  /**
   * <p>
   *   Contains information about query counts and average query times,
   *   along with their respective deltas between reference and current measurements.
   * </p>
   *
   * @param referenceQueryCount The total number of queries executed in the reference measurement.
   * @param currentQueryCount The total number of queries executed in the current measurement.
   * @param queryCountDelta The difference in query counts between current and reference measurements,
   *                        formatted as a string (e.g., "+10" or "-5").
   * @param referenceAverageQueryTime The average query time during the reference measurement,
   *                                  formatted as a string (e.g., "120 ms").
   * @param currentAverageQueryTime The average query time during the current measurement,
   *                                represented as a formatted string (e.g., "42 ms").
   * @param averageQueryTimeDelta The difference in average query times between current and reference measurements,
   *                              formatted as a string (e.g., "+20ms" or "-10ms").
   */
  public record Performance(
    long referenceQueryCount,
    long currentQueryCount,
    String queryCountDelta,

    String referenceAverageQueryTime,
    String currentAverageQueryTime,
    String averageQueryTimeDelta
  ){
    /**
     * <p>
     *   Returns a JSON string representation of this object.
     * </p>
     *
     * @return a JSON-formatted {@link String} representing the current state of this object
     */
    public String toJSONString() {
      return """
        {"referenceQueryCount":%d,"currentQueryCount":%d,"queryCountDelta":"%s","referenceAverageQueryTime":"%s","currentAverageQueryTime":"%s","averageQueryTimeDelta":"%s"}
        """.formatted(referenceQueryCount, currentQueryCount, queryCountDelta, referenceAverageQueryTime, currentAverageQueryTime, averageQueryTimeDelta)
        .strip();
    }
  }

  /**
   * <p>
   *   Represents an error record containing reference and current error counts,
   *   along with a delta indicating the change in total errors.
   * </p>
   *
   * @param referenceErrorCount the number of errors in the reference log file
   * @param currentErrorCount the number of errors in the current log file.
   * @param totalErrorsDelta a string describing the change in total errors (e.g., "+20%", "-10%").
   */
  public record Error(
    long referenceErrorCount,
    long currentErrorCount,
    String totalErrorsDelta
  ){
    /**
     * <p>
     *   Returns a JSON string representation of this object.
     * </p>
     *
     * @return a JSON-formatted {@link String} representing the current state of this object
     */
    public String toJSONString() {
      return """
        {"referenceErrorCount":%d,"currentErrorCount":%d,"totalErrorsDelta":"%s"}
        """.formatted(referenceErrorCount, currentErrorCount, totalErrorsDelta)
        .strip();
    }
  }

  /**
   * <p>
   *   Represents network usage statistics, including bytes consumed and produced,
   *   along with their reference values and deltas.
   * </p>
   *
   * @param referenceBytesConsumed the reference value for bytes consumed in human-readable format.
   * @param currentBytesConsumed the current value for bytes consumed in human-readable format.
   * @param bytesConsumedDelta the change in bytes consumed since the reference, formatted as a string (e.g., "+50%").
   * @param referenceBytesProduced the reference value for bytes produced in human-readable format.
   * @param currentBytesProduced the current value for bytes produced in human-readable format.
   * @param bytesProducedDelta the change in bytes produced since the reference, formatted as a string (e.g., "-25%").
   */
  public record Network(
    String referenceBytesConsumed,
    String currentBytesConsumed,
    String bytesConsumedDelta,
    String referenceBytesProduced,
    String currentBytesProduced,
    String bytesProducedDelta
  ){
    /**
     * <p>
     *   Returns a JSON string representation of this object.
     * </p>
     *
     * @return a JSON-formatted {@link String} representing the current state of this object
     */
    public String toJSONString() {
      return """
        {"referenceBytesConsumed":"%s","currentBytesConsumed":"%s","bytesConsumedDelta":"%s","referenceBytesProduced":"%s","currentBytesProduced":"%s","bytesProducedDelta":"%s"}
        """.formatted(referenceBytesConsumed, currentBytesConsumed,bytesConsumedDelta, referenceBytesProduced, currentBytesProduced, bytesProducedDelta)
        .strip();
    }
  }

}




