package helpers

import java.sql.Timestamp
import controllers.{ApiError, ContractTransactionData, PrefillContractData}
import models.Contract
import play.api.libs.json.{JsArray, JsObject, JsValue, Reads}
import play.api.mvc.{AnyContent, MessagesRequest, Result}
import utils.Utils.Math._
import utils.Utils.SeqHelper.seqToOpt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.reflect._

object ContractControllerHelper extends ContractControllerHelper

trait ContractControllerHelper {
    def readAndParseJsonWithErrorHandling[T : ClassTag](action: T => Future[Result])
                                                       (implicit request: MessagesRequest[AnyContent],
                                                                 jsonReads: Reads[T],
                                                                 ec: ExecutionContext): Future[Result] = {
        lazy val entityType = implicitly[ClassTag[T]].runtimeClass.getSimpleName

        request.body.asJson.map(json => Try(json.as[T])).map(_.fold(
            exception => ApiError.asAsyncResult(
                caption = "JSON PARSING PROBLEM",
                cause = s"Что-то пошло не так при попытке распарсить JSON сущности $entityType из тела запроса. " +
                        s"JSON: ${request.body.asJson.getOrElse("")}",
                details = Some(exception.getMessage)
            ),
            action
        )).getOrElse(
            ApiError.asAsyncResult(
                caption = "REQUEST BODY PROBLEM",
                cause = s"Что-то пошло не так при попытке получить JSON сущности $entityType из тела запроса",
                details = Some(s"Тело запроса: ${request.body}")
            )
        )
    }

    def processScreenshots(screenshotsUrls: String): Either[String, String] =
        if (screenshotsUrls.isEmpty) {
            Right("")
        } else {
            // TODO: Проверка очень костыль-костыль, сделать с этим что-нибудь!
            if (screenshotsUrls.startsWith("https")) {
                val urls = screenshotsUrls.split(';').toSeq
                val newScreenshotPaths = urls.map(ScreenshotHelper.screenshotFromUrlToBase64)

                seqToOpt(newScreenshotPaths)
                    .map(paths => Right(paths.mkString(";")))
                    .getOrElse(Left("Что-то пошло не так при попытке получить скриншот по одному из URL или при " +
                        "попытке сконвертировать его в Base64 формат"))
            } else {
                Left(s"Строка со скриншотами некорректна (не начинается с https): $screenshotsUrls")
            }
        }


    private def getDirection(data: ContractTransactionData) = {
        val direction = data.shortCode.split('_')(0)
        val expectedDirections = ContractHelper.ContractDirection.values.map(_.toString).toSeq

        if (!expectedDirections.contains(direction))
            Left(s"Некорректное направление сделки в data.shortCode: $direction. " +
                 s"Ожидалось одно из значений: ${expectedDirections.mkString(",")}")
        else
            Right(direction)
    }

    private def getExpiration(data: ContractTransactionData) = {
        val rawExpiration = """spot at (\d+)""".r.findFirstMatchIn(data.longCode).map(
            _.group(1).toIntOption.map(Right(_)).getOrElse(Left("Не удалось преобразовать экспирацию в Int"))
        ).getOrElse(Left(s"Не удалось найти экспирацию в data.longcode: ${data.longCode}"))

        rawExpiration.map(expiration =>
            if (data.longCode.contains("hour"))
                expiration * 60
            else
                expiration
        )
    }

    private def getSymbol(data: ContractTransactionData) = {
        val fxSymbol = """payout if (\w+/\w+)""".r.findFirstMatchIn(data.longCode).map(matchResult =>
            Right(matchResult.group(1))
        ).getOrElse(Left(s"Не удалось найти валютную пару в data.longcode: ${data.longCode}"))
        val expectedSymbols = ContractHelper.FxSymbol.values.map(_.toString)

        fxSymbol.flatMap(symbol =>
            if (!expectedSymbols.contains(symbol))
                Left(s"Некорректная валютная пара в data.longCode: $symbol. " +
                     s"Ожидалось одно из значений: ${expectedSymbols.mkString(",")}")
            else
                Right(symbol)
        )
    }

    def createPrefilledContract(profitTableJson: JsValue,
                                prefillContractData: PrefillContractData,
                                newContractNumber: Int): Either[String, Contract] = {
        val transactions: Either[String, Seq[ContractTransactionData]] =
            (profitTableJson \ "profit_table" \ "transactions").toEither match {
                case Right(ts: JsArray) => Try(
                    ts.value.toSeq.map { case jsObject: JsObject =>
                        val fields = jsObject.fields.toMap
                        ContractTransactionData(
                            fields("buy_price").as[Double],
                            fields("longcode").as[String],
                            fields("payout").as[Double],
                            fields("sell_price").as[Double],
                            fields("shortcode").as[String],
                            fields("sell_time").as[Long],
                            fields("transaction_id").as[Long]
                        )
                    }
                ).toEither.left.map(_.getMessage)

                case Right(other) => Left(s"От брокера ожидался список транзакций, но пришло $other")

                case Left(validationError) =>
                    Left(s"Ошибка парсинга списка транзакций: ${validationError.messages.mkString(";")}")
        }

        transactions.flatMap(_.find(_.transactionId.toString == prefillContractData.transactionId).map(data =>
            for {
                direction <- getDirection(data)
                expiration <- getExpiration(data)
                symbol <- getSymbol(data)
                screenshots <- processScreenshots(prefillContractData.screenshotUrls)
            } yield {
                val profitPercent = (data.payout - data.buyPrice) / data.buyPrice
                val isWin = data.sellPrice > 0

                Contract(
                    number = newContractNumber,
                    created = new Timestamp(data.time),
                    expiration = expiration,
                    fxSymbol = symbol,
                    direction = direction,
                    isWin = isWin,
                    screenshotPaths = screenshots,
                    buyPrice = Some(data.buyPrice.round2),
                    profitPercent = Some(profitPercent.round3)
                )
            }
        ).getOrElse(Left(s"Не удалось найти транзакцию ${prefillContractData.transactionId} в списке транзакций брокера")))
    }
}