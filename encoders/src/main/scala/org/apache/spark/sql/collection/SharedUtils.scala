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
package org.apache.spark.sql.collection

import java.nio.ByteBuffer

import scala.collection.mutable
import scala.language.existentials

import com.esotericsoftware.kryo.{Kryo, KryoSerializable}
import com.esotericsoftware.kryo.io.{Input, Output}
import com.gemstone.gemfire.internal.shared.BufferAllocator
import com.gemstone.gemfire.internal.shared.unsafe.UnsafeHolder
import com.gemstone.gemfire.internal.snappy.UMMMemoryTracker

import org.apache.spark._
import org.apache.spark.memory.{MemoryManagerCallback, MemoryMode, TaskMemoryManager}
import org.apache.spark.scheduler.TaskLocation
import org.apache.spark.scheduler.local.LocalSchedulerBackend
import org.apache.spark.sql.catalyst.expressions.UnsafeRow
import org.apache.spark.storage.BlockManagerId
import org.apache.spark.unsafe.Platform

object SharedUtils {

  def taskMemoryManager(context: TaskContext): TaskMemoryManager = context.taskMemoryManager()

  def toUnsafeRow(buffer: ByteBuffer, numColumns: Int): UnsafeRow = {
    if (buffer eq null) return null
    val row = new UnsafeRow(numColumns)
    if (buffer.isDirect) {
      row.pointTo(null, UnsafeHolder.getDirectBufferAddress(buffer) +
          buffer.position(), buffer.remaining())
    } else {
      row.pointTo(buffer.array(), Platform.BYTE_ARRAY_OFFSET +
          buffer.arrayOffset() + buffer.position(), buffer.remaining())
    }
    row
  }

  def getAllExecutorsMemoryStatus(
      sc: SparkContext): Map[BlockManagerId, (Long, Long)] = {
    val memoryStatus = sc.env.blockManager.master.getMemoryStatus
    // no filtering for local backend
    if (isLoner(sc)) memoryStatus else memoryStatus.filter(!_._1.isDriver)
  }

  def getHostExecutorId(blockId: BlockManagerId): String =
    TaskLocation.executorLocationTag + blockId.host + '_' + blockId.executorId

  def createStatsBuffer(statsData: Array[Byte], allocator: BufferAllocator): ByteBuffer = {
    // need to create a copy since underlying Array[Byte] can be re-used
    val statsLen = statsData.length
    val statsBuffer = allocator.allocateForStorage(statsLen)
    statsBuffer.put(statsData, 0, statsLen)
    statsBuffer.rewind()
    statsBuffer
  }

  final def isLoner(sc: SparkContext): Boolean =
    (sc ne null) && sc.schedulerBackend.isInstanceOf[LocalSchedulerBackend]

  def acquireStorageMemory(objectName: String, numBytes: Long,
      buffer: UMMMemoryTracker, shouldEvict: Boolean, offHeap: Boolean): Boolean = {
    val mode = if (offHeap) MemoryMode.OFF_HEAP else MemoryMode.ON_HEAP
    if (numBytes > 0) {
      return MemoryManagerCallback.memoryManager.acquireStorageMemoryForObject(objectName,
        MemoryManagerCallback.storageBlockId, numBytes, mode, buffer, shouldEvict)
    } else if (numBytes < 0) {
      MemoryManagerCallback.memoryManager.releaseStorageMemoryForObject(
        objectName, -numBytes, mode)
    }
    true
  }

  def releaseStorageMemory(objectName: String, numBytes: Long,
      offHeap: Boolean): Unit = {
    val mode = if (offHeap) MemoryMode.OFF_HEAP else MemoryMode.ON_HEAP
    MemoryManagerCallback.memoryManager.
        releaseStorageMemoryForObject(objectName, numBytes, mode)
  }

  /** for testing only (a long convoluted name chosen deliberately) */
  var TEST_RANDOM_BUCKETID_ASSIGNMENT: Boolean = java.lang.Boolean.getBoolean(
    "SNAPPYTEST_RANDOM_BUCKETID_TO_PARTITION_ASSIGNMENT")

  /**
   * TODO: check SmartConnectorRDDHelper for implementation
   * NOTE: JavaDoc for DataReaderFactory.preferredLocations says
   * that preferredLocations should return host name, so currently
   * this returns true
   */
  def preferHostName(): Boolean = {
    true
  }
}

final class SmartExecutorBucketPartition(private var _index: Int, private var _bucketId: Int,
    var hostList: mutable.ArrayBuffer[(String, String)])
    extends Partition with KryoSerializable {

  override def index: Int = _index

  def bucketId: Int = _bucketId

  override def write(kryo: Kryo, output: Output): Unit = {
    output.writeVarInt(_index, true)
    output.writeVarInt(_bucketId, true)
    val numHosts = hostList.length
    output.writeVarInt(numHosts, true)
    for ((host, url) <- hostList) {
      output.writeString(host)
      output.writeString(url)
    }
  }

  override def read(kryo: Kryo, input: Input): Unit = {
    _index = input.readVarInt(true)
    _bucketId = input.readVarInt(true)
    val numHosts = input.readVarInt(true)
    hostList = new mutable.ArrayBuffer[(String, String)](numHosts)
    for (_ <- 0 until numHosts) {
      val host = input.readString()
      val url = input.readString()
      hostList += host -> url
    }
  }

  override def toString: String =
    s"SmartExecutorBucketPartition($index, $bucketId, $hostList)"
}