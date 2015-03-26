package modules

import java.net.URI

import akka.actor.Props
import com.google.inject.AbstractModule
import com.google.inject.name.Names
import doc.DocRenderer

class ConductRModule extends AbstractModule {

  def configure(): Unit =
    bind(classOf[Props])
      .annotatedWith(Names.named("conductrDocRenderer"))
      .toInstance(DocRenderer.props(new URI("https://github.com/typesafehub/typesafe-conductr/tree/master/doc")))
}
