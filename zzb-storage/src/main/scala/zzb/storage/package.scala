package zzb

import zzb.datatype._
import zzb.storage.dirvers.{MongoDriver, MemoryDriver}

/**
 * Created with IntelliJ IDEA.
 * User: Simon Xiao
 * Date: 13-12-4
 * Time: 下午5:34
 * Copyright baoxian.com 2012~2020
 */
package object storage {

  def memoryStorage[K, KT <: DataType[K], T <: TStorable[K, KT]](dType: T, maxCache: Int = 1000, initCache: Int = 50) = {

    val driver = new MemoryDriver[K, KT, T]() {
      override val docType = dType
    }
    new Storage[K, KT, T](driver, maxCache, initCache)
  }

  def mongodbStorage[K, KT <: DataType[K], T <: TStorable[K, KT]](dbName: String, dType: T, maxCache: Int = 1000, initCache: Int = 50) = {

    val driver = new MongoDriver[K, KT, T]() {
      override val docType = dType

      def dbname = dbName
    }
    new Storage[K, KT, T](driver, maxCache, initCache)
  }
}
