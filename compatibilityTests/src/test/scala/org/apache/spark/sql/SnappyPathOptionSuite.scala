/*
 * Copyright (c) 2018 SnappyData, Inc. All rights reserved.
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
package org.apache.spark.sql

import org.apache.spark.sql.sources.PathOptionSuite
import org.apache.spark.sql.test.{SharedSnappySessionContext, SnappySparkTestUtil}

class SnappyPathOptionSuite extends PathOptionSuite
    with SharedSnappySessionContext with SnappySparkTestUtil {

  // TODO: SW: though this does not fail but data is not moved over due to small-case name
  // used by hive client by default, so will be fixed once the case is all converted to lower-case
  /*
  override def ignored: Seq[String] = Seq(
    "path option always represent the value of table location"
  )
  */
}
