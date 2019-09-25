package org.alephium.rpc.model

import org.alephium.util.AVector

sealed abstract class Module(val name: String)

object Module {
  object BlockFlow  extends Module("blockflow")
  object Clique     extends Module("clique")
  object Mining     extends Module("mining")

  val values: AVector[Module] = AVector(BlockFlow, Clique, Mining)

  def fromString(str: String): Option[Module] = {
    val index = Module.values.indexWhere(_.name == str)
    if (index < -1) { None }
    else {
      Some(values(index))
    }
  }
}
