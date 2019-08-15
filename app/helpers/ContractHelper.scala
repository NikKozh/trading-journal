package helpers

import java.sql.Timestamp

import play.api.data.Forms._
import play.api.data.Form
import play.api.data.format.Formats.doubleFormat

object ContractHelper {
    //noinspection TypeAnnotation
    object ContractType extends Enumeration {
        type ContractType = Value
        val REAL = Value("Реал")
        val DEMO = Value("Демо")
        val FORECAST = Value("Форкаст")
        val TESTER = Value("Тестер")
    }

    //noinspection TypeAnnotation
    object FxSymbol extends Enumeration {
        type FxSymbol = Value
        val EUR_USD = Value("EUR/USD")
        val USD_JPY = Value("USD/JPY")
    }

    //noinspection TypeAnnotation
    object ContractDirection extends Enumeration {
        type ContractDirection = Value
        val CALL = Value("CALL")
        val PUT = Value("PUT")
    }

    object ContractForm {
        case class ContractData(number: Int,
                                contractType: String,
                                created: Timestamp,
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

        //noinspection TypeAnnotation
        val form = Form[ContractData](
            mapping(
                "number" -> number,
                "contractType" -> text,
                "created" -> sqlTimestamp,
                "expiration" -> number,
                "fxSymbol" -> text,
                "direction" -> text,
                "buyPrice" -> of[Double],
                "profitPercent" -> of[Double],
                "isWin" -> boolean,
                "tags" -> text,
                "isCorrect" -> boolean,
                "description" -> text
            )(ContractData.apply)(ContractData.unapply)
        )
    }
}