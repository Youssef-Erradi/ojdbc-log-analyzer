/*
 ** OJDBC Log Analyzer version 1.0.0
 **
 ** Copyright (c) 2025 Oracle and/or its affiliates.
 ** Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */

package com.oracle.database.jdbc.logs.model;

/**
 * <p>
 *   POJO to hold the connection event type, timestamp and details.
 * </p>
 *
 * @param timestamp String representation of the timestamp.
 * @param event {@link Event} enum value.
 * @param details event details (such as socket and connection options) if available.
 */
public record JDBCConnectionEvent(String timestamp, Event event, String details) {

  /**
   * <p>
   *   Create an instance with no details.
   * </p>
   *
   * @param timestamp String
   * @param event {@link Event} type.
   */
  public JDBCConnectionEvent(String timestamp, Event event) {
    this(timestamp, event, null);
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
     {"timestamp": "%s","event": "%s","details": "%s"}
     """.formatted(timestamp, event.name(), details)
      .strip();
  }

  /**
   * <p>
   *   Enum values of supported connection event types.
   * </p>
   */
  public enum Event {
    /**
     * Connection creation event.
     */
    CONNECTION_OPENED,
    
    /**
     * Connection closed event.
     */
    CONNECTION_CLOSED;
  }
}
