package org.alephium.flow

import java.time.Duration
import java.io.File
import java.nio.file.{Path, Paths}

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging
import org.alephium.protocol.config.ConsensusConfig
import org.alephium.protocol.model.{Block, ChainIndex, GroupIndex, PeerId}
import org.alephium.util.{AVector, Files}

object PlatformConfig extends StrictLogging {
  val rootPath = Paths.get(System.getProperty("user.home"), ".alephium")

  def resolve(path: String): Path = rootPath.resolve(path)

  def getNoncesFilePath(groups: Int): Path = resolve(s"nonces-$groups.conf")

  def getNoncesFile(groups: Int): File = {
    val path = getNoncesFilePath(groups)
    val file = path.toFile
    if (!file.exists()) {
      logger.error(s"No nonces file exists: $path")
      System.exit(1)
    }
    file
  }

  def getUserFile(): File = {
    val directory = rootPath.toFile
    if (!directory.exists) directory.mkdir()

    val env = System.getenv("ALEPHIUM_ENV")
    val filename = env match {
      case "test"  => "user_test.conf"
      case "debug" => "user_debug.conf"
      case _       => "user.conf"
    }
    val path = resolve(filename)
    logger.info(s"Under environment $env, using conf file $path")

    val file = path.toFile
    if (!file.exists) Files.copyFromResource(s"/$filename.tmpl", path)
    file
  }

  def loadUserConfig(): Config = {
    ConfigFactory.parseFile(getUserFile).resolve()
  }

  def loadNoncesConfig(groups: Int): Config = {
    ConfigFactory.parseFile(getNoncesFile(groups)).resolve()
  }

  def load(withNonces: Boolean): PlatformConfig = {
    val user = loadUserConfig()
    val all = if (withNonces) {
      val groups = user.getInt("alephium.groups")
      val nonces = loadNoncesConfig(groups)
      user.withFallback(nonces)
    } else user
    new PlatformConfig(all)
  }

  object Default {
    val config: PlatformConfig = PlatformConfig.load(withNonces = true)
  }

  trait Default {
    implicit def config: PlatformConfig = Default.config
  }
}

class PlatformConfig(val all: Config) extends ConsensusConfig { self =>
  val underlying = all.getConfig("alephium")

  override val numZerosAtLeastInHash: Int = underlying.getInt("numZerosAtLeastInHash")
  override val maxMiningTarget: BigInt    = (BigInt(1) << (256 - numZerosAtLeastInHash)) - 1

  override val blockTargetTime: Duration = underlying.getDuration("blockTargetTime")
  override val blockConfirmNum: Int      = underlying.getInt("blockConfirmNum")
  override val retargetInterval
    : Int = underlying.getInt("retargetInterval") // number of blocks for retarget

  val port: Int               = underlying.getInt("port")
  val pingFrequency: Duration = underlying.getDuration("pingFrequency")
  val groups: Int             = underlying.getInt("groups")
  val nonceStep: BigInt       = underlying.getInt("nonceStep")
  val retryTimeout: Duration  = underlying.getDuration("retryTimeout")

  val mainGroup: GroupIndex = {
    val myGroup = underlying.getInt("mainGroup")
    assert(myGroup >= 0 && myGroup < groups)
    GroupIndex(myGroup)(this)
  }
  val peerId = PeerId.generateFor(mainGroup)(this)

  def loadBlockFlow(groups: Int): AVector[AVector[Block]] = {
    val nonces = underlying.getStringList("nonces")
    assert(nonces.size == self.groups * self.groups)

    AVector.tabulate(groups, groups) {
      case (from, to) =>
        val index      = from * self.groups + to
        val nonce      = nonces.get(index)
        val block      = Block.genesis(AVector.empty, maxMiningTarget, BigInt(nonce))
        val chainIndex = ChainIndex(from, to)(this)
        assert(chainIndex.accept(block)(this))
        block
    }
  }

  lazy val genesisBlocks: AVector[AVector[Block]] = loadBlockFlow(groups)
}
