package org.alephium.util

import java.util.concurrent.{ConcurrentHashMap => JCHashMap}
import java.util.function.Consumer

import scala.collection.JavaConverters._

object ConcurrentHashMap {
  def empty[K, V]: ConcurrentHashMap[K, V] = {
    val m = new JCHashMap[K, V]()
    new ConcurrentHashMap[K, V](m)
  }
}

class ConcurrentHashMap[K, V] private (m: JCHashMap[K, V]) {
  def size: Int = m.size()

  def apply(k: K): V = {
    val v = m.get(k)
    assert(v != null)
    v
  }

  def get(k: K): Option[V] = {
    Option(m.get(k))
  }

  def contains(k: K): Boolean = m.containsKey(k)

  def add(k: K, v: V): Unit = {
    val result = m.put(k, v)
    assert(result == null)
  }

  def put(k: K, v: V): Unit = {
    m.put(k, v)
    ()
  }

  def remove(k: K): Unit = {
    val result = m.remove(k)
    assert(result != null)
  }

  def removeIfExist(k: K): Unit = {
    m.remove(k)
    ()
  }

  def keys: Iterable[K] = m.keySet().asScala

  def values: Iterable[V] = m.values().asScala

  def foreachValue(f: V => Unit): Unit = {
    val consumer = new Consumer[V] { override def accept(v: V): Unit = f(v) }
    m.values().forEach(consumer)
  }

  // throw exception if the map is empty
  @SuppressWarnings(Array("org.wartremover.warts.OptionPartial"))
  def reduceValuesBy[W](f: V => W)(op: (W, W) => W): W = {
    assume(!m.isEmpty)
    var result: Option[W] = None
    foreachValue { v =>
      val w = f(v)
      result match {
        case Some(r) => result = Some(op(r, w))
        case None    => result = Some(w)
      }
    }
    result.get
  }
}
