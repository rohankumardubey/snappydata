/*
 * Copyright (c) 2017 SnappyData, Inc. All rights reserved.
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

import java.util.concurrent.{CyclicBarrier, Executors}

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

import com.gemstone.gemfire.internal.cache.{GemFireCacheImpl, PartitionedRegion}
import com.pivotal.gemfirexd.TestUtil
import com.pivotal.gemfirexd.internal.engine.Misc
import com.pivotal.gemfirexd.internal.engine.store.GemFireContainer
import io.snappydata.cluster.PreparedQueryRoutingSingleNodeSuite
import io.snappydata.{ColumnUpdateDeleteTests, Property}
import io.snappydata.test.dunit.{DistributedTestBase, SerializableRunnable}

import org.apache.spark.{Logging, SparkConf}
import org.apache.spark.memory.SnappyUnifiedMemoryManager
import org.apache.spark.sql.{Row, SnappySession}

/**
 * Tests for column table having sorted columns.
 */
class SortedColumnTests extends ColumnTablesTestBase {

  override def beforeAll(): Unit = {
    super.beforeAll()
    stopAll()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    stopAll()
  }

  override protected def newSparkConf(addOn: (SparkConf) => SparkConf): SparkConf = {
    val conf = new SparkConf()
    conf.setIfMissing("spark.master", "local[*]")
        .setAppName(getClass.getName)
    conf.set("snappydata.store.critical-heap-percentage", "95")
    if (SnappySession.isEnterpriseEdition) {
      conf.set("snappydata.store.memory-size", "1200m")
    }
    conf.set("spark.memory.manager", classOf[SnappyUnifiedMemoryManager].getName)
    conf.set("spark.serializer", "org.apache.spark.serializer.PooledKryoSerializer")
    conf.set("spark.closure.serializer", "org.apache.spark.serializer.PooledKryoSerializer")
    conf
  }

  test("basic insert") {
    SortedColumnTests.testBasicInsert(this.snc.snappySession)
  }

  ignore("basic update") {
    SortedColumnTests.testBasicUpdate(this.snc.snappySession)
  }

  ignore("basic delete") {
    SortedColumnTests.testBasicDelete(this.snc.snappySession)
  }

  ignore("SNAP-1925") {
    SortedColumnTests.testSNAP1925(this.snc.snappySession)
  }

  ignore("SNAP-1926") {
    SortedColumnTests.testSNAP1926(this.snc.snappySession)
  }

  ignore("concurrent ops") {
    SortedColumnTests.testConcurrentOps(this.snc.snappySession)
  }

  ignore("test update for all types") {
    val session = this.snc.snappySession
    // reduced size to ensure both column table and row buffer have data
    session.conf.set(Property.ColumnBatchSize.name, "100k")
    runAllTypesTest(session)
    session.conf.unset(Property.ColumnBatchSize.name)
  }

  ignore("SNAP-1985: update delete on string type") {
    val tableName1 = "order_line_1_col_str"
    val tableName2 = "order_line_2_ud_str"

    snc.sql(s"create table $tableName1 (ol_1_int_id  integer," +
        s" ol_1_int2_id  integer, ol_1_str_id STRING) using column " +
        "options( partition_by 'ol_1_int2_id', buckets '2'," +
        " COLUMN_BATCH_SIZE '100')")
    snc.sql(s"create table $tableName2 (ol_1_int_id  integer," +
        s" ol_1_int2_id  integer, ol_1_str_id STRING) using row " +
        "options( partition_by 'ol_1_int2_id', buckets '2')")

    // println("network server started")
    val serverHostPort = TestUtil.startNetServer()
    PreparedQueryRoutingSingleNodeSuite.insertRows(tableName1, 1000, serverHostPort)
    PreparedQueryRoutingSingleNodeSuite.insertRows(tableName2, 1000, serverHostPort)

    snc.sql(s"update $tableName2 set ol_1_str_id = '7777_a_1' where ol_1_int2_id = 500 ")
    snc.sql(s"update $tableName2 set ol_1_str_id = '7777_b_2' where ol_1_int2_id = 500 ")

    snc.sql(s"update $tableName1 set ol_1_str_id = '7777_a_1' where ol_1_int2_id = 500 ")
    snc.sql(s"update $tableName1 set ol_1_str_id = '7777_b_2' where ol_1_int2_id = 500 ")
  }

