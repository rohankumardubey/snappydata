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

package io.snappydata.hydra.slackIssues

import java.io.{File, FileOutputStream, PrintWriter}

import com.typesafe.config.Config
import org.apache.spark.sql._

import scala.util.{Failure, Success, Try}


class ExecuteQueriesJob extends SnappySQLJob {

  override def runSnappyJob(snappySession: SnappySession, jobConfig: Config): Any = {
    val snc = snappySession.sqlContext

    def getCurrentDirectory = new java.io.File(".").getCanonicalPath

    val outputFile = "ExecuteQueries_" + jobConfig.getString("logFileName")
    val pw = new PrintWriter(new FileOutputStream(new File(outputFile), true));
    /*val sc = SparkContext.getOrCreate()
    val sqlContext = SQLContext.getOrCreate(sc)*/
    var query: String = null;
    Try {
      //snc.sql("set spark.sql.shuffle.partitions=23")

      pw.println(s"ExecuteQueries job started at" +
          s" :  " + System.currentTimeMillis)
      //query = "select datekey, count(1) from test_table group by datekey order by datekey asc";
      query = "select count(1) from test_table";
      var start = System.currentTimeMillis
      snc.sql(query)
      //SnappyTestUtils.assertQueryFullResultSet(snc, query, "Q1", "column", pw, sqlContext)
      var end = System.currentTimeMillis
      pw.println(s"\nExecution Time for $query: " +
          (end - start) + " ms")
      /*query = "select count(*) from test_table"
      start = System.currentTimeMillis
      snc.sql(query).show(5)
      end = System.currentTimeMillis
      pw.println(s"\nExecution Time for $query: " +
          (end - start) + " ms")
      query = "select ID from test_table group by ID order by ID asc"
      start = System.currentTimeMillis
      snc.sql(query).show(5)
      end = System.currentTimeMillis
      pw.println(s"\nExecution Time for $query: " +
          (end - start) + " ms")
      query = "select COMP_PRICE1, VALID_STATUS from test_table WHERE ID >= 25000 AND ID <= 50000000"
      start = System.currentTimeMillis
      snc.sql(query).show(5)
      end = System.currentTimeMillis
      pw.println(s"\nExecution Time for $query: " +
          (end - start) + " ms")
      query = "select CHECKIN_DATE, CHECKOUT_DATE from test_table WHERE ID IN (500, 25000000)"
      start = System.currentTimeMillis
      snc.sql(query).show(5)
      end = System.currentTimeMillis
      pw.println(s"\nExecution Time for $query: " +
          (end - start) + " ms")*/
      pw.println(s"ExecuteQueries job completed  " +
          s"successfully at : " + System.currentTimeMillis)
      pw.close()
    } match {
      case Success(v) => pw.close()
        s"See ${getCurrentDirectory}/${outputFile}"
      case Failure(e) => pw.close();
        throw e;
    }
  }


  override def isValidJob(sc: SnappySession, config: Config): SnappyJobValidation = SnappyJobValid()
}