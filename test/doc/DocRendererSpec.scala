package doc

import java.io.File

import org.scalatest.Matchers
import org.scalatest.WordSpecLike
import play.api.libs.iteratee.Enumerator
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class DocRendererSpec extends WordSpecLike with Matchers {

  "DocRenderer" should {
    "Receive a zipped stream, decompress it and write it to a file" in {
      val zipFile = getClass.getClassLoader.getResource("conductr-doc.zip")
      val input = Enumerator.fromStream(zipFile.openStream())
      val result = DocRenderer.unzip(input)
      val docDir = Await.result(result, 5.seconds)
      docDir.resolve("conductr-doc").toFile.exists() shouldBe true
    }

    "Form html from the toc files of the zipped stream" in {
      val docDir = new File(getClass.getClassLoader.getResource("conductr-doc").toURI).toPath
      val html = DocRenderer.aggregateToc(docDir)
      println(html)
    }
  }
}
