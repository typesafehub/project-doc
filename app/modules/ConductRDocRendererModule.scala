package modules

import java.net.URI
import java.nio.file.Paths
import javax.inject.{Provider, Inject, Singleton}

import akka.actor.{ActorRef, ActorSystem}
import doc.DocRenderer
import play.api.{Configuration, Environment}
import play.api.inject.Module
import play.api.libs.ws.WSClient

object ConductRDocRendererModule {

  abstract class ConductRDocRendererProvider(actorSystem: ActorSystem, wsClient: WSClient, docArchive: URI, version: String)
    extends Provider[ActorRef] {

    private val renderer =
      actorSystem.actorOf(DocRenderer.props(
        docArchive,
        removeRootSegmentOfArchive = true,
        Paths.get("src/main/play-doc"),
        controllers.routes.Application.renderDocsHome(version).url,
        version,
        wsClient), s"conductr-doc-renderer-$version")

    override def get = renderer
  }

  @Singleton
  class ConductRDocRendererProvider10 @Inject()(actorSystem: ActorSystem, wsClient: WSClient)
    extends ConductRDocRendererProvider(
      actorSystem,
      wsClient,
      new URI("https://github.com/typesafehub/conductr-doc/archive/1.0.zip"),
      "1.0.x"
    )

  @Singleton
  class ConductRDocRendererProvider11 @Inject()(actorSystem: ActorSystem, wsClient: WSClient)
    extends ConductRDocRendererProvider(
      actorSystem,
      wsClient,
      new URI("https://github.com/typesafehub/conductr-doc/archive/master.zip"),
      "1.1.x"
    )
}

class ConductRDocRendererModule extends Module {
  import ConductRDocRendererModule._

  def bindings(environment: Environment,
               configuration: Configuration) = Seq(
    bind[ActorRef].qualifiedWith("ConductRDocRenderer10").toProvider[ConductRDocRendererProvider10],
    bind[ActorRef].qualifiedWith("ConductRDocRenderer11").toProvider[ConductRDocRendererProvider11]
  )
}