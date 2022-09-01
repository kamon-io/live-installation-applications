package controllers

import components.KeyStorage
import org.slf4j.LoggerFactory

import javax.inject._
import play.api.mvc._

import scala.concurrent.ExecutionContext

/**
 * Handles API key authentication requests
 */
@Singleton
class KeysController @Inject()(keyStorage: KeyStorage, cc: ControllerComponents, ec: ExecutionContext) extends AbstractController(cc) {
  implicit val iec = ec
  val logger = LoggerFactory.getLogger(classOf[KeysController])

  /**
    * Gets a key from storage
    */
  def get(id: String) = Action.async { implicit request: Request[AnyContent] =>
    logger.info("Processing API KEY retrieval {}", id)

    spiceUp {
      keyStorage.retrieve(id).map(keyOption => {
        keyOption
          .map(key => Ok(keyToJson(key)))
          .getOrElse(Unauthorized)
      })
    }
  }
}
