package modules

import java.net.URI
import javax.inject.{Provider, Inject, Singleton}

import akka.actor.{ActorRef, ActorSystem}
import doc.DocRenderer
import play.api.libs.ws.WSClient

object ConductRModule {

  @Singleton
  class ConductRDocRendererProvider @Inject()(actorSystem: ActorSystem, wsClient: WSClient) extends Provider[ActorRef] {

    override def get =
      actorSystem.actorOf(DocRenderer.props(
        new URI("https://github.com/typesafehub/typesafe-conductr/tree/master/doc"),
        wsClient), "conductr-doc-renderer")
  }
}
