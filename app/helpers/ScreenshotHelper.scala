package helpers

import java.util.Base64
import java.net.URL

import scala.sys.process._

object ScreenshotHelper {
    def screenshotFromUrlToBase64(url: String): Option[String] = {
        // TODO: обернуть в Try
        val outputStream = new java.io.ByteArrayOutputStream()
        val processResult = new URL(url) #> outputStream !

        if (processResult == 0) {
            val bytes = outputStream.toByteArray
            val encodedScreenshot = Base64.getEncoder.encodeToString(bytes)
            Some(encodedScreenshot)
        } else
            None
    }
}