package org.alephium.util

object ArrayF {
  def get[T](array: Array[T], index: Int): Option[T] = {
    if (checkIndex(array, index)) Some(array(index)) else None
  }

  @inline def checkIndex[T](array: Array[T], index: Int): Boolean = {
    index >= 0 && index < array.length
  }
}
