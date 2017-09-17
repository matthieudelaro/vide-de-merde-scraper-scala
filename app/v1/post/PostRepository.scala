package v1.post

import java.util.Locale
import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.{element, elementList, text}
import play.api.libs.concurrent.CustomExecutionContext
import play.api.libs.json._
import play.api.{Logger, MarkerContext}

import scala.concurrent.Future
import scala.io.Source

final case class PostData(id: String, content: String, date: java.util.Date, author: String) {
  def getDateAsString() : String =  {
    return PostData.outputFormat.format(date)
  }
}

object PostData {
  val outputFormat = new java.text.SimpleDateFormat("yyyy-mm-dd'T'HH:mm:ss'Z'")

  /**
    * Mapping to write a PostData out as a JSON value.
    */
  implicit val implicitWrites = new Format[PostData] {
//    implicit val implicitWrites = new Writes[PostData] {
    def writes(post: PostData): JsValue = {
      Json.obj(
        "id" -> post.id,
        "content" -> post.content,
        "date" -> post.getDateAsString(),
        "author" -> post.author
      )
    }

    def reads(json: JsValue):JsResult[PostData] = JsSuccess(PostData.this(
      (json \ "id").as[String],
      (json \ "content").as[String],
      outputFormat.parse((json \ "date").as[String]),
      (json \ "author").as[String]
    ))
  }
}

class PostExecutionContext @Inject()(actorSystem: ActorSystem)
  extends CustomExecutionContext(actorSystem, "repository.dispatcher")

/**
  * Interface for the PostRepository.
  */
trait PostRepository {
  def list(author: Option[String], from: Option[String], to: Option[String])
          (implicit mc: MarkerContext): Future[Iterable[PostData]]

  def get(id: String)(implicit mc: MarkerContext): Future[Option[PostData]]
  def fetch()
}


object PostRepositoryImpl {
  private val logger = Logger(this.getClass)
  val storageName = "./database_posts.json"

  private val authorAndDatePattern = "Par (.+) / (.+)".r
  private val anonymousAuthorAndDatePattern = "Par / (.+)".r
  private val patternIDinURL = ".*_([0-9]+).html".r

