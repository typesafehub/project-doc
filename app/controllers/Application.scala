package controllers

import javax.inject.Inject

import akka.pattern.{AskTimeoutException, ask}
import doc.DocRenderer
import modules.ConductRModule.ConductRDocRendererProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import play.twirl.api.Html
import settings.Settings

object Application {
  object Project extends Enumeration {
    val ConductR = Value
  }
}

class Application @Inject() (conductrDocRendererProvider: ConductRDocRendererProvider, settings: Settings)
  extends Controller {

  import Application._

  val conductrDocRenderer = conductrDocRendererProvider.get

  def index(project: Project.Value, path: String) = Action.async {
    project match {
      case Project.ConductR =>
        conductrDocRenderer.actorRef
          .ask(DocRenderer.Render(path))(settings.doc.renderer.timeout)
          .map {
            case html: Html              => Ok(html)
            case DocRenderer.NotFound(p) => NotFound(s"Cannot find $p")
            case DocRenderer.NotReady    => ServiceUnavailable("Initializing documentation. Please try again in a minute.")
          }
          .recover {
            case _: AskTimeoutException => InternalServerError
          }
    }
  }
}