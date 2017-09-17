package v1.post

import javax.inject.Inject

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

case class PostFormInput(title: String, body: String)

/**
  * Takes HTTP requests and produces JSON.
  */
class PostController @Inject()(cc: PostControllerComponents)(implicit ec: ExecutionContext)
    extends PostBaseController(cc) {

  private val logger = Logger(getClass)


  def list(author: Option[String], from: Option[String], to: Option[String]): Action[AnyContent] = PostAction.async { implicit request =>
    postResourceHandler.find(author, from, to).map { posts =>
      Ok(Json.obj("posts" -> posts, "count" -> posts.size))
    }
  }

  def show(id: String): Action[AnyContent] = PostAction.async { implicit request =>

    postResourceHandler.lookup(id)
      .map{ post =>
        if (post.isEmpty) {
          NotFound(Json.obj("post" -> post))
        } else {
          Ok(Json.obj("post" -> post))
        }
      }
  }

  // TODO : remove ? Because this feature is not desired
  def fetch = Action {
    postResourceHandler.fetch
    Ok("Fetching... This may take a while...")
  }
}
