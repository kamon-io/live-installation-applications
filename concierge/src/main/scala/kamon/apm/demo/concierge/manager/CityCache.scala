package kamon.apm.ticketmanager.manager

import kamon.apm.demo.concierge.Model
import scala.collection.concurrent.TrieMap
import scala.io.Source

object CityCache {

  private val _cities = TrieMap.empty[String, Model.City]

  {
    // Load all cities from a built-in CSV
    Source.fromResource("cities.csv").getLines().foreach { line =>
      val columns = line.split(",")
      val city = Model.City(columns(0), columns(1), columns(2))
      _cities.put(city.id, city)
    }
  }

  def get(id: String): Option[Model.City] = {
    _cities.get(id)
  }
}
