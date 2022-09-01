package kamon.apm.demo.concierge.manager
import kamon.apm.demo.concierge.Model

import scala.io.Source
import scala.util.Random

object EventBuilder {

  private val _movies = Source.fromResource("movies.csv").getLines().toIndexedSeq

  def randomEvents(city: Model.City): Seq[Model.Event] = {
    for(_ <- 1 to (1 + Random.nextInt(10))) yield {
      // It is important for events to have the id format as CITYID-EVENTID so that sharding router can rely on that.
      val eventID = city.id + "-" + Random.alphanumeric.take(8).mkString
      val randomMovie = _movies(Random.nextInt(_movies.size))

      Model.Event(eventID, s"Movie Review of $randomMovie", 100)
    }
  }
}
