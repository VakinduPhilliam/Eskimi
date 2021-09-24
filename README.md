
# Eskimi Real-time bidding system

A real-time bidding agent that accepts JSON Requests, does some pattern matching between advertising campaigns and the received bid requests and responds with either a matched campaign bid or an empty response (No Content).

## Technology Installation requirements

- Sbt 1.4.0

- Scala 2.13.1

- Akka 2.6.8

- VS Code (Code Editor)

(The app also uses Giter8 for templating and ScalaTest for Unit/ Integration Tests - preinstalled with app execution.)

## How to Run the App

- Clone the App or download onto your PC.

- Navigate to the app's top directly. Enter `cd path/to/app/directory` in your command terminal.

- Enter `sbt run` in the terminal. App will download dependencies and start running.

- Open your API tester (I use Postman) and make a bid request by following the steps below.


## Make a Bid Request to the API:

To make a bid request, send a valid bid post request to the /bid endpoint.

POST: http://127.0.0.1:8980/bid

The result of the request will be seen at the /banners API endpoint.

GET: http://127.0.0.1:8980/banners



## Bid Request Parameter Validation:

> The required bid parameters:

- At least one of either h, hmin, or hmax must exist. 

- At least one of w, wmin, or wmax must exist.

- The bidding price cannot be less than bidFloor.


## Pattern Matching Mechanics between Campaign and Bid  

Ad Pattern Matching Mechanics:

- All bid request inputs are converted into Options when passed onto the bidding api.

- I compare the Campaign banner's width and height with h, w, and wmax, hmin, wmin, hmax.

- I created a sortBy mechanism that prioritizes user.geo over device.geo

- I capture bid request parameters using for-comprehensions.

- I use slice() to ensure that only the last highest ranking banner in the Seq is left.

- I check if h and w are defined in bid request, if yes use them to filter Seq values.

- I compare h to hmin/hmax and w to wmin/wmax to ensure the bid request is within campaign parameters.

- I ensure that the bid price meets the desired campaign cost.