  object SortedColumnTests extends Logging {

    def testBasicInsert(session: SnappySession): Unit = {
      // session.conf.set(Property.ColumnBatchSize.name, "10k")
      session.conf.set(Property.ColumnMaxDeltaRows.name, "100")

      val numElements = 500

      session.sql("drop table if exists updateTable")

      session.sql("create table updateTable (id int, addr string, status boolean) " +
          "using column options(buckets '1', partition_by 'id')")

      session.range(numElements).filter(_  % 10 < 6).selectExpr("id",
        "concat('addr', cast(id as string))",
        "case when (id % 2) = 0 then true else false end").write.insertInto("updateTable")

      val rs1 = session.sql("select * from updateTable").collect()
      assert(rs1.length === 300)

      // scalastyle:off println
      println("")
      println("Done with first set of insert")
      println("")
      // scalastyle:on println


      try {
        // Final value should not exceed 495
        val rsAfterFilter = session.range(numElements - 5).filter(_  % 10 > 5)
        val cnt = rsAfterFilter.count()
        val rs2 = rsAfterFilter.selectExpr("id",
          "concat('addr', cast(id as string))",
          "case when (id % 2) = 0 then true else false end").collect()
        assert(rs2.length === 196)

        rs2.foreach(rs => {
          val idU = rs.getLong(0)
          val addrU = rs.getString(1)
          val statusU = rs.getBoolean(2)
          val rs3 = session.sql(s"update updateTable set " +
              s" id = $idU, " +
              s" addr = '$addrU', " +
              s" status = $statusU " +
              s" where (id = $idU)").collect()
          assert(rs3.map(_.getLong(0)).sum >= 0)
        })
      } catch {
        case t: Throwable =>
          logError(t.getMessage, t)
          throw t
      }

      // scalastyle:off println
      println("")
      println("Done with first set of update.")
      println("")
      // scalastyle:on println

//      session.range(numElements).filter(_  % 10 > 5).selectExpr("id",
//        "concat('addr', cast(id as string))",
//        "case when (id % 2) = 0 then true else false end").write.insertInto("updateTable")

      val rs2 = session.sql("select * from updateTable").collect()
      // assert(rs2.length === 500)

      // scalastyle:off println
      println("")
      println("Number of rows after update = " + rs2.length)
      println("")
      // scalastyle:on println

      /*
      var res = session.sql("select count(*) from updateTable").collect()
      assert(res.length === 1)
      assert(res(0).getLong(0) === 30000)

      res = session.sql("select * from updateTable where id = 31").collect()
      assert(res.length === 1)
      assert(res(0).getInt(0) === 31)
      assert(res(0).getString(1) === "addr31")
      assert(res(0).getBoolean(2) === false)

      res = session.sql("select * from updateTable where id = 39").collect()
      assert(res.length === 0)

      session.range(numElements).filter(_  % 10 > 5).selectExpr("id",
        "concat('addr', cast(id as string))",
        "case when (id % 2) = 0 then true else false end").write.insertInto("updateTable")

      val res_count = session.sql("select * from updateTable").count()
      assert(res_count === 50000)

      res = session.sql("select count(*) from updateTable").collect()
      assert(res.length === 1)
      assert(res(0).getLong(0) === 50000)

      res = session.sql("select * from updateTable where id = 39").collect()
      assert(res.length === 1)
      assert(res(0).getInt(0) === 39)
      assert(res(0).getString(1) === "addr39")
      assert(res(0).getBoolean(2) === false)
      */

      session.sql("drop table updateTable")

      session.conf.unset(Property.ColumnBatchSize.name)
    }

