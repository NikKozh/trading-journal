package models

import play.api.data.Form
import play.api.data.Forms._

/**
 * Первая строка - номер контракта у Бинари
 * <br>
 * Вторая и далее - ссылки на скриншоты сделки
*/
case class ContractDraftRawData(raw: String)

object ContractDraftRawData {
    //noinspection TypeAnnotation
    val form = Form[ContractDraftRawData](
        mapping(
            "raw" -> text
        )(ContractDraftRawData.apply)(ContractDraftRawData.unapply)
    )
}