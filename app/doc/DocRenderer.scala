package doc

import java.net.URI

import akka.actor.{Actor, Props}
import play.api.libs.ws.WSClient
import play.twirl.api.Html
import views.html.conductr.index

object DocRenderer {
  case class Render(path: String)

  def props(githubUri: URI, wsClient: WSClient): Props =
    Props(new DocRenderer(githubUri, wsClient))
}

class DocRenderer(githubUri: URI, wsClient: WSClient) extends Actor {

  import DocRenderer._

  override def receive: Receive = {
    case Render(path) if path.isEmpty || path == "/" =>
      // TODO: This TOC will be generated
      sender() ! Some(index(Html(
        """
          |<aside>
          |<h1><a href="/conductr">Home</a></h1>
          |<h1>Introducing ConductR</h1>
          |<ul>
          |  <li><a href="/conductr/install.html">Installation</a></li>
          |  <li><a href="/conductr/quickstart.html">Quickstart</a></li>
          |</ul>
          |</aside>""".stripMargin)))
    case Render(_) =>
      sender() ! None
  }
}
