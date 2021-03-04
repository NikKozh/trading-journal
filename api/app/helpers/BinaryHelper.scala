package helpers

import java.util.concurrent.Semaphore
import com.github.andyglow.websocket._
import com.github.andyglow.websocket.util.Uri
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import scala.concurrent.{ExecutionContext, Future, Promise}

object BinaryHelper {
    def getProfitTable(implicit ec: ExecutionContext, config: Configuration): Future[JsValue] = {
        val profitTablePromise = Promise[String]()
        val semaphore = new Semaphore(0)

        val protocolHandler = new WebsocketHandler[String]() {
            override def receive: PartialFunction[String, Unit] = {
                case response if (Json.parse(response) \ "authorize").isDefined =>
                    sender() ! """{ "profit_table": 1,
                                   |"description": 1 }""".stripMargin

                case response if (Json.parse(response) \ "profit_table").isDefined =>
                    profitTablePromise success response
                    sender().close
                    semaphore.release()

                case unexpectedResponse =>
                    println(s"<<| unexpected response: $unexpectedResponse")
                    profitTablePromise failure sys.error(s"Unexpected response: $unexpectedResponse")
                    sender().close
                    semaphore.release()
            }
        }

        def connectToBinaryAndGetData(uri: String, token: String) = {
            val cli = WebsocketClient(Uri(uri), protocolHandler)
            val ws = cli.open()

            ws ! s"""{ "authorize": "$token" }"""

            semaphore.acquire(1)
            cli.shutdownSync()

            profitTablePromise.future.map(Json.parse)
        }

        for {
            uri <- ConfigHelper.getConfigValue[String]("binaryConnection.uri")
            token <- ConfigHelper.getConfigValue[String]("binaryConnection.token")
            profitTable <- connectToBinaryAndGetData(uri, token)
        } yield profitTable
    }
}