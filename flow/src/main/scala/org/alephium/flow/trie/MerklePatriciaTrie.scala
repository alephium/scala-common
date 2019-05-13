package org.alephium.flow.trie

import akka.util.ByteString
import org.alephium.crypto.Keccak256
import org.alephium.flow.io.{IOError, IOResult, KeyValueStorage}
import org.alephium.serde._
import org.alephium.util.AVector

object MerklePatriciaTrie {

  def getHighNibble(byte: Byte): Byte = {
    ((byte & 0xF0) >> 4).toByte
  }

  def getLowNibble(byte: Byte): Byte = {
    (byte & 0x0F).toByte
  }

  /* branch [encodedPath, v0, ..., v15]
   * leaf   [encodedPath, data]
   * the length of encodedPath varies from 0 to 64
   * encoding flag byte = path length (7 bits) ++ type (1 bit)
   */
  sealed trait Node {
    lazy val serialized: ByteString = Node.SerdeNode.serialize(this)
    lazy val hash: Keccak256        = Keccak256.hash(serialized)

    def path: ByteString

    def preExtend(prefix: ByteString): Node

    def preCut(n: Int): Node
  }
  case class BranchNode(path: ByteString, children: AVector[Option[Keccak256]]) extends Node {
    def replace(nibble: Int, childHash: Keccak256): BranchNode = {
      BranchNode(path, children.replace(nibble, Some(childHash)))
    }
    def preExtend(prefix: ByteString): BranchNode = {
      BranchNode(prefix ++ path, children)
    }
    def preCut(n: Int): BranchNode = {
      assert(n < path.length)
      BranchNode(path.drop(n), children)
    }
  }
  case class LeafNode(path: ByteString, data: ByteString) extends Node {
    def preExtend(prefix: ByteString): Node = {
      LeafNode(prefix ++ path, data)
    }
    def preCut(n: Int): LeafNode = {
      assert(n < path.length)
      LeafNode(path.drop(n), data)
    }
  }

  object Node {
    def branch(path: ByteString,
               nibble1: Int,
               node1: Node,
               nibble2: Int,
               node2: Node): BranchNode = {
      assert(nibble1 != nibble2)
      val array = Array.fill[Option[Keccak256]](16)(None)
      array(nibble1) = Some(node1.hash)
      array(nibble2) = Some(node2.hash)
      BranchNode(path, AVector.unsafe(array))
    }

    implicit object SerdeNode extends Serde[Node] {
      def encodeFlag(length: Int, isLeaf: Boolean): Byte = {
        assert(length >= 0 && length + (if (isLeaf) 0 else 1) <= 64)
        if (isLeaf) (length | (1 << 7)).toByte
        else length.toByte
      }

      def encodeNibbles(path: ByteString): ByteString = {
        val length = (path.length + 1) / 2
        val nibbles = Array.tabulate(length) { i =>
          var twoNibbles = path(2 * i) << 4
          if (i < length - 1 || path.length % 2 == 0) {
            twoNibbles += path(2 * i + 1)
          }
          twoNibbles.toByte
        }
        ByteString(nibbles)
      }

      def decodeFlag(flag: Byte): (Int, Boolean) = {
        (flag & ((1 << 7) - 1), (flag & (1 << 7)) != 0)
      }

      def decodeNibbles(nibbles: ByteString, length: Int): ByteString = {
        assert(nibbles.length * 2 >= length && nibbles.length * 2 <= length + 1)
        val bytes = Array.tabulate(length) { i =>
          val byte = nibbles(i / 2)
          if (i % 2 == 0) getHighNibble(byte) else getLowNibble(byte)
        }
        ByteString(bytes)
      }

      val childrenSerde: Serde[AVector[Option[Keccak256]]] = {
        val bsOptSerde: Serde[Option[Keccak256]] = optionSerde
        Serde.fixedSizeBytesSerde(16, bsOptSerde)
      }

      override def serialize(node: Node): ByteString = node match {
        case n: BranchNode =>
          val flag     = SerdeNode.encodeFlag(n.path.length, isLeaf = false)
          val nibbles  = encodeNibbles(n.path)
          val children = childrenSerde.serialize(n.children)
          (flag +: nibbles) ++ children
        case n: LeafNode =>
          val flag    = SerdeNode.encodeFlag(n.path.length, isLeaf = true)
          val nibbles = encodeNibbles(n.path)
          (flag +: nibbles) ++ bytestringSerde.serialize(n.data)
      }

      override def _deserialize(input: ByteString): Either[SerdeError, (Node, ByteString)] = {
        byteSerde._deserialize(input).flatMap {
          case (flag, rest) =>
            val (length, isLeaf) = SerdeNode.decodeFlag(flag)
            val (left, right)    = rest.splitAt((length + 1) / 2)
            val path             = decodeNibbles(left, length)
            if (isLeaf) {
              bytestringSerde._deserialize(right).map {
                case (data, rest1) => (LeafNode(path, data), rest1)
              }
            } else {
              childrenSerde._deserialize(right).map {
                case (children, rest1) => (BranchNode(path, children), rest1)
              }
            }
        }
      }
    }
  }

