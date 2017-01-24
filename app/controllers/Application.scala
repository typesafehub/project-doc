package controllers

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.{Inject, Named}

import akka.actor.ActorRef
import akka.pattern.{AskTimeoutException, ask}
import akka.stream.scaladsl.Source
import akka.util.ByteString
import doc.DocRenderer
import play.api.http.HttpEntity
import play.api.libs.MimeTypes
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Iteratee
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}
import play.api.libs.streams.{Accumulator, Streams}
import play.api.mvc._
import play.twirl.api.Html
import settings.Settings

import scala.concurrent.Future

object Application {

  private[controllers] object MacBodyParser {
    def apply(hmacHeader: String, secret: SecretKeySpec, algorithm: String, maxBodySize: Int = 65536) =
      new MacBodyParser(hmacHeader, secret, algorithm, maxBodySize)
  }

  private[controllers] class MacBodyParser(
    hmacHeader: String,
    secret: SecretKeySpec,
    algorithm: String,
    maxBodySize: Int) extends BodyParser[ByteString] {

    def hex2bytes(hex: String): Array[Byte] =
      hex.replaceAll("[^0-9A-Fa-f]", "").sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte)

    override def apply(request: RequestHeader): Accumulator[ByteString, Either[Result, ByteString]] = {
      val hexSignature = request.headers.get(hmacHeader).map(_.dropWhile(_ != '=').drop(1)).getOrElse("")
      val signature = hex2bytes(hexSignature)
      val iteratee =
        Iteratee.fold[ByteString, (Mac, ByteString)] {
          val mac = Mac.getInstance(algorithm)
          mac.init(secret)
          (mac, ByteString.empty)
        } {
          case ((mac, buffer), bytes) =>
            mac.update(bytes.toArray)
            val newBuffer = if (buffer.length + bytes.length <= maxBodySize) buffer ++ bytes else buffer
            (mac, newBuffer)
        }
        .map {
          case _ if signature.isEmpty =>
            Left(Results.BadRequest(s"No $hmacHeader header present"))
          case (mac, buffer) =>
            val macBytes = mac.doFinal()
            if (macBytes.sameElements(signature))
              Right(buffer)
            else
              Left(Results.Unauthorized(s"""SHA1 signature of ${macBytes.map("%02x" format _).mkString} did not match that of the $hmacHeader header"""))
        }
      Streams.iterateeToAccumulator(iteratee)
    }
  }

  private def getDocRenderer(
    host: String,
    pathVersion: String => Option[String],
    docRenderers: Map[String, Map[String, ActorRef]],
    hostPrefixAliases: Map[String, String]): Option[ActorRef] = {

    val hostPrefix = host.takeWhile(c => c != '.' && c != ':')

    val resolvedHostPrefix = if (docRenderers.contains(hostPrefix)) Some(hostPrefix) else hostPrefixAliases.get(hostPrefix)

    for {
      hp <- resolvedHostPrefix
      dv <- docRenderers.get(hp)
      pv <- pathVersion(hp)
      dr <- dv.get(pv)
    } yield dr
  }
}

class Application @Inject() (
                              @Named("ConductRDocRenderer10") conductrDocRenderer10: ActorRef,
                              @Named("ConductRDocRenderer11") conductrDocRenderer11: ActorRef,
                              @Named("ConductRDocRenderer20") conductrDocRenderer20: ActorRef,
                              @Named("ConductRDocRenderer21") conductrDocRenderer21: ActorRef,
                              settings: Settings) extends Controller {

  import Application._

  def renderIndex = Action {
    Ok(views.html.conductr.index())
  }

  def renderDocsHome(version: String) =
    renderDocs("", version)

  def renderResources(path: String, version: String) =
    renderDocs(path, version)

  def renderDocs(path: String, version: String) = Action.async { request =>
    request.headers.get(HOST) match {
      case Some(host) =>
        getDocRenderer(host, _ => Some(version), docRenderers, settings.application.hostAliases) match {
          case Some(docRenderer) =>
            docRenderer
              .ask(DocRenderer.Render(path))(settings.doc.renderer.timeout)
              .map {
              case html: Html                     => Ok(html)
              case resource: DocRenderer.Resource => renderResource(resource, path)
              case DocRenderer.Redirect(rp, v)    => Redirect(routes.Application.renderDocs(rp, v))
              case DocRenderer.NotFound(rp)       => NotFound(s"Cannot find $rp")
              case DocRenderer.NotReady           => ServiceUnavailable("Initializing documentation. Please try again in a minute.")
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
        Json.parse(request.body.toArray).validate[String](webhookRef) match {
          case JsSuccess(ref, _) =>
            val branch = ref.reverse.takeWhile(_ != '/').reverse

            def branchToVersion(hostPrefix: String): Option[String] =
              branchesToVersions.get(hostPrefix).flatMap(_.get(branch))

            getDocRenderer(host, branchToVersion, docRenderers, settings.application.hostAliases) match {
              case Some(docRenderer) =>
                docRenderer ! DocRenderer.PropogateGetSite
                Ok("Site update requested")
              case None =>
                Ok(s"Site update requested for Unknown project: $host - ignoring")
            }
          case e: JsError =>
            BadRequest(s"Cannot parse webhook: $e")
        }
      case None =>
        NotFound("No host header")
    }
  }

  private final val MacAlgorithm = "HmacSHA1"
  private final val GitHubSignature = "X-Hub-Signature"

  private val docRenderers = Map(
    "conductr" -> Map(
      "" -> conductrDocRenderer20,
      "1.0.x" -> conductrDocRenderer10,
      "1.1.x" -> conductrDocRenderer11,
      "2.0.x" -> conductrDocRenderer20,
      "2.1.x" -> conductrDocRenderer21
    )
  )

  private val branchesToVersions = Map(
    "conductr" -> Map(
      "1.0" -> "1.0.x",
      "1.1" -> "1.1.x",
      "2.0" -> "2.0.x",
      "master" -> "2.1.x"
    )
  )

  private val secret = new SecretKeySpec(settings.play.crypto.secret.getBytes, MacAlgorithm)
  private val webhookRef = (JsPath \ "ref").read[String]

  private def renderResource(resource: DocRenderer.Resource, path: String): Result = {
    val fileName = path.drop(path.lastIndexOf('/') + 1)
    val bodyPublisher = Streams.enumeratorToPublisher(resource.content)
    val bodySource = Source.fromPublisher(bodyPublisher)
    val entity = HttpEntity.Streamed(bodySource.map(ByteString(_)), Some(resource.size), Some(MimeTypes.forFileName(fileName).getOrElse(BINARY)))
    Ok.sendEntity(entity)
  }

}
