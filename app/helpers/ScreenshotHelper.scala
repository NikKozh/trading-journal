package helpers

import java.util.Base64
import java.net.URL
import javax.imageio.ImageIO

object ScreenshotHelper {
    def screenshotFromUrlToBase64(urlString: String): Option[(String, String)] = {
        // TODO: обернуть в Try
        val url = new URL(urlString)
        val fullImageOS = new java.io.ByteArrayOutputStream()
        val croppedImageOS = new java.io.ByteArrayOutputStream()
        val fullImage = ImageIO.read(url)
        val croppedImage = fullImage.getSubimage(0, 0, 500, 40)

        ImageIO.write(fullImage, "png", fullImageOS)
        ImageIO.write(croppedImage, "png", croppedImageOS)

        val fullImageBytes = fullImageOS.toByteArray
        val croppedImageBytes = croppedImageOS.toByteArray
        val encodedFullImage = Base64.getEncoder.encodeToString(fullImageBytes)
        val encodedCroppedImage = Base64.getEncoder.encodeToString(croppedImageBytes)

        Some((encodedFullImage, encodedCroppedImage))
    }
}