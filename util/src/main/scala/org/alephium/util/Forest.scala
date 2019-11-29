package org.alephium.util

import scala.collection.mutable

object Forest {
  // Note: the parent node should comes first in values; otherwise return None
  def build[K, T](values: AVector[T], toKey: T => K, toParent: T => K): Option[Forest[K]] = {
    val roots = mutable.ArrayBuffer.empty[K]
    val nodes = mutable.HashMap.empty[K, Node[K]]
    values.foreach { value =>
      val key = toKey(value)
      if (roots.contains(key)) {
        // scalastyle:off return
        return None
        // scalastyle:on return
      } else {
        val node      = Node(key, mutable.ArrayBuffer.empty[Node[K]])
        val parentKey = toParent(value)
        nodes.get(parentKey) match {
          case Some(parentNode) =>
            parentNode.children.append(node)
          case None =>
            roots += parentKey
            nodes += parentKey -> Node(parentKey, mutable.ArrayBuffer(node))
        }
        nodes += key -> node
      }
    }
    Some(Forest(roots.map(nodes.apply)))
  }
}

// Note: we use ArrayBuffer instead of Set because the number of forks in blockchain is usually small
case class Forest[K](roots: mutable.ArrayBuffer[Node[K]]) {
  def removeRootNode(key: K): Boolean = {
    withRemove(key) { (index, node) =>
      roots.remove(index)
      roots.appendAll(node.children)
    }
  }

  def removeBranch(key: K): Boolean = {
    withRemove(key) { (index, _) =>
      roots.remove(index)
      ()
    }
  }

  def withRemove(key: K)(f: (Int, Node[K]) => Unit): Boolean = {
    val index = roots.indexWhere(_.key == key)
    if (index == -1) false
    else {
      val node = roots(index)
      f(index, node)
      true
    }
  }
}

case class Node[K](key: K, children: mutable.ArrayBuffer[Node[K]])
