package org.alephium.util

import scala.collection.mutable

import org.scalatest.Assertion

class ForestSpec extends AlephiumSpec {
  def check(roots: AVector[Int], pairs: List[(Int, Int)]): Assertion = {
    val links  = mutable.HashMap.empty ++ pairs
    val values = AVector.from(pairs.map(_._1))
    val forest = Forest.build[Int, Int](values, identity, links.apply).get
    forest.roots.map(_.key).toSet is roots.toSet

    def iter(node: Node[Int, Int]): Unit = {
      links.remove(node.key)
      node.children.foreach { child =>
        links(child.key) is node.key
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
    check(AVector(1), List(1 -> 0))
    check(AVector(1), List(1 -> 0, 2 -> 1))
    check(AVector(1), List(1 -> 0, 2 -> 1, 3 -> 2))
    check(AVector(1), List(1 -> 0, 2 -> 1, 3 -> 2, 4 -> 3))
  }

  it should "build tree" in {
    check(AVector(1, 2), List(1 -> 0, 2 -> 0))
    check(AVector(1, 2), List(1 -> 0, 2 -> 0, 3 -> 1, 4 -> 1))
    check(AVector(1, 2), List(1 -> 0, 2 -> 0, 3 -> 1, 4 -> 1, 5 -> 2))
    check(AVector(1, 2), List(1 -> 0, 2 -> 0, 3 -> 1, 4 -> 2, 5 -> 2))
  }

  it should "build forest" in {
    check(AVector(2, 3), List(2 -> 0, 3 -> 1))
    /* indentation split */
    check(AVector(2, 3, 4, 5), List(2 -> 0, 3 -> 0, 4 -> 1, 5 -> 1))
    check(AVector(2, 3, 4, 5), List(2 -> 0, 3 -> 0, 4 -> 1, 5 -> 1, 6 -> 2, 7 -> 5))
  }

  def build(pairs: List[(Int, Int)]): Option[Forest[Int, Int]] = {
    val links  = mutable.HashMap.empty ++ pairs
    val values = AVector.from(pairs.map(_._1))
    Forest.build[Int, Int](values, identity, links.apply)
  }

  def invalid(pairs: List[(Int, Int)]): Assertion = {
    build(pairs) is None
  }

  it should "not build" in {
    invalid(List(2 -> 1, 1 -> 0))
    invalid(List(1 -> 0, 4 -> 2, 2 -> 0, 3 -> 1, 5 -> 2))
    invalid(List(2 -> 0, 7 -> 5, 3 -> 0, 4 -> 1, 5 -> 1, 6 -> 2))
  }

  def checkRemoveNode(pairs: List[(Int, Int)],
                      key: Int,
                      expected: Option[Int],
                      newRootSize: Int): Assertion = {
    val forest = build(pairs).get
    forest.removeRootNode(key).map(_.key) is expected
    forest.roots.size is newRootSize
  }

  it should "remove nodes" in {
    // path
    checkRemoveNode(List(1 -> 0, 2 -> 1, 3 -> 2, 4 -> 3), 0, None, 1)
    checkRemoveNode(List(1 -> 0, 2 -> 1, 3 -> 2, 4 -> 3), 1, Some(1), 1)
    // tree
    checkRemoveNode(List(1 -> 0, 2 -> 0, 3 -> 1, 4 -> 1, 5 -> 2), 0, None, 2)
    checkRemoveNode(List(1 -> 0, 2 -> 0, 3 -> 1, 4 -> 1, 5 -> 2), 1, Some(1), 3)
    checkRemoveNode(List(1 -> 0, 2 -> 0, 3 -> 1, 4 -> 1, 5 -> 2), 2, Some(2), 2)
    // forest
    checkRemoveNode(List(2 -> 0, 3 -> 0, 4 -> 1, 5 -> 1, 6 -> 2, 7 -> 5), 0, None, 4)
    checkRemoveNode(List(2 -> 0, 3 -> 0, 4 -> 1, 5 -> 1, 6 -> 2, 7 -> 5), 1, None, 4)
    checkRemoveNode(List(2 -> 0, 3 -> 0, 4 -> 1, 5 -> 1, 6 -> 2, 7 -> 5), 2, Some(2), 4)
    checkRemoveNode(List(2 -> 0, 3 -> 0, 4 -> 1, 5 -> 1, 6 -> 2, 7 -> 5), 3, Some(3), 3)
  }

  def checkRemoveBranch(pairs: List[(Int, Int)],
                        key: Int,
                        expected: Option[Int],
                        newRootSize: Int): Assertion = {
    val forest = build(pairs).get
    forest.removeBranch(key).map(_.key) is expected
    forest.roots.size is newRootSize
  }

  it should "remove branch" in {
    // path
    checkRemoveBranch(List(1 -> 0, 2 -> 1, 3 -> 2, 4 -> 3), 0, None, 1)
    checkRemoveBranch(List(1 -> 0, 2 -> 1, 3 -> 2, 4 -> 3), 1, Some(1), 0)
    // tree
    checkRemoveBranch(List(1 -> 0, 2 -> 0, 3 -> 1, 4 -> 1, 5 -> 2), 0, None, 2)
    checkRemoveBranch(List(1 -> 0, 2 -> 0, 3 -> 1, 4 -> 1, 5 -> 2), 1, Some(1), 1)
    checkRemoveBranch(List(1 -> 0, 2 -> 0, 3 -> 1, 4 -> 1, 5 -> 2), 2, Some(2), 1)
    // forest
    checkRemoveBranch(List(2 -> 0, 3 -> 0, 4 -> 1, 5 -> 1, 6 -> 2, 7 -> 5), 0, None, 4)
    checkRemoveBranch(List(2 -> 0, 3 -> 0, 4 -> 1, 5 -> 1, 6 -> 2, 7 -> 5), 1, None, 4)
    checkRemoveBranch(List(2 -> 0, 3 -> 0, 4 -> 1, 5 -> 1, 6 -> 2, 7 -> 5), 2, Some(2), 3)
    checkRemoveBranch(List(2 -> 0, 3 -> 0, 4 -> 1, 5 -> 1, 6 -> 2, 7 -> 5), 3, Some(3), 3)
  }
}
