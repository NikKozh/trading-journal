package models

import java.sql.Timestamp
import java.util.UUID
import utils.Utils.Math._
import play.api.libs.json._
import helpers.{JsonHelper, OptionNullJsonWriter}

case class Contract(id: String = UUID.randomUUID().toString,
                    number: Int,
                    contractType: String = "Реал", // TODO: ContractType
                    created: Timestamp,
                    expiration: Int = 5, // в минутах
                    fxSymbol: String, // TODO: FxSymbol
                    direction: String, // TODO: ContractDirection
                    isWin: Boolean,
                    screenshotPaths: String, // TODO: потом вообще убрать, это здесь не нужно
                    tags: String = "", // TODO: пока просто строкой с разделителем в виде запятой, потом надо разбить на Seq[String] или даже на Seq с отдельными объектами
                    isCorrect: Boolean = false, // вход по ТС? TODO: сделать опциональным
                    description: String = "",
                    forGuest: Boolean = false, // TODO: потом выпилить, когда будет нормальная система пользователей
                    buyPrice: Option[Double], // в долларах
                    profitPercent: Option[Double] // от 0 до 1; потенциальный, обозначается даже в убыточных сделках
                   ) {

    def income: Option[Double] =
        for {
            price <- buyPrice
            percent <- profitPercent
        } yield (if (isWin) price * percent else -price).round2

    def screenshots: Seq[String] = screenshotPaths.split(';')
}

object Contract extends JsonHelper with OptionNullJsonWriter {
    implicit val contractWrites: OWrites[Contract] = Json.writes
    implicit val contractReads: Reads[Contract] = Json.reads
}