package org.alephium.protocol.model

import org.alephium.util.AlephiumSpec

class GroupIndexSpec extends AlephiumSpec with ConfigFixture {
  behavior of "GroupIndex"

  it should "equalize same values" in {
    forAll(groupGen) { n =>
      val groupIndex1 = GroupIndex(n)(config)
      val groupIndex2 = GroupIndex(n)(config)
      groupIndex1 is groupIndex2
    }

    val index1 = GroupIndex.unsafe(999999)
    val index2 = GroupIndex.unsafe(999999)
    index1 is index2
  }
}