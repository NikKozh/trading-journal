package models

import play.api.data.Form
import play.api.data.Forms._

case class ContractDraftData(screenshotsUrls: String, transactionId: String)

object ContractDraftData {
    //noinspection TypeAnnotation
    val form = Form[ContractDraftData](
        mapping(
            "screenshotsUrls" -> text,
            "transactionId" -> text
        )(ContractDraftData.apply)(ContractDraftData.unapply)
    )
}