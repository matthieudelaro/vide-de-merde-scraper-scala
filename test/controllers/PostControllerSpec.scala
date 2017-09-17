import javax.inject.Singleton

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._
import play.api.test.CSRFTokenHelper._
import v1.post.{PostRepository, PostRepositoryImpl}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import play.test.{WithApplication, WithServer}

class PostControllerSpec extends PlaySpec with GuiceOneAppPerTest {

  "PostController" should {

    "return the list of all posts" in {
      val request = FakeRequest(GET, "/api/posts/").withHeaders(HOST -> "localhost:9000").withCSRFToken
      val posts = route(app, request).get

      // TODO: work on attributes of parsed JSON, instead of working on the content of the response
      contentAsString(posts) must include (""""count":4""")
      contentAsString(posts) must include (""""posts":""")
    }

    "return the list of posts selected by author" in {
      val request = FakeRequest(GET, "/api/posts/?author=Tintin").withHeaders(HOST -> "localhost:9000").withCSRFToken
      val posts = route(app, request).get

      contentAsString(posts) must include (""""count":1""")
      contentAsString(posts) must include (""""posts":""")
      contentAsString(posts) must include (""""Tintin"""")
    }

    "return the list of posts selected by from (date)" in {
      val request = FakeRequest(GET, "/api/posts/?from=2003-01-01T00:00:00Z").withHeaders(HOST -> "localhost:9000").withCSRFToken
      val posts = route(app, request).get

      contentAsString(posts) must include (""""count":2""")
      contentAsString(posts) must include (""""posts":""")
      contentAsString(posts) must include (""""Captain"""")
      contentAsString(posts) must include (""""Dupond et Dupont"""")
    }

    "return the list of posts selected by to (date)" in {
      val request = FakeRequest(GET, "/api/posts/?to=2003-01-01T00:00:00Z").withHeaders(HOST -> "localhost:9000").withCSRFToken
      val posts = route(app, request).get

      contentAsString(posts) must include (""""count":2""")
      contentAsString(posts) must include (""""posts":""")
      contentAsString(posts) must include (""""Professeur Tournesol"""")
      contentAsString(posts) must include (""""Tintin"""")
    }

    "return the list of posts selected by from / to (date)" in {
      val request = FakeRequest(GET, "/api/posts/?from=2001-01-01T00:00:00Z&to=2005-01-01T00:00:00Z").withHeaders(HOST -> "localhost:9000").withCSRFToken
      val posts = route(app, request).get

      contentAsString(posts) must include (""""count":2""")
      contentAsString(posts) must include (""""posts":""")
      contentAsString(posts) must include (""""Captain"""")
      contentAsString(posts) must include (""""Tintin"""")
    }

    "return the list of posts selected by author / from / to (date)" in {
      val request = FakeRequest(GET, "/api/posts/?author=Tintin&from=2001-01-01T00:00:00Z&to=2005-01-01T00:00:00Z").withHeaders(HOST -> "localhost:9000").withCSRFToken
      val posts = route(app, request).get

      contentAsString(posts) must include (""""count":1""")
      contentAsString(posts) must include (""""posts":""")
      contentAsString(posts) must include (""""Tintin"""")
    }

    "return the list of the requested index" in {
      val request = FakeRequest(GET, "/api/posts/id10").withHeaders(HOST -> "localhost:9000").withCSRFToken
      val posts = route(app, request).get

      contentAsString(posts) must not include (""""count"""")
      contentAsString(posts) must include (""""post":""")
      contentAsString(posts) must include (""""id10"""")
    }

    // TODO: test 404 for unknown IDs
  }

}