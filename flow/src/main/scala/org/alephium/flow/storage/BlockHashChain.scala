package org.alephium.flow.storage

import org.alephium.crypto.Keccak256
import org.alephium.flow.PlatformConfig
import org.alephium.util.AVector

import scala.annotation.tailrec
import scala.collection.mutable.{ArrayBuffer, HashMap, HashSet}

trait BlockHashChain extends BlockHashPool {

  implicit def config: PlatformConfig

  protected def root: BlockHashChain.Root

  protected val blockHashesTable: HashMap[Keccak256, BlockHashChain.TreeNode] = HashMap.empty
  protected val tips: HashSet[Keccak256]                                      = HashSet.empty
  protected val confirmedHashes: ArrayBuffer[BlockHashChain.TreeNode]         = ArrayBuffer.empty

  protected def getNode(hash: Keccak256): BlockHashChain.TreeNode = blockHashesTable(hash)

  protected def addNode(node: BlockHashChain.TreeNode): Unit = {
    assert(node.isLeaf)

    val hash = node.blockHash
    blockHashesTable += hash -> node
    node match {
      case _: BlockHashChain.Root =>
        tips.add(hash)
        ()
      case n: BlockHashChain.Node =>
        tips.remove(n.parent.blockHash)
        tips.add(hash)
        ()
    }

    pruneDueto(node)
    confirmHashes()
    ()
  }

  protected def addHash(hash: Keccak256, parent: BlockHashChain.TreeNode, weight: Int): Unit = {
    val newNode = BlockHashChain.Node(hash, parent, parent.height + 1, weight)
    parent.successors += newNode
    addNode(newNode)
  }

  protected def removeNode(node: BlockHashChain.TreeNode): Unit = {
    val hash = node.blockHash
    blockHashesTable.remove(hash)
    if (tips.contains(hash)) tips.remove(hash)
    ()
  }

  private def pruneDueto(newNode: BlockHashChain.TreeNode): Boolean = {
    val toCut = tips.filter { key =>
      val tipNode = blockHashesTable(key)
      newNode.height >= tipNode.height + config.blockConfirmNum
    }

    toCut.foreach { key =>
      val node = blockHashesTable(key)
      pruneBranchFrom(node)
    }
    toCut.nonEmpty
  }

  @tailrec
  private def pruneBranchFrom(node: BlockHashChain.TreeNode): Unit = {
    removeNode(node)

    node match {
      case n: BlockHashChain.Node =>
        val parent = n.parent
        if (parent.successors.size == 1) {
          pruneBranchFrom(parent)
        }
      case _: BlockHashChain.Root => ()
    }
  }

  private def confirmHashes(): Unit = {
    val oldestTip = tips.view.map(blockHashesTable).minBy(_.height)

    @tailrec
    def iter(): Unit = {
      if (confirmedHashes.isEmpty && root.successors.size == 1) {
        confirmedHashes.append(root)
        iter()
      } else if (confirmedHashes.nonEmpty) {
        val lastConfirmed = confirmedHashes.last
        if (lastConfirmed.successors.size == 1 && (oldestTip.height >= lastConfirmed.height + config.blockConfirmNum)) {
          confirmedHashes.append(lastConfirmed.successors.head)
          iter()
        }
      }
    }

    iter()
  }

  def numHashes: Int = blockHashesTable.size

  def maxWeight: Int = blockHashesTable.values.map(_.weight).max

  def maxHeight: Int = blockHashesTable.values.map(_.height).max

  def contains(hash: Keccak256): Boolean = blockHashesTable.contains(hash)

  def getHeight(hash: Keccak256): Int = {
    assert(contains(hash))
    blockHashesTable(hash).height
  }

  def getWeight(hash: Keccak256): Int = {
    assert(contains(hash))
    blockHashesTable(hash).weight
  }

  def isTip(hash: Keccak256): Boolean = {
    tips.contains(hash)
  }

  def getHashesAfter(locator: Keccak256): AVector[Keccak256] = {
    blockHashesTable.get(locator) match {
      case Some(node) => getHashesAfter(node)
      case None       => AVector.empty
    }
  }

