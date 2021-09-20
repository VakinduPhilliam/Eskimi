//#Main App
package com.example


import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.Done
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._

import scala.io.StdIn

import scala.concurrent.Future

object WebServer {

      // Needed to run the route
      implicit val system = ActorSystem()
      implicit val materializer = ActorMaterializer()

      // Needed for the future map/flatmap in the end and future in fetchCampaign and matchBid
      implicit val executionContext = system.dispatcher
      
      // Storage for active campaigns
      val activeCampaigns = Seq(
          Campaign(
              id = 1,
              country = "LT",
              targeting = Targeting(
                  targetedSiteIds = Seq("0006a522ce0f4bbbbaa6b3c38cafaa0f") 
                  ),
                  banners = List(
                      Banner(
                          id = 1,
                          src ="https://business.eskimi.com/wp-content/uploads/2020/06/openGraph.jpeg",
                          width = 300,
                          height = 250
                          )
                        ),
                  bid = 5d
                )
            )

      // Storage for banners found
      var banners: List[BidResponse] = Nil

      // Campaign protocols
      case class Campaign(id: Int, country: String, targeting: Targeting, banners: List[Banner], bid: Double)
      case class Targeting(targetedSiteIds: Seq[String])
      case class Banner(id: Int, src: String, width: Int, height: Int)


      // Bid models
      case class BidRequest(id: String, imp: Option[List[Impression]], site:Site, user: Option[User], device: Option[Device])
      case class BidResponse(id: String, bidRequestId: String, price: Double, adid:Option[String], banner: Option[Banner])

      // Models for Datatypes
      case class Impression(id: String, wmin: Option[Int], wmax: Option[Int], w: Option[Int], hmin: Option[Int], hmax: Option[Int], h: Option[Int], bidFloor: Option[Double])
      case class Site(id: Int, domain: String)
      case class User(id: String, geo: Option[Geo])
      case class Device(id: String, geo: Option[Geo])
      case class Geo(country: Option[String])

      // Formats for unmarshalling and marshalling
      implicit val resFormat = jsonFormat2(BidResponse)
      implicit val bidFormat = jsonFormat1(BidRequest)

      // Fetch Response results
      def fetchBanners(): Future[Option[BidResponse]] = Future {
        banners
      }

      // Process the bid
      def processBid(bid: BidRequest): Future[Done] = {
        banners = bid match {
          case BidRequest(id, imp, site, user, device) => 

          /*
          * Ad Pattern Matching Mechanics:
          * - All Inputs are converted into Options between passed onto Bidding Agent.
          * - Compare the Campaign banner's width and height with h, w, and wmax, hmin, wmin, hmax.
          * - Created a sort by mechanics that prioritizes user.geo over device.geo
          * - Capture comparison parameters from bid request using for comprehensions
          * - SortBy should always ensure device.geo is ranked higher than user.geo
          * - Use slice() to ensure that only the last highest ranking banner in the Seq is left.
          * - Check if h and w are defined in bid request, if yes use them to filter Seq values.
          */

          // Begin by capturing comparison parameters from bid request using for comprehensions
          val h = for{ 
              val1 <- imp
              val2 <- imp.h
              } yield val2
              
          val w = for{ 
              val1 <- imp
              val2 <- imp.w
              } yield val2

          val hmin = for{ 
              val1 <- imp
              val2 <- imp.hmin
              } yield val2

          val hmax = for{ 
              val1 <- imp
              val2 <- imp.hmax
              } yield val2

          val wmin = for{ 
              val1 <- imp
              val2 <- imp.wmin
              } yield val2

          val wmax = for{ 
              val1 <- imp
              val2 <- imp.wmax
              } yield val2

          val bid_floor = for{ 
              val1 <- imp
              val2 <- imp.bidFloor
              } yield val2  

          val country = for{ 
              val1 <- user
              val2 <- user.country
              } yield val2  

          val s_id = for{ 
              val1 <- site
              val2 <- site.id
              } yield val2 

          val site_id = Targeting(Seq(s_id))

          val geoUser = for{ 
              val1 <- user
              val2 <- user.geo
              } yield val2      

          val geoDevice = for{ 
              val1 <- device
              val2 <- device.geo
              } yield val2  

          val imp = for{ 
              val1 <- imp
              } yield val1

          val id = for{ 
              val1 <- id
              } yield val1


          // Declare comparison variables
          var th=0
          var tw=0
          var tgeo="LT"

          // If h exists, use it as comparison variable, else use hmin and hmax
          if(toInt(h.toString)!=None){
              th=h
          } else {
              if (toInt(hmin.toString)!=None) {
                  th=hmin
              } else {
                  th=hmax
              }
          }

          // If w exists, use it as comparison variable, else use wmin and wmax
          if(toInt(w.toString)!=None){
              tw=w
          } else {
              if (toInt(wmin.toString)!=None) {
                  tw=wmin
              }
              else {
                  tw=wmax
              }
          }

          // If device.geo exists, use it as comparison variable over user.geo
          if(!geoDevice.isEmpty){
              tgeo=geoDevice
          } else {
              tgeo=geoUser
          }

          // Integer value detector
          def toInt(s: String): Option[Int] = {
              try {
                  Some(Integer.parseInt(s.trim))
                  } catch {
                      case e: Exception => None
                  }
          }

          // Appy the filtering logic to the campaigns
          val filteredCampaigns =
          activeCampaigns.filter(c => c.country == tgeo && // filter country
                     c.banners.exists(_.height == th)  && // filter campaign height
                     c.banners.exists(_.width == tw) && // filter campaign width
                     c.banners.exists(_.height <= hmax)  && // Campaign height must be less than max banner height
                     c.banners.exists(_.width <= wmax) && // Campaign width must be less than max banner width
                     c.banners.exists(_.height >= hmin)  && // Campaign height must be greater than min banner height
                     c.banners.exists(_.width >= wmin) && // Campaign width must be greater than min banner width
                     c.targeting == site_id && // Targeting sites must be present in campaign
                     (c.bid).toFloat >= bid_floor) // Bid request must be less than campaign cost
                     

          // Create Bid Response
          BidResponse("response1", id, bid_floor, filteredCampaigns.id, filteredCampaigns.banners)

          case _            => Nil
        }
        Future { Done }
      }

      def main(args: Array[String]) {

        val route: Route =
          get {
            pathPrefix("banners") { 

              // There might be no item for a given campaign
              val getBanner: Future[Option[BidResponse]] = fetchBanners()

              onSuccess(getBanner) {
                case Some(banner) => complete(banner)
                case None       => complete(StatusCodes.NotFound)
              }
            }
          } ~
            post {
              path("bid") {
                entity(as[String]) { bid_request =>
                  // Convert request to BidRequest format
                  val convertBid = BidRequest(bid_request.id, bid_request.imp, bid_request.site, bid_request.user, bid_request.device)
                  val matched: Future[Done] = processBid(convertBid) // Pass request for processing
                  onComplete(matched) { done =>
                    complete("bid matched")
                  }
                }
              }
            }

        val bindingFuture = Http().bindAndHandle(route, "localhost", 8980)
        println(s"Ekismi Bidding Agent Server is live at http://localhost:8080/\nPress RETURN to stop...")
        StdIn.readLine() // let it run until user presses return
        bindingFuture
          .flatMap(_.unbind()) // trigger unbinding from the port
          .onComplete(_ ⇒ system.terminate()) // and shutdown when done

      }
}
