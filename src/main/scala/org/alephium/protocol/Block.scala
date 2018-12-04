package org.alephium.protocol

import org.alephium.crypto.Keccak256
import org.alephium.serde.Serde

case class Block(blockHeader: BlockHeader, transactions: Seq[Transaction])

object Block {
  implicit val serde: Serde[Block] = Serde.forProduct2(apply, b => (b.blockHeader, b.transactions))

  def from(blockDeps: Seq[Keccak256], transactions: Seq[Transaction]): Block = {
    // TODO: validate all the block dependencies; the first block dep should be previous block in the same chain
    val txsHash     = Keccak256.hash(transactions)
    val timestamp   = System.currentTimeMillis()
    val blockHeader = BlockHeader(blockDeps, txsHash, timestamp)
    Block(blockHeader, transactions)
  }
}
