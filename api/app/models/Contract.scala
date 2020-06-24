package models

import java.sql.Timestamp
import java.util.{Date, UUID}

import utils.Utils.Math._
import helpers.ContractHelper.ContractType._
import helpers.ContractHelper.FxSymbol._
import helpers.ContractHelper.ContractDirection._
import play.api.libs.json.{Json, OWrites}

case class Contract(id: String = UUID.randomUUID().toString,
                    number: Int,
                    contractType: String = "Реал", // TODO: ContractType
                    created: Timestamp,
                    expiration: Int = 5, // в минутах
                    fxSymbol: String, // TODO: FxSymbol
                    direction: String, // TODO: ContractDirection
                    isWin: Boolean,
                    screenshotPaths: String, // TODO: потом вообще убрать, это здесь не нужно
                    tags: String = "PA 5M; сетап ", // TODO: пока просто строкой с разделителем в виде запятой, потом надо разбить на Seq[String] или даже на Seq с отдельными объектами
                    isCorrect: Boolean, // вход по ТС? TODO: сделать опциональным
                    description: String,
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

object Contract {
    implicit val contractWrites: OWrites[Contract] = Json.writes

    def fill(dto: ContractData): Contract =
        Contract(
            number = dto.number,
            contractType = dto.contractType,
            created = Timestamp.from(dto.created.toInstant),
            expiration = dto.expiration,
            fxSymbol = dto.fxSymbol,
            direction = dto.direction,
            buyPrice = Some(dto.buyPrice.round2),
            profitPercent = Some(dto.profitPercent.round3),
            isWin = dto.isWin,
            screenshotPaths = dto.screenshotUrls, // TODO: в строке на самом деле несколько путей, разделённых точкой с запятой
            tags = dto.tags,
            isCorrect = dto.isCorrect,
            description = dto.description
        )
}

case class ContractData(number: Int,
                        contractType: String,
                        created: Date,
                        expiration: Int = 5,
                        fxSymbol: String,
                        direction: String,
                        buyPrice: Double, // TODO: сделать опциональным (в т.ч. на самой форме)
                        profitPercent: Double, // TODO: сделать опциональным (в т.ч. на самой форме)
                        isWin: Boolean,
                        screenshotUrls: String, // TODO: в строке на самом деле несколько путей, разделённых точкой с запятой, потом мб сделаю Seq
                        tags: String,
                        isCorrect: Boolean, // TODO: сделать опциональным (в т.ч. на самой форме)
                        description: String)

object ContractData {
    def apply(contract: Contract): ContractData =
        new ContractData(
            number = contract.number,
            contractType = contract.contractType,
            created = new Date(contract.created.getTime),
            expiration = contract.expiration,
            fxSymbol = contract.fxSymbol,
            direction = contract.direction,
            buyPrice = contract.buyPrice.getOrElse(0.0).round2,
            profitPercent = contract.profitPercent.getOrElse(0.0).round3,
            isWin = contract.isWin,
            screenshotUrls = contract.screenshotPaths,
            tags = contract.tags,
            isCorrect = contract.isCorrect,
            description = contract.description
        )
}