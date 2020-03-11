package models.user

import java.util.UUID

case class UserSetting(id: String = UUID.randomUUID().toString,
                       strategyFilter: Option[String] = None)

object UserSetting {
    def fill(dto: UserSettingData): UserSetting = {
        val strategyOpt = dto.strategyFilter.map(_.trim)
        val strategy = if (strategyOpt.exists(_.isEmpty)) None else strategyOpt
        UserSetting(strategyFilter = strategy)
    }
}

case class UserSettingData(strategyFilter: Option[String])

object UserSettingData {
    def fill(userSetting: UserSetting): UserSettingData =
        new UserSettingData(strategyFilter = userSetting.strategyFilter)
}

