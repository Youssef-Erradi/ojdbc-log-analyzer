/*
 ** OJDBC Log Analyzer version 1.0.1
 **
 ** Copyright (c) 2026 Oracle and/or its affiliates.
 ** Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */

package com.oracle.database.jdbc.logs.model;

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
      {"log":%s,"formattedPacket":%s}
      """.formatted(JSONUtils.escape(log), JSONUtils.escape(formattedPacket))
      .strip();
  }

}
