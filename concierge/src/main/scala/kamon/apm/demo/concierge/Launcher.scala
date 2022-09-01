package kamon.apm.demo.concierge

import java.time.Duration
import akka.actor.{ActorSystem, Address}
import akka.cluster.Cluster
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.google.common.net.HostAndPort
import com.orbitz.consul.Consul
import com.orbitz.consul.option.ImmutableQueryOptions
import com.typesafe.config.ConfigFactory
import kamon.Kamon
import kamon.apm.demo.concierge.api.Routes
import kamon.apm.demo.concierge.manager.CityEntity
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

object Launcher extends App {
  Kamon.init()

  val logger = LoggerFactory.getLogger("Launcher")

  val config = ConfigFactory.load()
  val serviceName = config.getString("kamon.environment.service")
  val configContacts = config.getString("consul.contact-points").split(",").filter(_.nonEmpty)
  val configSelfAddress = config.getString("consul.self-address").split("\\:")
  val qualifier = config.getString("consul.qualifier")

  implicit val system: ActorSystem = ActorSystem(serviceName)
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher
  val cluster = Cluster(system)

  if (configContacts.nonEmpty) {
    logger.info("Starting the application with Consul Service Discovery, contacts: " + configContacts.mkString("[", ",", "]"))
    val consul = Consul.builder()
      .withHostAndPort(
        HostAndPort.fromParts(configContacts.head, 8500)
      ).build()

    val selfAddress = Address("akka.tcp", system.name, configSelfAddress(0), configSelfAddress(1).toInt)
    val joinSchedule = system.scheduler.schedule(5 seconds, 15 seconds, new Runnable {
      logger.info("Creating the join scheduled action")

      override def run(): Unit = {
        val queryOptions = ImmutableQueryOptions.builder().addTag("akka")
          if(qualifier.nonEmpty)
            queryOptions.addTag(qualifier)

        val availableNodes = consul.healthClient()
          .getHealthyServiceInstances(serviceName, queryOptions.build())
          .getResponse()
          .asScala
          .map { node => Address("akka.tcp", system.name, node.getService.getAddress, node.getService.getPort) }

        val seedNodes = availableNodes filter { address =>
          address != selfAddress || address == availableNodes.head
        }

        logger.info(s"Trying to join a cluster - Self[${selfAddress}], Available[${availableNodes.mkString(",")}], Seeds[${seedNodes.mkString(",")}]")
        cluster.joinSeedNodes(seedNodes.toList)
      }
    })

    cluster.registerOnMemberUp {
      logger.info("Successfully joined the cluster")
      joinSchedule.cancel()
    }

  } else {
    logger.info("Starting the application with local node only")
    cluster.join(cluster.selfAddress)
  }

  cluster.registerOnMemberUp {

    // Actual application startup logic
    val citiesShard = CityEntity.startSharding(system, Duration.ofMinutes(5))
    val bindingFuture = Http(system).newServerAt("0.0.0.0", 8080).bind(Routes.routes(citiesShard))
    logger.info("Successfully joined the cluster")
  }

  cluster.registerOnMemberRemoved {

  }

}
