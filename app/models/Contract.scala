package models

import java.sql.Timestamp
import java.util.UUID

import helpers.ContractHelper.ContractType._
import helpers.ContractHelper.FxSymbol._
import helpers.ContractHelper.ContractDirection._

case class Contract(id: String = UUID.randomUUID().toString,
                    number: Int,
                    contractType: String, // TODO: ContractType
                    created: Timestamp,
                    expiration: Int = 5, // в минутах
                    fxSymbol: String, // TODO: FxSymbol
                    direction: String, // TODO: ContractDirection
                    buyPrice: Option[Double], // в долларах
                    profitPercent: Option[Double], // от 0 до 1; потенциальный, обозначается даже в убыточных сделках
                    isWin: Boolean,
                    screenshotsIds: Seq[String],
                    tags: String, // TODO: пока просто строкой с разделителем в виде запятой, потом надо разбить на Seq[String] или даже на Seq с отдельными объектами
                    isCorrect: Boolean, // вход по ТС? TODO: сделать опциональным
                    description: String) {

    def income: Option[Double] =
        for {
            price <- buyPrice
            percent <- profitPercent
        } yield if (isWin) price + price * percent else -price
}