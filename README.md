# Oracle JDBC Log Analyzer

This log analyzer tool is used to parse and extract meaningful information from JDBC/UCP and RDBMS/SQLNet log files. It allows clients to access, query, and analyze log data stored in a log file.

Key Features of JDBC/UCP log parsing:
- Error Retrieval: Access raw or filtered error log entries based on criteria such as tenant or connection id.
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
import oracle.jdbc.logs.model.JDBCStats;

import java.io.IOException;

public class MainClass {

  public static void main(String[] args) throws IOException {
    final String logFilePath = "/Users/youssef/Desktop/logs/ojdbc17-test1.log";
    final JDBCLog jdbcLogParser = new JDBCLog(logFilePath);

    System.out.println("===== Extract Stats ======");
    final JDBCStats stats = jdbcLogParser.getStats();
    System.out.println(stats.toString());

    if (stats.errorCount() > 0) {
      System.out.println("\n===== Extract Errors ======");
      jdbcLogParser.getLogErrors()
        .forEach(System.out::println);
    }

    if (stats.queryCount() > 0) {
      System.out.println("\n===== Extract Executed Queries ======");
      jdbcLogParser.getQueries()
        .forEach(System.out::println);
    }

    if (stats.openedConnectionCount() > 0 ) {
      System.out.println("\n===== Extract Connection Events ======");
      jdbcLogParser.getConnectionEvents()
        .forEach(System.out::println);
    }

    final String logFilePath2 = "/Users/youssef/Desktop/logs/ojdbc17-test2.log";
    System.out.println("\n===== Comparison between 'ojdbc17-test1' and 'ojdbc17-test2' ======");
    System.out.println(jdbcLogParser.compareTo(logFilePath2));
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
{
    "logEntry": {
      "logFile": "ojdbc17-test1.log",
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
    "sqlexecutionTime": 16,
    "nearestTrace": {
      "timestamp": "2024-06-20T22:27:12",
      "executedMethod": "oracle.jdbc.driver.OracleStatement execute"
    },
    "connectionId": "0B9IF/DmROGJx/k2EsbJnw=="
  }
```

- Extract Executed Queries:

```json
[
  {
    "timestamp": "2024-06-20T22:27:12",
    "sql": "drop directory TEST_DIR",
    "executionTime": "18ms"
  },
  {
    "timestamp": "2024-06-20T22:27:12",
    "sql": "drop user tkpjb35428646 cascade",
    "executionTime": "16ms"
  },
  {
    "timestamp": "2024-06-20T22:27:12",
    "sql": "create user tkpjb35428646 identified by tkpjb35428646 default tablespace system quota unlimited on system",
    "executionTime": "28ms"
  },
  {
    "timestamp": "2024-06-20T22:27:12",
    "sql": "grant connect, resource, unlimited tablespace,ALTER SESSION to tkpjb35428646",
    "executionTime": "9ms"
  },
  {
    "timestamp": "2024-06-20T22:27:12",
    "sql": "grant create view, create session, create synonym,create type, create sequence, CREATE TABLE,create procedure, select any table to tkpjb35428646",
    "executionTime": "9ms"
  },
  {
    "timestamp": "2024-06-20T22:27:12",
    "sql": "grant create database link to tkpjb35428646",
    "executionTime": "7ms"
  },
  {
    "timestamp": "2024-06-20T22:27:12",
    "sql": "grant CREATE CLUSTER, CREATE OPERATOR, CREATE TRIGGER, CREATE INDEXTYPE to tkpjb35428646",
    "executionTime": "8ms"
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
    "details": null
  },
  {
    "timestamp": "2024-06-20T22:27:12",
    "event": "CONNECTION_CLOSED",
    "details": null
  },
  {
    "timestamp": "2024-06-20T22:27:12",
    "event": "CONNECTION_CLOSED",
    "details": null
  }
]
```

Comparison between two OJDBC log files:

```json
{
  "summary": {
    "referenceLogFileName": "ojdbc17-test2.log",
    "currentLogFileName": "ojdbc17-test1.log",
    "referenceLogFileSize": "578.626 MB",
    "currentLogFileSize": "1.363 MB",
    "referenceLogFileLineCount": 5643918,
    "currentLogFileLineCount": 25795,
    "lineCountDelta": "-99.54%",
    "referenceLogFileTimespan": "2024-10-21T00:32:39.117Z to 2024-10-21T00:34:41.424Z",
    "referenceLogFileDuration": "PT2M2.307S",
    "currentLogFileTimespan": "2024-06-20T22:27:11 to 2024-06-20T22:28:14",
    "currentLogFileDuration": "PT1M3S"
  },
  "performance": {
    "referenceQueryCount": 16572,
    "currentQueryCount": 17,
    "queryCountDelta": "-99.90%",
    "referenceAverageQueryTime": "24.25 ms",
    "currentAverageQueryTime": "19.71 ms",
    "averageQueryTimeDelta": "-18.72%"
  },
  "error": {
    "referenceErrorCount": 10,
    "currentErrorCount": 14,
    "totalErrorsDelta": "+40.00%"
  },
  "network": {
    "referenceBytesConsumed": "18.770 MB",
    "currentBytesConsumed": "55.197 kB",
    "bytesConsumedDelta": "-99.71%",
    "referenceBytesProduced": "2.387 MB",
    "currentBytesProduced": "3.286 MB",
    "bytesProducedDelta": "+37.66%"
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

public class MainClass {

  public static void main(String[] args) throws IOException {
    final String logFilePath = "/Users/youssef/Desktop/logs/rdbms/txn_ora_81646.trc.sav";
    final RDBMSLog rdbmsLogParser = new RDBMSLog(logFilePath);

    System.out.println("===== Extract ORA Errors ======");
    final List<RDBMSError> errors = rdbmsLogParser.getErrors();

    if (errors.isEmpty()) {
      System.out.println("No errors found!");
    } else {
      errors.forEach(System.out::println);
    }


    final String connectionId = "BZyObJFSTCyZV9xp+bad4Q==";
    System.out.println("\n===== Extract PacketDumps of {"+connectionId+"} ======");
    final List<RDBMSPacketDump> packetDumps = rdbmsLogParser.getPacketDumps(connectionId);

    if (packetDumps.isEmpty()) {
      System.out.println("No packet dumps found for: " + connectionId);
    } else {
      packetDumps.forEach(System.out::println);
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
    "documentationLink": "https://docs.oracle.com/en/error-help/db/ORA-07445/?r=23ai"
  },
  {
    "errorMessage": "ORA-01918: user 'USERNOTFOUND' does not exist",
    "documentationLink": "https://docs.oracle.com/en/error-help/db/ORA-01918/?r=23ai"
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
  }
]
```