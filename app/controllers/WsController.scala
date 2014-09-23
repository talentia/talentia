package controllers

import models.Qiita
import play.api.libs.concurrent.Execution.Implicits
import play.api.libs.json.{JsArray, Json, JsResult}
import play.api.libs.ws.{WSResponse, WSRequestHolder, WS}
import play.api.mvc._
import utils.APIConfigs
import scala.concurrent.Future
import play.api.Play.current

/**
 * Created by scova0731 on 9/22/14.
 */
object WsController extends Controller {

  implicit val context = Implicits.defaultContext

  val holder: WSRequestHolder = WS.url("http://qiita.com/api")

  case class Person(name: String, age: Int)

  implicit val personReads = Json.reads[Person]

  def sandbox = {
    val complexHolder: WSRequestHolder =
      holder.withHeaders("Accept" -> "application/json")
        .withRequestTimeout(10000)
        .withQueryString("search" -> "play")

    val futureResponse: Future[WSResponse] = complexHolder.get()


    val futureResult: Future[String] = holder.get().map {
      response =>
        (response.json \ "person" \ "name").as[String]
    }


    val futureResult2: Future[JsResult[Person]] = holder.get().map {
      response => (response.json \ "person").validate[Person]
    }
  }


  def listQiitaTags() = Action.async {
    val holder: WSRequestHolder = WS.url(Qiita.config.allTagsUrl())
    val futureResponse: Future[WSResponse] = holder.get()

    futureResponse.flatMap(response => {
      val list = response.json.as[JsArray]

      Qiita.replaceTagsJson(list).map(either =>
        either.fold(
          error => BadRequest(error),
          json => Ok(json)
        )
        )
    })
  }

  
  def listQiitaTagItems(tagName: String) = Action.async {
    val holder = WS.url(Qiita.config.tagItemsUrl(tagName))


    Future(Ok(1))
  }


}
