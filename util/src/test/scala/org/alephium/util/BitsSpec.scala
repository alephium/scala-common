package org.alephium.util

import java.nio.ByteBuffer

import scala.language.postfixOps

class BitsSpec extends AlephiumSpec {
  it should "convert byte into positive int" in {
    forAll { input: Byte =>
      val output = Bits.toPosInt(input)
      output >= 0 is true
      output.toByte is input
    }
  }

  it should "convert int to correct bytes" in {
    forAll { input: Int =>
      val output   = Bits.toBytes(input)
      val expected = ByteBuffer.allocate(4).putInt(input).array()
      output is AVector.unsafe(expected)
    }
  }

  it should "compute correct xor byte for int" in {
    forAll { input: Int =>
      val output   = Bits.xorByte(input)
      val bytes    = Bits.toBytes(input)
      val expected = bytes.tail.fold(bytes.head)(_ ^ _ toByte)
      output is expected
    }
  }
}
