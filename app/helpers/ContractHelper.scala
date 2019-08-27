package helpers

import models.ContractData
import play.api.data.Form
import play.api.data.Forms._
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

    //noinspection TypeAnnotation
    val contractForm = Form[ContractData](
        mapping(
            "number" -> number,
            "contractType" -> text,
            "created" -> date,
            "expiration" -> number,
            "fxSymbol" -> text,
            "direction" -> text,
            "buyPrice" -> of[Double],
            "profitPercent" -> of[Double],
            "isWin" -> boolean,
            "screenshotUrls" -> text,
            "tags" -> text,
            "isCorrect" -> boolean,
            "description" -> text
        )(ContractData.apply)(ContractData.unapply)
    )

    implicit class DoubleWithRounding(x: Double) {
        def round2: Double = BigDecimal(x).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
        def round3: Double = BigDecimal(x).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble
    }

    implicit class OptDoubleWithRounding(x: Option[Double]) {
        def round2: Option[Double] = x.map(BigDecimal(_).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble)
        def round3: Option[Double] = x.map(BigDecimal(_).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble)
    }
}