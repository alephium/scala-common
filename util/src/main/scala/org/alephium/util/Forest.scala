package org.alephium.util

import scala.collection.mutable

object Forest {
  // Note: the parent node should comes first in values; otherwise return None
  def tryBuild[K, T](values: AVector[T], toKey: T => K, toParent: T => K): Option[Forest[K, T]] = {
    val rootParents = mutable.HashMap.empty[K, mutable.ArrayBuffer[Node[K, T]]]
    val nodes       = mutable.HashMap.empty[K, Node[K, T]]
    values.foreach { value =>
      val key = toKey(value)
      if (rootParents.contains(key)) {
        // scalastyle:off return
        return None
        // scalastyle:on return
      } else {
        val node      = Node(key, value, mutable.ArrayBuffer.empty[Node[K, T]])
        val parentKey = toParent(value)
        rootParents.get(parentKey) match {
          case Some(children) => children.append(node)
          case None =>
            nodes.get(parentKey) match {
              case Some(parentNode) => parentNode.children.append(node)
              case None             => rootParents += (parentKey -> mutable.ArrayBuffer(node))
            }
        }
        nodes += key -> node
      }
    }

    val roots = mutable.ArrayBuffer.empty[Node[K, T]]
    rootParents.values.foreach(roots ++= _)
    Some(Forest(roots))
  }

  def build[K, T](value: T, toKey: T => K): Forest[K, T] = {
    val node = Node(toKey(value), value, mutable.ArrayBuffer.empty[Node[K, T]])
    Forest(mutable.ArrayBuffer(node))
  }
}

// Note: we use ArrayBuffer instead of Set because the number of forks in blockchain is usually small
case class Forest[K, T](roots: mutable.ArrayBuffer[Node[K, T]]) {
  def isEmpty: Boolean = roots.isEmpty

  def nonEmpty: Boolean = roots.nonEmpty

  def removeRootNode(key: K): Option[Node[K, T]] = {
    withRemove(key) { (index, node) =>
      roots.remove(index)
      roots.appendAll(node.children)
    }
  }

  def removeBranch(key: K): Option[Node[K, T]] = {
    withRemove(key) { (index, _) =>
      roots.remove(index)
      ()
    }
  }

  private def withRemove(key: K)(f: (Int, Node[K, T]) => Unit): Option[Node[K, T]] = {
    val index = roots.indexWhere(_.key == key)
    if (index == -1) None
    else {
      val node = roots(index)
      f(index, node)
      Some(node)
    }
  }
}

case class Node[K, T](key: K, value: T, children: mutable.ArrayBuffer[Node[K, T]])
