package helpers

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.Base64
import java.sql.Timestamp
import java.time.{Month, ZonedDateTime}
import java.util.UUID

import scalaj.http.{Http, MultiPart}
import com.sksamuel.scrimage._
import com.sksamuel.scrimage.filter.{SharpenFilter, ThresholdFilter}
import javax.imageio.ImageIO
import play.api.libs.json._

object OcrHelper {
    def getOcrResult(base64ImageString: String): String = {
        val id = UUID.randomUUID().toString

        val originalImageBytes = Base64.getDecoder.decode(base64ImageString)
        val originalImageStream = new ByteArrayInputStream(originalImageBytes)

        val updatedImageInputStream = Image.fromStream(originalImageStream).scale(2.5).filter(SharpenFilter).filter(ThresholdFilter(200)).stream
        val updateImageOutputStream = new ByteArrayOutputStream()
        ImageIO.write(ImageIO.read(updatedImageInputStream), "png", updateImageOutputStream)
        val updateImageBytes = updateImageOutputStream.toByteArray
        val updateImageBytesBase64 = Base64.getEncoder.encodeToString(updateImageBytes) // исключительно для дебага

        val httpResult =
            Http("https://licenta-ocr-parser.herokuapp.com/upload")
                .postMulti(MultiPart("picture", "screenshot.png", "image/png", updateImageBytes))
                .timeout(20000, 20000)
                .asString
                .throwError
                .body
        (Json.parse(httpResult) \ "text").asOpt[String].getOrElse(sys.error("Can't get json text from OCR service (connect was successful)"))
    }

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
            val dateTimeInGMT = ZonedDateTime.parse(preparedString).toInstant

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
