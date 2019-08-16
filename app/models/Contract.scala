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

object Contract {
    def apply(dto: ContractData): Contract =
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
            screenshotsIds = Seq.empty, // TODO: не забыть здесь исправить, когда добавлю поле в dto
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
                        // TODO: screenshotsIds, когда разберусь, как их сохранять и мапить
                        tags: String,
                        isCorrect: Boolean, // TODO: сделать опциональным (в т.ч. на самой форме)
                        description: String)