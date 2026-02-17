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
 * @param roundTripCount Number of Round-Trips.
 * @param sentPacketCount Number of sent packets.
 * @param receivedPacketCount Number of received packets.
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
                        long roundTripCount,
                        long sentPacketCount,
                        long receivedPacketCount,
                        long bytesConsumed,
                        long bytesProduced) {

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
      {"fileSize":%d,"lineCount":%d,"startTime":"%s","endTime":"%s","duration":"%s","errorCount":%d,"queryCount":%d,"averageQueryTime":%.3f,"openedConnectionCount":%d,"closedConnectionCount":%d,"roundTripCount":%d,"sentPacketCount":%d,"receivedPacketCount":%d,"bytesConsumed":%d,"bytesProduced":%d}
      """.formatted(fileSize,
        lineCount,
        startTime,
        endTime,
        duration.toString(),
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
