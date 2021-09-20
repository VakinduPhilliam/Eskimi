package com.example

//#bid-routes-spec
//#test-top
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

//#set-up
class UserRoutesSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest {
  //#test-top

  // the Akka HTTP route testkit does not yet support a typed actor system (https://github.com/akka/akka-http/issues/2036)
  // so we have to adapt for now
  lazy val testKit = ActorTestKit()
  implicit def typedSystem = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  // Here we need to implement all the abstract members of Bid Routes.
  // We use the real UserRegistryActor to test it while we hit the Routes,
  // but we could "mock" it by implementing it in-place or by using a TestProbe
  // created with testKit.createTestProbe()
  val bidRegistry = testKit.spawn(WebServer())
  lazy val routes = WebServer(bidRegistry).route

  // use the json formats to marshal and unmarshall objects in the test
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._
  //#set-up

  //#actual-test
  "CampaignRoutes" should {
    "return no campaigns if not present (GET /banners)" in {
      // note that there's no need for the host part in the uri:
      val request = HttpRequest(uri = "/banners")

      request ~> routes ~> check {
        status should ===(StatusCodes.NotFound)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and no entries should be in the list:
        entityAs[String] should ===("""{}""")
      }
    }
    //#actual-test

    //#testing-post
    "be able to add a bid (POST /bid)" in {
      val bid = BidRequest("SGu1Jpq1IO", """[{"id": "1","wmin": 50,"wmax": 300,"hmin": 100,"hmax": 300,"h": 250,"w": 300,"bidFloor": 3.12123}]""", """{"id": "0006a522ce0f4bbbbaa6b3c38cafaa0f","domain": "fake.tld"}""", """{"geo": {"country": "LT"},"id": "USARIO1"}""", """{"id": "440579f4b408831516ebd02f6e1c31b4","geo": {"country": "LT"}}""")
      val bidEntity = Marshal(bid).to[MessageEntity].futureValue // futureValue is from ScalaFutures

      // using the RequestBuilding DSL:
      val request = Post("/bid").withEntity(bidEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.Created)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and we know what message we're expecting back:
        entityAs[String] should ===("""{"id": "response1","bidRequestId": "SGu1Jpq1IO","price": 3.12123,"adId": "1","banner": [{"id": "1","height": 250,"width": 300,"src":"https://business.eskimi.com/wp-content/uploads/2020/06/openGraph.jpeg"}]}""")
      }
    }
    //#testing-post

  }
  //#actual-test

  //#set-up
}
//#set-up
//#bid-routes-spec
