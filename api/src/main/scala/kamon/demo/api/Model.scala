package kamon.demo.api

import java.time.LocalDateTime

object Model {

  case class Key(
    key: String,
    organizationID: Long
  )

  case class City(
    id: String,
    name: String,
    country: String
  )

  case class Event(
    id: String,
    name: String,
    capacity: Int
  )

  case class Reservation(
    id: String,
    eventID: String,
    seatCount: Int,
    expirationDate: LocalDateTime
  )
}
