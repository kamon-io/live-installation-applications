package kamon.apm.demo.concierge.api

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.pattern.ask
import akka.http.scaladsl.server.{Directives, Route}
import akka.util.Timeout
import kamon.apm.demo.concierge.manager.CityEntity.Protocol
import kamon.apm.demo.concierge.Model
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{Failure, Success}


object Routes extends Directives with Serialization {
  implicit val timeout = Timeout(2 seconds)
  private val log = LoggerFactory.getLogger("kamon.apm.demo.concierge.api.Routes")

  def routes(citiesShard: ActorRef): Route = {
    def getCityEvents(cityID: String): Future[Any] = {
      log.info("Processing getCityEvents")
      citiesShard ? Protocol.GetCityEvents(cityID)
    }

    def getEventReservations(eventID: String): Future[Any] = {
      log.info("Processing getEventReservations")
      citiesShard ? Protocol.GetReservations(eventID)
    }

    def getEvent(eventID: String): Future[Any] = {
      log.info("Processing getEvent")
      citiesShard ? Protocol.GetEvent(eventID)
    }

    def postReservation(eventID: String, seatCount: Int): Future[Any] = {
      log.info("Processing postReservation")
      citiesShard ? Protocol.ReserveSeats(eventID, seatCount)
    }


    /**
      * The available routes are:
      *
      *   GET   /concierge/cities/:id/events
      *   GET   /concierge/events/:id
      *   GET   /concierge/events/:id/reservations
      *   POST  /concierge/events/:id/reservations
      *
      */

    pathPrefix("concierge") {
      path("cities" / Segment / "events") { cityID =>
        extractRequest { req =>
          onComplete(getCityEvents(cityID)) {
            case Failure(t)                                   => complete(t)
            case Success(response) => response match {
              case Protocol.CityEvents(_, events)             => complete(events)
              case Protocol.NotFound                          => complete(StatusCodes.NotFound)
            }
          }
        }
      } ~
      pathPrefix("events" / Segment) { eventID =>
        pathEnd {
          onComplete(getEvent(eventID)) {
            case Failure(t)                                 => complete(t)
            case Success(response) => response match {
              case event: Model.Event                       => complete(event)
              case Protocol.NotFound                        => complete(StatusCodes.NotFound)
            }
          }
        } ~
        path("reservations") {
          get {
            onComplete(getEventReservations(eventID)) {
              case Failure(t)                               => complete(t)
              case Success(response) => response match {
                case Protocol.EventReservations(r)          => complete(r)
                case Protocol.NotFound                      => complete(StatusCodes.NotFound)
              }
            }
          } ~
          post {
            entity(as[String]) { seatCount =>
              onComplete(postReservation(eventID, seatCount.toInt)) {
                case Failure(t)                             => complete(t)
                case Success(response) => response match {
                  case reservation: Model.Reservation       => complete(reservation)
                  case Protocol.NotFound                    => complete(StatusCodes.NotFound)
                  case Protocol.NotEnoughSeatsAvailable     => complete(StatusCodes.Forbidden)
                }
              }
            }
          }
        }
      }
    } ~
    path("status") {
      complete(StatusCodes.OK)
    }
  }
}
