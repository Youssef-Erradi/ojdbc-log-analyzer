# Oracle JDBC Log Analyzer

The Oracle JDBC log analyzer tool is used to parse and extract meaningful information from JDBC/UCP and RDBMS/SQLNet log files.
It allows clients to access, query, and analyze log data stored in a file, whether the log file is located locally on the machine or available on the web (by supplying a URL).

The library support the following log formatters:
  - [OracleSimpleFormatter](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/diagnostics/OracleSimpleFormatter.html) (This is the recommended formatter, since it provides more details such as connection id and tenant).
  - [SimpleFormatter (Default JUL formatter)](https://docs.oracle.com/en/java/javase/17/docs/api/java.logging/java/util/logging/SimpleFormatter.html).
  - [UCPFormatter](https://docs.oracle.com/en/database/oracle/oracle-database/23/jjuar/oracle/ucp/util/logging/UCPFormatter.html).

Key Features of JDBC/UCP log parsing:
  - Error Retrieval: Access error log entries.
  - Insights: Generate statistics and metrics to identify trends (and maybe also anomalies).
  - Executed Queries: Retrieve the executed SQL statements with the timestamp and the execution time.
  - Connection Events: Retrieve the connection opened/closed events.
  - Log Comparison: Compare two log files focusing on performance metrics, error details, and network information.

Key Features of RDBMS/SQLNet trace files parsing:
  - Error Retrieval: Retrieve all the reported `ORA` errors.
  - Extract Packet dumps: Extracts all the packet dumps that corresponds with a connection id.

## Requirements

This project requires a JDK 17 or higher to build and/or run the code.

If the JAVA_HOME environment variable is defined, this is what Gradle will use to build and run the project.

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
```

If `JAVA_HOME` is not defined, Gradle will use the JDK found in the `PATH`, you can check the java version by running:

```bash
java -version
```

Please note that it's recommended to set the `JAVA_HOME` environment variable.

To build the project, use the embedded Gradle wrapper:

```bash
./gradlew build
```

## Usage Example

### 1. Analyzing an Oracle JDBC log file

JDBC and UCP logs can be parsed with `oracle.jdbc.logs.analyzer.JDBCLog` class

Here's an example of how the APIs can be used:

```java
import oracle.jdbc.logs.analyzer.JDBCLog;
import oracle.jdbc.logs.model.JDBCConnectionEvent;
import oracle.jdbc.logs.model.JDBCExecutedQuery;
import oracle.jdbc.logs.model.JDBCStats;
import oracle.jdbc.logs.model.LogError;

import java.io.IOException;
import java.util.stream.Collectors;

public class JDBCMainClass {

  public static void main(String[] args) throws IOException {
    final String logFilePath = "/Users/youssef/Desktop/logs/ojdbc17-test1.log";
    // We can also provide a URL instead of a local path.
    final JDBCLog jdbcLogParser = new JDBCLog(logFilePath);

    System.out.println("===== Extract Stats ======");
    final JDBCStats stats = jdbcLogParser.getStats();
    System.out.println(stats.toJSONString());


    if (stats.errorCount() > 0) {
      System.out.println("\n===== Extract Errors ======");
      System.out.println(jdbcLogParser.getLogErrors()
        .stream()
        .map(LogError::toJSONString)
        .collect(Collectors.joining(",", "[","]")));
    }

    if (stats.queryCount() > 0) {
      System.out.println("\n===== Extract Executed Queries ======");
      System.out.println(
        jdbcLogParser.getQueries()
          .stream()
          .map(JDBCExecutedQuery::toJSONString)
          .collect(Collectors.joining(",", "[","]"))
      );
    }

    if (stats.openedConnectionCount() > 0 ) {
      System.out.println("\n===== Extract Connection Events ======");
      System.out.println(
        jdbcLogParser.getConnectionEvents()
          .stream()
          .map(JDBCConnectionEvent::toJSONString)
          .collect(Collectors.joining(",", "[","]"))
      );
    }

    final String logFilePath2 = "/Users/youssef/Desktop/logs/ojdbc17-test2.log";
    System.out.println("\n===== Comparison between 'ojdbc17-test1' and 'ojdbc17-test2' ======");
    System.out.println(jdbcLogParser.compareTo(logFilePath2).toJSONString());
  }

}
```

Example of the extracted information printed as JSON:

- Extract Stats
```json
{
  "fileSize": "1.363 MB",
  "lineCount": 25795,
  "startTime": "2024-06-20T22:27:11",
  "endTime": "2024-06-20T22:28:14",
  "duration": "PT1M3S",
  "errorCount": 14,
  "queryCount": 17,
  "averageQueryTime": "19.71 ms",
  "openedConnectionCount": 5,
  "closedConnectionCount": 4,
  "roundTripCount": 80,
  "sentPacketCount": 113,
  "receivedPacketCount": 80,
  "bytesConsumed": "55.197 kB",
  "bytesProduced": "3.286 MB"
}
```

- Extract Errors:

```json
[{
  "logEntry": {
    "logFile": "/Users/youssef/Desktop/logs/ojdbc17-test1.log",
    "beginLine": 4513,
    "endLine": 4537
  },
  "sql": "drop user tkpjb35428646 cascade",
  "originalSql": "drop user tkpjb35428646 cascade",
  "errorMessage": "ORA-01918: user 'TKPJB35428646' does not exist",
  "packetDumps": [
    {
      "log": "FINEST: U:thread-1 main CONNECTION_ID=0B9IF/DmROGJx/k2EsbJnw==,TENANT=CDB1_PDB1,SQL=drop user tkpjb35428646 cascade,SSLEngineResult=[Status = OK HandshakeStatus = NOT_HANDSHAKING\nbytesConsumed = 109 bytesProduced = 138 sequenceNumber = 8]",
      "formattedPacket": "00 00 00 6D 06 00 00 00     |...m....|\n08 00 03 5E 04 00 02 81     |...^....|\n21 01 02 01 01 1F 01 01     |!.......|\n0D 00 00 00 00 04 7F FF     |........|\nFF FF 00 00 00 00 00 00     |........|\n00 00 00 00 00 01 00 00     |........|\n00 00 00 00 00 00 00 00     |........|\n00 00 00 00 00 64 72 6F     |.....dro|\n70 20 75 73 65 72 20 74     |p.user.t|\n6B 70 6A 62 33 35 34 32     |kpjb3542|\n38 36 34 36 20 63 61 73     |8646.cas|\n63 61 64 65 01 01 01 01     |cade....|\n00 00 00 00 00 00 00 02     |........|\n80 00 00 00 00              |.....   |"
    },
    {
      "log": "FINEST: U:thread-1 main CONNECTION_ID=0B9IF/DmROGJx/k2EsbJnw==,TENANT=CDB1_PDB1,SQL=drop user tkpjb35428646 cascade,138 bytes written to the Socket. Packet Dump : ",
      "formattedPacket": "17 03 03 00 85 00 00 00     |........|\n00 00 00 00 08 15 62 90     |......b.|\n32 E7 F2 67 E3 AA 9B E5     |2..g....|\nFD 41 9A EE FF 69 2B 18     |.A...i+.|\n9B 51 1B 02 75 EA 90 49     |.Q..u..I|\n25 A0 64 59 8D 42 2B 69     |%.dY.B+i|\nC1 30 BC 4E 21 6D A5 01     |.0.N!m..|\nA7 68 7C 47 A7 6A C8 77     |.h|G.j.w|\n3B 33 1D F6 01 01 26 27     |;3....&'|\n35 DC 30 EE 1E 19 D1 C0     |5.0.....|\nF0 DA 35 9D 23 39 11 FF     |..5.#9..|\n5D 15 EB 97 93 FB 44 86     |].....D.|\n38 C9 CC AB 5A 6E 27 CE     |8...Zn'.|\n9F 85 B9 E2 56 15 EE D9     |....V...|\n1B 43 52 74 BB 41 5D 96     |.CRt.A].|\nB8 07 C4 64 15 94 C8 C1     |...d....|\n6A 1E 15 F9 5F 46 78 07     |j..._Fx.|\n23 87                       |#.      |"
    },
    {
      "log": "FINEST: U:thread-1 main CONNECTION_ID=0B9IF/DmROGJx/k2EsbJnw==,TENANT=CDB1_PDB1,SQL=drop user tkpjb35428646 cascade,40 bytes",
      "formattedPacket": "17 03 03 00 23 CF 01 8A     |....#...|\nCE DD D1 B2 CA 17 62 1D     |......b.|\n29 CA 89 D8 74 0B A9 EB     |)...t...|\nDB C7 B5 01 A0 F2 A4 F8     |........|\nD6 C1 7B E9 89 3B C3 B0     |..{..;..|"
    },
    {
      "log": "FINEST: U:thread-1 main CONNECTION_ID=0B9IF/DmROGJx/k2EsbJnw==,TENANT=CDB1_PDB1,SQL=drop user tkpjb35428646 cascade,SSLEngineResult=[Status = OK HandshakeStatus = NOT_HANDSHAKING\nbytesConsumed = 40 bytesProduced = 11]",
      "formattedPacket": "00 00 00 0B 0C 20 00 00     |........|\n01 00 01                    |...     |"
    },
    {
      "log": "FINEST: U:thread-1 main CONNECTION_ID=0B9IF/DmROGJx/k2EsbJnw==,TENANT=CDB1_PDB1,SQL=drop user tkpjb35428646 cascade,SSLEngineResult=[Status = OK HandshakeStatus = NOT_HANDSHAKING\nbytesConsumed = 11 bytesProduced = 40 sequenceNumber = 9]",
      "formattedPacket": "00 00 00 0B 0C 20 00 00     |........|\n01 00 02                    |...     |"
    },
    {
      "log": "FINEST: U:thread-1 main CONNECTION_ID=0B9IF/DmROGJx/k2EsbJnw==,TENANT=CDB1_PDB1,SQL=drop user tkpjb35428646 cascade,40 bytes written to the Socket. Packet Dump : ",
      "formattedPacket": "17 03 03 00 23 00 00 00     |....#...|\n00 00 00 00 09 ED 98 F5     |........|\nE9 AC 53 51 1B BF 9D 95     |..SQ....|\n7B 1B 31 34 F1 4F 52 EB     |{.14.OR.|\n45 BB 78 1B 93 ED 45 E0     |E.x...E.|"
    },
    {
      "log": "FINEST: U:thread-1 main CONNECTION_ID=0B9IF/DmROGJx/k2EsbJnw==,TENANT=CDB1_PDB1,SQL=drop user tkpjb35428646 cascade,173 bytes",
      "formattedPacket": "17 03 03 00 23 CF 01 8A     |....#...|\nCE DD D1 B2 CB A6 78 66     |......xf|\nF1 C6 1C 73 46 ED 8D 7E     |...sF..~|\nF6 55 29 29 28 F6 E5 0B     |.U))(...|\nEA 34 A2 FD CF 62 32 ED     |.4...b2.|\n17 03 03 00 80 CF 01 8A     |........|\nCE DD D1 B2 CC D1 BF 99     |........|\n88 2C 79 06 14 03 70 F2     |.,y...p.|\n77 29 1D 42 C0 4C 61 AE     |w).B.La.|\n5C F9 00 B8 13 66 D9 C6     |\\....f..|\n51 21 B9 FD 37 40 A5 C8     |Q!..7@..|\n65 0D B9 80 1C 33 ED 1E     |e....3..|\n51 99 D0 7F A5 F6 6D 89     |Q.....m.|\n8D AB 52 62 37 06 43 5D     |..Rb7.C]|\n9E 1F C3 1A 12 98 86 CA     |........|\n7F 5E 47 D0 9D D1 32 D5     |.^G...2.|\nB8 9F 4B 26 43 4F 6D 7C     |..K&COm||\n51 79 68 19 85 13 22 B2     |Qyh...\".|\nD5 2B D1 CF D2 C5 CC E5     |.+......|\n24 80 66 42 C6 00 8A 02     |$.fB....|\n46 14 95 42 0B C7 10 B0     |F..B....|\n20 9B 21 75 A8              |..!u.   |"
    },
    {
      "log": "FINEST: U:thread-1 main CONNECTION_ID=0B9IF/DmROGJx/k2EsbJnw==,TENANT=CDB1_PDB1,SQL=drop user tkpjb35428646 cascade,SSLEngineResult=[Status = OK HandshakeStatus = NOT_HANDSHAKING\nbytesConsumed = 40 bytesProduced = 11]",
      "formattedPacket": "00 00 00 0B 0C 20 00 00     |........|\n01 00 02                    |...     |"
    },
    {
      "log": "FINEST: U:thread-1 main CONNECTION_ID=0B9IF/DmROGJx/k2EsbJnw==,TENANT=CDB1_PDB1,SQL=drop user tkpjb35428646 cascade,SSLEngineResult=[Status = OK HandshakeStatus = NOT_HANDSHAKING\nbytesConsumed = 133 bytesProduced = 104]",
      "formattedPacket": "00 00 00 68 06 00 00 00     |...h....|\n00 00 04 03 01 00 01 02     |........|\n02 B0 00 02 07 7E 00 00     |.....~..|\n01 02 01 0A 35 00 00 01     |...␤5...|\n02 00 00 00 00 00 00 00     |........|\n00 00 04 00 00 00 00 00     |........|\n00 02 07 7E 00 01 35 00     |...~..5.|\n2F 4F 52 41 2D 30 31 39     |/ORA-019|\n31 38 3A 20 75 73 65 72     |18:.user|\n20 27 54 4B 50 4A 42 33     |.'TKPJB3|\n35 34 32 38 36 34 36 27     |5428646'|\n20 64 6F 65 73 20 6E 6F     |.does.no|\n74 20 65 78 69 73 74 0A     |t.exist␤|"
    }
  ],
  "tenant": "CDB1_PDB1",
  "logLines": "INFO: U:thread-1 main CONNECTION_ID=0B9IF/DmROGJx/k2EsbJnw==,TENANT=CDB1_PDB1,null\njava.sql.SQLSyntaxErrorException: ORA-01918: user 'TKPJB35428646' does not exist\n\n\tat oracle.jdbc.driver.T4CTTIoer11.processError(T4CTTIoer11.java:709)\n\tat oracle.jdbc.driver.T4CTTIoer11.processError(T4CTTIoer11.java:609)\n\tat oracle.jdbc.driver.T4C8Oall.processError(T4C8Oall.java:1347)\n\tat oracle.jdbc.driver.T4CTTIfun.receive(T4CTTIfun.java:1145)\n\tat oracle.jdbc.driver.T4CTTIfun.doRPC(T4CTTIfun.java:414)\n\tat oracle.jdbc.driver.T4C8Oall.doOALL(T4C8Oall.java:499)\n\tat oracle.jdbc.driver.T4CStatement.doOall8(T4CStatement.java:190)\n\tat oracle.jdbc.driver.T4CStatement.executeForRows(T4CStatement.java:1399)\n\tat oracle.jdbc.driver.OracleStatement.executeSQLStatement(OracleStatement.java:2008)\n\tat oracle.jdbc.driver.OracleStatement.doExecuteWithTimeout(OracleStatement.java:1621)\n\tat oracle.jdbc.driver.OracleStatement.executeInternal(OracleStatement.java:2687)\n\tat oracle.jdbc.driver.OracleStatement.execute(OracleStatement.java:2636)\n\tat oracle.jdbc.driver.OracleStatementWrapper.execute(OracleStatementWrapper.java:334)\n\tat sqlj.qa.harness.AppJdbcHarness.createUser(AppJdbcHarness.java:872)\n\tat sqlj.qa.harness.AppJdbcHarness.lambda$main$0(AppJdbcHarness.java:182)\n\tat java.base/java.security.AccessController.doPrivileged(AccessController.java:551)\n\tat sqlj.qa.harness.AppJdbcHarness.main(AppJdbcHarness.java:180)\nCaused by: Error : 1918, Position : 10, SQL = drop user tkpjb35428646 cascade, Original SQL = drop user tkpjb35428646 cascade, Error Message = ORA-01918: user 'TKPJB35428646' does not exist\n\n\tat oracle.jdbc.driver.T4CTTIoer11.processError(T4CTTIoer11.java:717)\n\t... 16 more\n\n",
  "documentationLink": "https://docs.oracle.com/en/error-help/db/ORA-01918",
  "sqlExecutionTime": 16,
  "nearestTrace": {
    "timestamp": "2024-06-20T22:27:12",
    "executedMethod": "oracle.jdbc.driver.OracleStatement execute"
  },
  "connectionId": "0B9IF/DmROGJx/k2EsbJnw=="
}]
```

- Extract Executed Queries:

```json
[
  {
    "timestamp": "2024-06-20T22:27:12",
    "sql": "drop directory TEST_DIR",
    "executionTime": "18ms",
    "connectionId": "0B9IF/DmROGJx/k2EsbJnw==",
    "tenant": "CDB1_PDB1"
  },
  {
    "timestamp": "2024-06-20T22:27:12",
    "sql": "drop user tkpjb35428646 cascade",
    "executionTime": "16ms",
    "connectionId": "0B9IF/DmROGJx/k2EsbJnw==",
    "tenant": "CDB1_PDB1"
  },
  {
    "timestamp": "2024-06-20T22:27:12",
    "sql": "create user tkpjb35428646 identified by tkpjb35428646 default tablespace system quota unlimited on system",
    "executionTime": "28ms",
    "connectionId": "0B9IF/DmROGJx/k2EsbJnw==",
    "tenant": "CDB1_PDB1"
  },
  {
    "timestamp": "2024-06-20T22:27:12",
    "sql": "grant connect, resource, unlimited tablespace,ALTER SESSION to tkpjb35428646",
    "executionTime": "9ms",
    "connectionId": "0B9IF/DmROGJx/k2EsbJnw==",
    "tenant": "CDB1_PDB1"
  },
  {
    "timestamp": "2024-06-20T22:27:12",
    "sql": "grant create view, create session, create synonym,create type, create sequence, CREATE TABLE,create procedure, select any table to tkpjb35428646",
    "executionTime": "9ms",
    "connectionId": "0B9IF/DmROGJx/k2EsbJnw==",
    "tenant": "CDB1_PDB1"
  },
  {
    "timestamp": "2024-06-20T22:27:12",
    "sql": "grant create database link to tkpjb35428646",
    "executionTime": "7ms",
    "connectionId": "0B9IF/DmROGJx/k2EsbJnw==",
    "tenant": "CDB1_PDB1"
  },
  {
    "timestamp": "2024-06-20T22:27:12",
    "sql": "grant CREATE CLUSTER, CREATE OPERATOR, CREATE TRIGGER, CREATE INDEXTYPE to tkpjb35428646",
    "executionTime": "8ms",
    "connectionId": "0B9IF/DmROGJx/k2EsbJnw==",
    "tenant": "CDB1_PDB1"
  },
  {
    "timestamp": "2024-06-20T22:27:12",
    "sql": "grant create any directory,drop any directory to tkpjb35428646",
    "executionTime": "8ms",
    "connectionId": "0B9IF/DmROGJx/k2EsbJnw==",
    "tenant": "CDB1_PDB1"
  },
  {
    "timestamp": "2024-06-20T22:27:12",
    "sql": "grant select on emp to public",
    "executionTime": "10ms",
    "connectionId": "LuEy0UraR/CzZiaKrnoFJA==",
    "tenant": "CDB1_PDB1"
  },
  {
    "timestamp": "2024-06-20T22:27:12",
    "sql": "grant select on dept to public",
    "executionTime": "7ms",
    "connectionId": "LuEy0UraR/CzZiaKrnoFJA==",
    "tenant": "CDB1_PDB1"
  },
  {
    "timestamp": "2024-06-20T22:27:12",
    "sql": "create directory TEST_DIR as '/ade/b/1465033565/oracle/work'",
    "executionTime": "10ms",
    "connectionId": "akfhcI14TCqfOnUNs3XMww==",
    "tenant": "CDB1_PDB1"
  },
  {
    "timestamp": "2024-06-20T22:27:13",
    "sql": "ALTER SESSION SET TIME_ZONE = 'PST8PDT'",
    "executionTime": "2ms",
    "connectionId": "Ou7tTDJKTaqGwfDVqVR5Rw==",
    "tenant": "CDB1_PDB1"
  },
  {
    "timestamp": "2024-06-20T22:27:13",
    "sql": "SELECT DBTIMEZONE FROM DUAL",
    "executionTime": "112ms",
    "connectionId": "Ou7tTDJKTaqGwfDVqVR5Rw==",
    "tenant": "CDB1_PDB1"
  },
  {
    "timestamp": "2024-06-20T22:28:14",
    "sql": "drop user tkpjb35428646 cascade",
    "executionTime": "8ms",
    "connectionId": "oRBTwv3qQPOXXuktAXpezA==",
    "tenant": "CDB1_PDB1"
  },
  {
    "timestamp": "2024-06-20T22:28:14",
    "sql": "select sid,serial# from v$session where username='TKPJB35428646'",
    "executionTime": "15ms",
    "connectionId": "oRBTwv3qQPOXXuktAXpezA==",
    "tenant": "CDB1_PDB1"
  },
  {
    "timestamp": "2024-06-20T22:28:14",
    "sql": "alter system kill session '279,43180' immediate",
    "executionTime": "10ms",
    "connectionId": "oRBTwv3qQPOXXuktAXpezA==",
    "tenant": "CDB1_PDB1"
  },
  {
    "timestamp": "2024-06-20T22:28:14",
    "sql": "drop user tkpjb35428646 cascade",
    "executionTime": "58ms",
    "connectionId": "oRBTwv3qQPOXXuktAXpezA==",
    "tenant": "CDB1_PDB1"
  }
]
```

- Extract Connections Events:

```json
[
  {
    "timestamp": "2024-06-20T22:27:11",
    "event": "CONNECTION_OPENED",
    "details": "sdu=8192, tdu=2097152 nt: host=phxdbfdf83, port=3484, socketOptions={0=YES, 1=NO, 2=0, 6=1.2, 38=TLS, 40=false, 8=/ade/b/1465033565/oracle/work/jnetadmin_c/cwallet.sso, 9=SSO, 11=/ade/b/1465033565/oracle/work/jnetadmin_c/cwallet.sso, 12=SSO, 46=1, 17=0, 18=false, 20=true, 23=40, 24=50, 25=0}     socket=Socket[addr=phxdbfdf83/100.94.241.218,port=3484,localport=50382] client profile={oracle.net.encryption_types_client=(), oracle.net.crypto_seed=, oracle.net.authentication_services=(), oracle.net.setFIPSMode=false, oracle.net.kerberos5_mutual_authentication=false, oracle.net.encryption_client=ACCEPTED, oracle.net.crypto_checksum_client=ACCEPTED, oracle.net.crypto_checksum_types_client=()} connection options=[host=phxdbfdf83 port=3484 protocol=tcps service_name=cdb1_pdb1.regress.rdbms.dev.us.oracle.com addr=(ADDRESS=(PROTOCOL=tcps)(HOST=phxdbfdf83)(PORT=3484)) conn_data=(DESCRIPTION=(ADDRESS=(PROTOCOL=tcps)(PORT=3484)(HOST=100.94.241.218)(HOSTNAME=phxdbfdf83))(CONNECT_DATA=(CID=(PROGRAM=AppJdbcHarness)(HOST=phxdbfdf83)(USER=aime1))(SERVICE_NAME=cdb1_pdb1.regress.rdbms.dev.us.oracle.com))) done=true] onBreakReset=false, dataEOF=false, negotiatedOptions=0xc01, connected=true TTIINIT enabled=true, TTC cookie enabled=true , cookie found? no"
  },
  {
    "timestamp": "2024-06-20T22:27:12",
    "event": "CONNECTION_OPENED",
    "details": "sdu=8192, tdu=2097152 nt: host=phxdbfdf83, port=3484, socketOptions={0=YES, 1=NO, 2=0, 6=1.2, 38=TLS, 40=false, 8=/ade/b/1465033565/oracle/work/jnetadmin_c/cwallet.sso, 9=SSO, 11=/ade/b/1465033565/oracle/work/jnetadmin_c/cwallet.sso, 12=SSO, 46=1, 17=0, 18=false, 20=true, 23=40, 24=50, 25=0}     socket=Socket[addr=phxdbfdf83/100.94.241.218,port=3484,localport=50150] client profile={oracle.net.encryption_types_client=(), oracle.net.crypto_seed=, oracle.net.authentication_services=(), oracle.net.setFIPSMode=false, oracle.net.kerberos5_mutual_authentication=false, oracle.net.encryption_client=ACCEPTED, oracle.net.crypto_checksum_client=ACCEPTED, oracle.net.crypto_checksum_types_client=()} connection options=[host=phxdbfdf83 port=3484 protocol=tcps service_name=cdb1_pdb1.regress.rdbms.dev.us.oracle.com addr=(ADDRESS=(PROTOCOL=tcps)(HOST=phxdbfdf83)(PORT=3484)) conn_data=(DESCRIPTION=(ADDRESS=(PROTOCOL=tcps)(PORT=3484)(HOST=100.94.241.218)(HOSTNAME=phxdbfdf83))(CONNECT_DATA=(CID=(PROGRAM=AppJdbcHarness)(HOST=phxdbfdf83)(USER=aime1))(SERVICE_NAME=cdb1_pdb1.regress.rdbms.dev.us.oracle.com))) done=true] onBreakReset=false, dataEOF=false, negotiatedOptions=0xc01, connected=true TTIINIT enabled=true, TTC cookie enabled=true , cookie found? T4CTTICookie{version=1, connectionProtocolVersion=6, databaseCharSet=873, databaseCharSetFlag=33, databaseNCharSet=2000, databaseRuntimeCapabilities=[2, 1, 0, 1, 24, 0, 127, 1, 0, 0, 0, 0], databaseCompileTimeCapabilities=[6, 1, 1, 1, -17, 15, 1, 37, 1, 1, 1, 1, 1, 1, 1, 127, -1, 3, 16, 3, 3, 1, 1, -1, 1, -1, -1, 1, 12, 1, 1, -1, 1, 6, 12, -10, 9, 127, 5, 15, -1, 13, 11, 0, 63, 0, 0, 0, 0, 0, 0, 2, 1], databasePortage=[120, 56, 54, 95, 54, 52, 47, 76, 105, 110, 117, 120, 32, 50, 46, 52, 46, 120, 120]}"
  },
  {
    "timestamp": "2024-06-20T22:27:12",
    "event": "CONNECTION_OPENED",
    "details": "sdu=8192, tdu=2097152 nt: host=phxdbfdf83, port=3484, socketOptions={0=YES, 1=NO, 2=0, 6=1.2, 38=TLS, 40=false, 8=/ade/b/1465033565/oracle/work/jnetadmin_c/cwallet.sso, 9=SSO, 11=/ade/b/1465033565/oracle/work/jnetadmin_c/cwallet.sso, 12=SSO, 46=1, 17=0, 18=false, 20=true, 23=40, 24=50, 25=0}     socket=Socket[addr=phxdbfdf83/100.94.241.218,port=3484,localport=50164] client profile={oracle.net.encryption_types_client=(), oracle.net.crypto_seed=, oracle.net.authentication_services=(), oracle.net.setFIPSMode=false, oracle.net.kerberos5_mutual_authentication=false, oracle.net.encryption_client=ACCEPTED, oracle.net.crypto_checksum_client=ACCEPTED, oracle.net.crypto_checksum_types_client=()} connection options=[host=phxdbfdf83 port=3484 protocol=tcps service_name=cdb1_pdb1.regress.rdbms.dev.us.oracle.com addr=(ADDRESS=(PROTOCOL=tcps)(HOST=phxdbfdf83)(PORT=3484)) conn_data=(DESCRIPTION=(ADDRESS=(PROTOCOL=tcps)(PORT=3484)(HOST=100.94.241.218)(HOSTNAME=phxdbfdf83))(CONNECT_DATA=(CID=(PROGRAM=AppJdbcHarness)(HOST=phxdbfdf83)(USER=aime1))(SERVICE_NAME=cdb1_pdb1.regress.rdbms.dev.us.oracle.com))) done=true] onBreakReset=false, dataEOF=false, negotiatedOptions=0xc01, connected=true TTIINIT enabled=true, TTC cookie enabled=true , cookie found? T4CTTICookie{version=1, connectionProtocolVersion=6, databaseCharSet=873, databaseCharSetFlag=33, databaseNCharSet=2000, databaseRuntimeCapabilities=[2, 1, 0, 1, 24, 0, 127, 1, 0, 0, 0, 0], databaseCompileTimeCapabilities=[6, 1, 1, 1, -17, 15, 1, 37, 1, 1, 1, 1, 1, 1, 1, 127, -1, 3, 16, 3, 3, 1, 1, -1, 1, -1, -1, 1, 12, 1, 1, -1, 1, 6, 12, -10, 9, 127, 5, 15, -1, 13, 11, 0, 63, 0, 0, 0, 0, 0, 0, 2, 1], databasePortage=[120, 56, 54, 95, 54, 52, 47, 76, 105, 110, 117, 120, 32, 50, 46, 52, 46, 120, 120]}"
  },
  {
    "timestamp": "2024-06-20T22:27:12",
    "event": "CONNECTION_CLOSED",
    "details": "null"
  },
  {
    "timestamp": "2024-06-20T22:27:12",
    "event": "CONNECTION_CLOSED",
    "details": "null"
  },
  {
    "timestamp": "2024-06-20T22:27:12",
    "event": "CONNECTION_CLOSED",
    "details": "null"
  },
  {
    "timestamp": "2024-06-20T22:27:12",
    "event": "CONNECTION_OPENED",
    "details": "sdu=8192, tdu=2097152 nt: host=phxdbfdf83, port=3484, socketOptions={0=YES, 1=NO, 2=0, 6=1.2, 38=TLS, 40=false, 8=/ade/b/1465033565/oracle/work/jnetadmin_c/cwallet.sso, 9=SSO, 11=/ade/b/1465033565/oracle/work/jnetadmin_c/cwallet.sso, 12=SSO, 46=1, 17=0, 18=false, 20=true, 23=40, 24=50, 25=0}     socket=Socket[addr=phxdbfdf83/100.94.241.218,port=3484,localport=50176] client profile={oracle.net.encryption_types_client=(), oracle.net.crypto_seed=, oracle.net.authentication_services=(), oracle.net.setFIPSMode=false, oracle.net.kerberos5_mutual_authentication=false, oracle.net.encryption_client=ACCEPTED, oracle.net.crypto_checksum_client=ACCEPTED, oracle.net.crypto_checksum_types_client=()} connection options=[host=phxdbfdf83 port=3484 protocol=tcps service_name=cdb1_pdb1.regress.rdbms.dev.us.oracle.com addr=(ADDRESS=(PROTOCOL=tcps)(HOST=phxdbfdf83)(PORT=3484)) conn_data=(DESCRIPTION=(ADDRESS=(PROTOCOL=tcps)(PORT=3484)(HOST=100.94.241.218)(HOSTNAME=phxdbfdf83))(CONNECT_DATA=(CID=(PROGRAM=AppJdbcHarness)(HOST=phxdbfdf83)(USER=aime1))(SERVICE_NAME=cdb1_pdb1.regress.rdbms.dev.us.oracle.com))) done=true] onBreakReset=false, dataEOF=false, negotiatedOptions=0xc01, connected=true TTIINIT enabled=true, TTC cookie enabled=true , cookie found? T4CTTICookie{version=1, connectionProtocolVersion=6, databaseCharSet=873, databaseCharSetFlag=33, databaseNCharSet=2000, databaseRuntimeCapabilities=[2, 1, 0, 1, 24, 0, 127, 1, 0, 0, 0, 0], databaseCompileTimeCapabilities=[6, 1, 1, 1, -17, 15, 1, 37, 1, 1, 1, 1, 1, 1, 1, 127, -1, 3, 16, 3, 3, 1, 1, -1, 1, -1, -1, 1, 12, 1, 1, -1, 1, 6, 12, -10, 9, 127, 5, 15, -1, 13, 11, 0, 63, 0, 0, 0, 0, 0, 0, 2, 1], databasePortage=[120, 56, 54, 95, 54, 52, 47, 76, 105, 110, 117, 120, 32, 50, 46, 52, 46, 120, 120]}"
  },
  {
    "timestamp": "2024-06-20T22:28:14",
    "event": "CONNECTION_OPENED",
    "details": "sdu=8192, tdu=2097152 nt: host=phxdbfdf83, port=3484, socketOptions={0=YES, 1=NO, 2=0, 6=1.2, 38=TLS, 40=false, 8=/ade/b/1465033565/oracle/work/jnetadmin_c/cwallet.sso, 9=SSO, 11=/ade/b/1465033565/oracle/work/jnetadmin_c/cwallet.sso, 12=SSO, 46=1, 17=0, 18=false, 20=true, 23=40, 24=50, 25=0}     socket=Socket[addr=phxdbfdf83/100.94.241.218,port=3484,localport=38852] client profile={oracle.net.encryption_types_client=(), oracle.net.crypto_seed=, oracle.net.authentication_services=(), oracle.net.setFIPSMode=false, oracle.net.kerberos5_mutual_authentication=false, oracle.net.encryption_client=ACCEPTED, oracle.net.crypto_checksum_client=ACCEPTED, oracle.net.crypto_checksum_types_client=()} connection options=[host=phxdbfdf83 port=3484 protocol=tcps service_name=cdb1_pdb1.regress.rdbms.dev.us.oracle.com addr=(ADDRESS=(PROTOCOL=tcps)(HOST=phxdbfdf83)(PORT=3484)) conn_data=(DESCRIPTION=(ADDRESS=(PROTOCOL=tcps)(PORT=3484)(HOST=100.94.241.218)(HOSTNAME=phxdbfdf83))(CONNECT_DATA=(CID=(PROGRAM=AppJdbcHarness)(HOST=phxdbfdf83)(USER=aime1))(SERVICE_NAME=cdb1_pdb1.regress.rdbms.dev.us.oracle.com))) done=true] onBreakReset=false, dataEOF=false, negotiatedOptions=0xc01, connected=true TTIINIT enabled=true, TTC cookie enabled=true , cookie found? T4CTTICookie{version=1, connectionProtocolVersion=6, databaseCharSet=873, databaseCharSetFlag=33, databaseNCharSet=2000, databaseRuntimeCapabilities=[2, 1, 0, 1, 24, 0, 127, 1, 0, 0, 0, 0], databaseCompileTimeCapabilities=[6, 1, 1, 1, -17, 15, 1, 37, 1, 1, 1, 1, 1, 1, 1, 127, -1, 3, 16, 3, 3, 1, 1, -1, 1, -1, -1, 1, 12, 1, 1, -1, 1, 6, 12, -10, 9, 127, 5, 15, -1, 13, 11, 0, 63, 0, 0, 0, 0, 0, 0, 2, 1], databasePortage=[120, 56, 54, 95, 54, 52, 47, 76, 105, 110, 117, 120, 32, 50, 46, 52, 46, 120, 120]}"
  },
  {
    "timestamp": "2024-06-20T22:28:14",
    "event": "CONNECTION_CLOSED",
    "details": "null"
  }
]
```

Comparison between two OJDBC log files:

```json
{
  "summary": {
    "referenceLogFileName": "/Users/youssef/Desktop/logs/ojdbc17-test1.log",
    "currentLogFileName": "/Users/youssef/Desktop/logs/ojdbc17-test2.log",
    "referenceLogFileSize": "1.363 MB",
    "currentLogFileSize": "578.626 MB",
    "referenceLogFileLineCount": 25795,
    "currentLogFileLineCount": 5643918,
    "lineCountDelta": "+21779.89%",
    "referenceLogFileTimespan": "2024-06-20T22:27:11 to 2024-06-20T22:28:14",
    "referenceLogFileDuration": "PT1M3S",
    "currentLogFileTimespan": "2024-10-21T00:32:39.117Z to 2024-10-21T00:34:41.424Z",
    "currentLogFileDuration": "PT2M2.307S"
  },
  "performance": {
    "referenceQueryCount": 17,
    "currentQueryCount": 16572,
    "queryCountDelta": "+97382.35%",
    "referenceAverageQueryTime": "19.71 ms",
    "currentAverageQueryTime": "24.25 ms",
    "averageQueryTimeDelta": "+23.03%"
  },
  "error": {
    "referenceErrorCount": 14,
    "currentErrorCount": 10,
    "totalErrorsDelta": "-28.57%"
  },
  "network": {
    "referenceBytesConsumed": "55.197 kB",
    "currentBytesConsumed": "18.770 MB",
    "bytesConsumedDelta": "+33905.47%",
    "referenceBytesProduced": "3.286 MB",
    "currentBytesProduced": "2.388 MB",
    "bytesProducedDelta": "-27.33%"
  }
}
```

### 2. Analyzing a RDBMS/SQLNet trace file

RDBMS and SQLNet logs can be parsed with `oracle.jdbc.logs.analyzer.RDBMSLog` class

Here's an example of how the APIs can be used:

```java
import oracle.jdbc.logs.analyzer.RDBMSLog;
import oracle.jdbc.logs.model.RDBMSError;
import oracle.jdbc.logs.model.RDBMSPacketDump;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class RDBMSMainClass {

  public static void main(String[] args) throws IOException {
    final String logFilePath = "/Users/youssef/Desktop/logs/rdbms/txn_ora_81646.trc.sav";
    // We can also provide a URL instead of a local path.
    final RDBMSLog rdbmsLogParser = new RDBMSLog(logFilePath);

    System.out.println("===== Extract ORA Errors ======");
    final List<RDBMSError> errors = rdbmsLogParser.getErrors();

    if (errors.isEmpty()) {
      System.out.println("No errors found!");
    } else {
      System.out.println(
        errors
          .stream()
          .map(RDBMSError::toJSONString)
          .collect(Collectors.joining(",", "[","]"))
      );
    }


    final String connectionId = "BZyObJFSTCyZV9xp+bad4Q==";
    System.out.println("\n===== Extract PacketDumps of {"+connectionId+"} ======");
    final List<RDBMSPacketDump> packetDumps = rdbmsLogParser.getPacketDumps(connectionId);

    if (packetDumps.isEmpty()) {
      System.out.println("No packet dumps found for: " + connectionId);
    } else {
      System.out.println(
        packetDumps
          .stream()
          .map(RDBMSPacketDump::toJSONString)
          .collect(Collectors.joining(",", "[","]"))
      );
    }

  }

}
```

Example of the extracted information printed as JSON:

- Extract ORA Errors:

```json
[
  {
    "errorMessage": "ORA-07445: exception encountered: core dump [PC:0x1C8A1E74] [SIGSEGV] [ADDR:0x1C8A1E74] [PC:0x1C8A1E74] [Invalid permissions for mapped object] []",
    "documentationLink": "https://docs.oracle.com/en/error-help/db/ORA-07445/?r=26ai"
  },
  {
    "errorMessage": "ORA-01918: user 'USERNOTFOUND' does not exist",
    "documentationLink": "https://docs.oracle.com/en/error-help/db/ORA-01918/?r=26ai"
  }
]
```

- Extract PacketDumps of `connectionId`:

```json
[
  {
    "timestamp": "2025-04-08 15:08:45.91120",
    "formattedPacket": "00 4A 00 00 01 00 00 00  |.J......|\n01 3F 01 2C 0C 41 20 00  |.?.,.A..|\nFF FF 4F 98 00 00 00 01  |..O.....|\n00 F1 00 4A 00 00 00 00  |...J....|\n82 82 00 00 00 00 00 00  |........|\n00 00 00 00 00 00 00 00  |........|\n00 00 00 00 00 00 00 00  |........|\n00 00 00 00 20 00 00 20  |........|\n00 00 00 00 00 00 00 00  |........|\n00 01                    |..      |"
  },
  {
    "timestamp": "2025-04-08 15:08:45.91827",
    "formattedPacket": "00 3D 00 00 02 00 00 00  |.=......|\n01 3F 0C 41 00 00 00 00  |.?.A....|\n01 00 00 00 00 3D C3 0A  |.....=..|\n00 00 00 00 00 00 00 00  |........|\n00 00 20 00 00 20 00 00  |........|\n00 12 00 00 01 F2 0E E0  |........|\nAC 14 5B 3B 94 A7 EB BE  |..[;....|\n97 43 25 BA 70           |.C%.p   |"
  },
  {
    "timestamp": "2025-04-08 15:08:46.14721",
    "formattedPacket": "00 00 00 0B 0C 20 00 00  |........|\n02 00 03                 |...     |"
  },
  {
    "timestamp": "2025-04-08 15:08:46.27239",
    "formattedPacket": "00 00 0B AE 06 20 00 00  |........|\n08 00 22 01 00 00 01 06  |..\".....|\n05 04 03 02 01 00 4A 61  |......Ja|\n76 61 5F 54 54 43 2D 38  |va_TTC-8|\n2E 32 2E 30 00 69 03 01  |.2.0.i..|\nD0 07 18 02 69 03 69 03  |....i.i.|\n01 36 06 01 00 00 EA 0C  |.6......|\n01 18 01 01 01 01 01 01  |........|\n00 29 90 03 07 03 00 01  |.)......|\n00 CF 01 37 04 01 00 00  |...7....|\n00 1C 00 00 0B A0 03 B9  |........|\n00 0F 56 0D 0D 00 28 01  |..V...(.|\n00 00 00 00 00 00 03 35  |.......5|\n0C 02 00 00 00 00 00 05  |........|\n00 02 00 00 00 00 01 00  |........|\n01 00 01 00 00 00 02 00  |........|\n02 00 0A 00 00 00 FC 00  |........|\nFC 00 01 00 00 00 08 00  |........|\n08 00 01 00 00 00 0C 00  |........|\n0C 00 0A 00 00 00 17 00  |........|\n17 00 01 00 00 00 18 00  |........|\n18 00 01 00 00 00 19 00  |........|\n19 00 01 00 00 00 1A 00  |........|\n1A 00 01 00 00 00 1B 00  |........|\n1B 00 01 00 00 00 1C 00  |........|\n1C 00 01 00 00 00 1D 00  |........|\n1D 00 01 00 00 00 1E 00  |........|\n1E 00 01 00 00 00 1F 00  |........|\n1F 00 01 00 00 00 20 00  |........|\n20 00 01 00 00 00 21 00  |......!.|\n21 00 01 00 00 00 0A 00  |!.......|\n0A 00 01 00 00 00 0B 00  |........|\n0B 00 01 00 00 00 28 00  |......(.|\n28 00 01 00 00 00 29 00  |(.....).|\n29 00 01 00 00 00 75 00  |).....u.|\n75 00 01 00 00 00 78 00  |u.....x.|\n78 00 01 00 00 01 22 01  |x.....\".|\n22 00 01 00 00 01 23 01  |\".....#.|\n23 00 01 00 00 01 24 01  |#.....$.|\n24 00 01 00 00 01 25 01  |$.....%.|\n25 00 01 00 00 01 26 01  |%.....&.|\n26 00 01 00 00 01 2A 01  |&.....*.|\n2A 00 01 00 00 01 2B 01  |*.....+.|\n2B 00 01 00 00 01 2C 01  |+.....,.|\n2C 00 01 00 00 01 2D 01  |,.....-.|\n2D 00 01 00 00 01 2E 01  |-.......|\n2E 00 01 00 00 01 2F 01  |....../.|\n2F 00 01 00 00 01 30 01  |/.....0.|\n30 00 01 00 00 01 31 01  |0.....1.|\n31 00 01 00 00 01 32 01  |1.....2.|\n32 00 01 00 00 01 33 01  |2.....3.|\n33 00 01 00 00 01 34 01  |3.....4.|\n34 00 01 00 00 01 35 01  |4.....5.|\n35 00 01 00 00 01 36 01  |5.....6.|\n36 00 01 00 00 01 37 01  |6.....7.|\n37 00 01 00 00 01 38 01  |7.....8.|\n38 00 01 00 00 01 39 01  |8.....9.|\n39 00 01 00 00 01 3B 01  |9.....;.|\n3B 00 01 00 00 01 3C 01  |;.....<.|\n3C 00 01 00 00 01 3D 01  |<.....=.|\n3D 00 01 00 00 01 3E 01  |=.....>.|\n3E 00 01 00 00 01 3F 01  |>.....?.|\n3F 00 01 00 00 01 40 01  |?.....@.|\n40 00 01 00 00 01 41 01  |@.....A.|\n41 00 01 00 00 01 42 01  |A.....B.|\n42 00 01 00 00 01 43 01  |B.....C.|\n43 00 01 00 00 01 47 01  |C.....G.|\n47 00 01 00 00 01 48 01  |G.....H.|\n48 00 01 00 00 01 49 01  |H.....I.|\n49 00 01 00 00 01 4B 01  |I.....K.|\n4B 00 01 00 00 01 4D 01  |K.....M.|\n4D 00 01 00 00 01 4E 01  |M.....N.|\n4E 00 01 00 00 01 4F 01  |N.....O.|\n4F 00 01 00 00 01 50 01  |O.....P.|\n50 00 01 00 00 01 51 01  |P.....Q.|\n51 00 01 00 00 01 52 01  |Q.....R.|\n52 00 01 00 00 01 53 01  |R.....S.|\n53 00 01 00 00 01 54 01  |S.....T.|\n54 00 01 00 00 01 55 01  |T.....U.|\n55 00 01 00 00 01 56 01  |U.....V.|\n56 00 01 00 00 01 57 01  |V.....W.|\n57 00 01 00 00 01 58 01  |W.....X.|\n58 00 01 00 00 01 59 01  |X.....Y.|\n59 00 01 00 00 01 5A 01  |Y.....Z.|\n5A 00 01 00 00 01 5C 01  |Z.....\\.|\n5C 00 01 00 00 01 5D 01  |\\.....].|\n5D 00 01 00 00 01 62 01  |].....b.|\n62 00 01 00 00 01 63 01  |b.....c.|\n63 00 01 00 00 01 67 01  |c.....g.|\n67 00 01 00 00 01 6B 01  |g.....k.|\n6B 00 01 00 00 01 7C 01  |k.....|.|\n7C 00 01 00 00 01 7D 01  ||.....}.|\n7D 00 01 00 00 01 7E 01  |}.....~.|\n7E 00 01 00 00 01 7F 01  |~.......|\n7F 00 01 00 00 01 80 01  |........|\n80 00 01 00 00 01 81 01  |........|\n81 00 01 00 00 01 82 01  |........|\n82 00 01 00 00 01 83 01  |........|\n83 00 01 00 00 01 84 01  |........|\n84 00 01 00 00 01 85 01  |........|\n85 00 01 00 00 01 86 01  |........|\n86 00 01 00 00 01 87 01  |........|\n87 00 01 00 00 01 89 01  |........|\n89 00 01 00 00 01 8A 01  |........|\n8A 00 01 00 00 01 8B 01  |........|\n8B 00 01 00 00 01 8C 01  |........|\n8C 00 01 00 00 01 8D 01  |........|\n8D 00 01 00 00 01 8E 01  |........|\n8E 00 01 00 00 01 8F 01  |........|\n8F 00 01 00 00 01 90 01  |........|\n90 00 01 00 00 01 91 01  |........|\n91 00 01 00 00 01 94 01  |........|\n94 00 01 00 00 01 95 01  |........|\n95 00 01 00 00 01 96 01  |........|\n96 00 01 00 00 01 97 01  |........|\n97 00 01 00 00 01 9D 01  |........|\n9D 00 01 00 00 01 9E 01  |........|\n9E 00 01 00 00 01 9F 01  |........|\n9F 00 01 00 00 01 A0 01  |........|\nA0 00 01 00 00 01 A1 01  |........|\nA1 00 01 00 00 01 A2 01  |........|\nA2 00 01 00 00 01 A3 01  |........|\nA3 00 01 00 00 01 A4 01  |........|\nA4 00 01 00 00 01 A5 01  |........|\nA5 00 01 00 00 01 A6 01  |........|\nA6 00 01 00 00 01 A7 01  |........|\nA7 00 01 00 00 01 A8 01  |........|\nA8 00 01 00 00 01 A9 01  |........|\nA9 00 01 00 00 01 AA 01  |........|\nAA 00 01 00 00 01 AB 01  |........|\nAB 00 01 00 00 01 AD 01  |........|\nAD 00 01 00 00 01 AE 01  |........|\nAE 00 01 00 00 01 AF 01  |........|\nAF 00 01 00 00 01 B0 01  |........|\nB0 00 01 00 00 01 B1 01  |........|\nB1 00 01 00 00 01 C1 01  |........|\nC1 00 01 00 00 01 C2 01  |........|\nC2 00 01 00 00 01 C6 01  |........|\nC6 00 01 00 00 01 C7 01  |........|\nC7 00 01 00 00 01 C8 01  |........|\nC8 00 01 00 00 01 C9 01  |........|\nC9 00 01 00 00 01 CA 01  |........|\nCA 00 01 00 00 01 CB 01  |........|\nCB 00 01 00 00 01 CC 01  |........|\nCC 00 01 00 00 01 CD 01  |........|\nCD 00 01 00 00 01 CE 01  |........|\nCE 00 01 00 00 01 CF 01  |........|\nCF 00 01 00 00 01 D2 01  |........|\nD2 00 01 00 00 01 D3 01  |........|\nD3 00 01 00 00 01 D4 01  |........|\nD4 00 01 00 00 01 D5 01  |........|\nD5 00 01 00 00 01 D6 01  |........|\nD6 00 01 00 00 01 D7 01  |........|\nD7 00 01 00 00 01 D8 01  |........|\nD8 00 01 00 00 01 D9 01  |........|\nD9 00 01 00 00 01 DA 01  |........|\nDA 00 01 00 00 01 DB 01  |........|\nDB 00 01 00 00 01 DC 01  |........|\nDC 00 01 00 00 01 DD 01  |........|\nDD 00 01 00 00 01 DE 01  |........|\nDE 00 01 00 00 01 DF 01  |........|\nDF 00 01 00 00 01 E0 01  |........|\nE0 00 01 00 00 01 E1 01  |........|\nE1 00 01 00 00 01 E2 01  |........|\nE2 00 01 00 00 01 E3 01  |........|\nE3 00 01 00 00 01 E4 01  |........|\nE4 00 01 00 00 01 E5 01  |........|\nE5 00 01 00 00 01 E6 01  |........|\nE6 00 01 00 00 01 EA 01  |........|\nEA 00 01 00 00 01 EB 01  |........|\nEB 00 01 00 00 01 EC 01  |........|\nEC 00 01 00 00 01 ED 01  |........|\nED 00 01 00 00 01 EE 01  |........|\nEE 00 01 00 00 01 EF 01  |........|\nEF 00 01 00 00 01 F0 01  |........|\nF0 00 01 00 00 01 F2 01  |........|\nF2 00 01 00 00 01 F3 01  |........|\nF3 00 01 00 00 01 F4 01  |........|\nF4 00 01 00 00 01 F5 01  |........|\nF5 00 01 00 00 01 F6 01  |........|\nF6 00 01 00 00 01 FD 01  |........|\nFD 00 01 00 00 01 FE 01  |........|\nFE 00 01 00 00 02 01 02  |........|\n01 00 01 00 00 02 02 02  |........|\n02 00 01 00 00 02 04 02  |........|\n04 00 01 00 00 02 05 02  |........|\n05 00 01 00 00 02 06 02  |........|\n06 00 01 00 00 02 07 02  |........|\n07 00 01 00 00 02 08 02  |........|\n08 00 01 00 00 02 09 02  |........|\n09 00 01 00 00 02 0A 02  |........|\n0A 00 01 00 00 02 0B 02  |........|\n0B 00 01 00 00 02 0C 02  |........|\n0C 00 01 00 00 02 0D 02  |........|\n0D 00 01 00 00 02 0E 02  |........|\n0E 00 01 00 00 02 0F 02  |........|\n0F 00 01 00 00 02 10 02  |........|\n10 00 01 00 00 02 11 02  |........|\n11 00 01 00 00 02 12 02  |........|\n12 00 01 00 00 02 13 02  |........|\n13 00 01 00 00 02 14 02  |........|\n14 00 01 00 00 02 15 02  |........|\n15 00 01 00 00 02 16 02  |........|\n16 00 01 00 00 02 17 02  |........|\n17 00 01 00 00 02 18 02  |........|\n18 00 01 00 00 02 19 02  |........|\n19 00 01 00 00 02 1A 02  |........|\n1A 00 01 00 00 02 1B 02  |........|\n1B 00 01 00 00 02 1C 02  |........|\n1C 00 01 00 00 02 1D 02  |........|\n1D 00 01 00 00 02 1E 02  |........|\n1E 00 01 00 00 02 1F 02  |........|\n1F 00 01 00 00 02 30 02  |......0.|\n30 00 01 00 00 02 35 02  |0.....5.|\n35 00 01 00 00 02 3C 02  |5.....<.|\n3C 00 01 00 00 02 3D 02  |<.....=.|\n3D 00 01 00 00 02 3E 02  |=.....>.|\n3E 00 01 00 00 02 3F 02  |>.....?.|\n3F 00 01 00 00 02 40 02  |?.....@.|\n40 00 01 00 00 02 42 02  |@.....B.|\n42 00 01 00 00 02 33 02  |B.....3.|\n33 00 01 00 00 02 34 02  |3.....4.|\n34 00 01 00 00 02 43 02  |4.....C.|\n43 00 01 00 00 02 44 02  |C.....D.|\n44 00 01 00 00 02 45 02  |D.....E.|\n45 00 01 00 00 02 46 02  |E.....F.|\n46 00 01 00 00 02 47 02  |F.....G.|\n47 00 01 00 00 02 48 02  |G.....H.|\n48 00 01 00 00 02 49 02  |H.....I.|\n49 00 01 00 00 00 03 00  |I.......|\n02 00 0A 00 00 00 04 00  |........|\n02 00 0A 00 00 00 05 00  |........|\n01 00 01 00 00 00 06 00  |........|\n02 00 0A 00 00 00 07 00  |........|\n02 00 0A 00 00 00 09 00  |........|\n01 00 01 00 00 00 0D 00  |........|\n00 00 0E 00 00 00 0F 00  |........|\n17 00 01 00 00 00 10 00  |........|\n00 00 11 00 00 00 12 00  |........|\n00 00 13 00 00 00 14 00  |........|\n00 00 15 00 00 00 16 00  |........|\n00 00 27 02 86 00 01 00  |..'.....|\n00 00 3A 00 00 00 44 00  |..:...D.|\n02 00 0A 00 00 00 45 00  |......E.|\n00 00 46 00 00 00 4A 00  |..F...J.|\n00 00 4C 00 00 00 5B 00  |..L...[.|\n02 00 0A 00 00 00 5E 00  |......^.|\n01 00 01 00 00 00 5F 00  |......_.|\n17 00 01 00 00 00 60 00  |......`.|\n60 00 01 00 00 00 61 00  |`.....a.|\n60 00 01 00 00 00 64 00  |`.....d.|\n64 00 01 00 00 00 65 00  |d.....e.|\n65 00 01 00 00 00 66 00  |e.....f.|\n66 00 01 00 00 00 68 00  |f.....h.|\n0B 00 01 00 00 00 69 00  |......i.|\n00 00 6A 00 6A 00 01 00  |..j.j...|\n00 00 6C 00 6D 00 01 00  |..l.m...|\n00 00 6D 00 6D 00 01 00  |..m.m...|\n00 00 6E 00 6F 00 01 00  |..n.o...|\n00 00 6F 00 6F 00 01 00  |..o.o...|\n00 00 70 00 70 00 01 00  |..p.p...|\n00 00 71 00 71 00 01 00  |..q.q...|\n00 00 72 00 72 00 01 00  |..r.r...|\n00 00 73 00 73 00 01 00  |..s.s...|\n00 00 74 00 66 00 01 00  |..t.f...|\n00 00 76 00 00 00 77 00  |..v...w.|\n77 00 01 00 00 00 79 00  |w.....y.|\n00 00 7A 00 00 00 7B 00  |..z...{.|\n00 00 7F 00 7F 00 01 00  |........|\n00 00 88 00 00 00 92 00  |........|\n92 00 01 00 00 00 93 00  |........|\n00 00 98 00 02 00 0A 00  |........|\n00 00 99 00 02 00 0A 00  |........|\n00 00 9A 00 02 00 0A 00  |........|\n00 00 9B 00 01 00 01 00  |........|\n00 00 9C 00 0C 00 0A 00  |........|\n00 00 AC 00 02 00 0A 00  |........|\n00 00 B2 00 B2 00 01 00  |........|\n00 00 B3 00 B3 00 01 00  |........|\n00 00 B4 00 B4 00 01 00  |........|\n00 00 B5 00 B5 00 01 00  |........|\n00 00 B6 00 B6 00 01 00  |........|\n00 00 B7 00 B7 00 01 00  |........|\n00 00 B8 00 0C 00 0A 00  |........|\n00 00 B9 00 B9 00 01 00  |........|\n00 00 BA 00 BA 00 01 00  |........|\n00 00 BB 00 BB 00 01 00  |........|\n00 00 BC 00 BC 00 01 00  |........|\n00 00 BD 00 BD 00 01 00  |........|\n00 00 BE 00 BE 00 01 00  |........|\n00 00 BF 00 00 00 C0 00  |........|\n00 00 C3 00 70 00 01 00  |....p...|\n00 00 C4 00 71 00 01 00  |....q...|\n00 00 C6 00 77 00 01 00  |....w...|\n00 00 C5 00 72 00 01 00  |....r...|\n00 00 D0 00 D0 00 01 00  |........|\n00 00 D1 00 00 00 E7 00  |........|\nE7 00 01 00 00 00 E8 00  |........|\nE7 00 01 00 00 00 E9 00  |........|\nE9 00 01 00 00 00 F1 00  |........|\n6D 00 01 00 00 02 03 00  |m.......|\n00 02 4E 02 4E 00 01 00  |..N.N...|\n00 02 4F 02 4F 00 01 00  |..O.O...|\n00 02 50 02 50 00 01 00  |..P.P...|\n00 02 65 02 65 00 01 00  |..e.e...|\n00 02 66 02 66 00 01 00  |..f.f...|\n00 02 67 02 67 00 01 00  |..g.g...|\n00 02 68 02 68 00 01 00  |..h.h...|\n00 02 87 02 87 00 01 00  |........|\n00 02 63 02 63 00 01 00  |..c.c...|\n00 02 8C 02 8C 00 01 00  |........|\n00 02 88 02 88 00 01 00  |........|\n00 02 8D 02 8D 00 01 00  |........|\n00 02 64 02 64 00 01 00  |..d.d...|\n00 02 69 02 69 00 01 00  |..i.i...|\n00 02 90 02 90 00 01 00  |........|\n00 02 7F 02 7F 00 01 00  |........|\n00 02 51 02 51 00 01 00  |..Q.Q...|\n00 02 52 02 52 00 01 00  |..R.R...|\n00 02 53 02 53 00 01 00  |..S.S...|\n00 02 54 02 54 00 01 00  |..T.T...|\n00 02 55 02 55 00 01 00  |..U.U...|\n00 02 56 02 56 00 01 00  |..V.V...|\n00 02 57 02 57 00 01 00  |..W.W...|\n00 02 58 02 58 00 01 00  |..X.X...|\n00 02 59 02 59 00 01 00  |..Y.Y...|\n00 02 5A 02 5A 00 01 00  |..Z.Z...|\n00 02 5B 02 5B 00 01 00  |..[.[...|\n00 02 5C 02 5C 00 01 00  |..\\.\\...|\n00 02 5D 02 5D 00 01 00  |..].]...|\n00 02 6E 02 6E 00 01 00  |..n.n...|\n00 02 6F 02 6F 00 01 00  |..o.o...|\n00 02 70 02 70 00 01 00  |..p.p...|\n00 02 71 02 71 00 01 00  |..q.q...|\n00 02 72 02 72 00 01 00  |..r.r...|\n00 02 73 02 73 00 01 00  |..s.s...|\n00 02 74 02 74 00 01 00  |..t.t...|\n00 02 75 02 75 00 01 00  |..u.u...|\n00 02 76 02 76 00 01 00  |..v.v...|\n00 02 77 02 77 00 01 00  |..w.w...|\n00 02 78 02 78 00 01 00  |..x.x...|\n00 02 7D 02 7D 00 01 00  |..}.}...|\n00 02 7E 02 7E 00 01 00  |..~.~...|\n00 02 7C 02 7C 00 01 00  |..|.|...|\n00 02 97 02 97 00 01 00  |........|\n00 02 80 02 80 00 01 00  |........|\n00 03 83 03 83 00 01 00  |........|\n00 03 84 03 84 00 01 00  |........|\n00 03 85 03 85 00 01 00  |........|\n00 02 86 02 86 00 01 00  |........|\n00 02 94 02 94 00 01 00  |........|\n00 02 95 02 95 00 01 00  |........|\n00 02 99 02 99 00 01 00  |........|\n00 02 9B 02 9B 00 01 00  |........|\n00 00 00 03 76 01 00 01  |....v...|\n01 06 01 01 01 01 05 01  |........|\n01 53 59 53 54 45 4D 01  |.SYSTEM.|\n0D 0D 41 55 54 48 5F 54  |..AUTH_T|\n45 52 4D 49 4E 41 4C 01  |ERMINAL.|\n07 07 75 6E 6B 6E 6F 77  |..unknow|\n6E 00 01 0F 0F 41 55 54  |n....AUT|\n48 5F 50 52 4F 47 52 41  |H_PROGRA|\n4D 5F 4E 4D 01 0E 0E 4A  |M_NM...J|\n44 42 43 53 61 6E 69 74  |DBCSanit|\n79 43 6F 6E 6E 00 01 0C  |yConn...|\n0C 41 55 54 48 5F 4D 41  |.AUTH_MA|\n43 48 49 4E 45 01 0B 0B  |CHINE...|\n79 6F 75 73 73 65 66 2D  |youssef-|\n6D 61 63 00 01 08 08 41  |mac....A|\n55 54 48 5F 50 49 44 01  |UTH_PID.|\n05 05 35 37 34 39 30 00  |..57490.|\n01 08 08 41 55 54 48 5F  |...AUTH_|\n53 49 44 01 07 07 79 6F  |SID...yo|\n75 73 73 65 66 00        |ussef.  |"
  },
  {
    "timestamp": "2025-04-08 15:08:46.31242",
    "formattedPacket": "00 00 0D 4D 06 00 00 00  |...M....|\n00 00 01 06 00 78 38 36  |.....x86|\n5F 36 34 2F 4C 69 6E 75  |_64/Linu|\n78 20 32 2E 34 2E 78 78  |x.2.4.xx|\n00 69 03 21 0A 00 66 03  |.i.!..f.|\n40 03 01 40 03 66 03 01  |@..@.f..|\n66 03 48 03 01 48 03 66  |f.H..H.f|\n03 01 66 03 52 03 01 52  |..f.R..R|\n03 66 03 01 66 03 61 03  |.f..f.a.|\n01 61 03 66 03 01 66 03  |.a.f..f.|\n1F 03 08 1F 03 66 03 01  |.....f..|\n00 64 00 00 00 60 01 24  |.d...`.$|\n0F 05 0B 0C 03 0C 0C 05  |........|\n04 05 0D 06 09 07 08 05  |........|\n05 05 05 05 0F 05 05 05  |........|\n05 05 0A 05 05 05 05 05  |........|\n04 05 06 07 08 08 23 47  |......#G|\n23 47 08 11 23 08 11 41  |#G..#..A|\nB0 47 00 83 03 69 07 D0  |.G...i..|\n03 00 00 00 00 00 00 00  |........|\n00 00 00 00 00 00 00 00  |........|\n00 00 00 00 00 00 00 00  |........|\n00 00 00 00 00 00 00 00  |........|\n00 00 00 00 00 00 37 06  |......7.|\n01 01 01 EF 0F 01 25 01  |......%.|\n01 01 01 01 01 01 7F FF  |........|\n03 10 03 03 01 01 FF 01  |........|\nFF FF 01 0D 01 01 FF 01  |........|\n06 0D F6 09 7F 05 0F FF  |........|\n0D 0B 00 FF 00 00 00 00  |........|\n00 00 02 02 02 00 0C 02  |........|\n01 00 01 18 00 7F 01 02  |........|\n00 00 00 02 00 01 00 01  |........|\n00 01 00 00 00 02 00 02  |........|\n00 0A 00 00 00 08 00 08  |........|\n00 01 00 00 00 0C 00 0C  |........|\n00 0A 00 00 00 17 00 17  |........|\n00 01 00 00 00 18 00 18  |........|\n00 01 00 00 00 19 00 19  |........|\n00 01 00 00 00 1A 00 1A  |........|\n00 01 00 00 00 1B 00 1B  |........|\n00 01 00 00 00 1C 00 1C  |........|\n00 01 00 00 00 1D 00 1D  |........|\n00 01 00 00 00 1E 00 1E  |........|\n00 01 00 00 00 1F 00 1F  |........|\n00 01 00 00 00 20 00 20  |........|\n00 01 00 00 00 21 00 21  |.....!.!|\n00 01 00 00 00 0A 00 0A  |........|\n00 01 00 00 00 0B 00 0B  |........|\n00 01 00 00 00 28 00 28  |.....(.(|\n00 01 00 00 00 29 00 29  |.....).)|\n00 01 00 00 00 75 00 75  |.....u.u|\n00 01 00 00 00 78 00 78  |.....x.x|\n00 01 00 00 01 22 01 22  |.....\".\"|\n00 01 00 00 01 23 01 23  |.....#.#|\n00 01 00 00 01 24 01 24  |.....$.$|\n00 01 00 00 01 25 01 25  |.....%.%|\n00 01 00 00 01 26 01 26  |.....&.&|\n00 01 00 00 01 2A 01 2A  |.....*.*|\n00 01 00 00 01 2B 01 2B  |.....+.+|\n00 01 00 00 01 2C 01 2C  |.....,.,|\n00 01 00 00 01 2D 01 2D  |.....-.-|\n00 01 00 00 01 2E 01 2E  |........|\n00 01 00 00 01 2F 01 2F  |....././|\n00 01 00 00 01 31 01 31  |.....1.1|\n00 01 00 00 01 32 01 32  |.....2.2|\n00 01 00 00 01 33 01 33  |.....3.3|\n00 01 00 00 01 34 01 34  |.....4.4|\n00 01 00 00 01 35 01 35  |.....5.5|\n00 01 00 00 01 36 01 36  |.....6.6|\n00 01 00 00 01 37 01 37  |.....7.7|\n00 01 00 00 01 38 01 38  |.....8.8|\n00 01 00 00 01 39 01 39  |.....9.9|\n00 01 00 00 01 3B 01 3B  |.....;.;|\n00 01 00 00 01 3C 01 3C  |.....<.<|\n00 01 00 00 01 3D 01 3D  |.....=.=|\n00 01 00 00 01 3E 01 3E  |.....>.>|\n00 01 00 00 01 3F 01 3F  |.....?.?|\n00 01 00 00 01 40 01 40  |.....@.@|\n00 01 00 00 01 41 01 41  |.....A.A|\n00 01 00 00 01 42 01 42  |.....B.B|\n00 01 00 00 01 43 01 43  |.....C.C|\n00 01 00 00 01 47 01 47  |.....G.G|\n00 01 00 00 01 48 01 48  |.....H.H|\n00 01 00 00 01 49 01 49  |.....I.I|\n00 01 00 00 01 4B 01 4B  |.....K.K|\n00 01 00 00 01 4D 01 4D  |.....M.M|\n00 01 00 00 01 53 01 53  |.....S.S|\n00 01 00 00 01 54 01 54  |.....T.T|\n00 01 00 00 01 55 01 55  |.....U.U|\n00 01 00 00 01 56 01 56  |.....V.V|\n00 01 00 00 01 57 01 57  |.....W.W|\n00 01 00 00 01 58 01 58  |.....X.X|\n00 01 00 00 01 59 01 59  |.....Y.Y|\n00 01 00 00 01 5A 01 5A  |.....Z.Z|\n00 01 00 00 01 5C 01 5C  |.....\\.\\|\n00 01 00 00 01 5D 01 5D  |.....].]|\n00 01 00 00 01 62 01 62  |.....b.b|\n00 01 00 00 01 63 01 63  |.....c.c|\n00 01 00 00 01 67 01 67  |.....g.g|\n00 01 00 00 01 6B 01 6B  |.....k.k|\n00 01 00 00 01 7C 01 7C  |.....|.||\n00 01 00 00 01 7D 01 7D  |.....}.}|\n00 01 00 00 01 7E 01 7E  |.....~.~|\n00 01 00 00 01 80 01 80  |........|\n00 01 00 00 01 81 01 81  |........|\n00 01 00 00 01 82 01 82  |........|\n00 01 00 00 01 83 01 83  |........|\n00 01 00 00 01 84 01 84  |........|\n00 01 00 00 01 85 01 85  |........|\n00 01 00 00 01 86 01 86  |........|\n00 01 00 00 01 87 01 87  |........|\n00 01 00 00 01 89 01 89  |........|\n00 01 00 00 01 8A 01 8A  |........|\n00 01 00 00 01 8B 01 8B  |........|\n00 01 00 00 01 8C 01 8C  |........|\n00 01 00 00 01 8D 01 8D  |........|\n00 01 00 00 01 8E 01 8E  |........|\n00 01 00 00 01 8F 01 8F  |........|\n00 01 00 00 01 90 01 90  |........|\n00 01 00 00 01 91 01 91  |........|\n00 01 00 00 01 94 01 94  |........|\n00 01 00 00 01 95 01 95  |........|\n00 01 00 00 01 96 01 96  |........|\n00 01 00 00 01 97 01 97  |........|\n00 01 00 00 01 9D 01 9D  |........|\n00 01 00 00 01 9E 01 9E  |........|\n00 01 00 00 01 9F 01 9F  |........|\n00 01 00 00 01 A0 01 A0  |........|\n00 01 00 00 01 A1 01 A1  |........|\n00 01 00 00 01 A2 01 A2  |........|\n00 01 00 00 01 A3 01 A3  |........|\n00 01 00 00 01 A4 01 A4  |........|\n00 01 00 00 01 A5 01 A5  |........|\n00 01 00 00 01 A6 01 A6  |........|\n00 01 00 00 01 A7 01 A7  |........|\n00 01 00 00 01 A8 01 A8  |........|\n00 01 00 00 01 A9 01 A9  |........|\n00 01 00 00 01 AA 01 AA  |........|\n00 01 00 00 01 AB 01 AB  |........|\n00 01 00 00 01 AD 01 AD  |........|\n00 01 00 00 01 AE 01 AE  |........|\n00 01 00 00 01 AF 01 AF  |........|\n00 01 00 00 01 B0 01 B0  |........|\n00 01 00 00 01 B1 01 B1  |........|\n00 01 00 00 01 C1 01 C1  |........|\n00 01 00 00 01 C2 01 C2  |........|\n00 01 00 00 01 C6 01 C6  |........|\n00 01 00 00 01 C7 01 C7  |........|\n00 01 00 00 01 C8 01 C8  |........|\n00 01 00 00 01 C9 01 C9  |........|\n00 01 00 00 01 CA 01 CA  |........|\n00 01 00 00 01 CB 01 CB  |........|\n00 01 00 00 01 CC 01 CC  |........|\n00 01 00 00 01 CD 01 CD  |........|\n00 01 00 00 01 CE 01 CE  |........|\n00 01 00 00 01 CF 01 CF  |........|\n00 01 00 00 01 D2 01 D2  |........|\n00 01 00 00 01 D3 01 D3  |........|\n00 01 00 00 01 D4 01 D4  |........|\n00 01 00 00 01 D5 01 D5  |........|\n00 01 00 00 01 D6 01 D6  |........|\n00 01 00 00 01 D7 01 D7  |........|\n00 01 00 00 01 D8 01 D8  |........|\n00 01 00 00 01 D9 01 D9  |........|\n00 01 00 00 01 DA 01 DA  |........|\n00 01 00 00 01 DB 01 DB  |........|\n00 01 00 00 01 DC 01 DC  |........|\n00 01 00 00 01 DD 01 DD  |........|\n00 01 00 00 01 DE 01 DE  |........|\n00 01 00 00 01 DF 01 DF  |........|\n00 01 00 00 01 E0 01 E0  |........|\n00 01 00 00 01 E1 01 E1  |........|\n00 01 00 00 01 E2 01 E2  |........|\n00 01 00 00 01 E3 01 E3  |........|\n00 01 00 00 01 E4 01 E4  |........|\n00 01 00 00 01 E5 01 E5  |........|\n00 01 00 00 01 E6 01 E6  |........|\n00 01 00 00 01 EA 01 EA  |........|\n00 01 00 00 01 EB 01 EB  |........|\n00 01 00 00 01 EC 01 EC  |........|\n00 01 00 00 01 ED 01 ED  |........|\n00 01 00 00 01 EE 01 EE  |........|\n00 01 00 00 01 EF 01 EF  |........|\n00 01 00 00 01 F0 01 F0  |........|\n00 01 00 00 01 F2 01 F2  |........|\n00 01 00 00 01 F3 01 F3  |........|\n00 01 00 00 01 F4 01 F4  |........|\n00 01 00 00 01 F5 01 F5  |........|\n00 01 00 00 01 F6 01 F6  |........|\n00 01 00 00 01 FD 01 FD  |........|\n00 01 00 00 01 FE 01 FE  |........|\n00 01 00 00 02 01 02 01  |........|\n00 01 00 00 02 02 02 02  |........|\n00 01 00 00 02 04 02 04  |........|\n00 01 00 00 02 05 02 05  |........|\n00 01 00 00 02 06 02 06  |........|\n00 01 00 00 02 07 02 07  |........|\n00 01 00 00 02 08 02 08  |........|\n00 01 00 00 02 09 02 09  |........|\n00 01 00 00 02 0A 02 0A  |........|\n00 01 00 00 02 0B 02 0B  |........|\n00 01 00 00 02 0C 02 0C  |........|\n00 01 00 00 02 0D 02 0D  |........|\n00 01 00 00 02 0E 02 0E  |........|\n00 01 00 00 02 0F 02 0F  |........|\n00 01 00 00 02 10 02 10  |........|\n00 01 00 00 02 11 02 11  |........|\n00 01 00 00 02 12 02 12  |........|\n00 01 00 00 02 13 02 13  |........|\n00 01 00 00 02 14 02 14  |........|\n00 01 00 00 02 15 02 15  |........|\n00 01 00 00 02 16 02 16  |........|\n00 01 00 00 02 17 02 17  |........|\n00 01 00 00 02 18 02 18  |........|\n00 01 00 00 02 19 02 19  |........|\n00 01 00 00 02 1A 02 1A  |........|\n00 01 00 00 02 1B 02 1B  |........|\n00 01 00 00 02 1F 02 1F  |........|\n00 01 00 00 02 20 00 00  |........|\n02 21 00 00 02 22 00 00  |.!...\"..|\n02 23 00 00 02 24 00 00  |.#...$..|\n02 25 00 00 02 26 00 00  |.%...&..|\n02 27 00 00 02 28 00 00  |.'...(..|\n02 29 00 00 02 2A 00 00  |.)...*..|\n02 2B 00 00 02 2C 00 00  |.+...,..|\n02 2D 00 00 02 2E 00 00  |.-......|\n02 2F 00 00 02 30 02 30  |./...0.0|\n00 01 00 00 02 31 00 00  |.....1..|\n02 32 00 00 02 33 02 33  |.2...3.3|\n00 01 00 00 02 34 02 34  |.....4.4|\n00 01 00 00 02 36 00 00  |.....6..|\n02 37 00 00 02 38 00 00  |.7...8..|\n02 39 00 00 02 3A 00 00  |.9...:..|\n02 3B 00 00 02 3C 02 3C  |.;...<.<|\n00 01 00 00 02 3D 02 3D  |.....=.=|\n00 01 00 00 02 3E 02 3E  |.....>.>|\n00 01 00 00 02 3F 02 3F  |.....?.?|\n00 01 00 00 02 40 02 40  |.....@.@|\n00 01 00 00 02 41 00 00  |.....A..|\n02 42 02 42 00 01 00 00  |.B.B....|\n02 43 02 43 00 01 00 00  |.C.C....|\n02 44 02 44 00 01 00 00  |.D.D....|\n02 45 02 45 00 01 00 00  |.E.E....|\n02 46 02 46 00 01 00 00  |.F.F....|\n02 47 02 47 00 01 00 00  |.G.G....|\n02 48 02 48 00 01 00 00  |.H.H....|\n02 49 02 49 00 01 00 00  |.I.I....|\n02 4A 00 00 02 4B 00 00  |.J...K..|\n02 4C 00 00 02 4D 00 00  |.L...M..|\n02 4E 02 4E 00 01 00 00  |.N.N....|\n02 4F 02 4F 00 01 00 00  |.O.O....|\n02 50 02 50 00 01 00 00  |.P.P....|\n02 51 02 51 00 01 00 00  |.Q.Q....|\n02 52 02 52 00 01 00 00  |.R.R....|\n02 53 02 53 00 01 00 00  |.S.S....|\n02 54 02 54 00 01 00 00  |.T.T....|\n02 55 02 55 00 01 00 00  |.U.U....|\n02 56 02 56 00 01 00 00  |.V.V....|\n02 57 02 57 00 01 00 00  |.W.W....|\n02 58 02 58 00 01 00 00  |.X.X....|\n02 59 02 59 00 01 00 00  |.Y.Y....|\n02 5A 02 5A 00 01 00 00  |.Z.Z....|\n02 5B 02 5B 00 01 00 00  |.[.[....|\n02 5C 02 5C 00 01 00 00  |.\\.\\....|\n02 5D 02 5D 00 01 00 00  |.].]....|\n02 63 02 63 00 01 00 00  |.c.c....|\n02 64 02 64 00 01 00 00  |.d.d....|\n02 65 02 65 00 01 00 00  |.e.e....|\n02 66 02 66 00 01 00 00  |.f.f....|\n02 67 02 67 00 01 00 00  |.g.g....|\n02 68 02 68 00 01 00 00  |.h.h....|\n02 69 02 69 00 01 00 00  |.i.i....|\n02 6D 00 00 02 6E 02 6E  |.m...n.n|\n00 01 00 00 02 6F 02 6F  |.....o.o|\n00 01 00 00 02 70 02 70  |.....p.p|\n00 01 00 00 02 71 02 71  |.....q.q|\n00 01 00 00 02 72 02 72  |.....r.r|\n00 01 00 00 02 73 02 73  |.....s.s|\n00 01 00 00 02 74 02 74  |.....t.t|\n00 01 00 00 02 75 02 75  |.....u.u|\n00 01 00 00 02 76 02 76  |.....v.v|\n00 01 00 00 02 77 02 77  |.....w.w|\n00 01 00 00 02 78 02 78  |.....x.x|\n00 01 00 00 02 79 00 00  |.....y..|\n02 7A 00 00 02 7B 00 00  |.z...{..|\n02 7C 02 7C 00 01 00 00  |.|.|....|\n02 7D 02 7D 00 01 00 00  |.}.}....|\n02 7E 02 7E 00 01 00 00  |.~.~....|\n02 7F 02 7F 00 01 00 00  |........|\n02 80 02 80 00 01 00 00  |........|\n02 81 00 00 02 82 00 00  |........|\n02 83 00 00 02 84 00 00  |........|\n02 85 00 00 02 86 02 86  |........|\n00 01 00 00 02 87 02 87  |........|\n00 01 00 00 02 88 02 88  |........|\n00 01 00 00 02 89 00 00  |........|\n02 8A 00 00 02 8B 00 00  |........|\n02 8C 02 8C 00 01 00 00  |........|\n02 8D 02 8D 00 01 00 00  |........|\n02 8F 00 00 02 90 02 90  |........|\n00 01 00 00 02 91 00 00  |........|\n02 92 00 00 02 93 00 00  |........|\n02 94 02 94 00 01 00 00  |........|\n02 95 02 95 00 01 00 00  |........|\n02 96 00 00 02 97 02 97  |........|\n00 01 00 00 02 98 00 00  |........|\n02 99 02 99 00 01 00 00  |........|\n02 9A 00 00 02 9B 02 9B  |........|\n00 01 00 00 00 03 00 02  |........|\n00 0A 00 00 00 04 00 02  |........|\n00 0A 00 00 00 05 00 01  |........|\n00 01 00 00 00 06 00 02  |........|\n00 0A 00 00 00 07 00 02  |........|\n00 0A 00 00 00 09 00 01  |........|\n00 01 00 00 00 0D 00 00  |........|\n00 0E 00 00 00 0F 00 17  |........|\n00 01 00 00 00 10 00 00  |........|\n00 11 00 00 00 12 00 00  |........|\n00 13 00 00 00 14 00 00  |........|\n00 15 00 00 00 16 00 00  |........|\n00 27 00 00 00 3A 00 00  |.'...:..|\n00 44 00 02 00 0A 00 00  |.D......|\n00 45 00 00 00 46 00 00  |.E...F..|\n00 4A 00 00 00 4C 00 00  |.J...L..|\n00 5B 00 02 00 0A 00 00  |.[......|\n00 5E 00 01 00 01 00 00  |.^......|\n00 5F 00 17 00 01 00 00  |._......|\n00 60 00 60 00 01 00 00  |.`.`....|\n00 61 00 60 00 01 00 00  |.a.`....|\n00 64 00 64 00 01 00 00  |.d.d....|\n00 65 00 65 00 01 00 00  |.e.e....|\n00 66 00 66 00 01 00 00  |.f.f....|\n00 68 00 00 00 69 00 00  |.h...i..|\n00 6A 00 6A 00 01 00 00  |.j.j....|\n00 6C 00 6D 00 01 00 00  |.l.m....|\n00 6D 00 6D 00 01 00 00  |.m.m....|\n00 6E 00 6F 00 01 00 00  |.n.o....|\n00 6F 00 6F 00 01 00 00  |.o.o....|\n00 70 00 70 00 01 00 00  |.p.p....|\n00 71 00 71 00 01 00 00  |.q.q....|\n00 72 00 72 00 01 00 00  |.r.r....|\n00 73 00 00 00 74 00 66  |.s...t.f|\n00 01 00 00 00 76 00 00  |.....v..|\n00 77 00 77 00 01 00 00  |.w.w....|\n00 79 00 00 00 7A 00 00  |.y...z..|\n00 7B 00 00 00 7F 00 7F  |.{......|\n00 01 00 00 00 88 00 00  |........|\n00 92 00 92 00 01 00 00  |........|\n00 93 00 00 00 98 00 02  |........|\n00 0A 00 00 00 99 00 02  |........|\n00 0A 00 00 00 9A 00 02  |........|\n00 0A 00 00 00 9B 00 01  |........|\n00 01 00 00 00 9C 00 0C  |........|\n00 0A 00 00 00 AC 00 02  |........|\n00 0A 00 00 00 B2 00 B2  |........|\n00 01 00 00 00 B3 00 B3  |........|\n00 01 00 00 00 B4 00 B4  |........|\n00 01 00 00 00 B5 00 B5  |........|\n00 01 00 00 00 B6 00 B6  |........|\n00 01 00 00 00 B7 00 B7  |........|\n00 01 00 00 00 B8 00 0C  |........|\n00 0A 00 00 00 B9 00 00  |........|\n00 BA 00 00 00 BB 00 00  |........|\n00 BC 00 00 00 BD 00 00  |........|\n00 BE 00 00 00 BF 00 00  |........|\n00 C0 00 00 00 C3 00 70  |.......p|\n00 01 00 00 00 C4 00 71  |.......q|\n00 01 00 00 00 C5 00 72  |.......r|\n00 01 00 00 00 C6 00 77  |.......w|\n00 01 00 00 00 C7 00 00  |........|\n00 D0 00 D0 00 01 00 00  |........|\n00 D1 00 00 00 E7 00 E7  |........|\n00 01 00 00 00 E8 00 E7  |........|\n00 01 00 00 00 E9 00 E9  |........|\n00 01 00 00 00 F1 00 6D  |.......m|\n00 01 00 00 00 F5 00 00  |........|\n00 F6 00 00 00 FA 00 00  |........|\n00 FB 00 00 00 FC 00 FC  |........|\n00 01 00 00 02 03 00 00  |........|\n00 00 08 01 06 01 0C 0C  |........|\n41 55 54 48 5F 53 45 53  |AUTH_SES|\n53 4B 45 59 01 40 40 33  |SKEY.@@3|\n43 31 45 33 33 31 30 46  |C1E3310F|\n32 32 30 42 32 44 46 34  |220B2DF4|\n32 30 39 30 37 36 42 33  |209076B3|\n38 46 39 45 43 35 37 45  |8F9EC57E|\n42 32 42 45 31 43 43 38  |B2BE1CC8|\n36 33 38 31 38 42 37 34  |63818B74|\n32 36 35 34 44 39 43 32  |2654D9C2|\n36 31 30 44 36 39 33 00  |610D693.|\n01 0D 0D 41 55 54 48 5F  |...AUTH_|\n56 46 52 5F 44 41 54 41  |VFR_DATA|\n01 20 20 36 41 46 32 34  |...6AF24|\n35 44 45 33 31 37 31 46  |5DE3171F|\n35 34 45 32 33 44 38 38  |54E23D88|\n34 38 38 35 36 38 44 37  |488568D7|\n43 38 45 02 48 15 01 14  |C8E.H...|\n14 41 55 54 48 5F 50 42  |.AUTH_PB|\n4B 44 46 32 5F 43 53 4B  |KDF2_CSK|\n5F 53 41 4C 54 01 20 20  |_SALT...|\n35 46 34 35 38 33 46 32  |5F4583F2|\n35 44 45 32 31 41 33 31  |5DE21A31|\n38 32 44 41 38 32 43 35  |82DA82C5|\n34 42 42 36 32 32 30 31  |4BB62201|\n00 01 16 16 41 55 54 48  |....AUTH|\n5F 50 42 4B 44 46 32 5F  |_PBKDF2_|\n56 47 45 4E 5F 43 4F 55  |VGEN_COU|\n4E 54 01 04 04 34 30 39  |NT...409|\n36 00 01 16 16 41 55 54  |6....AUT|\n48 5F 50 42 4B 44 46 32  |H_PBKDF2|\n5F 53 44 45 52 5F 43 4F  |_SDER_CO|\n55 4E 54 01 01 01 33 00  |UNT...3.|\n01 1A 1A 41 55 54 48 5F  |...AUTH_|\n47 4C 4F 42 41 4C 4C 59  |GLOBALLY|\n5F 55 4E 49 51 55 45 5F  |_UNIQUE_|\n44 42 49 44 00 01 20 20  |DBID....|\n37 42 33 38 39 33 36 34  |7B389364|\n30 45 42 42 33 37 36 38  |0EBB3768|\n43 45 35 43 38 44 37 31  |CE5C8D71|\n30 37 45 34 45 42 44 44  |07E4EBDD|\n00 04 01 01 02 09 66 00  |......f.|\n00 00 00 00 00 00 00 00  |........|\n00 00 00 00 00 00 00 00  |........|\n00 00 01 00 00 00 00 00  |........|\n00 00 00 00 00           |.....   |"
  },
  {
    "timestamp": "2025-04-08 15:08:46.56022",
    "formattedPacket": "00 00 04 DA 06 00 00 00  |........|\n08 00 03 73 02 00 01 01  |...s....|\n06 02 01 01 01 01 0F 01  |........|\n01 53 59 53 54 45 4D 01  |.SYSTEM.|\n0D 0D 41 55 54 48 5F 50  |..AUTH_P|\n41 53 53 57 4F 52 44 01  |ASSWORD.|\n40 40 33 34 43 46 42 34  |@@34CFB4|\n32 33 38 38 32 44 44 44  |23882DDD|\n39 46 43 44 36 31 45 45  |9FCD61EE|\n34 44 35 35 39 46 43 31  |4D559FC1|\n30 37 42 38 35 35 31 38  |07B85518|\n35 31 35 39 39 32 30 33  |51599203|\n37 46 32 32 41 45 44 41  |7F22AEDA|\n32 41 32 37 32 44 31 33  |2A272D13|\n46 32 00 01 16 16 41 55  |F2....AU|\n54 48 5F 50 42 4B 44 46  |TH_PBKDF|\n32 5F 53 50 45 45 44 59  |2_SPEEDY|\n5F 4B 45 59 01 A0 A0 36  |_KEY...6|\n45 35 45 35 44 33 35 41  |E5E5D35A|\n35 30 46 32 33 39 43 45  |50F239CE|\n31 31 39 46 34 31 32 35  |119F4125|\n34 39 31 37 41 36 41 45  |4917A6AE|\n34 43 44 43 34 42 45 38  |4CDC4BE8|\n43 34 33 34 33 43 41 42  |C4343CAB|\n35 45 30 45 36 36 36 38  |5E0E6668|\n46 43 37 36 44 36 31 33  |FC76D613|\n30 46 45 39 35 45 38 31  |0FE95E81|\n30 36 37 46 32 44 34 46  |067F2D4F|\n32 42 32 37 33 46 36 35  |2B273F65|\n35 46 45 30 45 33 33 37  |5FE0E337|\n42 39 43 38 34 30 42 42  |B9C840BB|\n41 36 42 38 34 41 45 36  |A6B84AE6|\n35 36 37 35 30 42 39 46  |56750B9F|\n37 33 33 46 44 45 45 43  |733FDEEC|\n31 39 36 33 35 39 42 33  |196359B3|\n37 37 44 36 32 32 36 41  |77D6226A|\n31 35 33 35 37 37 43 36  |153577C6|\n41 31 42 43 38 30 37 00  |A1BC807.|\n01 0C 0C 41 55 54 48 5F  |...AUTH_|\n53 45 53 53 4B 45 59 01  |SESSKEY.|\n40 40 43 35 32 36 45 39  |@@C526E9|\n38 45 34 37 37 36 45 45  |8E4776EE|\n44 30 34 38 44 37 43 31  |D048D7C1|\n34 41 32 34 31 41 46 30  |4A241AF0|\n33 37 30 43 43 43 30 31  |370CCC01|\n35 32 43 30 35 41 34 30  |52C05A40|\n32 41 35 44 37 36 33 33  |2A5D7633|\n41 45 37 41 36 32 45 38  |AE7A62E8|\n33 34 01 01 01 0D 0D 41  |34.....A|\n55 54 48 5F 54 45 52 4D  |UTH_TERM|\n49 4E 41 4C 01 07 07 75  |INAL...u|\n6E 6B 6E 6F 77 6E 00 01  |nknown..|\n0F 0F 41 55 54 48 5F 50  |..AUTH_P|\n52 4F 47 52 41 4D 5F 4E  |ROGRAM_N|\n4D 01 0E 0E 4A 44 42 43  |M...JDBC|\n53 61 6E 69 74 79 43 6F  |SanityCo|\n6E 6E 00 01 0C 0C 41 55  |nn....AU|\n54 48 5F 4D 41 43 48 49  |TH_MACHI|\n4E 45 01 0B 0B 79 6F 75  |NE...you|\n73 73 65 66 2D 6D 61 63  |ssef-mac|\n00 01 08 08 41 55 54 48  |....AUTH|\n5F 50 49 44 01 05 05 35  |_PID...5|\n37 34 39 30 00 01 12 12  |7490....|\n41 55 54 48 5F 41 4C 54  |AUTH_ALT|\n45 52 5F 53 45 53 53 49  |ER_SESSI|\n4F 4E 01 60 60 41 4C 54  |ON.``ALT|\n45 52 20 53 45 53 53 49  |ER.SESSI|\n4F 4E 20 53 45 54 20 54  |ON.SET.T|\n49 4D 45 5F 5A 4F 4E 45  |IME_ZONE|\n3D 27 41 66 72 69 63 61  |='Africa|\n2F 43 61 73 61 62 6C 61  |/Casabla|\n6E 63 61 27 20 4E 4C 53  |nca'.NLS|\n5F 4C 41 4E 47 55 41 47  |_LANGUAG|\n45 3D 27 41 4D 45 52 49  |E='AMERI|\n43 41 4E 27 20 4E 4C 53  |CAN'.NLS|\n5F 54 45 52 52 49 54 4F  |_TERRITO|\n52 59 3D 27 4D 4F 52 4F  |RY='MORO|\n43 43 4F 27 00 01 01 01  |CCO'....|\n1A 1A 53 45 53 53 49 4F  |..SESSIO|\n4E 5F 43 4C 49 45 4E 54  |N_CLIENT|\n5F 44 52 49 56 45 52 5F  |_DRIVER_|\n4E 41 4D 45 01 17 17 6A  |NAME...j|\n64 62 63 74 68 69 6E 20  |dbcthin.|\n3A 20 32 36 2E 31 2E 30  |:.26.1.0|\n2E 32 34 2E 30 30 00 01  |.24.00..|\n16 16 53 45 53 53 49 4F  |..SESSIO|\n4E 5F 43 4C 49 45 4E 54  |N_CLIENT|\n5F 56 45 52 53 49 4F 4E  |_VERSION|\n01 09 09 34 33 36 32 37  |...43627|\n33 35 33 36 00 01 16 16  |3536....|\n53 45 53 53 49 4F 4E 5F  |SESSION_|\n43 4C 49 45 4E 54 5F 4C  |CLIENT_L|\n4F 42 41 54 54 52 01 01  |OBATTR..|\n01 31 00 01 13 13 41 55  |.1....AU|\n54 48 5F 43 4F 4E 4E 45  |TH_CONNE|\n43 54 5F 53 54 52 49 4E  |CT_STRIN|\n47 01 9C 9C 28 44 45 53  |G...(DES|\n43 52 49 50 54 49 4F 4E  |CRIPTION|\n3D 28 4C 4F 41 44 5F 42  |=(LOAD_B|\n41 4C 41 4E 43 45 3D 4F  |ALANCE=O|\n4E 29 28 41 44 44 52 45  |N)(ADDRE|\n53 53 3D 28 50 52 4F 54  |SS=(PROT|\n4F 43 4F 4C 3D 74 63 70  |OCOL=tcp|\n29 28 48 4F 53 54 3D 31  |)(HOST=1|\n30 30 2E 37 30 2E 37 30  |00.70.70|\n2E 36 39 29 28 50 4F 52  |.69)(POR|\n54 3D 35 35 32 31 29 29  |T=5521))|\n28 43 4F 4E 4E 45 43 54  |(CONNECT|\n5F 44 41 54 41 3D 28 53  |_DATA=(S|\n45 52 56 49 43 45 5F 4E  |ERVICE_N|\n41 4D 45 3D 63 64 62 31  |AME=cdb1|\n5F 70 64 62 31 2E 72 65  |_pdb1.re|\n67 72 65 73 73 2E 72 64  |gress.rd|\n62 6D 73 2E 64 65 76 2E  |bms.dev.|\n75 73 2E 6F 72 61 63 6C  |us.oracl|\n65 2E 63 6F 6D 29 29 29  |e.com)))|\n00 01 0E 0E 41 55 54 48  |....AUTH|\n5F 43 4F 50 59 52 49 47  |_COPYRIG|\n48 54 01 F1 F1 22 4F 72  |HT...\"Or|\n61 63 6C 65 0A 45 76 65  |acle.Eve|\n72 79 62 6F 64 79 20 66  |rybody.f|\n6F 6C 6C 6F 77 73 0A 53  |ollows.S|\n70 65 65 64 79 20 62 69  |peedy.bi|\n74 73 20 65 78 63 68 61  |ts.excha|\n6E 67 65 0A 53 74 61 72  |nge.Star|\n73 20 61 77 61 69 74 20  |s.await.|\n74 6F 20 67 6C 6F 77 22  |to.glow\"|\n0A 54 68 65 20 70 72 65  |.The.pre|\n63 65 64 69 6E 67 20 6B  |ceding.k|\n65 79 20 69 73 20 63 6F  |ey.is.co|\n70 79 72 69 67 68 74 65  |pyrighte|\n64 20 62 79 20 4F 72 61  |d.by.Ora|\n63 6C 65 20 43 6F 72 70  |cle.Corp|\n6F 72 61 74 69 6F 6E 2E  |oration.|\n0A 44 75 70 6C 69 63 61  |.Duplica|\n74 69 6F 6E 20 6F 66 20  |tion.of.|\n74 68 69 73 20 6B 65 79  |this.key|\n20 69 73 20 6E 6F 74 20  |.is.not.|\n61 6C 6C 6F 77 65 64 20  |allowed.|\n77 69 74 68 6F 75 74 20  |without.|\n70 65 72 6D 69 73 73 69  |permissi|\n6F 6E 0A 66 72 6F 6D 20  |on.from.|\n4F 72 61 63 6C 65 20 43  |Oracle.C|\n6F 72 70 6F 72 61 74 69  |orporati|\n6F 6E 2E 20 43 6F 70 79  |on..Copy|\n72 69 67 68 74 20 32 30  |right.20|\n30 33 20 4F 72 61 63 6C  |03.Oracl|\n65 20 43 6F 72 70 6F 72  |e.Corpor|\n61 74 69 6F 6E 2E 00 01  |ation...|\n08 08 41 55 54 48 5F 41  |..AUTH_A|\n43 4C 01 04 04 34 34 30  |CL...440|\n30 00 01 18 18 41 55 54  |0....AUT|\n48 5F 43 4C 49 45 4E 54  |H_CLIENT|\n5F 43 41 50 41 42 49 4C  |_CAPABIL|\n49 54 49 45 53 01 01 01  |ITIES...|\n33 00                    |3.      |"
  },
  {
    "timestamp": "2025-04-08 15:08:46.58252",
    "formattedPacket": "00 00 09 BB 06 00 00 00  |........|\n00 00 08 01 33 01 13 13  |....3...|\n41 55 54 48 5F 56 45 52  |AUTH_VER|\n53 49 4F 4E 5F 53 54 52  |SION_STR|\n49 4E 47 01 0D 0D 2D 20  |ING...-.|\n44 65 76 65 6C 6F 70 6D  |Developm|\n65 6E 74 00 01 10 10 41  |ent....A|\n55 54 48 5F 56 45 52 53  |UTH_VERS|\n49 4F 4E 5F 53 51 4C 01  |ION_SQL.|\n02 02 32 35 00 01 13 13  |..25....|\n41 55 54 48 5F 58 41 43  |AUTH_XAC|\n54 49 4F 4E 5F 54 52 41  |TION_TRA|\n49 54 53 01 01 01 33 00  |ITS...3.|\n01 0F 0F 41 55 54 48 5F  |...AUTH_|\n56 45 52 53 49 4F 4E 5F  |VERSION_|\n4E 4F 01 09 09 34 33 36  |NO...436|\n32 37 33 35 33 36 00 01  |273536..|\n13 13 41 55 54 48 5F 56  |..AUTH_V|\n45 52 53 49 4F 4E 5F 53  |ERSION_S|\n54 41 54 55 53 01 01 01  |TATUS...|\n33 00 01 15 15 41 55 54  |3....AUT|\n48 5F 43 41 50 41 42 49  |H_CAPABI|\n4C 49 54 59 5F 54 41 42  |LITY_TAB|\n4C 45 00 00 01 0F 0F 41  |LE.....A|\n55 54 48 5F 4C 41 53 54  |UTH_LAST|\n5F 4C 4F 47 49 4E 01 1A  |_LOGIN..|\n1A 37 38 37 44 30 34 30  |.787D040|\n38 31 30 30 39 31 44 30  |810091D0|\n30 30 30 30 30 30 30 30  |00000000|\n30 30 30 00 01 0B 0B 41  |000....A|\n55 54 48 5F 44 42 4E 41  |UTH_DBNA|\n4D 45 01 29 29 43 44 42  |ME.))CDB|\n31 5F 50 44 42 31 2E 52  |1_PDB1.R|\n45 47 52 45 53 53 2E 52  |EGRESS.R|\n44 42 4D 53 2E 44 45 56  |DBMS.DEV|\n2E 55 53 2E 4F 52 41 43  |.US.ORAC|\n4C 45 2E 43 4F 4D 00 01  |LE.COM..|\n11 11 41 55 54 48 5F 44  |..AUTH_D|\n42 5F 4D 4F 55 4E 54 5F  |B_MOUNT_|\n49 44 00 01 0A 0A 31 34  |ID....14|\n37 32 38 33 31 30 34 34  |72831044|\n00 01 0B 0B 41 55 54 48  |....AUTH|\n5F 44 42 5F 49 44 00 01  |_DB_ID..|\n09 09 37 31 30 31 35 31  |..710151|\n30 32 33 00 01 0C 0C 41  |023....A|\n55 54 48 5F 55 53 45 52  |UTH_USER|\n5F 49 44 01 01 01 39 00  |_ID...9.|\n01 0F 0F 41 55 54 48 5F  |...AUTH_|\n53 45 53 53 49 4F 4E 5F  |SESSION_|\n49 44 01 02 02 36 31 00  |ID...61.|\n01 0F 0F 41 55 54 48 5F  |...AUTH_|\n53 45 52 49 41 4C 5F 4E  |SERIAL_N|\n55 4D 01 05 05 32 32 33  |UM...223|\n39 33 00 01 10 10 41 55  |93....AU|\n54 48 5F 49 4E 53 54 41  |TH_INSTA|\n4E 43 45 5F 4E 4F 01 01  |NCE_NO..|\n01 31 00 01 10 10 41 55  |.1....AU|\n54 48 5F 46 41 49 4C 4F  |TH_FAILO|\n56 45 52 5F 49 44 01 01  |VER_ID..|\n01 31 00 01 0F 0F 41 55  |.1....AU|\n54 48 5F 53 45 52 56 45  |TH_SERVE|\n52 5F 50 49 44 01 05 05  |R_PID...|\n38 31 36 34 36 00 01 13  |81646...|\n13 41 55 54 48 5F 53 43  |.AUTH_SC|\n5F 53 45 52 56 45 52 5F  |_SERVER_|\n48 4F 53 54 01 0D 0D 70  |HOST...p|\n68 6F 65 6E 69 78 32 35  |hoenix25|\n35 35 37 37 00 01 15 15  |5577....|\n41 55 54 48 5F 53 43 5F  |AUTH_SC_|\n44 42 55 4E 49 51 55 45  |DBUNIQUE|\n5F 4E 41 4D 45 01 03 03  |_NAME...|\n74 78 6E 00 01 15 15 41  |txn....A|\n55 54 48 5F 53 43 5F 49  |UTH_SC_I|\n4E 53 54 41 4E 43 45 5F  |NSTANCE_|\n4E 41 4D 45 01 03 03 74  |NAME...t|\n78 6E 00 01 13 13 41 55  |xn....AU|\n54 48 5F 53 43 5F 49 4E  |TH_SC_IN|\n53 54 41 4E 43 45 5F 49  |STANCE_I|\n44 01 01 01 31 00 01 1B  |D...1...|\n1B 41 55 54 48 5F 53 43  |.AUTH_SC|\n5F 49 4E 53 54 41 4E 43  |_INSTANC|\n45 5F 53 54 41 52 54 5F  |E_START_|\n54 49 4D 45 01 24 24 32  |TIME.$$2|\n30 32 35 2D 30 34 2D 30  |025-04-0|\n37 20 30 39 3A 34 31 3A  |7.09:41:|\n31 34 2E 30 30 30 30 30  |14.00000|\n30 30 30 30 20 2B 30 31  |0000.+01|\n3A 30 30 00 01 11 11 41  |:00....A|\n55 54 48 5F 53 43 5F 44  |UTH_SC_D|\n42 5F 44 4F 4D 41 49 4E  |B_DOMAIN|\n01 1F 1F 72 65 67 72 65  |...regre|\n73 73 2E 72 64 62 6D 73  |ss.rdbms|\n2E 64 65 76 2E 75 73 2E  |.dev.us.|\n6F 72 61 63 6C 65 2E 63  |oracle.c|\n6F 6D 00 01 14 14 41 55  |om....AU|\n54 48 5F 53 43 5F 53 45  |TH_SC_SE|\n52 56 49 43 45 5F 4E 41  |RVICE_NA|\n4D 45 01 29 29 63 64 62  |ME.))cdb|\n31 5F 70 64 62 31 2E 72  |1_pdb1.r|\n65 67 72 65 73 73 2E 72  |egress.r|\n64 62 6D 73 2E 64 65 76  |dbms.dev|\n2E 75 73 2E 6F 72 61 63  |.us.orac|\n6C 65 2E 63 6F 6D 00 01  |le.com..|\n1B 1B 41 55 54 48 5F 4F  |..AUTH_O|\n4E 53 5F 52 4C 42 5F 53  |NS_RLB_S|\n55 42 53 43 52 5F 50 41  |UBSCR_PA|\n54 54 45 52 4E 01 55 55  |TTERN.UU|\n25 22 65 76 65 6E 74 54  |%\"eventT|\n79 70 65 3D 64 61 74 61  |ype=data|\n62 61 73 65 2F 65 76 65  |base/eve|\n6E 74 2F 73 65 72 76 69  |nt/servi|\n63 65 6D 65 74 72 69 63  |cemetric|\n73 2F 63 64 62 31 5F 70  |s/cdb1_p|\n64 62 31 2E 72 65 67 72  |db1.regr|\n65 73 73 2E 72 64 62 6D  |ess.rdbm|\n73 2E 64 65 76 2E 75 73  |s.dev.us|\n2E 6F 72 61 63 6C 65 2E  |.oracle.|\n63 6F 6D 22 00 00 01 1A  |com\"....|\n1A 41 55 54 48 5F 4F 4E  |.AUTH_ON|\n53 5F 48 41 5F 53 55 42  |S_HA_SUB|\n53 43 52 5F 50 41 54 54  |SCR_PATT|\n45 52 4E 01 49 49 28 22  |ERN.II(\"|\n65 76 65 6E 74 54 79 70  |eventTyp|\n65 3D 64 61 74 61 62 61  |e=databa|\n73 65 2F 65 76 65 6E 74  |se/event|\n2F 73 65 72 76 69 63 65  |/service|\n22 29 20 7C 20 28 22 65  |\").|.(\"e|\n76 65 6E 74 54 79 70 65  |ventType|\n3D 64 61 74 61 62 61 73  |=databas|\n65 2F 65 76 65 6E 74 2F  |e/event/|\n68 6F 73 74 22 29 00 00  |host\")..|\n01 1A 1A 41 55 54 48 5F  |...AUTH_|\n53 43 5F 52 45 41 4C 5F  |SC_REAL_|\n44 42 55 4E 49 51 55 45  |DBUNIQUE|\n5F 4E 41 4D 45 01 03 03  |_NAME...|\n74 78 6E 00 01 11 11 41  |txn....A|\n55 54 48 5F 49 4E 53 54  |UTH_INST|\n41 4E 43 45 4E 41 4D 45  |ANCENAME|\n01 03 03 74 78 6E 00 01  |...txn..|\n0F 0F 41 55 54 48 5F 4E  |..AUTH_N|\n4C 53 5F 4C 58 4C 41 4E  |LS_LXLAN|\n00 01 08 08 41 4D 45 52  |....AMER|\n49 43 41 4E 00 01 16 16  |ICAN....|\n41 55 54 48 5F 4E 4C 53  |AUTH_NLS|\n5F 4C 58 43 54 45 52 52  |_LXCTERR|\n49 54 4F 52 59 00 01 07  |ITORY...|\n07 4D 4F 52 4F 43 43 4F  |.MOROCCO|\n00 01 15 15 41 55 54 48  |....AUTH|\n5F 4E 4C 53 5F 4C 58 43  |_NLS_LXC|\n43 55 52 52 45 4E 43 59  |CURRENCY|\n00 01 06 06 D8 AF 2E D9  |........|\n85 2E 00 01 14 14 41 55  |......AU|\n54 48 5F 4E 4C 53 5F 4C  |TH_NLS_L|\n58 43 49 53 4F 43 55 52  |XCISOCUR|\n52 00 01 07 07 4D 4F 52  |R....MOR|\n4F 43 43 4F 00 01 15 15  |OCCO....|\n41 55 54 48 5F 4E 4C 53  |AUTH_NLS|\n5F 4C 58 43 4E 55 4D 45  |_LXCNUME|\n52 49 43 53 00 01 02 02  |RICS....|\n2E 2C 00 01 13 13 41 55  |.,....AU|\n54 48 5F 4E 4C 53 5F 4C  |TH_NLS_L|\n58 43 44 41 54 45 46 4D  |XCDATEFM|\n00 01 08 08 44 44 2D 4D  |....DD-M|\n4D 2D 52 52 00 01 15 15  |M-RR....|\n41 55 54 48 5F 4E 4C 53  |AUTH_NLS|\n5F 4C 58 43 44 41 54 45  |_LXCDATE|\n4C 41 4E 47 00 01 08 08  |LANG....|\n41 4D 45 52 49 43 41 4E  |AMERICAN|\n00 01 11 11 41 55 54 48  |....AUTH|\n5F 4E 4C 53 5F 4C 58 43  |_NLS_LXC|\n53 4F 52 54 00 01 06 06  |SORT....|\n42 49 4E 41 52 59 00 01  |BINARY..|\n15 15 41 55 54 48 5F 4E  |..AUTH_N|\n4C 53 5F 4C 58 43 43 41  |LS_LXCCA|\n4C 45 4E 44 41 52 00 01  |LENDAR..|\n09 09 47 52 45 47 4F 52  |..GREGOR|\n49 41 4E 00 01 15 15 41  |IAN....A|\n55 54 48 5F 4E 4C 53 5F  |UTH_NLS_|\n4C 58 43 55 4E 49 4F 4E  |LXCUNION|\n43 55 52 00 01 06 06 D8  |CUR.....|\nAF 2E D9 85 2E 00 01 13  |........|\n13 41 55 54 48 5F 4E 4C  |.AUTH_NL|\n53 5F 4C 58 43 54 49 4D  |S_LXCTIM|\n45 46 4D 00 01 0D 0D 48  |EFM....H|\n48 32 34 3A 4D 49 3A 53  |H24:MI:S|\n53 58 46 46 00 01 13 13  |SXFF....|\n41 55 54 48 5F 4E 4C 53  |AUTH_NLS|\n5F 4C 58 43 53 54 4D 50  |_LXCSTMP|\n46 4D 00 01 16 16 44 44  |FM....DD|\n2D 4D 4D 2D 52 52 20 48  |-MM-RR.H|\n48 32 34 3A 4D 49 3A 53  |H24:MI:S|\n53 58 46 46 00 01 13 13  |SXFF....|\n41 55 54 48 5F 4E 4C 53  |AUTH_NLS|\n5F 4C 58 43 54 54 5A 4E  |_LXCTTZN|\n46 4D 00 01 11 11 48 48  |FM....HH|\n32 34 3A 4D 49 3A 53 53  |24:MI:SS|\n58 46 46 20 54 5A 52 00  |XFF.TZR.|\n01 13 13 41 55 54 48 5F  |...AUTH_|\n4E 4C 53 5F 4C 58 43 53  |NLS_LXCS|\n54 5A 4E 46 4D 00 01 1A  |TZNFM...|\n1A 44 44 2D 4D 4D 2D 52  |.DD-MM-R|\n52 20 48 48 32 34 3A 4D  |R.HH24:M|\n49 3A 53 53 58 46 46 20  |I:SSXFF.|\n54 5A 52 00 01 18 18 41  |TZR....A|\n55 54 48 5F 4E 4C 53 5F  |UTH_NLS_|\n4C 58 4C 45 4E 53 45 4D  |LXLENSEM|\n41 4E 54 49 43 53 00 01  |ANTICS..|\n04 04 42 59 54 45 00 01  |..BYTE..|\n19 19 41 55 54 48 5F 4E  |..AUTH_N|\n4C 53 5F 4C 58 4E 43 48  |LS_LXNCH|\n41 52 43 4F 4E 56 45 58  |ARCONVEX|\n43 50 00 01 05 05 46 41  |CP....FA|\n4C 53 45 00 01 10 10 41  |LSE....A|\n55 54 48 5F 4E 4C 53 5F  |UTH_NLS_|\n4C 58 43 4F 4D 50 00 01  |LXCOMP..|\n06 06 42 49 4E 41 52 59  |..BINARY|\n00 01 11 11 41 55 54 48  |....AUTH|\n5F 53 56 52 5F 52 45 53  |_SVR_RES|\n50 4F 4E 53 45 01 60 60  |PONSE.``|\n35 44 37 46 31 33 42 41  |5D7F13BA|\n38 43 34 45 38 42 39 46  |8C4E8B9F|\n35 31 44 39 45 30 41 35  |51D9E0A5|\n35 32 39 34 37 33 42 35  |529473B5|\n41 31 30 37 42 34 38 45  |A107B48E|\n34 36 33 39 46 32 41 32  |4639F2A2|\n30 33 30 35 43 44 42 46  |0305CDBF|\n32 37 42 42 36 46 44 31  |27BB6FD1|\n46 38 46 38 42 30 46 31  |F8F8B0F1|\n46 38 38 41 34 42 34 41  |F88A4B4A|\n39 41 39 42 41 36 36 34  |9A9BA664|\n42 44 45 32 34 38 42 36  |BDE248B6|\n00 01 15 15 41 55 54 48  |....AUTH|\n5F 4D 41 58 5F 4F 50 45  |_MAX_OPE|\n4E 5F 43 55 52 53 4F 52  |N_CURSOR|\n53 01 03 03 32 30 30 00  |S...200.|\n01 0D 0D 41 55 54 48 5F  |...AUTH_|\n50 44 42 5F 55 49 44 00  |PDB_UID.|\n01 09 09 37 31 30 31 35  |...71015|\n31 30 32 33 00 01 14 14  |1023....|\n41 55 54 48 5F 4D 41 58  |AUTH_MAX|\n5F 49 44 45 4E 5F 4C 45  |_IDEN_LE|\n4E 47 54 48 01 03 03 31  |NGTH...1|\n32 38 00 01 0A 0A 41 55  |28....AU|\n54 48 5F 46 4C 41 47 53  |TH_FLAGS|\n01 01 01 31 00 01 10 10  |...1....|\n41 55 54 48 5F 53 45 52  |AUTH_SER|\n56 45 52 5F 54 59 50 45  |VER_TYPE|\n01 01 01 31 00 01 18 18  |...1....|\n41 55 54 48 5F 53 45 52  |AUTH_SER|\n56 45 52 5F 43 41 50 41  |VER_CAPA|\n42 49 4C 49 54 49 45 53  |BILITIES|\n01 01 01 31 00 17 05 01  |...1....|\n01 10 01 15 16 00 01 08  |........|\n08 41 4D 45 52 49 43 41  |.AMERICA|\n4E 01 10 00 01 07 07 4D  |N......M|\n4F 52 4F 43 43 4F 01 09  |OROCCO..|\n00 01 06 06 D8 AF 2E D9  |........|\n85 2E 00 00 01 07 07 4D  |.......M|\n4F 52 4F 43 43 4F 01 01  |OROCCO..|\n00 01 02 02 2E 2C 01 02  |.....,..|\n00 01 08 08 41 4C 33 32  |....AL32|\n55 54 46 38 01 0A 00 01  |UTF8....|\n09 09 47 52 45 47 4F 52  |..GREGOR|\n49 41 4E 01 0C 00 01 08  |IAN.....|\n08 44 44 2D 4D 4D 2D 52  |.DD-MM-R|\n52 01 07 00 01 08 08 41  |R......A|\n4D 45 52 49 43 41 4E 01  |MERICAN.|\n08 00 01 06 06 42 49 4E  |.....BIN|\n41 52 59 01 0B 00 01 0D  |ARY.....|\n0D 48 48 32 34 3A 4D 49  |.HH24:MI|\n3A 53 53 58 46 46 01 39  |:SSXFF.9|\n00 01 16 16 44 44 2D 4D  |....DD-M|\n4D 2D 52 52 20 48 48 32  |M-RR.HH2|\n34 3A 4D 49 3A 53 53 58  |4:MI:SSX|\n46 46 01 3A 00 01 11 11  |FF.:....|\n48 48 32 34 3A 4D 49 3A  |HH24:MI:|\n53 53 58 46 46 20 54 5A  |SSXFF.TZ|\n52 01 3B 00 01 1A 1A 44  |R.;....D|\n44 2D 4D 4D 2D 52 52 20  |D-MM-RR.|\n48 48 32 34 3A 4D 49 3A  |HH24:MI:|\n53 53 58 46 46 20 54 5A  |SSXFF.TZ|\n52 01 3C 00 01 06 06 D8  |R.<.....|\nAF 2E D9 85 2E 01 34 00  |......4.|\n01 06 06 42 49 4E 41 52  |...BINAR|\n59 01 32 00 01 04 04 42  |Y.2....B|\n59 54 45 01 3D 00 01 05  |YTE.=...|\n05 46 41 4C 53 45 01 3E  |.FALSE.>|\n00 01 0B 0B 80 00 80 F4  |........|\nB6 3C 3C 80 00 00 00 01  |.<<.....|\nA3 00 01 64 64 00 00 00  |...dd...|\n01 00 00 00 09 00 00 00  |........|\n04 00 00 00 0A 00 00 00  |........|\n43 00 00 00 0B 00 00 00  |C.......|\n44 00 00 00 0C 00 00 00  |D.......|\n0E 00 00 00 0F 00 00 00  |........|\n15 00 00 00 23 00 00 00  |....#...|\n24 00 00 00 32 00 00 00  |$...2...|\n33 00 00 00 3F 00 00 00  |3...?...|\n40 00 00 00 41 00 00 00  |@...A...|\n6A 00 00 00 6B 00 00 00  |j...k...|\n72 00 00 00 7A 00 00 00  |r...z...|\n7D 00 00 00 7F 00 00 00  |}.......|\n21 01 AA 01 1D 1D 22 44  |!.....\"D|\n42 41 22 2C 22 41 51 5F  |BA\",\"AQ_|\n41 44 4D 49 4E 49 53 54  |ADMINIST|\n52 41 54 4F 52 5F 52 4F  |RATOR_RO|\n4C 45 22 00 01 C7 00 04  |LE\".....|\n01 01 02 09 68 00 00 00  |....h...|\n00 00 00 00 00 00 00 00  |........|\n00 00 00 00 00 00 00 00  |........|\n02 00 00 00 00 00 00 00  |........|\n00 00 00                 |...     |"
  },
  {
    "timestamp": "2025-04-08 15:08:47.08361",
    "formattedPacket": "00 00 00 63 06 00 00 00  |...c....|\n08 00 03 5E 03 00 02 81  |...^....|\n21 00 01 01 16 01 01 0D  |!.......|\n00 00 00 00 04 7F FF FF  |........|\nFF 00 00 00 00 00 00 00  |........|\n00 00 00 00 01 00 00 00  |........|\n00 00 00 00 00 00 00 00  |........|\n00 00 00 00 64 72 6F 70  |....drop|\n20 75 73 65 72 20 55 73  |.user.Us|\n65 72 4E 6F 74 46 6F 75  |erNotFou|\n6E 64 01 01 01 01 00 00  |nd......|\n00 00 00 00 00 02 80 00  |........|\n00 00 00                 |...     |"
  }
]
```