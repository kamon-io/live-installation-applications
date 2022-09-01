package components

import play.api.{Configuration, Environment}
import play.api.inject.Module

class ComponentsModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
      bind[KeyStorage].to[KeyStorage.InDatabase]
    )
  }
}

