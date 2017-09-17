import javax.inject._

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule
import play.api.{Configuration, Environment}
import v1.post._

/**
  * Sets up custom components for Play.
  *
  * https://www.playframework.com/documentation/latest/ScalaDependencyInjection
  */
class Module(environment: Environment, configuration: Configuration)
    extends AbstractModule
    with ScalaModule {

  override def configure() = {
    // depending on configuration described in application.conf, use a mock for PostRepository to run tests
    if (configuration.underlying.getString("mock.mockDB") == "true") {
      bind[PostRepository].to[PostRepositoryMock].in[Singleton]
    } else {
      bind[PostRepository].to[PostRepositoryImpl].in[Singleton]
    }
  }
}
