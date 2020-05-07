package helpers

import models.user.UserSettingData
import play.api.data.Form
import play.api.data.Forms._
import services.{ContractService, UserSettingsService}
import javax.inject.Inject

import scala.concurrent.Await
import scala.concurrent.duration._

trait UserSettingHelper {
    @Inject var userSettingsService: UserSettingsService = null
    @Inject var contractService: ContractService = null

    val userSettingForm = Form[UserSettingData](
        mapping(
            "strategyFilter" -> optional(text)
        )(UserSettingData.apply)(UserSettingData.unapply)
    )
}

object UserSettingHelper extends UserSettingHelper