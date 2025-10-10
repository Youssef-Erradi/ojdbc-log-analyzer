/*
 ** OJDBC Log Analyzer version 1.0.0
 **
 ** Copyright (c) 2025 Oracle and/or its affiliates.
 ** Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */

package com.oracle.database.jdbc.logs.model;

/**
 * <p>
 *  POJO to store {@code ORA} error messages with its documentation link.
 * </p>
 *
 * @param errorMessage String
 * @param documentationLink Link to the error documentation in
 *                <a href="https://docs.oracle.com/en/error-help/db/index.html">
 *                          Database Error Messages website
 *                </a>.
 */

public record RDBMSError(String errorMessage, String documentationLink) {
  /**
   * <p>
   *   Returns a JSON string representation of this object.
   * </p>
   *
   * @return a JSON-formatted {@link String} representing the current state of this object
   */
  public String toJSONString() {
    return """
    {"errorMessage":"%s","documentationLink":"%s"}
    """.formatted(errorMessage, documentationLink)
      .strip();
  }
}
