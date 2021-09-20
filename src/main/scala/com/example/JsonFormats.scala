package com.example

import com.example.WebServer.ActionPerformed

//#json-formats
import spray.json.DefaultJsonProtocol

object JsonFormats  {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  //implicit val reqJsonFormat = jsonFormat3(BidRequest)
  //implicit val resJsonFormat = jsonFormat1(BidResponse)

  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)
}
//#json-formats
