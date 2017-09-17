package v1.post

import javax.inject.Inject

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

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
    postResourceHandler.lookup(id).map { post =>
      Ok(Json.obj("post" -> post))
    }
//    // TODO: 404
//    NotFound()
  }

  // TODO : remove ? 'cause not asked
  def fetch = Action {
    postResourceHandler.fetch
    Ok("Fetching... This may take a while...")
  }
}
