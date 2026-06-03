/*
 ** OJDBC Log Analyzer version 1.0.1
 **
 ** Copyright (c) 2026 Oracle and/or its affiliates.
 ** Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */

package com.oracle.database.jdbc.logs.model;

import java.time.Duration;

/**
 * <p>
 *   POJO to store statistics extracted from log file.
 * </p>
 *
 * @param fileSize File size in bytes.
 * @param lineCount Number of lines.
 * @param startTime String value of the earliest recorded timestamp.
 * @param endTime String value of the latest recorded timestamp.
 * @param duration {@link Duration Duration} between startTime and endTime.
 * @param errorCount Number of errors.
 * @param queryCount Number of executed queries.
 * @param averageQueryTime Average query time.
 * @param openedConnectionCount Number of opened connections.
 * @param closedConnectionCount Number of closed connections.
 * @param sentPacketCount Number of sent packets.
 * @param receivedPacketCount Number of received packets.
 * @param roundTripCount Number of Round-Trips.
 * @param bytesConsumed Amount of bytes consumed.
 * @param bytesProduced Amount of bytes produced.
 */
public record JDBCStats(long fileSize,
                        long lineCount,
                        String startTime,
                        String endTime,
                        Duration duration,
                        long errorCount,
                        long queryCount,
                        double averageQueryTime,
                        long openedConnectionCount,
                        long closedConnectionCount,
                        long sentPacketCount,
                        long receivedPacketCount,
                        long roundTripCount,
                        long bytesConsumed,
                        long bytesProduced) {

  /**
   * <p>
   *   POJO to store statistics extracted from log file.
   * </p>
   *
   * @param fileSize File size in bytes.
   * @param lineCount Number of lines.
   * @param startTime String value of the earliest recorded timestamp.
   * @param endTime String value of the latest recorded timestamp.
   * @param duration {@link Duration Duration} between startTime and endTime.
   * @param errorCount Number of errors.
   * @param queryCount Number of executed queries.
   * @param averageQueryTime Average query time.
   * @param openedConnectionCount Number of opened connections.
   * @param closedConnectionCount Number of closed connections.
   * @param sentPacketCount Number of sent packets.
   * @param receivedPacketCount Number of received packets.
   *                          This value is also used as round-trip count.
   * @param bytesConsumed Amount of bytes consumed.
   * @param bytesProduced Amount of bytes produced.
   */
  public JDBCStats(long fileSize,
                          long lineCount,
                          String startTime,
                          String endTime,
                          Duration duration,
                          long errorCount,
                          long queryCount,
                          double averageQueryTime,
                          long openedConnectionCount,
                          long closedConnectionCount,
                          long sentPacketCount,
                          long receivedPacketCount,
                          long bytesConsumed,
                          long bytesProduced) {
    this(fileSize, lineCount, startTime, endTime, duration, errorCount,
      queryCount, averageQueryTime, openedConnectionCount, closedConnectionCount,
      sentPacketCount, receivedPacketCount, receivedPacketCount, bytesConsumed,
      bytesProduced);
  }
  /**
   * <p>
   *   Returns a string representing the time span from {@code startTime} to {@code endTime}.
   * </p>
   *
   * @return a {@code String} in the format "{@code startTime} to {@code endTime}"
   */
  public String timespan() {
    return startTime + " to " + endTime;
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
      {"fileSize":%d,"lineCount":%d,"startTime":%s,"endTime":%s,"duration":%s,"errorCount":%d,"queryCount":%d,"averageQueryTime":%f,"openedConnectionCount":%d,"closedConnectionCount":%d,"roundTripCount":%d,"sentPacketCount":%d,"receivedPacketCount":%d,"bytesConsumed":%d,"bytesProduced":%d}
      """.formatted(fileSize,
        lineCount,
        JSONUtils.escape(startTime),
        JSONUtils.escape(endTime),
        JSONUtils.escape(duration),
        errorCount,
        queryCount,
        averageQueryTime,
        openedConnectionCount,
        closedConnectionCount,
        roundTripCount,
        sentPacketCount,
        receivedPacketCount,
        bytesConsumed,
        bytesProduced)
      .strip();
  }

}
