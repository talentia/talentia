package models

import org.joda.time.{DateTimeZone, DateTime}
import org.jsoup.Jsoup
import play.api.{Play, Logger}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import play.api.Play.current
import reactivemongo.core.commands.LastError

import scala.concurrent.Future

/**
 *
 * http://qiita.com/docs/api
 */
object Qiita {

  object config {

    lazy val rootUrl = Play.current.configuration
      .getString("api.qiita.rootApi").getOrElse("https://qiita.com/api/v1")
    lazy val rootHtmlUrl = Play.current.configuration
      .getString("api.qiita.rootHtml").getOrElse("https://qiita.com")

    def allTagsUrl() =
      addPagenation(s"$rootUrl/tags", perPage = 100)
    def tagItemsUrl(urlName: String) =
      addPagenation(s"$rootUrl/tags/$urlName/items", perPage = 100)
    def userUrl(urlName: String) =
      s"$rootUrl/users/$urlName"
    def userhtmlUrl(urlName: String) =
      s"$rootHtmlUrl/$urlName"


    /**
     * Add pagenation parameters
     * @param url
     * @param page 何ページ目を取得するか(1-origin)．最大50，デフォルト1
     * @param perPage 取得件数．最大100，デフォルト20
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
      val urlName = jsValue.\("url_name").as[String]

      // Should remove one by one ?
      //qiitaTags.remove(BSONDocument("url_name" -> urlName)).map { lastError =>
      qiitaTags.insert(added).map { lastError =>
        logLastError(lastError)
      }}
      //}

    dropping().map{x =>
      insertion()
      Right(json)
    }
  }


  def upsertTagItems(tagName: String, json: JsArray): Future[Either[String, JsArray]] = {

    json.value.foreach{ jsValue =>

      val added = jsValue.as[JsObject] +
        ("stored_at" -> dateTime.writes(new DateTime())) +
        ("tag_name" -> JsString(tagName))
      val uuid = jsValue.\("uuid").as[String]

      qiitaTagItems.remove(Json.obj("uuid" -> JsString(uuid))).map { lastError =>
        logLastError(lastError)
        qiitaTagItems.insert(added).map { lastError =>
          logLastError(lastError)
      }}
    }

    Future(Right(json))
  }


  def getAndAnalyzeUserHtml(userId: String, html: String):String = {
    val doc = Jsoup.parse(html)

    val href = doc.select("#main link").attr("href")
    Logger.debug(s"HTML href: $href")

    href
  }

  private def logLastError(lastError: LastError): Unit = {
    if (!lastError.ok)
      Logger.debug(s"LastError: $lastError")
  }
}
