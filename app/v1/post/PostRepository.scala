package v1.post

import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.util.{Date, Locale}
import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.{element, elementList, text}
import org.joda.time.DateTime
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
  val outputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.FRENCH)

  /**
    * Mapping to write a PostData out as a JSON value.
    */
  implicit val implicitWrites = new Format[PostData] {
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

  // SimpleDateFormat is not ThreadSafe => using DateTimeFormatter instead:
  val inputFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy HH:mm", Locale.FRENCH)
  def fetch(): List[PostData] = {
    logger.trace("Fetching posts... (This may take a while)")

    // @param: (post url, authorAndDate, content)
    def parseArticle(data: (String, String, String)) : PostData = {
      val patternIDinURL(idFromUrl) = data._1 // parse URL into idFromUrl
        data._2 match {
          case authorAndDatePattern(name, time) =>
            new PostData(idFromUrl, data._3, java.sql.Date.valueOf(LocalDate.parse(time, inputFormat)), name)
          case anonymousAuthorAndDatePattern(time) =>
            new PostData(idFromUrl, data._3, java.sql.Date.valueOf(LocalDate.parse(time, inputFormat)), "Anonymous")
        }
    }

    var posts: List[PostData] = Nil // url, id, PostData
    var parsedUrls: List[String] = Nil
    // (if this is a real post (not a "best of", etc))
    var nextPage = 1
    val desiredQuantity = 200
    val pagesPerTry = 7

    val browser = JsoupBrowser()

    while (posts.length < desiredQuantity) {
      // get a page with a list of articles
      logger.trace(s"Getting $pagesPerTry pages, starting from page $nextPage...")
      val docs = (nextPage to (nextPage+pagesPerTry)).par // process in parallel
        .map(i => browser.get("http://www.viedemerde.fr/news?page=" + i))
      nextPage += pagesPerTry

      // get the items and urls of articles that we did not fetch yet
      // TODO: using for {} yield () structure (instead of what follows) would look nicer
      val infiniteScroll = docs.map(doc => doc >> elementList(".infinite-scroll figure > a"))
      val itemUrl = infiniteScroll
        .flatMap(listofElements => listofElements.map(i => i.attr("href")))
        .filter(url => !parsedUrls.contains(url))

      // ignore extra URL (to avoid fetching more than the desiredQuantity)
      // itemUrl = itemUrl.dropRight(math.max(itemUrl.length + posts.length - desiredQuantity, 0))
      // actually, articles in some URL are invalid (eg: "best of the week"), so parse them all, and drop later

      // retrieve the page of each article
      val items = itemUrl.map(url => (
        url,
        browser.get("http://www.viedemerde.fr" + url) >> element(".art-panel")
      ))

      logger.trace(s"Retrieved data from " + items.size + " posts")
      // retrieve useful data from each article
      val data = items.map{ item => // item = (url, html)
        var authorDate : Option[String] = None
        var content : Option[String] = None
        var header : Option[String] = None
        var title : Option[String] = None
        try {
          authorDate = Some(item._2 >> text("div:eq(0) > div:eq(0) > div:eq(2)"))
          title = Some(item._2 >> text("h2"))
          header = Some(item._2 >> text(".chapo"))
          content = Some(item._2 >> text("div:eq(0) > div:eq(2) > div:eq(0) > div:eq(0)"))
        } catch {
          case ex: NoSuchElementException => {
            // => best post of the week, etc => There is no content, but still, there is a title and a header
          }

          case ex: Exception => {
            logger.trace(s"Exception: Could not parse given post: " + item._1
              + "\n\t" + ex.getStackTrace.mkString("\n\t"))
          }
        }
        val wholeContent: String = List(title.getOrElse(""), header.getOrElse(""), content.getOrElse("")).mkString("")
        (item._1, authorDate, wholeContent) // url, author and date, content
        // TODO: improve content retrieval again. Eg: include text from links
      }.filter(
        i => i._2.isDefined // ignore ~"best post of the week", etc
      )
        .map( i => (i._1, i._2.get, i._3))

      logger.trace(s"Extracted content author date from " + data.size + " posts")
      posts = posts ++ data.map(i => parseArticle(i))  // i : (post url, authorAndDate, content)
      logger.trace(s"Parsed articles")
      parsedUrls = parsedUrls ++ itemUrl
      logger.trace(s"There are " + posts.length + " posts")
    }

    posts = posts.dropRight(math.max(posts.length - desiredQuantity, 0))
    logger.trace(s"Reduced to " + posts.length + " posts")
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
  protected var postList: List[PostData] = Nil

  protected def loadPostsFromFileIfRequired(): Unit = {
    logger.trace("loadPostsFromFileIfRequired")
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

/**
  * This mock while create its own list of posts, instead of reading them from the database file.
  * This is handy to run tests.
  * @param ec
  */
@Singleton
class PostRepositoryMock @Inject()()(implicit ec: PostExecutionContext) extends PostRepositoryImpl {
  private val logger = Logger(this.getClass)
  override protected def loadPostsFromFileIfRequired(): Unit = {
    logger.trace("loadPostsFromFileIfRequired")
    if (postList.isEmpty) {
      postList =
        List(PostData("id123", "Dites-moi qui...",
            new DateTime().withYear(2000)
            .withMonthOfYear(1)
            .withDayOfMonth(1).toDate, "Professeur Tournesol"),
          PostData("id456", "je suis 1...",
            new DateTime().withYear(2002)
              .withMonthOfYear(1)
              .withDayOfMonth(1).toDate, "Tintin"),
          PostData("id789", "je suis 2 ...",
            new DateTime().withYear(2004)
              .withMonthOfYear(1)
              .withDayOfMonth(1).toDate, "Captain"),
          PostData("id10", "je suis 3 ...",
            new DateTime().withYear(2006)
              .withMonthOfYear(1)
              .withDayOfMonth(1).toDate, "Dupond et Dupont")
        )
    }
  }
}
