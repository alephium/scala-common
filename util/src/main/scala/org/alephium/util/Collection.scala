package org.alephium.util

import scala.collection.immutable.ArraySeq

object Collection {
  def get[T](array: Array[T], index: Int): Option[T] = {
    if (checkIndex(array, index)) Some(array(index)) else None
  }

  @inline def checkIndex[T](array: Array[T], index: Int): Boolean = {
    index >= 0 && index < array.length
  }

  def get[T](array: ArraySeq[T], index: Int): Option[T] = {
    if (checkIndex(array, index)) Some(array(index)) else None
  }

  @inline def checkIndex[T](array: ArraySeq[T], index: Int): Boolean = {
    index >= 0 && index < array.length
  }
}
