package kamon.apm.demo.concierge.api

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import kamon.apm.demo.concierge.Model
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat}

trait Serialization extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val DateFormat = new JsonFormat[LocalDateTime] {
    override def write(obj: LocalDateTime): JsValue =
      JsString(obj.format(DateTimeFormatter.ISO_DATE_TIME))

    override def read(json: JsValue): LocalDateTime = json match {
      case JsString(value) =>
        LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME)
      case x => spray.json.deserializationError("Expected string as JsString but got " + x)
    }
  }

  implicit val CityFormat = jsonFormat3(Model.City)
  implicit val EventFormat = jsonFormat3(Model.Event)
  implicit val ReservationFormat = jsonFormat4(Model.Reservation)


}
