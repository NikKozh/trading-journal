package helpers

import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

object OcrHelper {
    def parseOcrResult(json: String): Unit = {
        val currentMonth = ZonedDateTime.now().getMonth.getDisplayName(TextStyle.FULL, Locale.US).toLowerCase
        val startIndex = json.indexOfSlice(currentMonth)
        val date =
            if (startIndex != -1) {
                val rawDate = json.substring(startIndex, startIndex + 28)
            } else {
                None
            }
    }

    case class OcrContractData(screenshotDate: Option[ZonedDateTime], fxSymbol: Option[String])
}
