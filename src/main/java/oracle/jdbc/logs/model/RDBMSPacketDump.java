/*
 ** OJDBC Log Analyzer version 1.0.0
 **
 ** Copyright (c) 2025 Oracle and/or its affiliates.
 ** Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */

package oracle.jdbc.logs.model;

/**
 * <p>
 *   POJO to store RDBMS packet dump data.
 * </p>
 *
 * @param timestamp String representation of when the back was send/received.
 * @param formattedPacket String of formatted packet bytes
 *                        (as it appears in the SQLNet trace file).
 */
public record RDBMSPacketDump(String timestamp, String formattedPacket) {

}