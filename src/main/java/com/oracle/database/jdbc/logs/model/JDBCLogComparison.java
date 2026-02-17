/*
 ** OJDBC Log Analyzer version 1.0.0
 **
 ** Copyright (c) 2025 Oracle and/or its affiliates.
 ** Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */

package com.oracle.database.jdbc.logs.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

  /**
   * <p>
   *   Returns the difference between {@code referenceValue} and {@code comparedValue} as a percentage.
   *   For example: {@code 50.235} or {@code -67.563}
   * </p>
   * @param referenceValue reference value.
   * @param comparedValue compared value.
   * @return The difference between {@code referenceValue} and {@code comparedValue}.
   */
  public static double delta(long referenceValue, long comparedValue) {
    try {
      BigDecimal bd = BigDecimal.valueOf(((double) (comparedValue - referenceValue) / referenceValue) * 100);
      return bd.setScale(2, RoundingMode.HALF_UP).doubleValue();
    } catch (NumberFormatException e) {
      e.printStackTrace();
      return 0;
    }
  }

  /**
   * <p>
   *   Returns the difference between {@code referenceValue} and {@code comparedValue} as a percentage.
   *   For example: {@code 50.235} or {@code -67.563}
   * </p>
   * @param referenceValue reference value.
   * @param comparedValue compared value.
   * @return The difference between {@code referenceValue} and {@code comparedValue}.
   */
  public static double delta(double referenceValue, double comparedValue) {
    try {
      BigDecimal bd = BigDecimal.valueOf(((comparedValue - referenceValue) / referenceValue) * 100);
      return bd.setScale(2, RoundingMode.HALF_UP).doubleValue();
    } catch (NumberFormatException e) {
      e.printStackTrace();
      return 0;
    }
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
   * @param referenceLogFileSize       the size of the reference log file in bytes.
   * @param currentLogFileSize         the size of the current log file in bytes.
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

    long referenceLogFileSize,
    long currentLogFileSize,

    long referenceLogFileLineCount,
    long currentLogFileLineCount,
    double lineCountDelta,

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
        {"referenceLogFileName":"%s","currentLogFileName":"%s","referenceLogFileSize":%d,"currentLogFileSize":%d,"referenceLogFileLineCount":%d,"currentLogFileLineCount":%d,"lineCountDelta":%.3f,"referenceLogFileTimespan":"%s","referenceLogFileDuration":"%s","currentLogFileTimespan":"%s","currentLogFileDuration":"%s"}
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
   * @param queryCountDelta The difference in query counts between current and reference measurements as percentage.
   * @param referenceAverageQueryTime The average query time during the reference measurement in milliseconds.
   * @param currentAverageQueryTime The average query time during the current measurement in milliseconds.
   * @param averageQueryTimeDelta The difference in average query times between current and reference measurements as a percentage.
   */
  public record Performance(
    long referenceQueryCount,
    long currentQueryCount,
    double queryCountDelta,
    double referenceAverageQueryTime,
    double currentAverageQueryTime,
    double averageQueryTimeDelta
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
        {"referenceQueryCount":%d,"currentQueryCount":%d,"queryCountDelta":%.3f,"referenceAverageQueryTime":%.3f,"currentAverageQueryTime":%.3f,"averageQueryTimeDelta":%.3f}
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
   * @param totalErrorsDelta a double describing the change in total errors as a percentage.
   */
  public record Error(
    long referenceErrorCount,
    long currentErrorCount,

    double totalErrorsDelta
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
        {"referenceErrorCount":%d,"currentErrorCount":%d,"totalErrorsDelta":%.3f}
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
   * @param referenceBytesConsumed the reference value for bytes consumed.
   * @param currentBytesConsumed the current value for bytes consumed.
   * @param bytesConsumedDelta the change in bytes consumed since the reference presented as a percentage.
   * @param referenceBytesProduced the reference value for bytes produced.
   * @param currentBytesProduced the current value for bytes produced.
   * @param bytesProducedDelta the change in bytes produced since the reference presented as a percentage.
   */
  public record Network(
    long referenceBytesConsumed,
    long currentBytesConsumed,
    double bytesConsumedDelta,
    long referenceBytesProduced,
    long currentBytesProduced,
    double bytesProducedDelta
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
        {"referenceBytesConsumed":%d,"currentBytesConsumed":%d,"bytesConsumedDelta":%.3f,"referenceBytesProduced":%d,"currentBytesProduced":%d,"bytesProducedDelta":%.3f}
        """.formatted(referenceBytesConsumed, currentBytesConsumed,bytesConsumedDelta, referenceBytesProduced, currentBytesProduced, bytesProducedDelta)
        .strip();
    }
  }

}




