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
        Ok(views.html.contractEdit(contractForm))
    }

    def submitContract: Action[AnyContent] = Action.async { implicit request =>
        contractForm.bindFromRequest.fold(
            errorForm => Future.successful(Ok(views.html.contractEdit(errorForm))),
            contractData =>
                contractService.save(Contract.fill(contractData)).map { id =>
                    println("id: " + id)
                    Redirect(routes.ContractController.contractList())
                }
        )
    }

    def viewContract(id: String): Action[AnyContent] = Action.async { implicit request =>
        contractService.get(id).map {
            case Some(contract) => Ok(views.html.contractCard(contract))
            case None => NotFound
        }
    }
}