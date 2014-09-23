package controllers

import jp.co.bizreach.play2handlebars.HBS
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo._
import reactivemongo.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


object Application extends Controller with MongoController {

  /*
     * Get a JSONCollection (a Collection implementation that is designed to work
     * with JsObject, Reads and Writes.)
     * Note that the `collection` is not a `val`, but a `def`. We do _not_ store
     * the collection reference to avoid potential problems in development with
     * Play hot-reloading.
     */
  def collection: JSONCollection = db.collection[JSONCollection]("persons")


  def index = Action { res =>
    Ok(HBS("index"))
  }


  def create(name: String, age: Int) = Action.async {
    val json = Json.obj(
      "name" -> name,
      "age" -> age,
      "created" -> new java.util.Date().getTime())

    collection.insert(json).map(lastError =>
      Ok("Mongo LastError: %s".format(lastError)))
  }

  def createFromJson = Action.async(parse.json) { request =>
    import play.api.libs.json.Reads._
    /*
     * request.body is a JsValue.
     * There is an implicit Writes that turns this JsValue as a JsObject,
     * so you can call insert() with this JsValue.
     * (insert() takes a JsObject as parameter, or anything that can be
     * turned into a JsObject using a Writes.)
     */
    val transformer: Reads[JsObject] =
      Reads.jsPickBranch[JsString](__ \ "firstName") and
        Reads.jsPickBranch[JsString](__ \ "lastName") and
        Reads.jsPickBranch[JsNumber](__ \ "age") reduce

    request.body.transform(transformer).map { result =>
      collection.insert(result).map { lastError =>
        Logger.debug(s"Successfully inserted with LastError: $lastError")
        Created
      }
    }.getOrElse(Future.successful(BadRequest("invalid json")))
  }


  def findByName(name: String) = Action.async {
    val cursor: Cursor[JsObject] = collection.
      find(Json.obj("name" -> name)).  // find all people with name `name`
      sort(Json.obj("created" -> -1)). // sort them by creation date
      cursor[JsObject]                 // perform the query and get a cursor of JsObject

    // gather all the JsObjects in a list
    val futurePersonsList: Future[List[JsObject]] = cursor.collect[List]()

    // transform the list into a JsArray
    val futurePersonsJsonArray: Future[JsArray] = futurePersonsList.map { persons =>
      Json.arr(persons)
    }

    // everything's ok! Let's reply with the array
    futurePersonsJsonArray.map { persons =>
      Ok(persons)
    }
  }

}
