package models

import org.joda.time.{DateTimeZone, DateTime}
import play.api.{Play, Logger}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import play.api.Play.current

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
 * Created by scova0731 on 9/23/14.
 */
object Qiita {

  object config {

    lazy val rootUrl = Play.current.configuration
      .getString("api.qiita.root").getOrElse("https://qiita.com/api/v1")

    def allTagsUrl() = addPagenation(s"$rootUrl/tags", perPage = 100)
    def tagItemsUrl(urlName: String) = s"$rootUrl/tags/$urlName/items"
    def userUrl(urlName: String) = s"$rootUrl/users/$urlName"


    /**
     * Add pagenation parameters
     * @param url
     * @param page 取得件数．最大100，デフォルト20
     * @param perPage 何ページ目を取得するか(1-origin)．最大50，デフォルト1
     * @return
     */
    def addPagenation(url:String, page:Int = 1, perPage:Int = 20): String =
      s"$url?page=$page&per_page=$perPage"
  }



  private def mongoCollections(name: String) =
    ReactiveMongoPlugin.db.collection[JSONCollection](name)

  private def qiitaTags = mongoCollections("qiita_tags")
  private def qiitaTagItems = mongoCollections("qiita_tag_items")

  def sandbox():Unit = {

    def f(a:Int) = Option(a.toString)

    val result = Some(1) flatMap f

  }

  implicit val dateTime = new Writes[DateTime] {
    def writes(d: DateTime): JsValue =
      JsString(d.toDateTime(DateTimeZone.UTC)
        .toString("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
  }


  def replaceTagsJson(json: JsArray): Future[Either[String, JsArray]] = {
    def dropping() = qiitaTags.drop()
    def insertion() = json.value.foreach{ jsValue =>
      val added =
        jsValue.as[JsObject] + ("stored_at" -> dateTime.writes(new DateTime()))

      qiitaTags.insert(added).map { lastError =>
        Logger.debug(s"Successfully inserted with LastError: $lastError")

      }}

    dropping().map{x =>
      insertion()
      Right(json)
    }
  }


  def upsertTagItems(tagName: String, json: JsArray): Future[Either[String, JsArray]] = {



  }
}
