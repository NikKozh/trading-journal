package controllers

import helpers.ContractHelper._
import javax.inject._
import play.api.mvc._
import services.ContractService
import models.{Contract, ContractData}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContractController @Inject()(mcc: MessagesControllerComponents,
                                   contractService: ContractService
                                  )(implicit ec: ExecutionContext)
    extends MessagesAbstractController(mcc) {

    def contractList: Action[AnyContent] = Action.async {
        contractService.list.map(contracts =>
            Ok(views.html.contractList(contracts))
        )
    }

    def createContract: Action[AnyContent] = Action { implicit request =>
        Ok(views.html.createContract(contractForm))
    }

    def submitContract: Action[AnyContent] = Action.async { implicit request =>
        contractForm.bindFromRequest.fold(
            errorForm => Future.successful(Ok(views.html.createContract(errorForm))),
            contractData =>
                contractService.save(Contract(contractData)).map { _ =>
                    Redirect(routes.ContractController.contractList())
                }
        )
    }
}