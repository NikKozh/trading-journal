package helpers

import java.util.Base64
import java.net.URL
import javax.imageio.ImageIO

object ScreenshotHelper {
    def screenshotFromUrlToBase64(urlString: String): Option[String] = {
        // TODO: обернуть в Try
        val url = new URL(urlString)
        val fullImageOS = new java.io.ByteArrayOutputStream()
        val fullImage = ImageIO.read(url)

        ImageIO.write(fullImage, "png", fullImageOS)

        val fullImageBytes = fullImageOS.toByteArray
        val encodedFullImage = Base64.getEncoder.encodeToString(fullImageBytes)

        Some(encodedFullImage)
    }
}