package services

import javax.inject.{Inject, Singleton}
import models.user.UserSetting
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

trait UserSettingsService {
    def get(id: String): Future[Option[UserSetting]]
    def update(userSettings: UserSetting): Future[Int]
}

@Singleton
class UserSettingsServiceImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends UserSettingsService {
    private val dbConfig = dbConfigProvider.get[JdbcProfile]

    import dbConfig._
    import profile.api._

    private class UserSettingsTable(tag: Tag) extends Table[UserSetting](tag, "UserSetting") {
        def id = column[String]("id", O.PrimaryKey)
        def strategyFilter = column[Option[String]]("strategyFilter")

        def * = (id, strategyFilter) <> ((UserSetting.apply _).tupled, UserSetting.unapply)
    }

    private val userSettings = TableQuery[UserSettingsTable]

    override def get(id: String): Future[Option[UserSetting]] = db.run {
        userSettings.filter(_.id === id).result.headOption
    }

    override def update(userSetting: UserSetting): Future[Int] = db.run {
        userSettings.update(userSetting)
    }
}