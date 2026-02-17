/*
 ** OJDBC Log Analyzer version 1.0.0
 **
 ** Copyright (c) 2025 Oracle and/or its affiliates.
 ** Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */

package com.oracle.database.jdbc.logs.model;

/**
 * <p>
 *   POJO to store an executed query's info.
 * </p>
 *
 * @param timestamp     String representation of when the query was executed.
 * @param sql           The actual SQL query as a String.
 * @param executionTime The time it took to run the query in milliseconds.
 * @param connectionId  The identifier of the database connection used to execute the query.
 * @param tenant        The tenant in which the query was run.
 */
public record JDBCExecutedQuery(String timestamp, String sql, int executionTime, String connectionId, String tenant) {

  /**
   * <p>
   *   Returns a JSON string representation of this object.
   * </p>
   *
   * @return a JSON-formatted {@link String} representing the current state of this object
   */
  public String toJSONString() {
    return """
     {"timestamp":"%s","sql":"%s","executionTime":%d,"connectionId":%s,"tenant":%s}
     """.formatted(timestamp,
        sql.replace("\n", "\\n")
          .replace("\t", "\\t"),
        executionTime,
        connectionId == null ? "null" : "\""+connectionId+"\"",
        tenant == null ? "null" : "\""+tenant+"\"")
      .strip();
  }

}
