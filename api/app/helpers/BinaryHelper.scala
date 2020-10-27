package helpers

import java.util.concurrent.Semaphore
import com.github.andyglow.websocket._
import com.github.andyglow.websocket.util.Uri
import play.api.libs.json.{JsValue, Json}
import scala.concurrent.{ExecutionContext, Future, Promise}

object BinaryHelper {
    def getProfitTable(implicit ec: ExecutionContext): Future[JsValue] = {
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

        // TODO: небезопасно, надо app_id перенести в конфиг
        val cli = WebsocketClient(Uri("wss://ws.binaryws.com/websockets/v3?app_id=18937"), protocolHandler)
        val ws = cli.open()

        // TODO: небезопасно x2, надо токен перенести в конфиг
        ws ! """{ "authorize": "soDLW2O3RrKz0sw" }"""

        semaphore.acquire(1)
        cli.shutdownSync()

        profitTablePromise.future.map(Json.parse)
    }
}