package org.alephium.serde

import java.net.InetSocketAddress

import akka.util.ByteString
import org.scalacheck.Gen
import org.scalatest.EitherValues._

import org.alephium.serde.Serde.{ByteSerde, IntSerde, LongSerde}
import org.alephium.util.{AlephiumSpec, AVector, U32, U64}

class SerdeSpec extends AlephiumSpec {

  def checkException[T](serde: FixedSizeSerde[T]): Unit = {
    it should "throw correct exceptions when error occurs" in {
      forAll { inputs: Array[Byte] =>
        lazy val exception = serde.deserialize(ByteString(inputs)).left.value
        if (inputs.length < serde.serdeSize) {
          exception is a[SerdeError.NotEnoughBytes]
        } else if (inputs.length > serde.serdeSize) {
          exception is a[SerdeError.WrongFormat]
        }
      }
    }
  }

  "Serde for Byte" should "serialize Byte into 1 byte" in {
    byteSerde.asInstanceOf[FixedSizeSerde[Byte]].serdeSize is 1
  }

  it should "serde correctly" in {
    forAll { b: Byte =>
      val bb = deserialize[Byte](serialize(b))
      bb isE b
    }

    forAll { b: Byte =>
      val input  = ByteString(b)
      val output = serialize(deserialize[Byte](input).toOption.get)
      output is input
    }
  }

  checkException(ByteSerde)

  "Serde for Int" should "serialize int into 4 bytes" in {
    intSerde.asInstanceOf[FixedSizeSerde[Int]].serdeSize is 4
  }

  it should "serde correctly" in {
    forAll { n: Int =>
      val nn = deserialize[Int](serialize(n))
      nn isE n
    }

    forAll { (a: Byte, b: Byte, c: Byte, d: Byte) =>
      val input  = ByteString(a, b, c, d)
      val output = serialize(deserialize[Int](input).toOption.get)
      output is input
    }
  }

  checkException(IntSerde)

  "Serde for Long" should "serialize long into 8 bytes" in {
    longSerde.asInstanceOf[FixedSizeSerde[Long]].serdeSize is 8
  }

  it should "serde correctly" in {
    forAll { n: Long =>
      val nn = deserialize[Long](serialize(n))
      nn isE n
    }
  }

  checkException(LongSerde)

  "Serde for U32" should "serde correct" in {
    forAll { n: U32 =>
      deserialize[U32](serialize(n)) isE n
    }
  }

  "Serde for U64" should "serde correct" in {
    forAll { n: U64 =>
      deserialize[U64](serialize(n)) isE n
    }
  }

  "Serde for ByteString" should "serialize correctly" in {
    deserialize[ByteString](serialize(ByteString.empty)) isE ByteString.empty
    forAll { n: Int =>
      val bs = ByteString.fromInts(n)
      deserialize[ByteString](serialize(bs)) isE bs
    }
  }

  "Serde for String" should "serialize correctly" in {
    forAll { s: String =>
      deserialize[String](serialize(s)) isE s
    }
  }

  "Serde for BigInt" should "serde correctly" in {
    forAll { n: Long =>
      val bn  = BigInt(n)
      val bnn = deserialize[BigInt](serialize(bn)).toOption.get
      bnn is bn
    }
  }

  "Serde for fixed size sequence" should "serde correctly" in {
    forAll { input: AVector[Byte] =>
      {
        val serde  = Serde.fixedSizeSerde(input.length, byteSerde)
        val output = serde.deserialize(serde.serialize(input)).toOption.get
        output is input
      }
      {
        val serde     = Serde.fixedSizeSerde(input.length + 1, byteSerde)
        val exception = serde.deserialize(ByteString(input.toArray)).left.value
        exception is a[SerdeError.NotEnoughBytes]
      }

      if (input.nonEmpty) {
        val serde     = Serde.fixedSizeSerde(input.length - 1, byteSerde)
        val exception = serde.deserialize(ByteString(input.toArray)).left.value
        exception is a[SerdeError.WrongFormat]
      }
    }
  }

  "Serde for sequence" should "serde correctly" in {
    forAll { input: AVector[Byte] =>
      val serde  = Serde.dynamicSizeSerde(byteSerde)
      val output = serde.deserialize(serde.serialize(input)).toOption.get
      output is input
    }
  }

  "Serde for option" should "work" in {
    forAll(Gen.option(Gen.choose(0, Int.MaxValue))) { input =>
      deserialize[Option[Int]](serialize(input)) isE input
    }
  }

  "Serde for either" should "work" in {
    forAll { (left: Int, right: Long) =>
      val input1: Either[Int, Long] = Left(left)
      deserialize[Either[Int, Long]](serialize(input1)) isE input1
      val input2: Either[Int, Long] = Right(right)
      deserialize[Either[Int, Long]](serialize(input2)) isE input2
    }
  }

  case class Test1(x: Int)
  object Test1 {
    implicit val serde: Serde[Test1] = Serde.forProduct1(apply, t => t.x)
  }

  case class Test2(x: Int, y: Int)
  object Test2 {
    implicit val serde: Serde[Test2] = Serde.forProduct2(apply, t => (t.x, t.y))
  }

  case class Test3(x: Int, y: Int, z: Int)
  object Test3 {
    implicit val serde: Serde[Test3] = Serde.forProduct3(apply, t => (t.x, t.y, t.z))
  }

  "Serde for case class" should "serde one field correctly" in {
    forAll { x: Int =>
      val input  = Test1(x)
      val output = deserialize[Test1](serialize(input))
      output isE input
    }
  }

  it should "serde two fields correctly" in {
    forAll { (x: Int, y: Int) =>
      val input  = Test2(x, y)
      val output = deserialize[Test2](serialize(input))
      output isE input
    }
  }

  it should "serde three fields correctly" in {
    forAll { (x: Int, y: Int, z: Int) =>
      val input  = Test3(x, y, z)
      val output = deserialize[Test3](serialize(input))
      output isE input
    }
  }

  "Serde for address" should "serde correctly" in {
    val address0 = new InetSocketAddress("127.0.0.1", 9000)
    val output0  = deserialize[InetSocketAddress](serialize(address0))
    output0 isE address0

    val address1 = new InetSocketAddress("2001:0db8:85a3:0000:0000:8a2e:0370:7334", 9000)
    val output1  = deserialize[InetSocketAddress](serialize(address1))
    output1 isE address1
  }

  it should "fail for address based on host name" in {
    val address = serialize("localhost") ++ serialize("9000")
    deserialize[InetSocketAddress](address).left.value is a[SerdeError.WrongFormat]
  }
}
