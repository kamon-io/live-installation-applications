package controllers

import javax.inject._
import play.api.mvc._

/**
 * Status controller, only used from health checks
 */
@Singleton
class StatusController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  /**
    * Just reply so that the service will be recognized as "alive"
    */
  def get() = Action { implicit request: Request[AnyContent] =>
    Ok
  }
}