    def testBasicUpdate(session: SnappySession): Unit = {
      session.conf.set(Property.ColumnBatchSize.name, "10k")
      // session.conf.set(Property.ColumnMaxDeltaRows.name, "200")

      val numElements = 50000

      session.sql("drop table if exists updateTable")
      session.sql("drop table if exists checkTable1")
      session.sql("drop table if exists checkTable2")
      session.sql("drop table if exists checkTable3")

      session.sql("create table updateTable (id int, addr string, status boolean) " +
          "using column options(buckets '5')")
      session.sql("create table checkTable1 (id int, addr string, status boolean) " +
          "using column options(buckets '5')")
      session.sql("create table checkTable2 (id int, addr string, status boolean) " +
          "using column options(buckets '3')")
      session.sql("create table checkTable3 (id int, addr string, status boolean) " +
          "using column options(buckets '1')")

      session.range(numElements).selectExpr("id",
        "concat('addr', cast(id as string))",
        "case when (id % 2) = 0 then true else false end").write.insertInto("updateTable")

      // check updates to integer column

      session.range(numElements).selectExpr(s"id + $numElements",
        "concat('addr', cast(id as string))",
        "case when (id % 2) = 0 then true else false end").write.insertInto("checkTable1")

      assert(session.table("updateTable").count() === numElements)
      assert(session.table("checkTable1").count() === numElements)


      session.sql(s"update updateTable set id = id + ($numElements / 2) where id <> 73")
      session.table("updateTable").show()

      session.sql(s"update updateTable set id = id + ($numElements / 2) where id <> 73")
      session.table("updateTable").show()

      assert(session.table("updateTable").count() === numElements)
      assert(session.table("checkTable1").count() === numElements)

      var res = session.sql("select * from updateTable where id = 73").collect()
      assert(res.length === 1)
      assert(res(0).getInt(0) === 73)
      assert(res(0).getString(1) === "addr73")

      res = session.sql("select * from updateTable where id = cast(substr(addr, 5) as int)")
          .collect()
      assert(res.length === 1)
      assert(res(0).getInt(0) === 73)
      assert(res(0).getString(1) === "addr73")

      res = session.sql("select * from updateTable EXCEPT select * from checkTable1").collect()
      assert(res.length === 1)
      assert(res(0).getInt(0) === 73)
      assert(res(0).getString(1) === "addr73")


      // now check updates to string column

      session.sql(s"update updateTable set id = id - $numElements where id <> 73")
      session.range(numElements).selectExpr(s"id",
        "concat(concat('addr', cast(id as string)), '_update')",
        "case when (id % 2) = 0 then true else false end").write.insertInto("checkTable2")

      session.sql(s"update updateTable set addr = concat(addr, '_update') where id <> 32")
      session.table("updateTable").show()

      assert(session.table("updateTable").count() === numElements)
      assert(session.table("checkTable2").count() === numElements)

      res = session.sql("select * from updateTable where id = 32").collect()
      assert(res.length === 1)
      assert(res(0).getInt(0) === 32)
      assert(res(0).getString(1) === "addr32")

      res = session.sql("select * from updateTable where addr not like '%_update'").collect()
      assert(res.length === 1)
      assert(res(0).getInt(0) === 32)
      assert(res(0).getString(1) === "addr32")

      res = session.sql("select * from updateTable EXCEPT select * from checkTable2").collect()
      assert(res.length === 1)
      assert(res(0).getInt(0) === 32)
      assert(res(0).getString(1) === "addr32")


      // lastly to boolean column

      session.range(numElements).selectExpr(s"id",
        "concat(concat('addr', cast(id as string)), '_update')",
        "case when (id % 2) = 1 then true else false end").write.insertInto("checkTable3")

      session.sql(s"update updateTable set status = not status where id <> 87")
      session.table("updateTable").show()

      assert(session.table("updateTable").count() === numElements)
      assert(session.table("checkTable3").count() === numElements)

      res = session.sql("select * from updateTable where id = 87").collect()
      assert(res.length === 1)
      assert(res(0).getInt(0) === 87)
      assert(res(0).getString(1) === "addr87_update")
      assert(res(0).getBoolean(2) === false)

      res = session.sql("select * from updateTable where status <> ((id % 2) = 1)").collect()
      assert(res.length === 1)
      assert(res(0).getInt(0) === 87)
      assert(res(0).getString(1) === "addr87_update")
      assert(res(0).getBoolean(2) === false)

      res = session.sql("select * from updateTable EXCEPT select * from checkTable3").collect()
      assert(res.length === 2)
      assert(res.toSet === Set(Row(87, "addr87_update", false), Row(32, "addr32", false)))

      // check BroadcastNestedLoopJoin
      res = session.sql("select u.* from updateTable u, checkTable3 c where " +
          "u.id < 100 and c.id < 100 and (u.status <> c.status or u.addr <> c.addr)").collect()
      assert(res.length === 9902)

      // also with multiple updates leading to delta merges
      session.sql("truncate table checkTable3")
      session.range(numElements).selectExpr(s"id",
        "concat(concat('addr', cast(id as string)), '_update')",
        "case when (id % 2) = 1 then false else true end").write.insertInto("checkTable3")

      session.sql(s"update updateTable set status = not status where id <> 39")

      assert(session.table("updateTable").count() === numElements)
      assert(session.table("checkTable3").count() === numElements)

      res = session.sql("select * from updateTable where id = 39").collect()
      assert(res.length === 1)
      assert(res(0).getInt(0) === 39)
      assert(res(0).getString(1) === "addr39_update")
      assert(res(0).getBoolean(2) === true)

      res = session.sql("select * from updateTable where status = ((id % 2) = 1)").collect()
      assert(res.length === 2)
      assert(res.toSet === Set(Row(39, "addr39_update", true), Row(87, "addr87_update", true)))

      res = session.sql("select * from updateTable EXCEPT select * from checkTable3").collect()
      assert(res.length === 3)
      assert(res.toSet === Set(Row(39, "addr39_update", true),
        Row(87, "addr87_update", true), Row(32, "addr32", true)))

      // check no caching for BroadcastNestedLoopJoin
      res = session.sql("select u.* from updateTable u, checkTable3 c where " +
          "u.id < 100 and c.id < 100 and (u.status <> c.status or u.addr <> c.addr)").collect()
      assert(res.length === 9903)

      session.sql("drop table updateTable")
      session.sql("drop table checkTable1")
      session.sql("drop table checkTable2")
      session.sql("drop table checkTable3")

      session.conf.unset(Property.ColumnBatchSize.name)
    }

