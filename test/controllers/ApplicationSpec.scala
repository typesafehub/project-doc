package controllers

import javax.crypto.spec.SecretKeySpec

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.TestProbe
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import doc.DocRenderer
import org.scalatest.{EitherValues, Matchers, WordSpecLike}
import play.api.Configuration
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.Json
import play.api.libs.streams.Streams
import play.api.test._
import play.api.test.Helpers._
import settings.Settings

import scala.concurrent.Await
import scala.concurrent.duration._

class ApplicationSpec  extends WordSpecLike with Matchers with EitherValues {

  "MacBodyParser" should {
    "produce the body when the body digest matches the signature" in withActorSystem { implicit system =>
      val fixtures = macBodyParserFixtures()
      import fixtures._

      implicit val mat = ActorMaterializer()

      val request = FakeRequest("GET", "/").withHeaders(hmacHeader -> s"sha1=1f30c9572859472be574afa5dcc641d3184894bb")
      val bodyParser = Application.MacBodyParser(hmacHeader, secret, algorithm)

      val bodyBytes = ByteString(body)
      val result = Enumerator(bodyBytes) |>>> Streams.accumulatorToIteratee(bodyParser(request))
      Await.result(result, 5.seconds).right.value shouldBe bodyBytes
    }

    "produce an unauthorized result when the signature in the header has an invalid value" in withActorSystem { implicit system =>
      val fixtures = macBodyParserFixtures()
      import fixtures._

      implicit val mat = ActorMaterializer()

      val signature = Array[Byte](0, 1, 2)

      val request = FakeRequest("GET", "/").withHeaders(hmacHeader -> bytes2hex(signature))
      val bodyParser = Application.MacBodyParser(hmacHeader, secret, algorithm)

      val result = Enumerator(ByteString(body)) |>>> Streams.accumulatorToIteratee(bodyParser(request))
      Await.result(result, 5.seconds).isLeft shouldBe true
    }
  }

  /*
   * These tests use the default secret contained in application.conf to verify the HMAC sent
   * through thus giving settings a test also. To determine what the HMAC should be for a given
   * JSON body, take the parsed string of the JSON and paste it into: http://www.freeformatter.com/hmac-generator.html
   */

