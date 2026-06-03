/*
 ** OJDBC Log Analyzer version 1.0.1
 **
 ** Copyright (c) 2026 Oracle and/or its affiliates.
 ** Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */

package com.oracle.database.jdbc.logs.model;

/**
 * Utility helpers for producing JSON-safe literals from Java objects.
 * <p>
 * This class is non-instantiable and exposes static conversion methods only.
 * </p>
 */
final class JSONUtils {
  private JSONUtils() {}

  /**
   * Converts the given object into a JSON-safe literal.
   * <p>
   * If {@code object} is a {@link Number} or {@link Boolean}, its JSON literal
   * representation is returned as-is (without surrounding quotes). For any
   * other non-null value, this method converts it to its {@link String}
   * representation using {@link String#valueOf(Object)} and returns a
   * JSON-escaped string surrounded by double quotes. The following characters
   * are escaped according to the JSON specification:
   * <ul>
   *   <li>{@code '"'} as {@code \"}</li>
   *   <li>{@code '\\'} as {@code \\}</li>
   *   <li>Backspace ({@code '\b'}) as {@code \b}</li>
   *   <li>Form feed ({@code '\f'}) as {@code \f}</li>
   *   <li>New line ({@code '\n'}) as {@code \n}</li>
   *   <li>Carriage return ({@code '\r'}) as {@code \r}</li>
   *   <li>Horizontal tab ({@code '\t'}) as {@code \t}</li>
   * </ul>
   * Any other control character with a code point less than {@code 0x20}
   * (ASCII 32) is escaped using a Unicode escape sequence of the form
   * {@code \\uXXXX}. All other characters are appended unchanged.
   * <p>
   * If the supplied {@code object} is {@code null}, the literal string
   * {@code "null"} (without surrounding quotes) is returned.
   *
   * @param object the object to be converted and escaped as a JSON string
   * @return JSON literal text: {@code "null"} for null, unquoted text for
   *         numbers/booleans, or a quoted and escaped JSON string literal for
   *         other objects.
   */
  public static String escape(final Object object) {
    if (object == null)
      return "null";

    if (object instanceof Number || object instanceof Boolean)
      return String.valueOf(object);

    final var value = String.valueOf(object);

    StringBuilder sb = new StringBuilder(value.length() + 16);

    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);

      switch (c) {
        case '"'  -> sb.append("\\\"");
        case '\\' -> sb.append("\\\\");
        case '\b' -> sb.append("\\b");
        case '\f' -> sb.append("\\f");
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        case '\t' -> sb.append("\\t");
        default -> {
          if (c < 0x20) {
            sb.append(String.format("\\u%04x", (int) c));
          } else {
            sb.append(c);
          }
        }
      }
    }

    return "\"" + sb + "\"";
  }
}