  case class TrieDBActions(toDelete: AVector[Node], toAdd: AVector[Node])
  case class TrieUpdateActions(newNodeOpt: Option[Node],
                               toDelete: AVector[Keccak256],
                               toAdd: AVector[Node])
  case class MPTException(message: String) extends Exception(message)
  object MPTException {
    def keyNotFound(action: String): MPTException = MPTException("Key not found in " ++ action)
  }

  def hash2Nibbles(hash: Keccak256): ByteString = {
    hash.bytes.flatMap { byte =>
      ByteString(getHighNibble(byte), getLowNibble(byte))
    }
  }
}

// TODO: batch mode
class MerklePatriciaTrie[K: Serde, V: Serde](var rootHash: Keccak256, storage: KeyValueStorage) {
  import MerklePatriciaTrie.{BranchNode, LeafNode, MPTException, Node, TrieUpdateActions}

  def getOpt(key: Keccak256): IOResult[Option[ByteString]] = {
    val nibbles = MerklePatriciaTrie.hash2Nibbles(key)
    getOpt(rootHash, nibbles)
  }

  def getOpt(hash: Keccak256, nibbles: ByteString): IOResult[Option[ByteString]] = {
    getNode(hash) flatMap {
      case BranchNode(path, children) =>
        if (path == nibbles.take(path.length)) {
          children(nibbles(0) & 0xFF) match {
            case Some(childHash) =>
              getOpt(childHash, nibbles.tail)
            case None =>
              Right(None)
          }
        } else Right(None)
      case LeafNode(path, data) =>
        if (path == nibbles) Right(Some(data)) else Right(None)
    }
  }

  def getNode(hash: Keccak256): IOResult[Node] = storage.get(hash.bytes).flatMap { data =>
    deserialize[Node](data) match {
      case Left(e)  => Left(IOError.Serde(e))
      case Right(n) => Right(n)
    }
  }

  def persist(result: TrieUpdateActions): IOResult[Unit] = {
    result.toAdd.foreachF { node =>
      storage.remove(node.hash.bytes, node.serialized)
    }
  }

  // assume that the key exists in the trie
  def remove(key: Keccak256): IOResult[Unit] = {
    val nibbles = MerklePatriciaTrie.hash2Nibbles(key)
    for {
      result <- remove(rootHash, nibbles)
      _      <- persist(result)
    } yield {
      result.newNodeOpt.foreach(root => rootHash = root.hash)
    }
  }

  def remove(hash: Keccak256, nibbles: ByteString): IOResult[TrieUpdateActions] = {
    getNode(hash) flatMap {
      case node @ BranchNode(path, children) =>
        val nibble   = nibbles(path.length) & 0xFF
        val childOpt = children(nibble)
        if (path == nibbles.take(path.length) && childOpt.nonEmpty) {
          val restNibbles = nibbles.drop(path.length + 1)
          val childHash   = childOpt.get
          remove(childHash, restNibbles) flatMap { result =>
            handleChildUpdateResult(hash, node, nibble, result)
          }
        } else {
          Left(IOError.Other(MPTException.keyNotFound("removal")))
        }
      case leaf @ LeafNode(path, _) =>
        if (path == nibbles) {
          Right(TrieUpdateActions(None, AVector(leaf.hash), AVector.empty))
        } else {
          Left(IOError.Other(MPTException.keyNotFound("removal")))
        }
    }
  }

