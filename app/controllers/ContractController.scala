package controllers

import helpers.ContractHelper
import javax.inject._
import play.api.mvc._

@Singleton
class ContractController @Inject()(mcc: MessagesControllerComponents) extends MessagesAbstractController(mcc) {
    def contractList: Action[AnyContent] = Action {
        Ok(views.html.index("Hello, World!"))
    }

    def createContract: Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
        Ok(views.html.createContract(ContractHelper.ContractForm.form))
    }

    def submitContract: Action[AnyContent] = Action { implicit request =>
        ContractHelper.ContractForm.form.bindFromRequest.fold(
            errors => BadRequest(views.html.createContract(errors)),
            contractData => Ok(views.html.index(s"Contract data: $contractData"))
        )
    }
}