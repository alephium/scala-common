package org.alephium.util

sealed trait Env {
  def name: String
}

object Env {
  case object Prod  extends Env { override def name: String = "prod" }
  case object Debug extends Env { override def name: String = "debug" }
  case object Test  extends Env { override def name: String = "test" }

  def resolve(): Env = {
    val env = System.getenv("ALEPHIUM_ENV")
    env match {
      case "prod"  => Prod
      case "debug" => Debug
      case "test"  => Test
    }
  }
}
