package settings

import java.util.concurrent.TimeUnit
import javax.inject.Inject

import akka.util.Timeout
import play.api.Configuration

class Settings @Inject() (configuration: Configuration) {
  object doc {
    object renderer {
      val timeout = Timeout(configuration.getMilliseconds("doc.renderer.timeout").getOrElse(5000L), TimeUnit.MILLISECONDS)
    }
  }
}
