import v1.post.PostRepositoryImpl

object Scraper {
  def main(args: Array[String]): Unit = {
    PostRepositoryImpl.fetch()
  }
}

