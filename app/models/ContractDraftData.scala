package models

import play.api.data.Form
import play.api.data.Forms._

case class ContractDraftData(screenshotsUrls: String)

object ContractDraftData {
    //noinspection TypeAnnotation
    val form = Form[ContractDraftData](
        mapping("screenshotsUrls" -> text)(ContractDraftData.apply)(ContractDraftData.unapply)
    )
}