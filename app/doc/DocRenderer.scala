package doc

import java.io.{File, FileOutputStream}
import java.net.URI
import java.nio.file.{Path, Files}

import akka.actor.{Actor, Props}
import akka.pattern.pipe
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.io.{FileUtils, IOUtils}
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.ws.{WSResponseHeaders, WSClient}
import play.twirl.api.Html
import views.html.conductr.index

import scala.collection.immutable
import scala.collection.JavaConverters._
import scala.concurrent.{Future, blocking, ExecutionContext}

object DocRenderer {
  case class Render(path: String)

  private[doc] sealed trait Entry
  private[doc] case class Folder(name: String, documents: immutable.Seq[Entry]) extends Entry
  private[doc] case class Document(name: String, ref: URI) extends Entry

  def props(docArchive: URI, wsClient: WSClient): Props =
    Props(new DocRenderer(docArchive, wsClient))
  
  private[doc] def unzip(input: Enumerator[Array[Byte]])(implicit ec: ExecutionContext): Future[Path] = {
    val archive = Files.createTempFile(null, null)
    val outputDir = Files.createTempDirectory("zip")
    FileUtils.forceDeleteOnExit(outputDir.toFile)

    val fos = new FileOutputStream(archive.toFile)

    val writeFile = Iteratee.foreach[Array[Byte]] { bytes =>
      fos.write(bytes)
    }

    (input |>>> writeFile).andThen {
      case _ =>
        blocking {
          fos.close()
          writeZip(archive, outputDir)
          archive.toFile.delete()
        }
    }.map(_ => outputDir)
  }

  private def writeZip(archive: Path, outputDir: Path): Unit = {
    val zipFile = new ZipFile(archive.toFile)
    try {
      import scala.collection.JavaConversions._
      for (entry <- zipFile.getEntries if !entry.isDirectory) {
        val path = outputDir.resolve(entry.getName)
        Files.createDirectories(path.getParent)
        val out = Files.newOutputStream(path)
        try {
          IOUtils.copyLarge(zipFile.getInputStream(entry), out,0, 8192)
        } finally {
          out.close()
        }
      }
    } finally {
      zipFile.close()
    }
  }

  private[doc] def aggregateToc(docDir: Path): Html = {
    import HtmlPrettyPrinter._

    val folder = createEntries(docDir.resolve("src/main/play-doc/index.toc"), Folder("", List.empty))

    def toDoc(documents: immutable.Seq[Entry]): Doc =
      ul(documents.map {
        case document: Document =>
          li(document.name)
        case folder: Folder =>
          toDoc(folder.documents)
      })

    val markup = aside(toDoc(folder.documents))

    Html(HtmlPrettyPrinter.pretty(markup))
  }

  private def createEntries(tocFile: Path, folder: Folder): Folder = {
    val tocEntryLines = FileUtils.readLines(tocFile.toFile, "UTF-8")
    val newDocuments = tocEntryLines.asScala.map { entry =>
      val entries = entry.split(":")
      val (filename, name) = if (entries.size == 2) (entries(0), entries(1)) else ("", "")
      if (filename.charAt(0).isLower) {
        createEntries(new File(new File(tocFile.getParent.toFile, filename), "index.toc").toPath, Folder(name, List.empty))
      } else {
        Document(name, new URI(filename))
      }
    }
    folder.copy(documents = folder.documents ++ newDocuments)
  }
}

/**
 * Renders documentation for a given URI representing an archive of documentation.
 * Performs blocking IO and thus requires a blocking dispatcher.
 */
class DocRenderer(docArchive: URI, wsClient: WSClient) extends Actor {

  import DocRenderer._
  import context.dispatcher

  override def preStart(): Unit =
    wsClient.url(docArchive.toString).getStream().pipeTo(self)

  override def receive: Receive =
    receiveSite

  private def receiveSite: Receive = {
    case (_: WSResponseHeaders, body: Enumerator[Array[Byte]] @unchecked) =>
      unzip(body).pipeTo(self)

    case docDir: Path =>
      val toc = aggregateToc(docDir)
      context.become(renderer(docDir, toc))
  }

  private def renderer(docDir: Path, toc: Html): Receive = {
    case Render(path) if path.isEmpty || path == "/" =>
      sender() ! Some(index(toc))
    case Render(_) =>
      sender() ! None
  }
}
