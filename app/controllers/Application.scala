package controllers

import java.util.concurrent.TimeUnit
import javax.inject.{Named, Inject}

import akka.actor.{ActorSystem, Props}
import akka.pattern.{AskTimeoutException, ask}
import akka.util.Timeout
import doc.DocRenderer
import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import play.twirl.api.Html

object Application {
  object Project extends Enumeration {
    val ConductR = Value
  }
}

class Application @Inject() (
  @Named("conductrDocRenderer") conductrDocRendererProps: Props,
  configuration: Configuration,
  system: ActorSystem) extends Controller {

  import Application._

  val conductrDocRenderer = system.actorOf(conductrDocRendererProps)

  implicit val timeout = Timeout(configuration.getMilliseconds("doc.renderer.timeout").getOrElse(5000L), TimeUnit.MILLISECONDS)

  def index(project: Project.Value, path: String) = Action.async {
    project match {
      case Project.ConductR =>
        conductrDocRenderer.actorRef
          .ask(DocRenderer.Render(path))
          .mapTo[Option[Html]]
          .map {
            case Some(html) => Ok(html)
            case None       => NotFound
          }
          .recover { case _: AskTimeoutException => NotFound }
    }
  }
}