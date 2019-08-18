package helpers

import java.io.File
import java.net.URL

import scala.sys.process._

object ScreenshotHelper {
    def screenshotFromUrl(url: String, pathToSave: String): Option[File] = {
        // TODO: обернуть в Try
        val screenshotFile = new File(pathToSave)
        val processResult = new URL(url) #> screenshotFile !

        if (processResult == 0) Some(screenshotFile)
        else None
    }
}