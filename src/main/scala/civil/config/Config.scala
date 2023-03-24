package civil.config

import com.typesafe.config._

object Config {
  val env = if (System.getenv("SCALA_ENV") == null) "dev" else System.getenv("SCALA_ENV")

  val conf = ConfigFactory.load()
  def apply() = conf.getConfig(env)
}