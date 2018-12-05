package org.alephium.util

import akka.util.ByteString
import org.alephium.crypto.Keccak256
import org.alephium.serde.Serde
import org.alephium.serde._

class WithKeccak256[T: Serde] extends Bytes { self: T =>
  override lazy val bytes: ByteString = serialize[T](this)
  lazy val hash: Keccak256            = Keccak256.hash(bytes)
}