  "Application" should {
    "reject bad webhooks" in withActorSystem { system =>
      val fixtures = controllerFixtures(system)
      import fixtures._

      implicit val mat = ActorMaterializer()

      val request = FakeRequest("POST", "/")

      status(call(application.update(), request)) shouldBe BAD_REQUEST
    }

    "reject unauthorized webhooks" in withActorSystem { system =>
      val fixtures = controllerFixtures(system)
      import fixtures._

      implicit val mat = ActorMaterializer()

      val request =
        FakeRequest("POST", "/")
          .withHeaders(
            HOST -> "conductr.lightbend.com",
            hmacHeader -> "sha1=somerubbish"
          )

      status(call(application.update(), request)) shouldBe UNAUTHORIZED
    }

    "select a renderer corresponding to a branch related to its webhook event" in withActorSystem { system =>
      val fixtures = controllerFixtures(system)
      import fixtures._

      implicit val mat = ActorMaterializer()

      val request1 =
        FakeRequest("POST", "/")
          .withHeaders(
            HOST -> "conductr.lightbend.com",
            hmacHeader -> "sha1=3e9b7bc70183ec26fa834fca39907c61b4138b6a"
          )
          .withJsonBody(Json.parse("""{
                                     |  "ref": "refs/heads/1.0",
                                     |  "before": "0000000000000000000000000000000000000000",
                                     |  "after": "335aa74f382a36c7a3594a8fec14fdd2ac754cb2"
                                     |}""".stripMargin))

      val result1 = call(application.update(), request1)
      status(result1) shouldBe OK
      contentAsString(result1) shouldBe "Site update requested"

      conductrDocRenderer10.expectMsg(DocRenderer.PropogateGetSite)
      conductrDocRenderer11.expectNoMsg(500.millis)
      conductrDocRenderer20.expectNoMsg(500.millis)
      conductrDocRenderer21.expectNoMsg(500.millis)

      val request2 =
        FakeRequest("POST", "/")
          .withHeaders(
            HOST -> "conductr.lightbend.com",
            hmacHeader -> "sha1=b12698344d3cf850c4c00d80e7223de9df569b32"
          )
          .withJsonBody(Json.parse("""{
                                     |  "ref": "refs/heads/1.1",
                                     |  "before": "0000000000000000000000000000000000000000",
                                     |  "after": "335aa74f382a36c7a3594a8fec14fdd2ac754cb2"
                                     |}""".stripMargin))

      val result2 = call(application.update(), request2)
      status(result2) shouldBe OK
      contentAsString(result2) shouldBe "Site update requested"

      conductrDocRenderer11.expectMsg(DocRenderer.PropogateGetSite)
      conductrDocRenderer10.expectNoMsg(500.millis)
      conductrDocRenderer20.expectNoMsg(500.millis)
      conductrDocRenderer21.expectNoMsg(500.millis)

      val request3 =
        FakeRequest("POST", "/")
          .withHeaders(
            HOST -> "conductr.lightbend.com",
            hmacHeader -> "sha1=fd03a52d80be9c184ab9a6318e1d1e0b5f629d71"
          )
          .withJsonBody(Json.parse("""{
                                     |  "ref": "refs/heads/master",
                                     |  "before": "0000000000000000000000000000000000000000",
                                     |  "after": "335aa74f382a36c7a3594a8fec14fdd2ac754cb2"
                                     |}""".stripMargin))

      val result3 = call(application.update(), request3)
      status(result3) shouldBe OK

      contentAsString(result3) shouldBe "Site update requested"

      conductrDocRenderer21.expectNoMsg(500.millis)
      conductrDocRenderer20.expectMsg(DocRenderer.PropogateGetSite)
      conductrDocRenderer11.expectNoMsg(500.millis)
      conductrDocRenderer10.expectNoMsg(500.millis)
    }

    "silenty reject webhooks specifying branches we don't know about" in withActorSystem { system =>
      val fixtures = controllerFixtures(system)
      import fixtures._

      implicit val mat = ActorMaterializer()

      val request =
        FakeRequest("POST", "/")
          .withHeaders(
            HOST -> "conductr.lightbend.com",
            hmacHeader -> "sha1=a07de662e1ff3a4e5374aecd07fd480a1ebfe153"
          )
          .withJsonBody(Json.parse("""{
                                     |  "ref": "refs/heads/rubbish",
                                     |  "before": "0000000000000000000000000000000000000000",
                                     |  "after": "335aa74f382a36c7a3594a8fec14fdd2ac754cb2"
                                     |}""".stripMargin))

      status(call(application.update(), request)) shouldBe OK

      conductrDocRenderer10.expectNoMsg(500.millis)
      conductrDocRenderer11.expectNoMsg(500.millis)
    }
  }

  val hmacHeader = "X-Hub-Signature"

  private def macBodyParserFixtures() = new {
    val algorithm = "HmacSHA1"
    val secret = new SecretKeySpec("somesecret".getBytes, algorithm)

    val body = "some body"

    def bytes2hex(bytes: Array[Byte]): String =
    bytes.map("%02x".format(_)).mkString
  }

  private def controllerFixtures(system: ActorSystem) = new {
    implicit val s = system

    val conductrDocRenderer10 = TestProbe()
    val conductrDocRenderer11 = TestProbe()
    val conductrDocRenderer20 = TestProbe()
    val conductrDocRenderer21 = TestProbe()
    val config = ConfigFactory.load()
    val settings = new Settings(Configuration(config))

    val application = new Application(conductrDocRenderer10.ref, conductrDocRenderer11.ref, conductrDocRenderer20.ref, conductrDocRenderer21.ref, settings)
  }

  private def withActorSystem[T](block: ActorSystem => T): T = {
    val system = ActorSystem("ApplicationSpec")
    try {
      block(system)
    } finally {
      system.shutdown()
    }
  }
}