  private val inputFormat = new java.text.SimpleDateFormat("EEEE d MMMM yyyy HH:mm", Locale.FRENCH)
  def fetch(): List[PostData] = {
    logger.trace("Fetching posts... (This may take a while)")

    // @param: (post url, authorAndDate, content)
    def parseArticle(data: (String, String, String)) : PostData = {
      var author, timeString : String = ""
      data._2 match {
        case authorAndDatePattern(name, time) =>
          author = name
          timeString = time
        case anonymousAuthorAndDatePattern(time) =>
          author = "Anonymous"
          timeString = time
      }
      val patternIDinURL(idFromUrl) = data._1 // parse URL into idFromUrl
      // PostData(id: String, content: String, date: java.util.Date, author: String)
      new PostData(idFromUrl, data._3, inputFormat.parse(timeString), author)
    }

    var posts: List[PostData] = Nil // url, id, PostData
    var parsedUrls: List[String] = Nil
    // (if this is a real post (not a "best of", etc))
    var nextPage = 1
    val desiredQuantity = 200

    val browser = JsoupBrowser()

    // TODO: load articles asynchronously,
    // See getPlayerScores from https://nordicapis.com/building-rest-api-java-scala-using-play-framework-part-2/
    while (posts.length < desiredQuantity) {
      // get a page with a list of articles
      logger.trace(s"Getting page $nextPage...")
      val doc = browser.get("http://www.viedemerde.fr/news?page=" + nextPage)
      nextPage += 1

      // get the items and urls of articles that we did not fetch yet
      // TODO: use for {} yield () structure instead of what follows
      val infiniteScroll = doc >> elementList(".infinite-scroll figure > a")
      var itemUrl = infiniteScroll
        .map(i => i.attr("href"))
        .filter(url => !parsedUrls.contains(url))

      // ignore extra URL (to avoid fetching more than the desiredQuantity)
      // itemUrl = itemUrl.dropRight(math.max(itemUrl.length + posts.length - desiredQuantity, 0))
      // actually, articles in some URL are invalid (eg: "best of the week"), so parse them all, and drop later

      // retrieve the page of each article
      val items = itemUrl.map(url => (
        url,
        browser.get("http://www.viedemerde.fr" + url) >> element(".art-panel")
      )
      )

      logger.trace(s"Retrieved data from " + items.size + " articles")
      // retrieve useful data from each article
      val data = items.map{ item => // item = (url, html)
        var authorDate : Option[String] = None
        var content : Option[String] = None
        try {
          authorDate = Some(item._2 >> text("div:eq(0) > div:eq(0) > div:eq(2)"))
          //          logger.trace(authorDate.toString() + " => getting content...")
          content = Some(item._2 >> text("div:eq(0) > div:eq(2) > div:eq(0) > div:eq(0)"))
          //          logger.trace(authorDate.toString() + " => got content")
        } catch {
          case ex: NoSuchElementException => { // recalling the best post of the week, etc => not real posts
            logger.trace(s"NoSuchElementException: Could not parse given article: " + item._1)
            logger.trace(ex.getStackTrace().toString())
          }

          case ex: Exception => {
            logger.trace(s"Could not parse given article: ")
            logger.trace(ex.getStackTrace().toString())
          }
        }
        (item._1, authorDate, content) // url, author and date, content
        // TODO: improve content retrieval. Eg: include text from links
      }.filter(
        i => i._2.isDefined && i._3.isDefined // ignore ~"best post of the week", etc
      )
        .map( i => (i._1, i._2.get, i._3.get))


      logger.trace(s"Extracted content author date from " + data.size + " articles")
      posts = posts ++ data.map(i => parseArticle(i))  // i : (post url, authorAndDate, content)
        .dropRight(math.max(data.size + posts.length - desiredQuantity, 0))
      logger.trace(s"Parsed articles")
      parsedUrls = parsedUrls ++ itemUrl
      logger.trace(s"There are " + posts.length + " articles")
    }

    // Write articles in the database
    // TODO: implement a real database, instead of this simple JSON file
    import java.io._
    logger.trace("Opening file")
    val pw = new PrintWriter(new File(storageName))
    logger.trace("Writing to file")
    pw.write(Json.toJson(posts).toString())
    pw.close()
    logger.trace("Done")
    posts
  }

}


/**
  * Implementation of the Post Repository to serve posts from VDM.
  */
@Singleton
class PostRepositoryImpl @Inject()()(implicit ec: PostExecutionContext) extends PostRepository {
  private val logger = Logger(this.getClass)

  // postList holds the posts in memory.
  // The scraper writes them into the file `storageName`, and the server loads them from this file
  // upon REST request, when postList is empty
  // TODO: implement a real database, instead of this simple JSON file
  private var postList: List[PostData] = Nil

  private def loadPostsFromFileIfRequired(): Unit = {

    if (postList.isEmpty) {
      val contents = Source.fromFile(PostRepositoryImpl.storageName).mkString
      val json = Json.parse(contents)
      postList = json.as[List[PostData]]
    }
  }

  override def list(author: Option[String], from: Option[String], to: Option[String])(implicit mc: MarkerContext): Future[Iterable[PostData]] = {
    Future {
      logger.trace(s"list: ")
      loadPostsFromFileIfRequired()
      var res = postList
      if (from.isDefined) {
        val dateFrom = PostData.outputFormat.parse(from.get)
        res = res.filter(p => p.date.after(dateFrom))
      }
      if (to.isDefined) {
        val dateTo = PostData.outputFormat.parse(to.get)
        res = res.filter(p => dateTo.after(p.date))
      }
      if (author.isDefined) {
        res = res.filter(p => author.get == p.author)
      }
      res
    }
  }

  override def get(id: String)(implicit mc: MarkerContext): Future[Option[PostData]] = {
    Future {
      loadPostsFromFileIfRequired()
      logger.trace(s"get: id = $id")
      postList.find(post => post.id == id)
    }
  }

  def fetch(): Unit = {
    postList = PostRepositoryImpl.fetch()
  }
}