    def testBasicDelete(session: SnappySession): Unit = {
      session.conf.set(Property.ColumnBatchSize.name, "10k")
      // session.conf.set(Property.ColumnMaxDeltaRows.name, "200")

      session.sql("drop table if exists updateTable")
      session.sql("drop table if exists checkTable1")
      session.sql("drop table if exists checkTable2")
      session.sql("drop table if exists checkTable3")

      session.sql("create table updateTable (id int, addr string, status boolean) " +
          "using column options(buckets '5', partition_by 'addr')")
      session.sql("create table checkTable1 (id int, addr string, status boolean) " +
          "using column options(buckets '3')")
      session.sql("create table checkTable2 (id int, addr string, status boolean) " +
          "using column options(buckets '7')")
      session.sql("create table checkTable3 (id int, addr string, status boolean) " +
          "using column options(buckets '3')")

      for (_ <- 1 to 3) {
        testBasicDeleteIter(session)

        session.sql("truncate table updateTable")
        session.sql("truncate table checkTable1")
        session.sql("truncate table checkTable2")
        session.sql("truncate table checkTable3")
      }

      session.sql("drop table updateTable")
      session.sql("drop table checkTable1")
      session.sql("drop table checkTable2")
      session.sql("drop table checkTable3")

      session.conf.unset(Property.ColumnBatchSize.name)
    }

