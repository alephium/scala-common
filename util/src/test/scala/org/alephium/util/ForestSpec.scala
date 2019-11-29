package org.alephium.util

import scala.collection.mutable

import org.scalatest.Assertion

class ForestSpec extends AlephiumSpec {
  def check(roots: AVector[Int], pairs: List[(Int, Int)]): Assertion = {
    val links  = mutable.HashMap.empty ++ pairs
    val values = AVector.from(pairs.map(_._1))
    val forest = Forest.build[Int, Int](values, identity, links.apply).get
    forest.roots.map(_.key).toSet is roots.toSet

    def iter(node: Node[Int]): Unit = {
      node.children.foreach { child =>
        links(child.key) is node.key
        links.remove(child.key)
        iter(child)
      }
    }
    forest.roots.foreach(iter)
    links.isEmpty is true
  }

  it should "build empty forest" in {
    check(AVector.empty[Int], List.empty)
  }

  it should "build path" in {
    check(AVector(0), List(1 -> 0))
    check(AVector(0), List(1 -> 0, 2 -> 1))
    check(AVector(0), List(1 -> 0, 2 -> 1, 3 -> 2))
    check(AVector(0), List(1 -> 0, 2 -> 1, 3 -> 2, 4 -> 3))
  }

  it should "build tree" in {
    check(AVector(0), List(1 -> 0, 2 -> 0))
    check(AVector(0), List(1 -> 0, 2 -> 0, 3 -> 1, 4 -> 1))
    check(AVector(0), List(1 -> 0, 2 -> 0, 3 -> 1, 4 -> 1, 5 -> 2))
    check(AVector(0), List(1 -> 0, 2 -> 0, 3 -> 1, 4 -> 2, 5 -> 2))
  }

  it should "build forest" in {
    check(AVector(0, 1), List(2 -> 0, 3 -> 1))
    check(AVector(0, 1), List(2 -> 0, 3 -> 0, 4 -> 1, 5 -> 1))
    check(AVector(0, 1), List(2 -> 0, 3 -> 0, 4 -> 1, 5 -> 1, 6 -> 2, 7 -> 5))
  }

  def invalid(pairs: List[(Int, Int)]): Assertion = {
    val links  = mutable.HashMap.empty ++ pairs
    val values = AVector.from(pairs.map(_._1))
    Forest.build[Int, Int](values, identity, links.apply) is None
  }

  it should "not build" in {
    invalid(List(2 -> 1, 1 -> 0))
    invalid(List(1 -> 0, 4 -> 2, 2 -> 0, 3 -> 1, 5 -> 2))
    invalid(List(2 -> 0, 7 -> 5, 3 -> 0, 4 -> 1, 5 -> 1, 6 -> 2))
  }
}
