package helpers

import java.sql.Timestamp

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object BinaryHelper {
    import akka.actor.ActorSystem
    import akka.{ Done, NotUsed }
    import akka.http.scaladsl.Http
    import akka.stream.ActorMaterializer
    import akka.stream.scaladsl._
    import akka.http.scaladsl.model._
    import akka.http.scaladsl.model.ws._

    import scala.concurrent.Future

    /*def getBinaryResult(contractDate: Timestamp, contractFxSymbol: String)(implicit ec: ExecutionContext): Future[String] = {
        /*authorizeWebsocketRequest.flatMap { authorizeMessage =>
            println("authorize message: " + authorizeMessage.asInstanceOf[TextMessage.Strict].text)
            getPortfolioWebsocketRequest(contractDate).mapTo[TextMessage.Strict].map(_.text) // TODO: ОПАСНЫЙ КАСТИНГ! ДЕЛАТЬ ПО-ДРУГОМУ!
        }*/
        authorizeWebsocketRequest.onComplete(t => println("Auth request was closed: " + t))
        Future.successful("dummy")
//        getPortfolioWebsocketRequest(contractDate).mapTo[TextMessage.Strict].map(_.text)
    }*/

    import akka.actor.ActorSystem
    import akka.Done
    import akka.http.scaladsl.Http
    import akka.stream.ActorMaterializer
    import akka.stream.scaladsl._
    import akka.http.scaladsl.model._
    import akka.http.scaladsl.model.ws._

    import scala.concurrent.Future

    def now = java.time.LocalDateTime.now.toString

    def authorizeWebsocketRequest = {
        implicit val system = ActorSystem()
        implicit val materializer = ActorMaterializer()
        import system.dispatcher


        // Future[Done] is the materialized value of Sink.foreach,
        // emitted when the stream completes
        val incoming = Flow[Any].to(Sink.foreach(t => println(s"$now response: $t")))
        /*Sink.foreach[Message] {
            case message: TextMessage.Strict =>
                println(now + " response: " + message.text)
        }*/

        // send this as a message over the WebSocket
        val outgoing = {
            Source.single(TextMessage {
                println(s"$now sending message...")
                s"""{"authorize": "9wB0Mo5iy5ZfRk3"}"""
            })
        }

        // flow to use (note: not re-usable!)
        val webSocketFlow = Http().webSocketClientFlow(WebSocketRequest("wss://ws.binaryws.com/websockets/v3?app_id=18937"))

        // the materialized value is a tuple with
        // upgradeResponse is a Future[WebSocketUpgradeResponse] that
        // completes or fails when the connection succeeds or fails
        // and closed is a Future[Done] with the stream completion from the incoming sink
        val (upgradeResponse, closed) =
        outgoing
            .viaMat(webSocketFlow)(Keep.right) // keep the materialized Future[WebSocketUpgradeResponse]
            .toMat(incoming)(Keep.both) // also keep the Future[Done]
            .run()

        // just like a regular http request we can access response status which is available via upgrade.response.status
        // status code 101 (Switching Protocols) indicates that server support WebSockets
        val connected = upgradeResponse.flatMap { upgrade =>
            if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
                println(now + " Connect successful")
                Future.successful(Done)
            } else {
                throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
            }
        }

        // in a real application you would not side effect here
        connected.onComplete(t => println(s"$now onComplete: $t"))
//        closed.foreach(t => println(now + " closed: " + t.toString))
    }

    // TODO: наверное, придётся использовать не сингл, а непрерывный WebSocketRewequest (см. документацию акки)
    /*private def authorizeWebsocketRequest = {
        implicit val system = ActorSystem()
        implicit val materializer = ActorMaterializer()
        import system.dispatcher

        // print each incoming strict text message
        val printSink: Sink[Message, Future[Done]] =
//            Sink.head[Message]
            Sink.foreach(t => println("input auth message: " + t))

        val helloSource: Source[Message, NotUsed] = {
            println("send message with authorize json")
            Source.single(TextMessage(
s"""{"authorize": "9wB0Mo5iy5ZfRk3"}"""
            ))
        }

        // the Future[Done] is the materialized value of Sink.foreach
        // and it is completed when the stream completes
        val flow: Flow[Message, Message, Future[Done]] =
        Flow.fromSinkAndSourceMat(printSink, helloSource)(Keep.left)

        // upgradeResponse is a Future[WebSocketUpgradeResponse] that
        // completes or fails when the connection succeeds or fails
        // and closed is a Future[Done] representing the stream completion from above
        val (upgradeResponse, closed) =
        Http().singleWebSocketRequest(WebSocketRequest("wss://ws.binaryws.com/websockets/v3?app_id=18937"), flow)

        val connected = upgradeResponse.map { upgrade =>
            // just like a regular http request we can access response status which is available via upgrade.response.status
            // status code 101 (Switching Protocols) indicates that server support WebSockets
            if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
                println("connect 1 is successful")
                Done
            } else {
                throw new RuntimeException(s"Connection 1 failed: ${upgrade.response.status}")
            }
        }

        // in a real application you would not side effect here
        // and handle errors more carefully
        connected.onComplete(t => println("on complete 1: " + t.toString))
        //        closed.foreach(t => println("closed: " + t.toString))
        closed
    }*/

    // TODO: потом во всём разобраться и переписать этот CTRL-C CTRL-V кошмар (как минимум начать использовать средства самого Play, а не голого Akka
    private def getPortfolioWebsocketRequest(fromDate: Timestamp) = {
        implicit val system = ActorSystem()
        implicit val materializer = ActorMaterializer()
        import system.dispatcher

        // print each incoming strict text message
        val printSink: Sink[Message, Future[Message]] =
            Sink.head[Message]

        val helloSource: Source[Message, NotUsed] = {
            println("send message with date json")
            val dateForJs = fromDate.getTime.toString.take(10)
            println(s"dateAsLong: $dateForJs")
            Source.single(TextMessage(
s"""{
"profit_table": 1,
"description": 1,
"limit": 25,
"sort": "ASC",
"date_from": $dateForJs
}"""
            ))
        }

        // the Future[Done] is the materialized value of Sink.foreach
        // and it is completed when the stream completes
        val flow: Flow[Message, Message, Future[Message]] =
            Flow.fromSinkAndSourceMat(printSink, helloSource)(Keep.left)

        // upgradeResponse is a Future[WebSocketUpgradeResponse] that
        // completes or fails when the connection succeeds or fails
        // and closed is a Future[Done] representing the stream completion from above
        val (upgradeResponse, closed) =
            Http().singleWebSocketRequest(WebSocketRequest("wss://ws.binaryws.com/websockets/v3?app_id=18937"), flow)

        val connected = upgradeResponse.map { upgrade =>
            // just like a regular http request we can access response status which is available via upgrade.response.status
            // status code 101 (Switching Protocols) indicates that server support WebSockets
            if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
                println("connect 2 is successful")
                Done
            } else {
                throw new RuntimeException(s"Connection 2 failed: ${upgrade.response.status}")
            }
        }

        // in a real application you would not side effect here
        // and handle errors more carefully
        connected.onComplete(t => println("on complete 2: " + t.toString))
//        closed.foreach(t => println("closed: " + t.toString))
        closed
    }
}