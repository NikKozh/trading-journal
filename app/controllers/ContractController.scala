package controllers

import helpers.ContractHelper._
import javax.inject._
import play.api.mvc._
import services.ContractService
import models.{Contract, ContractData}

@Singleton
class ContractController @Inject()(mcc: MessagesControllerComponents,
                                   contractService: ContractService)
    extends MessagesAbstractController(mcc) {

    def contractList: Action[AnyContent] = Action {
        Ok(views.html.contractList(contractService.list))
    }

    def createContract: Action[AnyContent] = Action { implicit request =>
        Ok(views.html.createContract(contractForm))
    }

    def submitContract: Action[AnyContent] = Action { implicit request =>
        contractForm.bindFromRequest.fold(
            errors => BadRequest(views.html.createContract(errors)),
            contractData => {
                contractService.save(Contract(contractData))
                Ok(views.html.contractList(contractService.list)) // TODO: заменить на вызов contractList
            }
        )
    }
}