  private def getHashesAfter(node: BlockHashChain.TreeNode): AVector[Keccak256] = {
    if (node.isLeaf) AVector(node.blockHash)
    else {
      val buffer = node.successors.foldLeft(ArrayBuffer[Keccak256](node.blockHash)) {
        case (blocks, successor) =>
          blocks ++ getHashesAfter(successor).toIterable
      }
      AVector.from(buffer)
    }
  }

  def getBestTip: Keccak256 = {
    getAllTips.map(blockHashesTable.apply).maxBy(_.height).blockHash
  }

  def getAllTips: AVector[Keccak256] = {
    AVector.from(tips)
  }

  def getBlockHashSlice(hash: Keccak256): AVector[Keccak256] = {
    blockHashesTable.get(hash) match {
      case Some(node) =>
        getChain(node).map(_.blockHash)
      case None =>
        AVector.empty
    }
  }

  private def getChain(node: BlockHashChain.TreeNode): AVector[BlockHashChain.TreeNode] = {
    @tailrec
    def iter(acc: AVector[BlockHashChain.TreeNode],
             current: BlockHashChain.TreeNode): AVector[BlockHashChain.TreeNode] = {
      current match {
        case n: BlockHashChain.Root => acc :+ n
        case n: BlockHashChain.Node => iter(acc :+ current, n.parent)
      }
    }
    iter(AVector.empty, node).reverse
  }

  def getAllBlockHashes: Iterable[Keccak256] = {
    blockHashesTable.values.map(_.blockHash)
  }

  def isBefore(hash1: Keccak256, hash2: Keccak256): Boolean = {
    assert(blockHashesTable.contains(hash1) && blockHashesTable.contains(hash2))
    val node1 = blockHashesTable(hash1)
    val node2 = blockHashesTable(hash2)
    isBefore(node1, node2)
  }

  def getPredecessor(hash: Keccak256, height: Int): Keccak256 = {
    val node = getNode(hash)
    getPredecessor(node, height).blockHash
  }

  private def getPredecessor(node: BlockHashChain.TreeNode,
                             height: Int): BlockHashChain.TreeNode = {
    @tailrec
    def iter(current: BlockHashChain.TreeNode): BlockHashChain.TreeNode = {
      assert(current.height >= height && height >= root.height)
      current match {
        case n: BlockHashChain.Node =>
          if (n.height == height) {
            current
          } else {
            iter(n.parent)
          }
        case _: BlockHashChain.Root =>
          assert(height == root.height)
          current
      }
    }

    if (height < confirmedHashes.size) {
      val targetHash = confirmedHashes(height).blockHash
      getNode(targetHash)
    } else {
      iter(node)
    }
  }

  private def isBefore(node1: BlockHashChain.TreeNode, node2: BlockHashChain.TreeNode): Boolean = {
    val height1 = node1.height
    val height2 = node2.height
    if (height1 < height2) {
      val node1Infer = getPredecessor(node2, node1.height)
      node1Infer.eq(node1)
    } else if (height1 == height2) {
      node1.eq(node2)
    } else false
  }

  def getConfirmedHash(height: Int): Option[Keccak256] = {
    assert(height >= 0)
    if (height < confirmedHashes.size) {
      Some(confirmedHashes(height).blockHash)
    } else None
  }
}

object BlockHashChain {

  sealed trait TreeNode {
    val blockHash: Keccak256
    val successors: ArrayBuffer[Node]
    val height: Int
    val weight: Int

    def isRoot: Boolean
    def isLeaf: Boolean = successors.isEmpty
  }

  case class Root(
      blockHash: Keccak256,
      successors: ArrayBuffer[Node],
      height: Int,
      weight: Int
  ) extends TreeNode {
    override def isRoot: Boolean = true
  }

  object Root {
    def apply(blockHash: Keccak256, height: Int, weight: Int): Root =
      Root(blockHash, ArrayBuffer.empty, height, weight)
  }

  case class Node(
      blockHash: Keccak256,
      parent: TreeNode,
      successors: ArrayBuffer[Node],
      height: Int,
      weight: Int
  ) extends TreeNode {
    def isRoot: Boolean = false
  }

  object Node {
    def apply(blockHash: Keccak256, parent: TreeNode, height: Int, weight: Int): Node = {
      new Node(blockHash, parent, ArrayBuffer.empty, height, weight)
    }
  }
}
