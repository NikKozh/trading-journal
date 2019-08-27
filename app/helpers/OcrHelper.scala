package helpers

import java.sql.Timestamp
import java.time.{Month, ZoneId, ZonedDateTime}

object OcrHelper {
    def parseOcrResult(contractId: String, json: String): OcrContractData = {
        val lowerJson = json.toLowerCase
        val date = parseRawOcrDate(lowerJson)
        val symbol = parseRawOcrFxSymbol(lowerJson)

        OcrContractData(contractId, date, symbol)
    }

    case class OcrContractData(contractId: String, screenshotDate: Option[Timestamp], fxSymbol: Option[String])

    // "Month DD, YEAR HH:MM:SS +TZ" -> Option[Timestamp in GMT]
    private def parseRawOcrDate(rawString: String): Option[Timestamp] = {
        val datePattern =
            """(?m)([a-z]*) (\d{2}), (\d{4}) (\d{2}):(\d{2}):(\d{2}) (\+\d{2})"""
                .r("month", "day", "year", "hours", "minutes", "seconds", "timezone")

        datePattern.findFirstMatchIn(rawString).map { regexMatch =>
            val r = regexMatch.groupNames.zip(regexMatch.subgroups).toMap
            val monthNumber = Month.valueOf(r("month").toUpperCase).getValue
            val monthString = if (monthNumber < 10) "0" + monthNumber else monthNumber.toString
            // 2007-12-03T10:15:30+01:00
            val preparedString = s"${r("year")}-$monthString-${r("day")}T${r("hours")}:${r("minutes")}:${r("seconds")}${r("timezone")}:00"
            val dateTimeInGMT = ZonedDateTime.parse(preparedString).withZoneSameInstant(ZoneId.of("GMT")).toInstant

            Timestamp.from(dateTimeInGMT)
        }
    }

    // "fx:eurusd" -> Some("EUR/USD")
    private def parseRawOcrFxSymbol(rawString: String): Option[String] = {
        val symbolPattern = """fx:([a-z]*)""".r("symbol")

        symbolPattern.findFirstMatchIn(rawString).map { regexMatch =>
            val (first, second) = regexMatch.group("symbol").splitAt(3)
            s"$first/$second".toUpperCase
        }
    }
}
