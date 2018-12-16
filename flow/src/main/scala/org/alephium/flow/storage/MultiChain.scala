package org.alephium.flow.storage

import org.alephium.crypto.Keccak256
import org.alephium.flow.PlatformConfig
import org.alephium.protocol.model.{Block, BlockHeader, ChainIndex, Transaction}
import org.alephium.util.{AVector, Hex}

import scala.reflect.ClassTag

// scalastyle:off number.of.methods
trait MultiChain extends BlockPool with BlockHeaderPool {
  implicit def config: PlatformConfig

  def groups: Int

  protected def aggregate[T: ClassTag](f: BlockHashPool => T)(op: (T, T) => T): T

  def numHashes: Int = aggregate(_.numHashes)(_ + _)

  def maxWeight: Int = aggregate(_.maxWeight)(math.max)

  def maxHeight: Int = aggregate(_.maxHeight)(math.max)

  /* BlockHash apis */

  def contains(hash: Keccak256): Boolean = {
    val index = ChainIndex.fromHash(hash)
    val chain = getHashChain(index)
    chain.contains(hash)
  }

  def getIndex(hash: Keccak256): ChainIndex = {
    ChainIndex.fromHash(hash)
  }

  protected def getHashChain(from: Int, to: Int): BlockHashChain

  def getHashChain(chainIndex: ChainIndex): BlockHashChain = {
    getHashChain(chainIndex.from, chainIndex.to)
  }

  def getHashChain(hash: Keccak256): BlockHashChain = {
    val index = ChainIndex.fromHash(hash)
    getHashChain(index.from, index.to)
  }

  def isTip(hash: Keccak256): Boolean = {
    getHashChain(hash).isTip(hash)
  }

  def getHashesAfter(locator: Keccak256): AVector[Keccak256] =
    getHashChain(locator).getHashesAfter(locator)

  def getHeight(hash: Keccak256): Int = {
    getHashChain(hash).getHeight(hash)
  }

  def getWeight(hash: Keccak256): Int = {
    getHashChain(hash).getWeight(hash)
  }

  def getAllBlockHashes: Iterable[Keccak256] = aggregate(_.getAllBlockHashes)(_ ++ _)

  def getBlockHashSlice(hash: Keccak256): AVector[Keccak256] =
    getHashChain(hash).getBlockHashSlice(hash)

  /* BlockHeader apis */

  protected def getHeaderChain(from: Int, to: Int): BlockHeaderPool

  def getHeaderChain(chainIndex: ChainIndex): BlockHeaderPool = {
    getHeaderChain(chainIndex.from, chainIndex.to)
  }

  def getHeaderChain(header: BlockHeader): BlockHeaderPool = {
    getHeaderChain(header.chainIndex)
  }

  def getHeaderChain(hash: Keccak256): BlockHeaderPool = {
    getHeaderChain(ChainIndex.fromHash(hash))
  }

  def getBlockHeader(hash: Keccak256): BlockHeader =
    getHeaderChain(hash).getBlockHeader(hash)

  def getHeadersAfter(locator: Keccak256): AVector[BlockHeader] =
    getHeaderChain(locator).getHeadersAfter(locator)

  def getHeadersAfter(locators: AVector[Keccak256]): AVector[BlockHeader] =
    locators.flatMap(getHeadersAfter)

  def add(header: BlockHeader): AddBlockHeaderResult

  /* BlockChain apis */

  protected def getBlockChain(from: Int, to: Int): BlockChain

  def getBlockChain(chainIndex: ChainIndex): BlockChain = {
    getBlockChain(chainIndex.from, chainIndex.to)
  }

  def getBlockChain(block: Block): BlockChain = getBlockChain(block.chainIndex)

  def getBlockChain(hash: Keccak256): BlockChain = {
    getBlockChain(ChainIndex.fromHash(hash))
  }

  def getBlock(hash: Keccak256): Block = {
    getBlockChain(hash).getBlock(hash)
  }

  def getBlocksAfter(locator: Keccak256): AVector[Block] = {
    getBlockChain(locator).getBlocksAfter(locator)
  }

  def getBlocksAfter(locators: AVector[Keccak256]): AVector[Block] =
    locators.flatMap(getBlocksAfter)

  def add(block: Block): AddBlockResult

  def getTransaction(hash: Keccak256): Transaction = ???

  def getInfo: String = {
    val infos = for {
      i <- 0 until groups
      j <- 0 until groups
    } yield s"($i, $j): ${getHashChain(i, j).maxHeight}/${getHashChain(i, j).numHashes - 1}"
    infos.mkString("; ")
  }

  def getBlockInfo: String = {
    val blocks = for {
      i    <- 0 until groups
      j    <- 0 until groups
      hash <- getHashChain(i, j).getAllBlockHashes
    } yield toJson(i, j, hash)
    val blocksJson = blocks.sorted.mkString("[", ",", "]")
    val heights = for {
      i <- 0 until groups
      j <- 0 until groups
    } yield s"""{"chainFrom":$i,"chainTo":$j,"height":${getHashChain(i, j).maxHeight}}"""
    val heightsJson = heights.mkString("[", ",", "]")
    s"""{"blocks":$blocksJson,"heights":$heightsJson}"""
  }

  def toJson(from: Int, to: Int, blockHash: Keccak256): String = {
    val header    = getBlockHeader(blockHash)
    val timestamp = header.timestamp
    val height    = getWeight(blockHash)
    val hash      = header.shortHex
    val deps = header.blockDeps
      .map(h => "\"" + Hex.toHexString(h.bytes).take(16) + "\"")
      .mkString("[", ",", "]")
    s"""{"timestamp":$timestamp,"chainFrom":$from,"chainTo":$to,"height":"$height","hash":"$hash","deps":$deps}"""
  }
}
