package org.alephium

import org.bouncycastle.math.ec.rfc8032.{Ed25519 => bcEd25519}

package object crypto {
  val ed25519PubLength: Int = bcEd25519.PUBLIC_KEY_SIZE
  val ed25519PriLength: Int = bcEd25519.SECRET_KEY_SIZE
  val ed25519SigLength: Int = bcEd25519.SIGNATURE_SIZE
  val sha256Length: Int     = 32
  val keccak256Length: Int  = 32
}
