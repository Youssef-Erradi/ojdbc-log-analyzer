/*
 ** OJDBC Log Analyzer version 1.0.0
 **
 ** Copyright (c) 2025 Oracle and/or its affiliates.
 ** Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl/
 */

package com.oracle.database.jdbc.logs.analyzer;

import com.oracle.database.jdbc.logs.model.RDBMSLogEntry;
import com.oracle.database.jdbc.logs.model.RDBMSPacketDump;
import com.oracle.database.jdbc.logs.model.RDBMSError;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.oracle.database.jdbc.logs.analyzer.Utils.*;

/**
 * <p>
 *   This class is capable of parsing RDBMS (SQLNet) trace files and extract
 *   errors and packet dumps.
 * </p>
 *
 * @see #getErrors()
 * @see #getPacketDumps(String)
 */
public class RDBMSLog {

  /**
   * <p> Pattern to extract packet dumps with timestamp from RDBMS log file.</p>
   * i.e. {@code D:2025-04-08 15:08:46.272391 : nsbasic_brc:00 00 0B AE 06 20 00 00  |........|}
   */
  static final Pattern PACKET_DUMP_PATTERN = Pattern.compile("[DCI]:(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{5}).*([\\s0-9A-F]+\\|.{8}\\|)");

  /**
   * Pattern to extract {@code ORA-} error from RDBMS trace file.
   */
  static final Pattern SERVER_ERROR_PATTERN = Pattern.compile("(ORA-\\d{5}):.+");
  static final Pattern CONNECTION_ID_PATTERN = Pattern.compile("(.*):connection_id = (.*)");
  static final String MY_CONNECTION_ID_PATTERN_STRING = "(.*):connection_id = %s";

  private final String logLocation;
  private List<RDBMSLogEntry> logEntries;

  /**
   * <p>
   *   Create an instance to parse an RDBMS (SQLNet) trace file.
   * </p>
   * @param logLocation path to the RDBMS (SQLNet) trace file.
   * @throws IllegalArgumentException if {@code logLocation} is null or empty.
   */
  public RDBMSLog(String logLocation) throws IllegalArgumentException {
    requireNonBlank(logLocation, "logLocation cannot be empty");
    this.logLocation = logLocation;
  }

  private List<RDBMSLogEntry> parse() throws IOException {
    if (logEntries != null) {
      return logEntries;
    }

    logEntries = new ArrayList<>();

    int previousLogStartLine = 0;
    long previousLogPositionInFile = 0;
    long positionInFile = 0;

    try (BufferedReader reader = getBufferedReader(logLocation)) {
      int lineNumber = 1;
      while (reader.ready()) {
        String line = reader.readLine();
        Matcher matcher = CONNECTION_ID_PATTERN.matcher(line);
        if (matcher.find()) {
          logEntries.add(new RDBMSLogEntry(logLocation, previousLogStartLine, lineNumber - 1, previousLogPositionInFile));

          previousLogStartLine = lineNumber;
          previousLogPositionInFile = positionInFile;
        }
        lineNumber++;
        positionInFile += line.length() + 1;
      }
    }

    return logEntries;
  }

  private List<RDBMSLogEntry> getLogs(String connectionId) throws IOException {
    parse();

    List<RDBMSLogEntry> logs = new ArrayList<>();

    Pattern connectionIdPattern = Pattern.compile( String.format(MY_CONNECTION_ID_PATTERN_STRING, connectionId) );

    for (RDBMSLogEntry log : logEntries) {
      Matcher connectionIdMatcher = connectionIdPattern.matcher(log.getFirstLine());
      if (connectionIdMatcher.find()) {
        logs.add(log);
      }
    }

    return logs;
  }

  /**
   * <p>
   *   Extracts errors from the RDBMS (SQLNet) trace file.
   * </p>
   *
   * @return List of {@link RDBMSError}
   * @throws IOException if an error occurs while reading the trace file.
   */
  public List<RDBMSError> getErrors() throws IOException {
    final var errors = new ArrayList<RDBMSError>();

    try (final var bufferedReader = getBufferedReader(logLocation)) {
      String docLinkTemplate = null;
      String dbVersion = null;
      String line;

      while( (line = bufferedReader.readLine()) != null) {
        // This if block will only run once
        if (dbVersion == null || line.startsWith("Oracle Database")) {
          dbVersion = line.split(" ")[2];
          docLinkTemplate = "https://docs.oracle.com/en/error-help/db/%s/?r=" + dbVersion;
          continue;
        }

        final Matcher matcher = SERVER_ERROR_PATTERN.matcher(line);
        if (matcher.matches())
          errors.add( new RDBMSError(line, docLinkTemplate.formatted(matcher.group(1))) );
      }
    }

    return errors;
  }

  /**
   * <p>
   *   Extracts all the packet dumps that corresponds with a {@code connectionId}.
   * </p>
   *
   * @param connectionId String representation of the connection id.
   * @return List of {@link RDBMSPacketDump}
   * @throws IOException if an error occurs while reading the trace file.
   */
  public List<RDBMSPacketDump> getPacketDumps(final String connectionId) throws IOException {
    final List<RDBMSPacketDump> packetDumps = new ArrayList<>();
    String line;

    try (final var bufferedReader = getBufferedReader(logLocation)) {
      while( (line = bufferedReader.readLine()) != null) {
        if (!line.contains("connection_id = " + connectionId))
          continue;

        boolean parsingTheFirstFoundPacket = false;
        String timestamp = null;
        final StringJoiner packetDump = new StringJoiner(System.lineSeparator());

        // Keep reading the following lines to find the packet dumps
        while( (line = bufferedReader.readLine()) != null) {
          if (line.contains("connection_id = ") && !line.contains("connection_id = " + connectionId)) {
            // We found a connection_id on this line that is not the connection id we are working on.
            break;
          }

          final Matcher matcher = PACKET_DUMP_PATTERN.matcher(line);
          if (matcher.matches()) {
            if (timestamp == null)
              timestamp = matcher.group(1);

            parsingTheFirstFoundPacket = true;
            packetDump.add(line.substring(line.length() - 35));
          } else if (parsingTheFirstFoundPacket)
            break;
        }

        if (packetDump.length() > 0) {
          packetDumps.add(new RDBMSPacketDump(timestamp, packetDump.toString()));
        }
      }
    }

    return packetDumps;
  }

}
