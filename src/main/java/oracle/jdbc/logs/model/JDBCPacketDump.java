/*
 ** OJDBC Log Analyzer version 1.0.0
 **
 ** Copyright (c) 2025 Oracle and/or its affiliates.
 ** Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */

package oracle.jdbc.logs.model;

/**
 * <p>
 *   POJO to store JDBC packet dump data.
 * </p>
 *
 * @param log the corresponding log line for the packet.
 * @param formattedPacket String of formatted packet bytes
 *                        (as it appears in the Oracle JDBC log file).
 */

public record JDBCPacketDump(String log, String formattedPacket) {

  /**
   * <p>
   *   Returns a JSON string representation of this object.
   * </p>
   *
   * @return a JSON-formatted {@link String} representing the current state of this object
   */
  public String toJSONString() {
    return """
      {"log":"%s","formattedPacket":"%s"}
      """.formatted(log.replace("\n", "\\n").replace("\t", "\\t"),
        formattedPacket.replace("\n", "\\n")
          .replace("\t", "\\t")
          .replace("\\", "\\\\")
          .replace("\"", "\\\""))
      .strip();
  }

}