  def handleChildUpdateResult(branchHash: Keccak256,
                              branchNode: BranchNode,
                              nibble: Int,
                              result: TrieUpdateActions): IOResult[TrieUpdateActions] = {
    val children     = branchNode.children
    val childOptHash = result.newNodeOpt.map(_.hash)
    val newChildren  = children.replace(nibble, childOptHash)
    if (childOptHash.isEmpty && newChildren.map(_.fold(0)(_ => 1)).sum == 1) {
      val onlyChildIndex = children.indexWhere(_.nonEmpty)
      val onlyChildHash  = children(onlyChildIndex).get
      getNode(onlyChildHash) map { onlyChild =>
        val newNode  = onlyChild.preExtend(branchNode.path :+ onlyChildIndex.toByte)
        val toDelete = result.toDelete ++ AVector(onlyChildHash, branchHash)
        TrieUpdateActions(Some(newNode), toDelete, result.toAdd :+ newNode)
      }
    } else {
      val oldChildOptHash = children(nibble)
      if (oldChildOptHash != childOptHash) {
        val newBranchNode = BranchNode(branchNode.path, newChildren)
        val toDelete =
          if (oldChildOptHash.isEmpty) result.toDelete :+ branchHash
          else result.toDelete ++ AVector(oldChildOptHash.get, branchHash)
        Right(TrieUpdateActions(Some(newBranchNode), toDelete, result.toAdd :+ newBranchNode))
      } else
        Right(TrieUpdateActions(None, result.toDelete, result.toAdd))
    }
  }

  def put(key: Keccak256, value: ByteString): IOResult[Unit] = {
    val nibbles = MerklePatriciaTrie.hash2Nibbles(key)
    for {
      result <- put(rootHash, nibbles, value)
      _      <- persist(result)
    } yield {
      result.newNodeOpt.foreach(root => rootHash = root.hash)
    }
  }

  def put(hash: Keccak256, nibbles: ByteString, value: ByteString): IOResult[TrieUpdateActions] = {
    getNode(hash) flatMap { node =>
      val path = node.path
      assert(path.length <= nibbles.length)
      val branchIndex = node.path.indices.indexWhere(i => nibbles(i) != path(i))
      if (branchIndex == -1) {
        node match {
          case branchNode @ BranchNode(_, children) =>
            val nibble       = nibbles(path.length) & 0xFF
            val nibblesRight = nibbles.drop(path.length + 1)
            children(nibble) match {
              case Some(childHash) =>
                put(childHash, nibblesRight, value) flatMap { result =>
                  handleChildUpdateResult(hash, branchNode, nibble, result)
                }
              case None =>
                val newLeaf   = LeafNode(nibblesRight, value)
                val newBranch = branchNode.replace(nibble, newLeaf.hash)
                Right(
                  TrieUpdateActions(Some(newBranch), AVector(hash), AVector(newBranch, newLeaf)))
            }
          case leaf: LeafNode =>
            val newLeaf = LeafNode(path, value)
            Right(TrieUpdateActions(Some(newLeaf), AVector(leaf.hash), AVector(newLeaf)))
        }
      } else {
        branch(hash, node, branchIndex, nibbles, value)
      }
    }
  }

  def branch(hash: Keccak256,
             node: Node,
             branchIndex: Int,
             nibbles: ByteString,
             value: ByteString): IOResult[TrieUpdateActions] = {
    val path         = node.path
    val prefix       = path.take(branchIndex)
    val nibble1      = path(branchIndex) & 0xFF
    val node1        = node.preCut(branchIndex + 1)
    val nibblesRight = nibbles.drop(branchIndex + 1)
    val nibble2      = nibbles(branchIndex) & 0xFF
    val newLeaf      = LeafNode(nibblesRight, value)
    val branchNode   = Node.branch(prefix, nibble1, node1, nibble2, newLeaf)

    val toAdd = AVector[Node](branchNode, node1, newLeaf)
    Right(TrieUpdateActions(Some(branchNode), AVector(hash), toAdd))
  }
}