    def testBasicDeleteIter(session: SnappySession): Unit = {

      val numElements = 50000

      session.range(numElements).selectExpr("id",
        "concat('addr', cast(id as string))",
        "case when (id % 2) = 0 then true else false end").write.insertInto("updateTable")

      // check deletes

      session.range(numElements).filter("(id % 10) <> 0").selectExpr(s"id",
        "concat('addr', cast(id as string))",
        "case when (id % 2) = 0 then true else false end").write.insertInto("checkTable1")

      assert(session.table("updateTable").count() === numElements)
      assert(session.table("checkTable1").count() === (numElements * 9) / 10)

      session.sql(s"delete from updateTable where (id % 10) = 0")

      assert(session.table("updateTable").count() === (numElements * 9) / 10)
      assert(session.table("updateTable").collect().length === (numElements * 9) / 10)

      var res = session.sql("select * from updateTable EXCEPT select * from checkTable1").collect()
      assert(res.length === 0)


      // now check deletes after updates to columns

      session.range(numElements).filter("(id % 10) <> 0").selectExpr(s"id + $numElements",
        "concat('addr', cast(id as string))",
        "case when (id % 2) = 0 then true else false end").write.insertInto("checkTable2")

      session.sql(s"update updateTable set id = id + ($numElements / 2) where id <> 73")
      session.table("updateTable").show()

      session.sql(s"update updateTable set id = id + ($numElements / 2) where id <> 73")
      session.table("updateTable").show()

      assert(session.table("updateTable").count() === (numElements * 9) / 10)
      assert(session.table("updateTable").collect().length === (numElements * 9) / 10)

      res = session.sql("select * from updateTable where id = 73").collect()
      assert(res.length === 1)
      assert(res(0).getInt(0) === 73)
      assert(res(0).getString(1) === "addr73")

      res = session.sql("select * from updateTable where id = cast(substr(addr, 5) as int)")
          .collect()
      assert(res.length === 1)
      assert(res(0).getInt(0) === 73)
      assert(res(0).getString(1) === "addr73")

      res = session.sql("select * from updateTable EXCEPT select * from checkTable2").collect()
      assert(res.length === 1)
      assert(res(0).getInt(0) === 73)
      assert(res(0).getString(1) === "addr73")

      // more deletes on the same rows as updates

      session.range(numElements).filter("(id % 5) <> 0").selectExpr(s"id + $numElements",
        "concat('addr', cast(id as string))",
        "case when (id % 2) = 0 then true else false end").write.insertInto("checkTable3")

      session.sql(s"delete from updateTable where (cast(substr(addr, 5) as int) % 5) = 0")

      assert(session.table("updateTable").count() === (numElements * 8) / 10)
      assert(session.table("updateTable").collect().length === (numElements * 8) / 10)

      res = session.sql("select * from updateTable EXCEPT select * from checkTable3").collect()
      assert(res.length === 1)
      assert(res(0).getInt(0) === 73)
      assert(res(0).getString(1) === "addr73")

      // lastly delete everything and check there is nothing in table
      session.sql("delete from updateTable")
      assert(session.sql("select * from updateTable").collect().length === 0)
    }

