package controllers

import javax.inject.{Inject, Singleton}
import models.user.UserSetting
import play.api.Environment
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import services.{ContractService, UserSettingsService}
import utils.ExceptionHandler

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserSettingController @Inject()(mcc: MessagesControllerComponents,
                                      userSettingsService: UserSettingsService,
                                      af: AssetsFinder,
                                      env: Environment
                                     )(implicit ec: ExecutionContext)
    extends ExceptionHandler(mcc) {

    def submitStrategyFilter(backUrl: String): Action[AnyContent] = asyncActionWithExceptionPage { implicit request =>
        import helpers.UserSettingHelper.userSettingForm

        userSettingForm.bindFromRequest.fold(
            _ => Future.successful(BadRequest),
            userSettingData => {
                userSettingsService
                    .update(UserSetting.fill(userSettingData).copy(id = "0"))
                    .map(_ => Redirect(Call("GET", backUrl)))
            }
        )
    }
}