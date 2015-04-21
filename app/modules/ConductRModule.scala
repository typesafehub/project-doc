package modules

import java.net.URI
import java.nio.file.Paths
import javax.inject.{Provider, Inject, Singleton}

import akka.actor.{ActorRef, ActorSystem}
import doc.DocRenderer
import play.api.{Configuration, Environment}
import play.api.inject.Module
import play.api.libs.ws.WSClient

object ConductRModule {

  @Singleton
  class ConductRDocRendererProvider @Inject()(actorSystem: ActorSystem, wsClient: WSClient)
    extends Provider[ActorRef] {

    private val renderer =
      actorSystem.actorOf(DocRenderer.props(
        new URI("https://github.com/typesafehub/conductr-doc/archive/master.zip"),
        removeRootSegment = true,
        Paths.get("src/main/play-doc"),
        "1.0.x",
        wsClient), "conductr-doc-renderer")

    override def get = renderer
  }
}

class ConductRModule extends Module {
  import ConductRModule._

  def bindings(environment: Environment,
               configuration: Configuration) = Seq(
    bind[ActorRef].qualifiedWith("ConductRDocRenderer").toProvider[ConductRDocRendererProvider]
  )
}