import components.KeyStorage
import play.api.libs.json._
import play.api.mvc.{Result, Results}

import scala.concurrent.Future
import scala.util.Random

package object controllers {

  def keyToJson(key: KeyStorage.Key): JsValue = {
    Json.obj(
      "key" -> JsString(key.key),
      "organizationID" -> JsNumber(key.organizationID)
    )
  }

  def keyListToJson(organizationID: Long, keys: Seq[KeyStorage.Key]): JsValue = {
    Json.obj(
      "organizationID" -> organizationID,
      "keys" -> JsArray(keys.map(keyToJson))
    )
  }


  def spiceUp(original: => Future[Result]): Future[Result] = {
    Random.nextDouble() match {
      case p if p > 0.9 => Future.successful(Results.InternalServerError)
      case p if p > 0.8 => Future.successful(Results.Unauthorized)
      case _ => original
    }
  }
}
