package settings

import java.util.concurrent.TimeUnit
import javax.inject.Inject

import akka.util.Timeout
import play.api.Configuration
import scala.collection.JavaConverters._

class Settings @Inject() (configuration: Configuration) {
  object application {
    val hostAliases = configuration
      .getObject("app.host-aliases")
      .map(_.unwrapped().asScala.toMap.map(e => e._1 -> e._2.toString))
      .getOrElse(Map.empty)
  }

  object doc {
    object renderer {
      val timeout = Timeout(configuration.getMilliseconds("doc.renderer.timeout").getOrElse(5000L), TimeUnit.MILLISECONDS)
    }
  }

  object play {
    object crypto {
      val secret = configuration.getString("play.crypto.secret").getOrElse("secret")
    }
  }
}
