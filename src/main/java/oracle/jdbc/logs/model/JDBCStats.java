/*
 ** OJDBC Log Analyzer version 1.0.0
 **
 ** Copyright (c) 2025 Oracle and/or its affiliates.
 ** Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */

package oracle.jdbc.logs.model;

import java.time.Duration;

/**
 * <p>
 *   POJO to store statistics extracted from log file.
 * </p>
 *
 * @param fileSize File size in human-readable format (3 Mb).
 * @param lineCount Number of lines.
 * @param startTime String value of the earliest recorded timestamp.
 * @param endTime String value of the latest recorded timestamp.
 * @param duration {@link java.time.Duration Duration}
 *                 between startTime and endTime.
 * @param errorCount Number of errors.
 * @param queryCount Number of executed queries.
 * @param averageQueryTime Average query time with "ms" appended at the end.
 * @param openedConnectionCount Number of opened connections.
 * @param closedConnectionCount Number of closed connections.
 * @param roundTripCount Number of Round-Trips.
 * @param sentPacketCount Number of sent packets.
 * @param receivedPacketCount Number of received packets.
 * @param bytesConsumed Amount of bytes consumed in human-readable format (3 Mb).
 * @param bytesProduced Amount of bytes produced in human-readable format (3 Mb).
 */
public record JDBCStats(String fileSize,
                        long lineCount,
                        String startTime,
                        String endTime,
                        Duration duration,
                        long errorCount,
                        long queryCount,
                        String averageQueryTime,
                        long openedConnectionCount,
                        long closedConnectionCount,
                        long roundTripCount,
                        long sentPacketCount,
                        long receivedPacketCount,
                        String bytesConsumed,
                        String bytesProduced) {

  private static final String[] UNITS = new String[]{"B", "kB", "MB", "GB", "TB"};

  /**
   * <p>
   *   This constructor takes care of converting {@code long} bytes arguments to
   *   human-readable format with units({@code kB}, {@code MB}, {@code GB} etc.)
   * </p>
   *
   * @param fileSize file size in bytes.
   * @param lineCount Number of lines in the log file.
   * @param startTime String value of the earliest recorded timestamp.
   * @param endTime String value of the latest recorded timestamp.
   * @param duration {@link java.time.Duration Duration}
   *                 between startTime and endTime.
   * @param errorCount Number of errors.
   * @param queryCount Number of executed queries.
   * @param averageQueryTime Tha average time for query execution.
   * @param openedConnectionCount Number of opened connections.
   * @param closedConnectionCount Number of closed connections
   * @param receivedPacketCount Number of received packets
   *                             (also represents the number of round-trips).
   * @param sentPacketCount Number of sent packets.
   * @param bytesConsumed amount of bytes consumed.
   * @param bytesProduced amount of bytes produced.
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
                   long receivedPacketCount,
                   long sentPacketCount,
                   long bytesConsumed,
                   long bytesProduced) {
    this(convertBytes(fileSize),
      lineCount,
      startTime,
      endTime,
      duration,
      errorCount,
      queryCount,
      formatTimeInMillis(averageQueryTime),
      openedConnectionCount,
      closedConnectionCount,
      receivedPacketCount,
      sentPacketCount,
      receivedPacketCount,
      convertBytes(bytesConsumed),
      convertBytes(bytesProduced));
  }

  /**
   * <p>
   *   Converts an amount of bytes to human-readable string, for example
   *   {@code 12947 B} will be represented as {@code 12.947 kB}
   * </p>
   *
   * @param bytes {@code long} amount of byte to be converted.
   * @return bytes represented in human-readable {@link String}.
   */
  private static String convertBytes(final long bytes) {
    final int conversionThreshold = 1000;

    double output = bytes;
    int unit = 0;
    while (output >= conversionThreshold) {
      output /= conversionThreshold;
      unit++;
    }

    return "%.3f %s".formatted(output, UNITS[unit]);
  }

  /**
   * <p>
   *   Converts human-readable {@link String} bytes to {@code double} primitive
   *   type.
   * </p>
   *
   * @param bytes represented in human-readable {@link String}.
   * @return {@code double} value of the bytes.
   */
  private static double convertBytesStringToDouble(final String bytes) {
    final var unit = bytes.split(" ")[1];

    var exp = 0;
    for (int i = 0; i < UNITS.length; i++)
      if (UNITS[i].equals(unit)) {
        exp = i;
        break;
      }

    return Double.parseDouble(bytes.split(" ")[0]) * Math.pow(1000, exp);
  }

  /**
   * <p>
   *   Returns a {@link String} representation of time with 2 decimal points and
   *   {@code ms} unit appended at the end.
   * </p>
   *
   * @param time amount of time ({code double} because it could be an average).
   * @return {@link String} with 2 decimal points with {@code ms} appended.
   */
  private static String formatTimeInMillis(final double time) {
    return "%.2f ms".formatted(time);
  }

  /**
   * <p>
   *   Parses the {@code averageQueryTime} string to a double value representing
   *   the average query time in milliseconds.
   * </p>
   *
   * <p>
   *   <strong>Note: </strong>Assumes {@code averageQueryTime} ends with "ms" and contains a valid number.
   * </p>
   *
   * @return the average query time as a double in milliseconds.
   *
   */
  public double averageQueryTimeAsDouble() {
    return Double.parseDouble(averageQueryTime.replace("ms", "").strip());
  }

  /**
   * <p>
   *   Converts the {@code bytesConsumed} string to its double representation.
   * </p>
   *
   * @return the value of {@code bytesConsumed} as a double.
   */
  public double bytesConsumedAsDouble() {
    return convertBytesStringToDouble(bytesConsumed);
  }

  /**
   * <p>
   *   Converts the {@code bytesProduced} string to its double representation.
   * </p>
   *
   * @return the value of {@code bytesProduced} as a double.
   */
  public double bytesProducedAsDouble() {
    return convertBytesStringToDouble(bytesProduced);
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

}
