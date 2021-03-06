# Configuring Logging

By default, log files for SnappyData members are created inside the working directory of the member. To change the log file directory, you can specify a property `-log-file` as the path of the directory, while starting a member.

SnappyData uses [log4j 2](http://logging.apache.org/log4j/) for logging.
You can configure logging by copying the existing template file **log4j2.properties.template** to the **conf** directory and renaming it to **log4j2.properties**.

For example, the following can be added to the **log4j2.properties** file to change the logging level of the classes of Spark scheduler.

```pre
$ cat conf/log4j2.properties
logger.scheduler.name = org.apache.spark.scheduler.TaskSchedulerImpl
logger.scheduler.level = debug
```

The default template uses a layout that includes both the thread name and ID.

For example, the default pattern:

```pre
appender.rolling.layout.pattern = %d{yy/MM/dd HH:mm:ss.SSS zzz} %t<tid=%T> %p %c{1}: %m%n
```

produces

```pre
17/11/07 16:42:05.115 IST serverConnector<tid=9> INFO snappystore: GemFire P2P Listener started on  tcp:///192.168.1.6:53116
```

This is the recommended pattern to use for SnappyData logging.

## Setting Log Level at Runtime

The inbuilt procedure `set_log_level` can be used to set the log level of SnappyData classes at runtime. You must execute the procedure as a system user.

Following is the usage of the procedure:
```pre
call sys.set_log_level (loggerName, logLevel);
```

The `logLevel` can be a log4j level, that is, ALL, DEBUG, INFO, WARN, ERROR, FATAL, OFF, TRACE. `loggerName` can be a class name or a package name. If it is left empty, the root logger's level is set.

For example:
```pre
// sets the root logger's level as WARN
snappy> call sys.set_log_level ('', 'WARN' );

// sets the WholeStageCodegenExec class level as DEBUG
snappy> call sys.set_log_level ('org.apache.spark.sql.execution.WholeStageCodegenExec', 'DEBUG');

// sets the apache spark package's log level as INFO
snappy> call sys.set_log_level ('org.apache.spark', 'INFO');
```

## SnappyData Store logging

The fine-grained log settings are applicable for classes other than the SnappyData store classes. SnappyData store does not honor fine-grained log settings. That is, you can only set the log level for the root category. However, log level of specific features of SnappyData store can be controlled both during the start and during runtime.

## Using Trace Flags for Advanced Logging For SnappyData Store

<a id="trace-flag"></a>
SnappyData Store provides the following trace flags that you can use with the `gemfirexd.debug.true` system property to log additional details about specific features:

| Trace flag                 | Enables        |
|----------------------------|-----------------------------------------------------|
| QueryDistribution          | Detailed logging for distributed queries and DML statements, including information about message distribution to SnappyData members and scan types that were opened. |
| StatementMatching          | Logging for optimizations that are related to unprepared statements.             |
| TraceAuthentication        | Additional logging for authentication.|
| TraceDBSynchronizer        | DBSynchronizer and WAN distribution logging.       |
| TraceClientConn            | Client-side connection open and close stack traces.                      |
| TraceClientStatement       | Client-side, basic timing logging.			|
| TraceClientStatementMillis | Client-side wall clock timing.                            |
| TraceIndex                 | Detailed index logging.|
| TraceJars                  | Logging for JAR installation, replace, and remove events.|
| TraceTran                  | Detailed logging for transaction events and operations, including commit and rollback.                                                                               |
| TraceLock\_\*              | Locking and unlocking information for all internal locks.|
| TraceLock\_DD              | Logging for all DataDictionary and table locks that are acquired or released.|

To enable logging of specific features of SnappyData, set the required trace flag in the `gemfirexd.debug.true` system property. For example, you can add the following setting inside the configuration file of the SnappyData member to enable logging for query distribution and indexing:

```pre
localhost -J-Dgemfirexd.debug.true=QueryDistribution,TraceIndex
```

If you need to set a trace flag in a running system, use the [SYS.SET_TRACE_FLAG](../reference/inbuilt_system_procedures/set-trace-flag.md) system procedure. The procedure sets the trace flag in all members of the distributed system, including locators. You must execute the procedure as a system user. For example:

```pre
snappy> call sys.set_trace_flag('traceindex', 'true');
Statement executed.
```
!!! Note
	Trace flags work only for `snappy` and `jdbc` and not for `snappy-sql`.
