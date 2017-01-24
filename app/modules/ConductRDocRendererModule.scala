package modules

import java.net.URI
import java.nio.file.Paths
import javax.inject.{Provider, Inject, Singleton}

import akka.actor.{ActorRef, ActorSystem}
import doc.DocRenderer
import play.api.{Configuration, Environment}
import play.api.inject.Module
import play.api.libs.ws.WSClient

import scala.collection.immutable

object ConductRDocRendererModule {

  abstract class ConductRDocRendererProvider(actorSystem: ActorSystem, wsClient: WSClient, docArchive: URI, version: String, versions: immutable.Seq[String])
    extends Provider[ActorRef] {

    private val renderer =
      actorSystem.actorOf(DocRenderer.props(
        docArchive,
        removeRootSegmentOfArchive = true,
        Paths.get("src/main/play-doc"),
        controllers.routes.Application.renderDocsHome(version).url,
        version,
        versions,
        wsClient), s"conductr-doc-renderer-$version")

    override def get = renderer
  }

  private val versions = immutable.Seq(
    "1.0.x",
    "1.1.x",
    "2.0.x",
    "2.1.x"
  )

  @Singleton
  class ConductRDocRendererProvider10 @Inject()(actorSystem: ActorSystem, wsClient: WSClient)
    extends ConductRDocRendererProvider(
      actorSystem,
      wsClient,
      new URI("https://github.com/typesafehub/conductr-doc/archive/1.0.zip"),
      "1.0.x",
      versions
    )

  @Singleton
  class ConductRDocRendererProvider11 @Inject()(actorSystem: ActorSystem, wsClient: WSClient)
    extends ConductRDocRendererProvider(
      actorSystem,
      wsClient,
      new URI("https://github.com/typesafehub/conductr-doc/archive/1.1.zip"),
      "1.1.x",
      versions
    )

  @Singleton
  class ConductRDocRendererProvider20 @Inject()(actorSystem: ActorSystem, wsClient: WSClient)
    extends ConductRDocRendererProvider(
      actorSystem,
      wsClient,
      new URI("https://github.com/typesafehub/conductr-doc/archive/2.0.zip"),
      "2.0.x",
      versions
    )

  @Singleton
  class ConductRDocRendererProvider21 @Inject()(actorSystem: ActorSystem, wsClient: WSClient)
    extends ConductRDocRendererProvider(
      actorSystem,
      wsClient,
      new URI("https://github.com/typesafehub/conductr-doc/archive/master.zip"),
      "2.1.x",
      versions
    )
}

class ConductRDocRendererModule extends Module {
  import ConductRDocRendererModule._

  def bindings(environment: Environment,
               configuration: Configuration) = Seq(
    bind[ActorRef].qualifiedWith("ConductRDocRenderer10").toProvider[ConductRDocRendererProvider10],
    bind[ActorRef].qualifiedWith("ConductRDocRenderer11").toProvider[ConductRDocRendererProvider11],
    bind[ActorRef].qualifiedWith("ConductRDocRenderer20").toProvider[ConductRDocRendererProvider20],
    bind[ActorRef].qualifiedWith("ConductRDocRenderer21").toProvider[ConductRDocRendererProvider21]
  )
}