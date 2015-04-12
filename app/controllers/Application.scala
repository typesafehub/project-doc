package controllers

import java.io.File
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

import akka.actor.ActorRef
import akka.pattern.{AskTimeoutException, ask}
import doc.DocRenderer
import modules.ConductRModule.ConductRDocRendererProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Iteratee
import play.api.mvc._
import play.twirl.api.Html
import settings.Settings

import scala.concurrent.Future

object Application {

  private[controllers] object MacBodyParser {
    def apply(hmacHeader: String, secret: SecretKeySpec, algorithm: String) =
      new MacBodyParser(hmacHeader, secret, algorithm)
  }

  private[controllers] class MacBodyParser(
    hmacHeader: String,
    secret: SecretKeySpec,
    algorithm: String) extends BodyParser[Unit] {

    def hex2bytes(hex: String): Array[Byte] =
      hex.replaceAll("[^0-9A-Fa-f]", "").sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte)

    override def apply(request: RequestHeader): Iteratee[Array[Byte], Either[Result, Unit]] = {
      val hexSignature = request.headers.get(hmacHeader).map(_.dropWhile(_ != '=').drop(1)).getOrElse("")
      val signature = hex2bytes(hexSignature)
      Iteratee.fold[Array[Byte], Mac] {
        val mac = Mac.getInstance(algorithm)
        mac.init(secret)
        mac
      } { (mac, bytes) =>
        mac.update(bytes)
        mac
      }.map {
        case _   if signature.isEmpty                     => Left(Results.BadRequest(s"No $hmacHeader header present"))
        case mac if mac.doFinal().sameElements(signature) => Right(())
        case _                                            => Left(Results.Unauthorized("Bad signature"))
      }
    }
  }

  private def getDocRenderer(
    host: String,
    docRenderers: Map[String, ActorRef],
    hostPrefixAliases: Map[String, String]): Option[ActorRef] = {
    val hostPrefix = host.takeWhile(c => c != '.' && c != ':')
    docRenderers.get(hostPrefix).orElse {
      hostPrefixAliases.get(hostPrefix) match {
        case Some(aliasedHostPrefix) => docRenderers.get(aliasedHostPrefix)
        case None                    => None
      }
    }
  }
}

class Application @Inject() (
  conductrDocRendererProvider: ConductRDocRendererProvider,
  settings: Settings) extends Controller {

  import Application._

  private final val MacAlgorithm = "HmacSHA1"
  private final val GitHubSignature = "X-Hub-Signature"

  private val docRenderers = Map("conductr" -> conductrDocRendererProvider.get)

  private val secret = new SecretKeySpec(settings.play.crypto.secret.getBytes, MacAlgorithm)

  def render(path: String) = Action.async { request =>
    request.headers.get(HOST) match {
      case Some(host) =>
        getDocRenderer(host, docRenderers, settings.application.hostAliases) match {
          case Some(docRenderer) =>
            docRenderer
              .ask(DocRenderer.Render(path))(settings.doc.renderer.timeout)
              .map {
              case html: Html               => Ok(html)
              case resource: File           => Ok.sendFile(resource)
              case DocRenderer.NotFound(rp) => NotFound(s"Cannot find $rp")
              case DocRenderer.NotReady     => ServiceUnavailable("Initializing documentation. Please try again in a minute.")
            }
            .recover {
              case _: AskTimeoutException => InternalServerError
            }
          case None =>
            Future.successful(NotFound(s"Unknown project: $host"))
        }
      case None =>
        Future.successful(NotFound("No host header"))
    }
  }

  def update() = Action(MacBodyParser(GitHubSignature, secret, MacAlgorithm)) { request =>
    request.headers.get(HOST) match {
      case Some(host) =>
        getDocRenderer(host, docRenderers, settings.application.hostAliases) match {
          case Some(docRenderer) =>
            docRenderer ! DocRenderer.GetSite
            Ok("Site update requested")
          case None =>
            NotFound(s"Unknown project: $host")
        }
      case None =>
        NotFound("No host header")
    }
  }
}
