package kamon.apm.demo.concierge.manager

import java.time
import java.time.{Duration, LocalDateTime}

import kamon.apm.demo.concierge.Model
import java.util.UUID

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import kamon.apm.ticketmanager.manager.CityCache
import scala.concurrent.duration._


class CityEntity(reservationPeriod: time.Duration) extends Actor {
  import CityEntity.Protocol
  type EventID = String

  context.system.scheduler.schedule(1 minute, 1 minute, self, Protocol.ReleaseExpiredReservations)(context.dispatcher)
  override def receive: Receive = unstarted


  private def unstarted: Actor.Receive = {
    def operatingCity(city: Model.City): Actor.Receive = {
      val events = EventBuilder.randomEvents(city)
      val eventMap = events.map(e => (e.id -> e)).toMap
      operating(city, eventMap, Map.empty)
    }

    def cityNotFound(): Actor.Receive = {
      case _ =>
        sender() ! Protocol.NotFound
        context.stop(self)
    }

    val cityID = self.path.name
    val behavior = findCity(cityID)
      .map( city => operatingCity(city))
      .getOrElse(cityNotFound())

    {
      case anyMessage => behavior(anyMessage)
    }
  }


  private def operating(city: Model.City, events: Map[EventID, Model.Event], reservations: Map[EventID, List[Model.Reservation]]): Actor.Receive = {

    /**
      * Sends back a single event if it exists
      */
    def handleGetEvent(eventID: EventID): Actor.Receive = {
      sender ! events.get(eventID).getOrElse(Protocol.NotFound)
      operating(city, events, reservations)
    }

    /**
      * Sends back all reservations for an event.
      */
    def handleGetReservations(eventID: EventID): Actor.Receive = {
      sender ! reservations.get(eventID).map(r => Protocol.EventReservations(r)).getOrElse(Protocol.NotFound)
      operating(city, events, reservations)
    }

    /**
      * Sends back all events in this city
      */
    def handleGetAllEvents(cityID: String): Actor.Receive = {
      sender ! Protocol.CityEvents(city, events.values.toList)
      operating(city, events, reservations)
    }

    /**
      * Creates a reservation for a number of seats, if available
      */
    def handleReserveSeats(eventID: EventID, seatCount: Int): Actor.Receive = {
      events.get(eventID).map(event => {
        val previousReservations = reservations.get(eventID).getOrElse(List.empty)
        val availableSeats = event.capacity - previousReservations.foldLeft(0)(_ + _.seatCount)

        if(availableSeats >= seatCount) {

          // Happy path, seats get reserved!
          val reservationID = UUID.randomUUID().toString()
          val newReservation = Model.Reservation(reservationID, eventID, seatCount, LocalDateTime.now().plusMinutes(reservationPeriod.toMinutes))
          sender ! newReservation
          operating(city, events, reservations.updated(eventID, newReservation +: previousReservations))

        } else {

          // Not enough seats :(
          sender() ! Protocol.NotEnoughSeatsAvailable
          operating(city, events, reservations)
        }
      }).getOrElse {

        // The event does not exist
        sender() ! Protocol.NotFound
        operating(city, events, reservations)
      }
    }

    /**
      * Cleans up all reservations that have been up for more than 30 minutes. Eventually we will implement some sort
      * of "confirmation" or "booking" that will make the reservation permanent.
      */
    def handleReleaseExpired(): Actor.Receive = {
      val now = LocalDateTime.now()
      val cleanedUpReservations = reservations.map {
        case (eventID, reservations) => (eventID, reservations.filter(r => r.expirationDate.isBefore(now)))
      }

      operating(city, events, cleanedUpReservations)
    }

    {
      case Protocol.GetEvent(eventID)                 => context.become(handleGetEvent(eventID))
      case Protocol.GetCityEvents(city)               => context.become(handleGetAllEvents(city))
      case Protocol.ReserveSeats(eventID, seatCount)  => context.become(handleReserveSeats(eventID, seatCount))
      case Protocol.ReleaseExpiredReservations        => context.become(handleReleaseExpired())
      case Protocol.GetReservations(eventID)          => context.become(handleGetReservations(eventID))
    }
  }

  private def findCity(cityID: String): Option[Model.City] =
    CityCache.get(cityID)

}

object CityEntity {

  object Protocol {

    // Gets one event
    case class GetEvent(eventID: String)

    // Request events available in this city
    case class GetCityEvents(cityID: String)
    case class CityEvents(city: Model.City, events: List[Model.Event])

    // Reserve seats on an event
    case class ReserveSeats(eventID: String, seatCount: Int)
    case class Reserved(reservation: Model.Reservation)
    case class GetReservations(eventID: String)
    case class EventReservations(reservations: List[Model.Reservation])

    // Error response codes
    case object NotFound
    case object NotEnoughSeatsAvailable

    // Utility
    case object ReleaseExpiredReservations
  }


  def props(reservationPeriod: Duration): Props =
    Props[CityEntity](new CityEntity(reservationPeriod))

  def startSharding(system: ActorSystem, reservationPeriod: Duration): ActorRef = {
    def cityIDFromEventID(eventID: String): String = eventID.split("-")(0)
    val numberOfShards = 100

    val entityExtractor: ShardRegion.ExtractEntityId = {
      def print(id: String, msg: Any): (String, Any) = {
        (id, msg)
      }

      {
        case msg @ Protocol.GetEvent(eventID)         => print(cityIDFromEventID(eventID), msg)
        case msg @ Protocol.GetCityEvents(cityID)     => print(cityID, msg)
        case msg @ Protocol.ReserveSeats(eventID, _)  => print(cityIDFromEventID(eventID), msg)
        case msg @ Protocol.GetReservations(eventID)  => print(cityIDFromEventID(eventID), msg)
      }
    }

    val shardExtractor: ShardRegion.ExtractShardId = entityExtractor.andThen {
      case (entityID, _) => (math.abs(entityID.hashCode) % numberOfShards).toString
    }

    ClusterSharding(system).start(
      typeName = "city",
      entityProps = props(reservationPeriod),
      settings = ClusterShardingSettings(system),
      extractEntityId = entityExtractor,
      extractShardId = shardExtractor
    )
  }
}

