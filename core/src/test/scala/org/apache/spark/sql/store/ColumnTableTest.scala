/*
 * Copyright (c) 2016 SnappyData, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License. See accompanying
 * LICENSE file.
 */
package org.apache.spark.sql.store

import java.sql.{DriverManager, SQLException}

import scala.util.{Failure, Success, Try}

import com.gemstone.gemfire.cache.{EvictionAction, EvictionAlgorithm}
import com.gemstone.gemfire.internal.cache.PartitionedRegion
import com.pivotal.gemfirexd.internal.engine.Misc
import com.pivotal.gemfirexd.internal.impl.jdbc.EmbedConnection
import com.pivotal.gemfirexd.internal.impl.sql.compile.ParserImpl
import io.snappydata.core.{Data, TestData, TestData2}
import io.snappydata.{Property, SnappyEmbeddedTableStatsProviderService, SnappyFunSuite}
import org.apache.commons.io.FileUtils
import org.apache.hadoop.hive.ql.parse.ParseDriver
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}

import org.apache.spark.Logging
import org.apache.spark.sql._
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.apache.spark.sql.execution.columnar.impl.ColumnFormatRelation
import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType}

/**
  * Tests for column tables in GFXD.
  */
class ColumnTableTest
    extends SnappyFunSuite
        with Logging
        with BeforeAndAfter
        with BeforeAndAfterAll {

  after {
    snc.dropTable(tableName, ifExists = true)
    snc.dropTable("ROW_TABLE2", ifExists = true)
    snc.dropTable("COLUMN_TEST_TABLE1", ifExists = true)
    snc.dropTable("COLUMN_TEST_TABLE2", ifExists = true)
    snc.dropTable("COLUMN_TEST_TABLE4", ifExists = true)
    snc.dropTable("COLUMN_TEST_TABLE5", ifExists = true)
    snc.dropTable("COLUMN_TEST_TABLE6", ifExists = true)
    snc.dropTable("COLUMN_TEST_TABLE7", ifExists = true)
    snc.dropTable("COLUMN_TEST_TABLE8", ifExists = true)
    snc.dropTable("COLUMN_TEST_TABLE9", ifExists = true)
    snc.dropTable("COLUMN_TEST_TABLE10", ifExists = true)
  }

  val tableName: String = "ColumnTable"

  val props = Map.empty[String, String]


  val options = "OPTIONS (PARTITION_BY 'col1')"

  val optionsWithURL = "OPTIONS (PARTITION_BY 'Col1', URL 'jdbc:snappydata:;')"


  private def checkSetSchema(pattern: String, schemaName: String,
      tableName: String, df: DataFrame, startCount: Int, size: Int): Int = {

    var count = startCount
    var result = snc.sql(s"SELECT * FROM $schemaName.$tableName")
    assert(result.collect().length === count)

    snc.sql(String.format(pattern, schemaName))
    df.write.insertInto(tableName)
    count += size

    result = snc.sql(s"SELECT * FROM $schemaName.$tableName")
    assert(result.collect().length === count)

    result = snc.sql(s"SELECT * FROM $tableName")
    assert(result.collect().length === count)

    snc.sql(String.format(pattern, "app"))
    try {
      df.write.insertInto(tableName)
      fail("expected TableNotFoundException")
    } catch {
      case _: TableNotFoundException => // expected
        assert(result.collect().length === count)
    }
    // check that write using qualified name should work
    df.write.insertInto(s"$schemaName.$tableName")
    count += size
    assert(result.collect().length === count)

    result = snc.sql(s"SELECT 1 FROM $schemaName.$tableName")
    assert(result.count() === count)

    val tempView = s"TABLE_VIEW_$tableName"
    df.createOrReplaceTempView(tempView)

    // check failure with quoted schema but case as passed
    snc.sql("set spark.sql.caseSensitive = true")
    snc.sql(String.format(pattern, "`" + schemaName + "`"))
    try {
      snc.sql(s"insert into $tableName select * from $tempView")
      // TODO: SW: correct case-sensitivity
      // fail("expected TableNotFoundException")
      count += size
    } catch {
      case _: TableNotFoundException => // expected
        assert(result.collect().length === count)
    }
    // check the same with quoted schema with upper case as stored
    snc.sql(String.format(pattern, "`" + schemaName.toUpperCase + "`"))
    snc.sql(s"insert into $tableName select * from $tempView")
    count += size

    result = snc.sql(s"SELECT * FROM `${tableName.toUpperCase}`")
    assert(result.collect().length === count)

    // finally check quoted table too but incorrect case
    try {
      result = snc.sql(s"SELECT * FROM `$tableName`")
      // TODO: SW: fix case-sensitivity
      // fail("expected TableNotFoundException")
    } catch {
      case _: TableNotFoundException => // expected
        assert(result.collect().length === count)
    }

    count
  }

  test("More columns -- SNAP-1345") {
    snc.sql(s"Create Table coltab (a INT) " +
        "using column options()")
    try {
      snc.sql("insert into coltab values (1, 2)")
    } catch {
      case ex: SQLException => assert("42802".equals(ex.getSQLState))
    }
    
    snc.sql("drop table coltab")
  }

  test("Test the creation/dropping of column table using Schema") {
    val data = Seq(Seq(1, 2, 3), Seq(7, 8, 9), Seq(9, 2, 3),
      Seq(4, 2, 3), Seq(5, 6, 7))
    val rdd = sc.parallelize(data, data.length)
        .map(s => Data(s.head, s(1), s(2)))
    val dataDF = snc.createDataFrame(rdd)

    val schema = "test"
    val table = "MY_TABLE"

    snc.sql(s"Drop Table if exists $schema.$table")
    snc.sql(s"Create Table $schema.$table (a INT, b INT, c INT) " +
        "using column options()")

    // try different variant of set schema
    val size = dataDF.count().toInt
    var count = 0

    try {
      // CURRENT SCHEMA = name
      count = checkSetSchema("set current schema = %s", schema, table,
        dataDF, count, size)

      // SCHEMA = name
      count = checkSetSchema("set schema = %s", schema, table,
        dataDF, count, size)

      // CURRENT SCHEMA name
      count = checkSetSchema("set current schema %s", schema, table,
        dataDF, count, size)

      // SCHEMA name
      count = checkSetSchema("set schema %s", schema, table,
        dataDF, count, size)

      snc.sql(s"drop table $table")
    } finally {
      snc.sql("set spark.sql.caseSensitive = false")
      snc.sql("set schema = APP")
    }

    logInfo("Successful")
  }


  test("Test the creation/dropping of table using Snappy API") {
    // shouldn't be able to create without schema
    intercept[AnalysisException] {
      snc.createTable(tableName, "column", props, allowExisting = false)
    }

    val data = Seq(Seq(1, 2, 3), Seq(7, 8, 9), Seq(9, 2, 3), Seq(4, 2, 3),
      Seq(5, 6, 7))
    val rdd = sc.parallelize(data, data.length).map(s => Data(s.head, s(1), s(2)))
    val dataDF = snc.createDataFrame(rdd)

    snc.createTable(tableName, "column", dataDF.schema, props)


    val result = snc.sql("SELECT * FROM " + tableName)
    val r = result.collect
    assert(r.length === 0)
    logInfo("Successful")
  }

  // TODO: Suranjan Test is invalid. Not clear from the bug why the decision
  // to make concurrency checks false was taken
  test("Test SNAP-947") {
    val table = "APP.TEST_TABLE"

    snc.sql (s"drop table if exists $table")

    // check that default concurrency checks is set to false for column table.
    snc.sql(s"create table $table (col1 int) using column" )

    assert (Misc.getRegionForTable(table , true).getAttributes.getConcurrencyChecksEnabled == true)

    snc.dropTable(table)

    // check that default concurrency checks setting is not modified.

    snc.sql(s"create table $table (col1 int) using row options(PERSISTENT 'SYNCHRONOUS')" )

    assert (Misc.getRegionForTable(table , true).getAttributes.getConcurrencyChecksEnabled == true)

    snc.dropTable(table)

    snc.sql(s"create table $table (col1 int) using row " +
        s"options(PERSISTENT 'SYNCHRONOUS' , PARTITION_BY 'COL1')" )

    assert (Misc.getRegionForTable(table , true).getAttributes.getConcurrencyChecksEnabled == true)

    snc.dropTable(table)

  }


  test("Test the creation of table using DataSource API") {

    val data = Seq(Seq(1, 2, 3), Seq(7, 8, 9), Seq(9, 2, 3), Seq(4, 2, 3),
      Seq(5, 6, 7))
    val rdd = sc.parallelize(data, data.length).map(s => Data(s.head, s(1), s(2)))
    val dataDF = snc.createDataFrame(rdd)

    dataDF.write.format("column").mode(SaveMode.Append).options(props)
        .saveAsTable(tableName)

    val result = snc.sql("SELECT * FROM " + tableName)
    val r = result.collect
    assert(r.length === 5)

    // check that table is created with default schema APP
    val result2 = snc.sql(s"SELECT * FROM APP.$tableName")
    assert(result2.collect().length === 5)

    logInfo("Successful")
  }

  test("Test table creation using Snappy API, then append/ignore/overwrite " +
      "DF using DataSource API") {
    var data = Seq(Seq(1, 2, 3), Seq(7, 8, 9), Seq(9, 2, 3), Seq(4, 2, 3),
      Seq(5, 6, 7))
    var rdd = sc.parallelize(data, data.length).map(s => Data(s.head, s(1), s(2)))
    var dataDF = snc.createDataFrame(rdd)

    snc.createTable(tableName, "column", dataDF.schema, props)

    intercept[AnalysisException] {
      dataDF.write.format("column").mode(SaveMode.ErrorIfExists).options(props)
          .saveAsTable(tableName)
    }
    dataDF.write.format("column").mode(SaveMode.Append).options(props)
        .saveAsTable(tableName)

    var result = snc.sql("SELECT * FROM " + tableName)
    var r = result.collect
    assert(r.length === 5)

    // Ignore if table is present
    data = Seq(Seq(100, 200, 300), Seq(700, 800, 900), Seq(900, 200, 300),
      Seq(400, 200, 300), Seq(500, 600, 700), Seq(800, 900, 1000))
    rdd = sc.parallelize(data, data.length).map(s => Data(s.head, s(1), s(2)))
    dataDF = snc.createDataFrame(rdd)
    dataDF.write.mode(SaveMode.Ignore).saveAsTable(tableName)
    result = snc.sql("SELECT * FROM " + tableName)
    r = result.collect
    assert(r.length === 5)

    // Append if table is present
    dataDF.write.insertInto(tableName)
    result = snc.sql("SELECT * FROM " + tableName)
    r = result.collect
    assert(r.length === 11)

    // Overwrite if table is present
    dataDF.write.format("column").options(props).mode(SaveMode.Overwrite)
        .saveAsTable(tableName)
    result = snc.sql("SELECT * FROM " + tableName)
    r = result.collect
    assert(r.length === 6)

    logInfo("Successful")
  }


  test("Test the creation/dropping of table using SQL") {

    snc.sql("CREATE TABLE " + tableName + " (Col1 INT, Col2 INT, Col3 INT) " +
        " USING column " + options)
    val result = snc.sql("SELECT * FROM " + tableName)
    val r = result.collect
    assert(r.length === 0)
    logInfo("Successful")
  }

  test("Test the creation/dropping of table using SQ with explicit URL") {
    //TODO: Suranjan URL misses the hint in connection that gfTx must not be cleared.
    snc.sql("CREATE TABLE " + tableName + " (Col1 INT, Col2 INT, Col3 INT) " +
        " USING column " + optionsWithURL)
    val result = snc.sql("SELECT * FROM " + tableName)
    val r = result.collect
    assert(r.length === 0)
    logInfo("Successful")
  }

  test("Test the creation using SQL and insert a DF in " +
      "append/overwrite/errorifexists mode") {

    snc.sql("CREATE TABLE " + tableName + " (Col1 INT, Col2 INT, Col3 INT) " +
        " USING column " + options)

    val data = Seq(Seq(1, 2, 3), Seq(7, 8, 9), Seq(9, 2, 3), Seq(4, 2, 3),
      Seq(5, 6, 7))
    val rdd = sc.parallelize(data, data.length).map(s => Data(s.head, s(1), s(2)))
    val dataDF = snc.createDataFrame(rdd)

    dataDF.write.format("column").mode(SaveMode.Ignore).options(props)
        .saveAsTable(tableName)

    intercept[AnalysisException] {
      dataDF.write.format("column").mode(SaveMode.ErrorIfExists).options(props)
          .saveAsTable(tableName)
    }

    dataDF.write.insertInto(tableName)

    val result = snc.sql("SELECT * FROM " + tableName)
    val r = result.collect
    assert(r.length === 5)

    // check that default schema is added to the table
    val result2 = snc.sql(s"SELECT * FROM APP.$tableName")
    assert(result2.collect().length === 5)

    logInfo("Successful")
  }

  test("Test the creation of table using SQL and SnappyContext ") {

    snc.sql("CREATE TABLE " + tableName + " (Col1 INT, Col2 INT, Col3 INT) " +
        " USING column " + options)
    val data = Seq(Seq(1, 2, 3), Seq(7, 8, 9), Seq(9, 2, 3), Seq(4, 2, 3),
      Seq(5, 6, 7))
    val rdd = sc.parallelize(data, data.length).map(s => Data(s.head, s(1), s(2)))
    val dataDF = snc.createDataFrame(rdd)

    intercept[AnalysisException] {
      snc.createTable(tableName, "column", dataDF.schema, props)
    }

    dataDF.write.format("column").mode(SaveMode.Append).options(props)
        .saveAsTable(tableName)
    val result = snc.sql("SELECT * FROM " + tableName)
    val r = result.collect
    assert(r.length === 5)
    logInfo("Successful")
  }

  test("Test the creation of table using CREATE TABLE AS STATEMENT ") {
    val data = Seq(Seq(1, 2, 3), Seq(7, 8, 9), Seq(9, 2, 3), Seq(4, 2, 3),
      Seq(5, 6, 7))
    val rdd = sc.parallelize(data, data.length).map(s => Data(s.head, s(1), s(2)))
    val dataDF = snc.createDataFrame(rdd)
    snc.createTable(tableName, "column", dataDF.schema, props)
    dataDF.write.insertInto(tableName)

    val tableName2 = "CoulmnTable2"
    snc.sql("DROP TABLE IF EXISTS CoulmnTable2")
    snc.sql("CREATE TABLE " + tableName2 + " USING column " +
        options + " AS (SELECT * FROM " + tableName + ")"
    )
    var result = snc.sql("SELECT * FROM " + tableName2)
    var r = result.collect
    assert(r.length === 5)

    dataDF.write.insertInto(tableName2)
    result = snc.sql("SELECT * FROM " + tableName2)
    r = result.collect
    assert(r.length === 10)

    snc.dropTable(tableName2)
    logInfo("Successful")
  }

  test("Test alter table SQL not supported for column tables") {
    snc.sql("drop table if exists employee")
    snc.sql("create table employee(name string, surname string) using column options()")
    assert (snc.sql("select * from employee").schema.fields.length == 2)
    intercept[AnalysisException] { // not supported
      snc.sql("alter table employee add column age int")
    }
    assert (snc.sql("select * from employee").schema.fields.length == 2)
    intercept[AnalysisException] { // not supported
      snc.sql("alter table employee drop column surname")
    }
    assert (snc.sql("select * from employee").schema.fields.length == 2)
    intercept[TableNotFoundException] {
      snc.sql("alter table non_employee add column age int")
    }
  }

  test("Test alter table not supported for temp/external tables") {
    val data = Seq(Seq(1, 2, 3), Seq(7, 8, 9), Seq(9, 2, 3), Seq(4, 2, 3), Seq(5, 6, 7))
    val rdd = sc.parallelize(data, data.length).map(s => new Data(s(0), s(1), s(2)))
    val dataDF = snc.createDataFrame(rdd)
    snc.registerDataFrameAsTable(dataDF, "tempTable")
    snc.sql("select * from tempTable").show
    intercept[AnalysisException] { // not supported
      snc.sql("alter table tempTable add column age int")
    }
    snc.dropTempTable("tempTable")

    val schema = StructType(Array(
      StructField("col_int", IntegerType, false),
      StructField("col_string", StringType, false)))
    snc.createExternalTable("extTable", "com.databricks.spark.csv",
      schema, Map.empty[String, String])
    intercept[AnalysisException] { // not supported
      snc.sql("alter table extTable add column age int")
    }
    snc.sql("drop table extTable")
  }

  test("Test alter table API not supported for column tables") {
    val data = Seq(Seq(1, 2, 3), Seq(7, 8, 9), Seq(9, 2, 3), Seq(4, 2, 3), Seq(5, 6, 7))
    val rdd = sc.parallelize(data, data.length).map(s => new Data(s(0), s(1), s(2)))
    val dataDF = snc.createDataFrame(rdd)
    snc.createTable(tableName, "column", dataDF.schema, props)
    dataDF.write.format("column").mode(SaveMode.Append).options(props).saveAsTable(tableName)

    intercept[AnalysisException] {
      snc.alterTable(tableName, true, StructField("col4", IntegerType, true))
    }
    assert(snc.sql("SELECT * FROM " + tableName).schema.fields.length == 3)
    intercept[AnalysisException] {
      snc.alterTable(tableName, false, StructField("col3", IntegerType, true))
    }
    assert(snc.sql("SELECT * FROM " + tableName).schema.fields.length == 3)
  }

  test("Test the truncate syntax SQL and SnappyContext") {
    val data = Seq(Seq(1, 2, 3), Seq(7, 8, 9), Seq(9, 2, 3), Seq(4, 2, 3),
      Seq(5, 6, 7))
    val rdd = sc.parallelize(data, data.length).map(s => Data(s.head, s(1), s(2)))
    val dataDF = snc.createDataFrame(rdd)
    snc.createTable(tableName, "column", dataDF.schema, props)
    dataDF.write.insertInto(tableName)

    snc.truncateTable(tableName)

    var result = snc.sql("SELECT * FROM " + tableName)
    var r = result.collect
    assert(r.length === 0)

    dataDF.write.insertInto(tableName)

    // truncating the table with default schema
    snc.sql("TRUNCATE TABLE " + s"APP.$tableName")

    result = snc.sql("SELECT * FROM " + tableName)
    r = result.collect
    assert(r.length === 0)

    logInfo("Successful")
  }

  test("Test the drop syntax SnappyContext and SQL ") {
    val data = Seq(Seq(1, 2, 3), Seq(7, 8, 9), Seq(9, 2, 3), Seq(4, 2, 3),
      Seq(5, 6, 7))
    val rdd = sc.parallelize(data, data.length).map(s => Data(s.head, s(1), s(2)))
    val dataDF = snc.createDataFrame(rdd)
    snc.createTable(tableName, "column", dataDF.schema, props)
    dataDF.write.insertInto(tableName)

    snc.dropTable(s"APP.$tableName", ifExists = true)

    intercept[AnalysisException] {
      snc.dropTable(tableName, ifExists = false)
    }

    intercept[AnalysisException] {
      snc.sql("DROP TABLE " + tableName)
    }

    snc.sql("DROP TABLE IF EXISTS " + tableName)

    logInfo("Successful")
  }

  test("Test the drop syntax SQL and SnappyContext ") {
    val data = Seq(Seq(1, 2, 3), Seq(7, 8, 9), Seq(9, 2, 3), Seq(4, 2, 3),
      Seq(5, 6, 7))
    val rdd = sc.parallelize(data, data.length).map(s => Data(s.head, s(1), s(2)))
    val dataDF = snc.createDataFrame(rdd)
    snc.createTable(tableName, "column", dataDF.schema, props)
    dataDF.write.insertInto(tableName)

    snc.sql("DROP TABLE IF EXISTS " + tableName)

    intercept[AnalysisException] {
      snc.dropTable(tableName, ifExists = false)
    }

    intercept[AnalysisException] {
      snc.sql("DROP TABLE " + tableName)
    }

    snc.dropTable(tableName, ifExists = true)

    logInfo("Successful")
  }

  test("Test PR with REDUNDANCY") {
    snc.sql("DROP TABLE IF EXISTS COLUMN_TEST_TABLE1")
    snc.sql("CREATE TABLE COLUMN_TEST_TABLE1(OrderId INT ,ItemId INT) " +
        "USING column " +
        "options " +
        "(" +
        "PARTITION_BY 'OrderId'," +
        "REDUNDANCY '2')")

    val region = Misc.getRegionForTable("APP.COLUMN_TEST_TABLE1", true)
        .asInstanceOf[PartitionedRegion]

    val rCopy = region.getPartitionAttributes.getRedundantCopies
    assert(rCopy === 2)
  }

  test("Test PR with buckets") {
    snc.sql("DROP TABLE IF EXISTS COLUMN_TEST_TABLE2")
    snc.sql("CREATE TABLE COLUMN_TEST_TABLE2(OrderId INT ,ItemId INT) " +
        "USING column " +
        "options " +
        "(" +
        "PARTITION_BY 'OrderId'," +
        "BUCKETS '213')")

    val region = Misc.getRegionForTable("APP.COLUMN_TEST_TABLE2", true)
        .asInstanceOf[PartitionedRegion]

    val numPartitions = region.getTotalNumberOfBuckets
    assert(numPartitions === 213)
  }


  test("Test PR with RECOVERDELAY") {
    snc.sql("DROP TABLE IF EXISTS COLUMN_TEST_TABLE4")
    snc.sql("CREATE TABLE COLUMN_TEST_TABLE4(OrderId INT ,ItemId INT) " +
        "USING column " +
        "options " +
        "(" +
        "PARTITION_BY 'OrderId'," +
        "BUCKETS '213'," +
        "RECOVERYDELAY '2')")

    val region = Misc.getRegionForTable("APP.COLUMN_TEST_TABLE4", true)
        .asInstanceOf[PartitionedRegion]

    val rDelay = region.getPartitionAttributes.getRecoveryDelay
    assert(rDelay === 2)
  }

  test("Test PR with MAXPART") {
    snc.sql("DROP TABLE IF EXISTS COLUMN_TEST_TABLE5")
    snc.sql("CREATE TABLE COLUMN_TEST_TABLE5(OrderId INT ,ItemId INT) " +
        "USING column " +
        "options " +
        "(" +
        "PARTITION_BY 'OrderId'," +
        "MAXPARTSIZE '200')")

    val region = Misc.getRegionForTable("APP.COLUMN_TEST_TABLE5", true)
        .asInstanceOf[PartitionedRegion]

    val rMaxMem = region.getPartitionAttributes.getLocalMaxMemory
    assert(rMaxMem === 200)
  }

  test("Test PR with EVICTION BY") {
    val snc = org.apache.spark.sql.SnappyContext(sc)
    snc.sql("DROP TABLE IF EXISTS COLUMN_TEST_TABLE6")
    snc.sql("CREATE TABLE COLUMN_TEST_TABLE6(OrderId INT ,ItemId INT) " +
        "USING column " +
        "options " +
        "(" +
        "PARTITION_BY 'OrderId'," +
        "EVICTION_BY 'LRUMEMSIZE 200')")

    val region = Misc.getRegionForTable("APP.COLUMN_TEST_TABLE6", true)
        .asInstanceOf[PartitionedRegion]
    assert(region.getEvictionAttributes.getAlgorithm ===
        EvictionAlgorithm.LRU_MEMORY)
    assert(region.getEvictionAttributes.getAction ===
        EvictionAction.LOCAL_DESTROY)
    assert(region.getEvictionAttributes.getMaximum === 200)
    snc.sql("DROP TABLE IF EXISTS COLUMN_TEST_TABLE6")
  }

  test("Test PR with EVICTION BY OVERFLOW") {
    val snc = org.apache.spark.sql.SnappyContext(sc)
    snc.sql("DROP TABLE IF EXISTS COLUMN_TEST_TABLE6")
    snc.sql("CREATE TABLE COLUMN_TEST_TABLE6(OrderId INT ,ItemId INT) " +
        "USING column " +
        "options " +
        "(" +
        "PARTITION_BY 'OrderId'," +
        "EVICTION_BY 'LRUMEMSIZE 200'," +
        "OVERFLOW 'true')")

    val region = Misc.getRegionForTable("APP.COLUMN_TEST_TABLE6", true)
        .asInstanceOf[PartitionedRegion]
    assert(region.getEvictionAttributes.getAlgorithm ===
        EvictionAlgorithm.LRU_MEMORY)
    assert(region.getEvictionAttributes.getAction ===
        EvictionAction.OVERFLOW_TO_DISK)
    assert(region.getEvictionAttributes.getMaximum === 200)
    snc.sql("DROP TABLE IF EXISTS COLUMN_TEST_TABLE6")
  }

  test("Test PR with Colocation") {
    val snc = org.apache.spark.sql.SnappyContext(sc)

    snc.sql("CREATE TABLE COLUMN_TEST_TABLE20(OrderId INT ,ItemId INT) " +
        "USING column " +
        "options " +
        "(" +
        "PARTITION_BY 'OrderId'," +
        "EVICTION_BY 'LRUMEMSIZE 200')")

    // snc.sql("DROP TABLE IF EXISTS COLUMN_TEST_TABLE21")
    snc.sql("CREATE TABLE COLUMN_TEST_TABLE21(OrderId INT ,ItemId INT) " +
        "USING column " +
        "options " +
        "(" +
        "PARTITION_BY 'OrderId'," +
        "COLOCATE_WITH 'COLUMN_TEST_TABLE20')")


    val region = Misc.getRegionForTable("APP.COLUMN_TEST_TABLE20", true)
        .asInstanceOf[PartitionedRegion]
    assert(region.colocatedByList.size() == 2)
    snc.sql("DROP TABLE IF EXISTS COLUMN_TEST_TABLE21")
    snc.sql("DROP TABLE IF EXISTS COLUMN_TEST_TABLE20")
  }

  test("Test PR with PERSISTENT") {
    snc.sql("DROP TABLE IF EXISTS COLUMN_TEST_TABLE7")
    snc.sql("CREATE TABLE COLUMN_TEST_TABLE7(OrderId INT ,ItemId INT) " +
        "USING column " +
        "options " +
        "(" +
        "PARTITION_BY 'OrderId'," +
        "PERSISTENT 'ASYNCHRONOUS')")

    val region = Misc.getRegionForTable("APP.COLUMN_TEST_TABLE7", true)
        .asInstanceOf[PartitionedRegion]
    assert(region.getDiskStore != null)
    assert(!region.getAttributes.isDiskSynchronous)
  }

  test("Test RR with PERSISTENT") {
    val snc = org.apache.spark.sql.SnappyContext(sc)
    snc.sql("DROP TABLE IF EXISTS COLUMN_TEST_TABLE8")
    snc.sql("CREATE TABLE COLUMN_TEST_TABLE8(OrderId INT ,ItemId INT) " +
        "USING column " +
        "options " +
        "(" +
        "PERSISTENT 'ASYNCHRONOUS')")

    val region = Misc.getRegionForTable("APP.COLUMN_TEST_TABLE8", true)
        .asInstanceOf[PartitionedRegion]
    assert(region.getDiskStore != null)
    assert(!region.getAttributes.isDiskSynchronous)
  }

  test("Test PR with multiple columns") {
    snc.sql("DROP TABLE IF EXISTS COLUMN_TEST_TABLE9")
    snc.sql("CREATE TABLE COLUMN_TEST_TABLE9(OrderId INT ,ItemId INT, ItemRef INT) " +
        "USING column " +
        "options " +
        "(" +
        "PARTITION_BY 'OrderId, ItemRef'," +
        "PERSISTENT 'ASYNCHRONOUS')")

    val rdd = sc.parallelize(
      (1 to 1000).map(i => TestData2(i, i.toString, i)))

    val dataDF = snc.createDataFrame(rdd)

    dataDF.write.insertInto("COLUMN_TEST_TABLE9")
    val count = snc.sql("select * from COLUMN_TEST_TABLE9").count()
    assert(count === 1000)
  }

  test("Test Non parttitioned tables") {
    val rdd = sc.parallelize(
      (1 to 1000).map(i => TestData(i, i.toString)))

    val dataDF = snc.createDataFrame(rdd)
    snc.sql("DROP TABLE IF EXISTS COLUMN_TEST_TABLE10")

    snc.sql("CREATE TABLE row_table2(OrderId INT ,ItemId INT)" +
        "USING column options()")


    dataDF.write.format("column").mode(SaveMode.Append).options(props)
        .saveAsTable("COLUMN_TEST_TABLE10")

    val count = snc.sql("select * from COLUMN_TEST_TABLE10").count()
    assert(count === 1000)
  }

  test("Test PR Incorrect option") {
    snc.sql("DROP TABLE IF EXISTS COLUMN_TEST_TABLE27")

    Try(snc.sql("CREATE TABLE COLUMN_TEST_TABLE7(OrderId INT ,ItemId INT) " +
        "USING column " +
        "options " +
        "(" +
        "PARTITIONBY 'OrderId'," +
        "PERSISTENT 'ASYNCHRONOUS')")) match {
      case Success(df) => throw new AssertionError(
        "Should not have succedded with incorrect options")
      case Failure(error) => // Do nothing
    }
  }

  test("Test DataSource API  with fully qualified table name") {
    val tableName = "test.table1"
    val data = Seq(Seq(1, 2, 3), Seq(7, 8, 9), Seq(9, 2, 3), Seq(4, 2, 3),
      Seq(5, 6, 7))
    val rdd = sc.parallelize(data, data.length).map(s => Data(s.head, s(1), s(2)))
    val dataDF = snc.createDataFrame(rdd)
    snc.createTable(tableName, "column", dataDF.schema, props)
    dataDF.write.insertInto(tableName)
    assert(snc.sql(s"select * from $tableName").collect().length == 5)
    snc.truncateTable(tableName)
    assert(snc.sql(s"select * from $tableName").collect().length == 0)
    snc.dropTable(tableName)
    logInfo("Successful")
  }

  test("Test SQL API with fully qualified table name") {
    val tableName = "test.table1"
    snc.sql(s"CREATE TABLE $tableName (Col1 INT, Col2 INT, Col3 INT) USING column ")
    assert(snc.sql("SELECT * FROM " + tableName).collect().length == 0)
    snc.sql(s" insert into $tableName values ( 1, 2, 3)")
    snc.sql(s" insert into $tableName values ( 2, 2, 3)")
    snc.sql(s" insert into $tableName values ( 3, 2, 3)")
    assert(snc.sql("SELECT * FROM " + tableName).collect().length == 3)
    snc.sql(s"  truncate table $tableName")
    assert(snc.sql("SELECT * FROM " + tableName).collect().length == 0)
    snc.sql(s"DROP TABLE $tableName")
  }

  test("Test Row buffer eviction with fully qualified table name") {
    testRowBufferEviction("test.testTableWithSchema")
  }

  test("Test Row buffer eviction with table name without schema") {
    testRowBufferEviction("testTableWithoutSchema")
  }

  private def testRowBufferEviction(tableName: String): Unit = {
    val props = Map("BUCKETS" -> "1", "PARTITION_BY" -> "col1")
    val data = Seq(Seq(1, 2, 3), Seq(7, 8, 9), Seq(9, 2, 3), Seq(4, 2, 3),
      Seq(5, 6, 7))
    val rdd = sc.parallelize(data, 1).map(s => Data(s.head, s(1), s(2)))
    val snc = new SnappySession(sc)
    Property.ColumnBatchSize.set(snc.sessionState.conf, "50")
    val dataDF = snc.createDataFrame(rdd)
    snc.createTable(tableName, "column", dataDF.schema, props)
    dataDF.write.insertInto(tableName)
    assert(snc.sql(s"select * from $tableName").collect().length == 5)

    val conn = DriverManager.getConnection("jdbc:snappydata:;query-routing=false")
    val stmt = conn.createStatement()
    var rs = stmt.executeQuery(s"select count (*) from $tableName")
    assert(rs.next())
    // The row buffer should not have more than 3 rows with small batch size
    assert(rs.getInt(1) <= 3)
    assert(!rs.next())
    rs.close()

    // also check with the insert API
    snc.truncateTable(tableName)
    snc.insert(tableName, dataDF.collect(): _*)
    rs = stmt.executeQuery(s"select count (*) from $tableName")
    assert(rs.next())
    assert(rs.getInt(1) <= 3)
    assert(!rs.next())
    rs.close()

    conn.close()
  }

  test("Test PR with EXPIRY") {
    val snc = org.apache.spark.sql.SnappyContext(sc)
    snc.sql("DROP TABLE IF EXISTS COLUMN_TEST_TABLE27")
    Try(snc.sql("CREATE TABLE COLUMN_TEST_TABLE27(" +
        "OrderId INT NOT NULL PRIMARY KEY,ItemId INT) " +
        "USING column " +
        "options " +
        "(" +
        "PARTITION_BY 'OrderId'," +
        "EXPIRE '200')")) match {
      case Success(df) => throw new AssertionError(
        "Should not have succedded with incorrect options")
      case Failure(error) => // Do nothing
    }
  }

  test("compare parser performance") {
    val snc = this.snc
    val sqlText = " select" +
        "         SUPP_NATION," +
        "         CUST_NATION," +
        "         L_YEAR, " +
        "         sum(VOLUME) as REVENUE" +
        " from (" +
        "         select" +
        "                 N1.N_NAME as SUPP_NATION," +
        "                 N2.N_NAME as CUST_NATION," +
        //        "                 extract m(year from l_shipdate) as l_year," +
        "                 year(L_SHIPDATE) as L_YEAR," +
        "                 L_EXTENDEDPRICE * (1 - L_DISCOUNT) as VOLUME" +
        "         from" +
        "                 SUPPLIER," +
        "                 LINEITEM," +
        "                 ORDERS," +
        "                 CUSTOMER," +
        "                 NATION N1," +
        "                 NATION N2" +
        "         where" +
        "                 S_SUPPKEY = L_SUPPKEY" +
        "                 and O_ORDERKEY = L_ORDERKEY" +
        "                 and C_CUSTKEY = O_CUSTKEY" +
        "                 and S_NATIONKEY = N1.N_NATIONKEY" +
        "                 and C_NATIONKEY = N2.N_NATIONKEY" +
        "                 and (" +
        "                         (trim(upper(N1.N_NAME)) = 'FRANCE' and " +
        "                          trim(upper(N2.N_NAME)) = 'GERMANY')" +
        "                      or (trim(upper(N1.N_NAME)) = 'GERMANY' and " +
        "                          trim(upper(N2.N_NAME)) = 'FRANCE')" +
        "                 )" +
        "                 and L_SHIPDATE between '1995-01-01' and '1996-12-31'" +
        "         ) as SHIPPING" +
        " group by" +
        "         SUPP_NATION," +
        "         CUST_NATION," +
        "         L_YEAR" +
        " order by" +
        "         SUPP_NATION," +
        "         CUST_NATION," +
        "         L_YEAR"

    // warmup runs
    var plan: LogicalPlan = null
    val conn = DriverManager.getConnection("jdbc:snappydata:")
        .asInstanceOf[EmbedConnection]
    conn.setupContextStack(true)
    val cc = conn.getLanguageConnection.pushCompilerContext()
    // scalastyle:off println
    try {

      val pi = new ParserImpl(cc)
      val pd = new ParseDriver

      // timed runs for the parsers
      var start: Double = 0.0
      var end: Double = 0.0
      var elapsed: Double = 0.0
      val warmupRuns = 20000
      val timedRuns = 5000

      println()
      println(s"===============  Comparing $timedRuns runs  ===============")
      println()

      println(s"Warmup runs for Snappy parser ...")
      val sqlParser = snc.sessionState.sqlParser
      for (i <- 0 until warmupRuns) {
        plan = sqlParser.parsePlan(sqlText)
      }
      println(s"Done with warmup runs")
      start = System.nanoTime()
      for (i <- 0 until timedRuns) {
        plan = sqlParser.parsePlan(sqlText)
      }
      end = System.nanoTime()
      elapsed = (end - start) / 1000000.0
      println(s"Time taken by Snappy parser = ${elapsed}ms " +
          s"average=${elapsed / timedRuns}ms")
      println()

      println(s"Warmup runs for GemXD parser ...")
      for (i <- 0 until warmupRuns) {
        pi.parseStatement(sqlText)
      }
      println(s"Done with warmup runs")
      start = System.nanoTime()
      for (i <- 0 until timedRuns) {
        pi.parseStatement(sqlText)
      }
      end = System.nanoTime()
      elapsed = (end - start) / 1000000.0
      println(s"Time taken by GemXD parser = ${elapsed}ms " +
          s"average=${elapsed / timedRuns}ms")
      println()

      println(s"Warmup runs for Spark parser ...")
      val sparkSession = new SparkSession(sc)
      val sparkParser = sparkSession.sessionState.sqlParser
      for (i <- 0 until warmupRuns) {
        plan = sparkParser.parsePlan(sqlText)
      }
      println(s"Done with warmup runs")
      start = System.nanoTime()
      for (i <- 0 until timedRuns) {
        plan = sparkParser.parsePlan(sqlText)
      }
      end = System.nanoTime()
      elapsed = (end - start) / 1000000.0
      println(s"Time taken by Spark parser = ${elapsed}ms " +
          s"average=${elapsed / timedRuns}ms")
      println()

      println(s"Warmup runs for Hive parser ...")
      for (i <- 0 until warmupRuns) {
        pd.parse(sqlText)
      }
      println(s"Done with warmup runs")
      start = System.nanoTime()
      for (i <- 0 until timedRuns) {
        pd.parse(sqlText)
      }
      end = System.nanoTime()
      elapsed = (end - start) / 1000000.0
      println(s"Time taken by Hive parser = ${elapsed}ms " +
          s"average=${elapsed / timedRuns}ms")

    } finally {
      conn.getLanguageConnection.popCompilerContext(cc)
      conn.restoreContextStack()
      conn.close()
    }
    // scalastyle:on println
  }

  test("Check columnBatch num rows") {
    val data = (1 to 200) map (i => Seq(i, +i, +i))
    val snc = new SnappySession(sc)
    Property.ColumnBatchSize.set(snc.sessionState.conf, "100")
    val rdd = sc.parallelize(data, data.length).map(s => Data(s.head, s(1), s(2)))
    val dataDF = snc.createDataFrame(rdd)

    val parDF = dataDF.repartition(1)

    snc.sql("CREATE TABLE " + tableName + " (Col1 INT, Col2 INT, Col3 INT) " +
        " USING column")
    parDF.write.insertInto(tableName)

    val result = snc.sql("SELECT * FROM " + tableName)

    val r = result.collect()
    assert(r.length === 200)

    val rowBuffer = Misc.getRegionForTable(("APP." + tableName).toUpperCase, true)
    val rowBufferCount = rowBuffer.asInstanceOf[PartitionedRegion].getPrStats
        .getDataStoreEntryCount

    val region = Misc.getRegionForTable("APP." +
      ColumnFormatRelation.columnBatchTableName(tableName).toUpperCase, true)
    SnappyEmbeddedTableStatsProviderService.publishColumnTableRowCountStats()
    val entries = region.asInstanceOf[PartitionedRegion].getPrStats
        .getPRNumRowsInColumnBatches

    assert(entries > 180)
    assert(rowBufferCount !== 0)
    assert(entries + rowBufferCount === 200)
    logInfo("Successful")
  }

  test("Test Dropping Colocated column table") {


    snc.sql("create table ORDER_DETAILS_COL(SINGLE_ORDER_DID BIGINT ," +
        "SYS_ORDER_ID VARCHAR(64)" +
        " ,SYS_ORDER_VER INTEGER ," +
        "DATA_SNDG_SYS_NM VARCHAR(128)) " +
        "USING column OPTIONS(BUCKETS '13', " +
        "REDUNDANCY '1', EVICTION_BY 'LRUHEAPPERCENT'," +
        " PERSISTENT 'ASYNCHRONOUS')");

    snc.sql("create table EXEC_DETAILS_COL(EXEC_DID BIGINT," +
        "SYS_EXEC_VER INTEGER,SYS_EXEC_ID VARCHAR(64)," +
        "TRD_DATE VARCHAR(20),ALT_EXEC_ID VARCHAR(64)) " +
        "USING column OPTIONS(COLOCATE_WITH 'ORDER_DETAILS_COL', " +
        "BUCKETS '13', REDUNDANCY '1', " +
        "EVICTION_BY 'LRUHEAPPERCENT', PERSISTENT 'ASYNCHRONOUS')");

    try {
      snc.sql("DROP TABLE ORDER_DETAILS_COL");
    } catch {
      case e: AnalysisException => {
        assert(e.getMessage() === "Object APP.ORDER_DETAILS_COL cannot be dropped because of " +
            "dependent objects: APP.EXEC_DETAILS_COL;")
        // Execute second time to see we are getting same exception instead of table not found
        try {
          snc.sql("DROP TABLE ORDER_DETAILS_COL");
        } catch {
          case e: AnalysisException => {
            assert(e.getMessage() === "Object APP.ORDER_DETAILS_COL cannot be dropped because of " +
                "dependent objects: APP.EXEC_DETAILS_COL;")
          }
          case t: Throwable => throw new AssertionError(t.getMessage, t);
        }
      } // Expected Exception hence ignore
      case _: Throwable => throw new AssertionError;
    }

    try {
      snc.sql("DROP TABLE EXEC_DETAILS_COL")
      snc.sql("DROP TABLE ORDER_DETAILS_COL");
    } catch {
      case t: Throwable => throw new AssertionError(t.getMessage, t);
    }
  }

  test("Creation of table using other table and verify schema") {

    snc.sql("create table t1(a int,b int) using column options()")
    snc.sql("insert into t1 values(1,2)")
    snc.sql("select * from t1").show
    snc.sql("create table t2(c int,d int) using column options() as (select * from t1)")

    snc.sql("create table t3 using column options() as (select * from t1)")

    val struct = (new StructType())
      .add(StructField("C", IntegerType, true))
      .add(StructField("D", IntegerType, true))


    val df1=snc.sql("select * from t1")
    val df2=snc.sql("select * from t2")
    val df3=snc.sql("select * from t3")

    assert(struct == df2.schema)
    assert(df1.schema == df3.schema)

  }

  test("Test create table from CSV without header") {
    snc.sql(s"create table test1 using com.databricks.spark.csv options(path '${(getClass.getResource
    ("/northwind/orders" +
      ".csv").getPath)}', header 'false', inferschema 'true')")
    snc.sql("create table test2 using column options() as (select * from test1)")
    val df2 = snc.sql("select * from test2")
    df2.show()

    snc.sql("drop table test2")
    snc.sql("create table test2(_col1 integer,__col2 integer) using column options()")
    snc.sql("insert into test2 values(1,2)")
    snc.sql("insert into test2 values(2,3)")
    val df3 = snc.sql("select _col1,__col2 from test2")
    df3.show()
    val struct = (new StructType())
      .add(StructField("_COL1", IntegerType, true))
      .add(StructField("__COL2", IntegerType, true))

    df3.printSchema()
    assert(struct == df3.schema)

  }

  test("Test loading json data to column table") {
    val some_people_path = s"${(getClass.getResource("/person.json").getPath)}"
    println(some_people_path)
    // Read a JSON file using Spark API
    val people = snc.read.json(some_people_path)

    //Drop the table if it exists.
    snc.dropTable("people", ifExists = true)

    //Create a columnar table with the Json DataFrame schema
    snc.createTable(tableName = "people",
      provider = "column",
      schema = people.schema,
      options = Map.empty[String,String],
      allowExisting = false)

    // Write the created DataFrame to the columnar table.
    people.write.insertInto("people")


    val nameAndAddress = snc.sql("SELECT " +
      "name, " +
      "address.city, " +
      "address.state, " +
      "address.district, " +
      "address.lane " +
      "FROM people")
    nameAndAddress.toJSON.show(truncate = false)
    assert(nameAndAddress.count() ==2)
    val rows:Array[String] = nameAndAddress.toJSON.collect()

    assert(rows(0) =="{\"NAME\":\"Yin\",\"CITY\":\"Columbus\",\"STATE\":\"Ohio\",\"DISTRICT\":\"Pune\"}")
    assert(rows(1) == "{\"NAME\":\"Michael\",\"STATE\":\"California\",\"LANE\":\"15\"}")

  }

  test("Test for SNAP-1878 create external table using api") {

    snc.sql("drop table if exists t1")
    snc.sql(s"create table t1 (c1 integer,c2 string)")
    snc.sql(s"insert into t1 values(1,'test1')")
    snc.sql(s"insert into t1 values(2,'test2')")
    snc.sql(s"insert into t1 values(3,'test3')")
    val df = snc.sql("select * from t1")
    df.show
    val tempPath = "/tmp/" + System.currentTimeMillis()

    assert(df.count() == 3)
    df.write.option("header", "true").csv(tempPath)
    snc.createExternalTable("TEST_EXTERNAL", "csv",
      Map("path" -> tempPath, "header" -> "true", "inferSchema"-> "true"))
    val dataDF = snc.sql("select * from TEST_EXTERNAL order by c1")

    snc.sql("select * from TEST_EXTERNAL").show

    assert(dataDF.count == 3)

    val rows=dataDF.collect()

    for(i<- 0 to 2) assert(rows(i)(0)==i+1)

    snc.sql("drop table if exists TEST_EXTERNAL")
    snc.sql("drop table if exists t1")
    FileUtils.deleteDirectory(new java.io.File(tempPath))
  }

  test("Creating column table from dataframe with complex datatype ") {

    snc.sql("drop table if exists test")

    val rawData=Seq(Seq(1,"emp1",1),Seq(2,"emp2",2),Seq(3,"emp3",3))

    val rdd = sc.parallelize(rawData,1).map(s=> Record(s(0).asInstanceOf[Int],Employee(s(1)
      .toString,s(2).asInstanceOf[Int])))
    val df = snc.createDataFrame(rdd)
    df.write.format("column").saveAsTable("test")

    snc.sql("drop table if exists test")
  }
}
case class Record(id:Int, data: Employee)
case class Employee(empName: String, empId: Int)
