package v1.post

import javax.inject.{Inject, Provider}

import play.api.MarkerContext
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

/**
  * DTO for displaying post information.
  */
case class PostResource(id: String, content: String, date: String, author: String)


object PostResource {

  /**
    * Mapping to write a PostResource out as a JSON value.
    */
  implicit val implicitWrites = new Writes[PostResource] {
    def writes(post: PostResource): JsValue = {
      Json.obj(
        "id" -> post.id,
        "content" -> post.content,
        "date" -> post.date,
        "author" -> post.author
      )
    }
  }
}

/**
  * Controls access to the backend data, returning [[PostResource]]
  */
class PostResourceHandler @Inject()(postRepository: PostRepository)(implicit ec: ExecutionContext) {


  def lookup(id: String)(implicit mc: MarkerContext): Future[Option[PostResource]] = {
    val postFuture = postRepository.get(id)
    postFuture.map { maybePostData =>
      maybePostData.map { postData =>
        createPostResource(postData)
      }
    }
  }

  def find(author: Option[String], from: Option[String], to: Option[String])(implicit mc: MarkerContext): Future[Iterable[PostResource]] = {
    postRepository.list(author: Option[String], from: Option[String], to: Option[String]).map { postDataList =>
      postDataList.map(postData => createPostResource(postData))
    }
  }

  private def createPostResource(p: PostData): PostResource = {
    PostResource(p.id, p.content, p.getDateAsString(), p.author)
  }

  def fetch() = {
    postRepository.fetch()
  }

}