    def testSNAP1925(session: SnappySession): Unit = {
      // reduced size to ensure both column table and row buffer have data
      session.conf.set(Property.ColumnBatchSize.name, "10k")

      val numElements = 50000

      session.sql("drop table if exists order_details")
      session.sql("create table order_details (OrderID int, ProductID int," +
          "UnitPrice double, Quantity smallint, Discount double, tid int) " +
          "using column options(partition_by 'OrderID', buckets '8')")

      session.range(numElements).selectExpr("id", "id + 2", "1.0", "2", "rand()", "id + 1")
          .write.insertInto("order_details")

      session.sql("UPDATE order_details SET UnitPrice = UnitPrice * 1.1 WHERE tid = 6")

      var result = session.sql("select UnitPrice, tid from order_details where tid <> 6").collect()
      assert(result.length === numElements - 1)
      assert(result.toSeq.filter(_.getDouble(0) != 1.0) === Seq.empty)

      result = session.sql("select UnitPrice from order_details where tid = 6").collect()
      assert(result.length === 1)
      assert(result(0).getDouble(0) == 1.1)

      session.sql("UPDATE order_details SET UnitPrice = UnitPrice * 1.1 WHERE tid <> 6")

      result = session.sql("select UnitPrice from order_details where tid = 6").collect()
      assert(result.length === 1)
      assert(result(0).getDouble(0) == 1.1)
      result = session.sql("select UnitPrice, tid from order_details where tid <> 6").collect()
      assert(result.length === numElements - 1)
      assert(result.toSeq.filter(_.getDouble(0) != 1.1) === Seq.empty)
      result = session.sql("select UnitPrice, tid from order_details").collect()
      assert(result.length === numElements)
      assert(result.toSeq.filter(_.getDouble(0) != 1.1) === Seq.empty)


      session.sql("UPDATE order_details SET UnitPrice = 1.1 WHERE tid <> 11")

      result = session.sql("select UnitPrice from order_details where tid = 11").collect()
      assert(result.length === 1)
      assert(result(0).getDouble(0) == 1.1)
      result = session.sql("select UnitPrice, tid from order_details where tid <> 6").collect()
      assert(result.length === numElements - 1)
      assert(result.toSeq.filter(_.getDouble(0) != 1.1) === Seq.empty)
      result = session.sql("select UnitPrice, tid from order_details").collect()
      assert(result.length === numElements)
      assert(result.toSeq.filter(_.getDouble(0) != 1.1) === Seq.empty)

      session.sql("drop table order_details")
      session.conf.unset(Property.ColumnBatchSize.name)
    }

    def testSNAP1926(session: SnappySession): Unit = {
      // reduced size to ensure both column table and row buffer have data
      session.conf.set(Property.ColumnBatchSize.name, "10k")

      val numElements = 50000

      session.sql("drop table if exists customers")
      session.sql("CREATE TABLE CUSTOMERS (CUSTOMERID VARCHAR(100), COMPANYNAME VARCHAR(100), " +
          "CONTACTNAME VARCHAR(100), CONTACTTITLE VARCHAR(100), ADDRESS VARCHAR(100), " +
          "CITY VARCHAR(100), REGION VARCHAR(100), POSTALCODE VARCHAR(100), " +
          "COUNTRY VARCHAR(100), PHONE VARCHAR(100), FAX VARCHAR(100), TID INTEGER) " +
          "using column options(partition_by 'City,Country', buckets '8')")

      session.range(numElements).selectExpr("id", "id + 1", "id + 2", "id + 3", "id + 4",
        "id + 5", "id + 6", "id + 7", "id + 8", "id + 9", "id + 10", "id % 20")
          .write.insertInto("customers")

      session.sql("delete from customers where CustomerID IN (SELECT min(CustomerID) " +
          "from customers where tid=10) AND tid=10")

      var result = session.sql("select CustomerID, tid from customers where tid = 10").collect()
      assert(result.length === (numElements / 20) - 1)
      result = session.sql("select CustomerID, tid from customers").collect()
      assert(result.length === numElements - 1)

      session.sql("drop table customers")
      session.conf.unset(Property.ColumnBatchSize.name)
    }

    def testConcurrentOps(session: SnappySession): Unit = {
      // reduced size to ensure both column table and row buffer have data
      session.conf.set(Property.ColumnBatchSize.name, "10k")
      // session.conf.set(Property.ColumnMaxDeltaRows.name, "200")

      session.sql("drop table if exists updateTable")
      session.sql("drop table if exists checkTable1")
      session.sql("drop table if exists checkTable2")
      session.sql("drop table if exists checkTable3")

      session.sql("create table updateTable (id int, addr string, status boolean) " +
          "using column options(buckets '4')")
      session.sql("create table checkTable1 (id int, addr string, status boolean) " +
          "using column options(buckets '2')")
      session.sql("create table checkTable2 (id int, addr string, status boolean) " +
          "using column options(buckets '8')")

      // avoid rollover in updateTable during concurrent updates
      val avoidRollover = new SerializableRunnable() {
        override def run(): Unit = {
          if (GemFireCacheImpl.getInstance ne null) {
            val pr = Misc.getRegionForTable("APP.UPDATETABLE", false)
                .asInstanceOf[PartitionedRegion]
            if (pr ne null) {
              pr.getUserAttribute.asInstanceOf[GemFireContainer].fetchHiveMetaData(true)
              pr.setColumnBatchSizes(10000000, 10000, 1000)
            }
          }
        }
      }
      DistributedTestBase.invokeInEveryVM(avoidRollover)
      avoidRollover.run()

      for (_ <- 1 to 3) {
        testConcurrentOpsIter(session)

        session.sql("truncate table updateTable")
        session.sql("truncate table checkTable1")
        session.sql("truncate table checkTable2")
      }

      // cleanup
      session.sql("drop table updateTable")
      session.sql("drop table checkTable1")
      session.sql("drop table checkTable2")
      session.conf.unset(Property.ColumnBatchSize.name)
    }

