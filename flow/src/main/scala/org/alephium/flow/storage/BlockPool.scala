package org.alephium.flow.storage

import org.alephium.crypto.{ED25519PublicKey, Keccak256}
import org.alephium.protocol.model.{Block, Transaction, TxInput}
import org.alephium.util.AVector

trait BlockPool extends BlockHashPool {

  def numTransactions: Int

  def contains(block: Block): Boolean = contains(block.hash)

  // Assuming the hash is in the pool
  def getBlock(hash: Keccak256): Block

  def getBlocks(locators: AVector[Keccak256]): AVector[Block] = {
    val blocks = locators.map(getBlocks)
    blocks.fold(AVector.empty[Block]) {
      case (acc, newBlocks) =>
        val toAdd = newBlocks.filterNot(acc.contains)
        acc ++ toAdd
    }
  }

  def getBlocks(locator: Keccak256): AVector[Block]

  def getHeight(block: Block): Int = getHeight(block.hash)

  def getWeight(block: Block): Int = getWeight(block.hash)

  // TODO: use ChainSlice instead of AVector[Block]
  def getBlockSlice(hash: Keccak256): AVector[Block]
  def getBlockSlice(block: Block): AVector[Block] = getBlockSlice(block.hash)

  def isTip(block: Block): Boolean = isTip(block.hash)

  def getBestBlockChain: AVector[Block] = getBlockSlice(getBestTip)

  def getAllBlocks: Iterable[Block]

  // TODO: have a safe version
  def getTransaction(hash: Keccak256): Transaction

  def getTxInputValue(transaction: Transaction, address: ED25519PublicKey): BigInt = {
    transaction.unsigned.inputs.sumBy {
      case TxInput(txHash, outputIndex) =>
        val tx       = getTransaction(txHash)
        val txOutput = tx.unsigned.outputs(outputIndex)
        if (txOutput.publicKey == address) txOutput.value else BigInt(0)
    }
  }

  def getTxOutputValue(transaction: Transaction, address: ED25519PublicKey): BigInt = {
    transaction.unsigned.outputs.filter(_.publicKey == address).sumBy(_.value)
  }

  def getBalance(transaction: Transaction, address: ED25519PublicKey): BigInt = {
    getTxOutputValue(transaction, address) - getTxInputValue(transaction, address)
  }

  def getBalance(block: Block, address: ED25519PublicKey): BigInt = {
    block.transactions.sumBy(transaction => getBalance(transaction, address))
  }

  // calculated from best chain
  def getBalance(address: ED25519PublicKey): (Keccak256, BigInt) = {
    val bestTip = getBestTip
    val balance = getBestBlockChain.sumBy(block => getBalance(block, address))
    (bestTip, balance)
  }
}
