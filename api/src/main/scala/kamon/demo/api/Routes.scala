package kamon.demo.api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.HttpEntity.Strict
import akka.http.scaladsl.model.{ContentTypes, HttpResponse, RequestEntity, StatusCodes}
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.Materializer
import akka.util.{ByteString, Timeout}
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.duration._

object Routes extends Directives with Serialization with RequestBuilding {
  private implicit val timeout = Timeout(2 seconds)
  private val logger = LoggerFactory.getLogger("Routes")

  def routes(bouncerAddress: String, conciergeAddress: String)(implicit system: ActorSystem, mat: Materializer): Route = {
    import system.dispatcher

    def sendGet(address: String): Future[HttpResponse] =
      Http().singleRequest(Get(address)).map(cleanupHeaders)

    def sendPost(address: String, entity: RequestEntity): Future[HttpResponse] =
      Http().singleRequest(Post(address, entity)).map(cleanupHeaders)

    def cleanupHeaders(response: HttpResponse): HttpResponse =
      response.withHeaders(response.headers.filterNot(h => h.name() == "trace-id" || h.name().toLowerCase.startsWith("x-b3")))

    def validateKey(key: String): Future[HttpResponse] =
      Http().singleRequest(Get(s"http://${bouncerAddress}/bouncer/keys/$key")).map(cleanupHeaders)

    // We only forward the request to the API if we get a 200 from the key validation,
    // otherwise we use the same response that we got from bouncer.
    def withValidKey(apiKey: String, request: => Future[HttpResponse]): Future[HttpResponse] = {
      validateKey(apiKey).flatMap(validationResponse => {
        if (validationResponse.status == StatusCodes.OK)
          request
        else
          Future.successful(validationResponse)
      })
    }

    def getCityEvents(apiKey: String, cityID: String): Future[HttpResponse] = {
      logger.info("Getting city events [cityId={}]", cityID)
      withValidKey(apiKey, sendGet(s"http://${conciergeAddress}/concierge/cities/$cityID/events"))
    }

    def getEvent(apiKey: String, eventID: String): Future[HttpResponse] = {
      logger.info("Getting event [eventId={}]", eventID)
      withValidKey(apiKey, sendGet(s"http://${conciergeAddress}/concierge/events/$eventID"))
    }

    def getReservations(apiKey: String, eventID: String): Future[HttpResponse] = {
      logger.info("Getting reservations for event [eventId={}]", eventID)
      withValidKey(apiKey, sendGet(s"http://${conciergeAddress}/concierge/events/$eventID/reservations"))
    }

    def postReservation(apiKey: String, eventID: String, seatCount: Int): Future[HttpResponse] = {
      logger.info("Creating reservations for event [eventId={}, seatCount={}]", eventID, seatCount)
      withValidKey(apiKey, sendPost(s"http://${conciergeAddress}/concierge/events/$eventID/reservations",
        Strict(ContentTypes.`text/plain(UTF-8)`, ByteString(seatCount.toString))))
    }

    /**
     * The available routes are:
     *
     * GET   /api/v1/cities/:id/events
     * GET   /api/v1/events/:id
     * GET   /api/v1/events/:id/reservations
     * POST  /api/v1/events/:id/reservations
     *
     */

    parameter("apiKey") { apiKey =>
      pathPrefix("api" / "v1") {
        path("cities" / Segment / "events") { cityID =>
          complete(getCityEvents(apiKey, cityID))
        } ~
        pathPrefix("events" / Segment) { eventID =>
          pathEnd {
            get {
              complete(getEvent(apiKey, eventID))
            }
          } ~
          path("reservations") {
            get {
              complete(getReservations(apiKey, eventID))
            } ~
            post {
              entity(as[String]) { seatCount =>
                complete(postReservation(apiKey, eventID, seatCount.toInt))
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
