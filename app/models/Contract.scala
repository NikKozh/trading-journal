package models

import java.sql.Timestamp
import java.util.{Date, UUID}

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
                    screenshotsIds: String, // TODO: потом вообще убрать, это здесь не нужно
                    tags: String, // TODO: пока просто строкой с разделителем в виде запятой, потом надо разбить на Seq[String] или даже на Seq с отдельными объектами
                    isCorrect: Boolean, // вход по ТС? TODO: сделать опциональным
                    description: String) {

    def income: Option[Double] =
        for {
            price <- buyPrice
            percent <- profitPercent
        } yield if (isWin) price + price * percent else -price

    def screenshots: Seq[String] = Seq(screenshotsIds)
}

object Contract {
    def fill(dto: ContractData): Contract =
        new Contract(
            number = dto.number,
            contractType = dto.contractType,
            created = Timestamp.from(dto.created.toInstant),
            expiration = dto.expiration,
            fxSymbol = dto.fxSymbol,
            direction = dto.direction,
            buyPrice = Some(dto.buyPrice),
            profitPercent = Some(dto.profitPercent),
            isWin = dto.isWin,
            screenshotsIds = dto.screenshotUrls, // TODO: пока только один скрин
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
                        screenshotUrls: String, // TODO: пока подразумевается один скриншот, потом сделаю Seq
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
            buyPrice = contract.buyPrice.getOrElse(0),
            profitPercent = contract.profitPercent.getOrElse(0),
            isWin = contract.isWin,
            screenshotUrls = contract.screenshotsIds,
            tags = contract.tags,
            isCorrect = contract.isCorrect,
            description = contract.description
        )
}