    def testConcurrentOpsIter(session: SnappySession): Unit = {
      val numElements = 100000
      val concurrency = 8
      // each thread will update/delete after these many rows
      val step = 10

      session.range(numElements).selectExpr("id", "concat('addr', cast(id as string))",
        "case when (id % 2) = 0 then true else false end").write.insertInto("updateTable")

      // expected results after updates in this table
      val idUpdate = s"id + ($numElements / 2)"
      val idSet = s"case when (id % $step) < $concurrency then id + ($numElements / 2) else id end"
      val addrSet = s"case when (id % $step) < $concurrency " +
          s"then concat('addrUpd', cast(($idUpdate) as string)) " +
          s"else concat('addr', cast(id as string)) end"
      session.range(numElements).selectExpr(idSet, addrSet,
        "case when (id % 2) = 0 then true else false end").write.insertInto("checkTable1")

      // expected results after updates and deletes in this table
      session.table("checkTable1").filter(s"(id % $step) < ${step - concurrency}")
          .write.insertInto("checkTable2")

      val exceptions = new TrieMap[Thread, Throwable]
      val executionContext = ExecutionContext.fromExecutorService(
        Executors.newFixedThreadPool(concurrency + 2))

      // concurrent updates to different rows but same batches
      val barrier = new CyclicBarrier(concurrency)
      var tasks = Array.tabulate(concurrency)(i => Future {
        try {
          val snappy = new SnappySession(session.sparkContext)
          var res = snappy.sql("select count(*) from updateTable").collect()
          assert(res(0).getLong(0) === numElements)

          barrier.await()
          res = snappy.sql(s"update updateTable set id = $idUpdate, " +
              s"addr = concat('addrUpd', cast(($idUpdate) as string)) " +
              s"where (id % $step) = $i").collect()
          assert(res.map(_.getLong(0)).sum > 0)
        } catch {
          case t: Throwable =>
            logError(t.getMessage, t)
            exceptions += Thread.currentThread() -> t
            throw t
        }
      }(executionContext))
      tasks.foreach(Await.ready(_, Duration(300, "s")))

      assert(exceptions.isEmpty, s"Failed with exceptions: $exceptions")

      session.table("updateTable").show()

      var res = session.sql(
        "select * from updateTable EXCEPT select * from checkTable1").collect()
      assert(res.length === 0)

      // concurrent deletes
      tasks = Array.tabulate(concurrency)(i => Future {
        try {
          val snappy = new SnappySession(session.sparkContext)
          var res = snappy.sql("select count(*) from updateTable").collect()
          assert(res(0).getLong(0) === numElements)

          barrier.await()
          res = snappy.sql(
            s"delete from updateTable where (id % $step) = ${step - i - 1}").collect()
          assert(res.map(_.getLong(0)).sum > 0)
        } catch {
          case t: Throwable =>
            logError(t.getMessage, t)
            exceptions += Thread.currentThread() -> t
            throw t
        }
      }(executionContext))
      tasks.foreach(Await.ready(_, Duration(300, "s")))

      assert(exceptions.isEmpty, s"Failed with exceptions: $exceptions")

      res = session.sql(
        "select * from updateTable EXCEPT select * from checkTable2").collect()
      assert(res.length === 0)
    }
  }
}
