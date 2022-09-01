package kamon.demo.api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import com.typesafe.config.ConfigFactory
import kamon.Kamon

import scala.concurrent.ExecutionContext

object Launcher extends App {
  Kamon.init()
  val config = ConfigFactory.load()


  implicit val system: ActorSystem = ActorSystem("api")
  implicit val executionContext: ExecutionContext = system.dispatcher

  val routes = Routes.routes(
    bouncerAddress = config.getString("api.services.bouncer"),
    conciergeAddress = config.getString("api.services.concierge"))

  val bindingFuture = Http(system)
    .newServerAt("0.0.0.0", 8090)
    .bind(routes)

}
