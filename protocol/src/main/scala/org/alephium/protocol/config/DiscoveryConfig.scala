package org.alephium.protocol.config

import java.net.InetSocketAddress

import scala.concurrent.duration.FiniteDuration

import org.alephium.crypto.{ED25519PrivateKey, ED25519PublicKey}

// TODO: refactor this as two configs
trait DiscoveryConfig extends CliqueConfig {
  def publicAddress: InetSocketAddress

  def discoveryPrivateKey: ED25519PrivateKey

  def discoveryPublicKey: ED25519PublicKey

  /* Maximum number of peers to track. */
  def peersPerGroup: Int

  /* Maximum number of peers used for probing during a scan. */
  def scanMaxPerGroup: Int

  /* Wait time between two scan. */
  def scanFrequency: FiniteDuration

  def scanFastFrequency: FiniteDuration

  /* Maximum number of peers returned from a query (`k` in original kademlia paper). */
  def neighborsPerGroup: Int

  /** Duration we wait before considering a peer dead. **/
  def peersTimeout: FiniteDuration = scanFrequency * 3
}
