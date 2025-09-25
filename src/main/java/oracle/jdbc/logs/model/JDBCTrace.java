/*
 ** OJDBC Log Analyzer version 1.0.0
 **
 ** Copyright (c) 2025 Oracle and/or its affiliates.
 ** Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */

package oracle.jdbc.logs.model;

/**
 * <p>
 *   POJO to store JDBC trace information.
 * </p>
 * @param timestamp String representation the date and time.
 * @param executedMethod fully qualified class name with method name.
 */
public record JDBCTrace(String timestamp, String executedMethod) {

  /**
   * <p>
   *   Returns a JSON string representation of this object.
   * </p>
   *
   * @return a JSON-formatted {@link String} representing the current state of this object
   */
  public String toJSONString() {
    return """
      {"timestamp": "%s","executedMethod": "%s"}
      """.formatted(timestamp, executedMethod)
      .strip();
  }

}
