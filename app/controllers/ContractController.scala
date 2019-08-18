package controllers

import java.util.UUID

import helpers.ContractHelper._
import helpers.ScreenshotHelper
import javax.inject._
import play.api.mvc._
import services.ContractService
import models.{Contract, ContractData}
import play.api.Environment
import org.apache.commons.io.FilenameUtils

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContractController @Inject()(mcc: MessagesControllerComponents,
                                   contractService: ContractService,
                                   af: AssetsFinder,
                                   env: Environment
                                  )(implicit ec: ExecutionContext)
    extends MessagesAbstractController(mcc) {

    def contractList: Action[AnyContent] = Action.async {
        contractService.list.map(contracts =>
            Ok(views.html.contractList(contracts))
        )
    }

    def addEditContract(id: Option[String] = None): Action[AnyContent] = Action.async { implicit request =>
        id.map {
            contractService.get(_).map {
                case Some(contract) => Ok(views.html.contractEdit(contractForm.fill(ContractData(contract)), id))
                case None => NotFound
            }
        }.getOrElse {
            Future.successful(Ok(views.html.contractEdit(contractForm)))
        }
    }

    def submitContract(idForUpdate: Option[String] = None): Action[AnyContent] = Action.async { implicit request =>
        contractForm.bindFromRequest.fold(
            errorForm => Future.successful(Ok(views.html.contractEdit(errorForm))),
            contractData => {
                val contract = idForUpdate.map(id => Contract.fill(contractData).copy(id = id)).getOrElse(Contract.fill(contractData))
                val screenshotPaths: Seq[String] =
                    if (contract.screenshotPaths.nonEmpty) {
                        val urls = contract.screenshotPaths.split(';')
                        urls.map { url =>
                            val screenshotId = UUID.randomUUID().toString
                            val path = env.rootPath + af.assetsBasePath + "/images/" + screenshotId + ".png"
                            ScreenshotHelper.screenshotFromUrl(url, path)
                            screenshotId + ".png"
                        }
                    } else {
                        Seq.empty
                    }

                println("screenshots: " + screenshotPaths)
                val finalContract = contract.copy(screenshotPaths = screenshotPaths.mkString(";"))

                contractService.save(finalContract).map { contractOpt => // None если update, Some если insert
                    println("id: " + finalContract.id)
                    Redirect(routes.ContractController.contractList())
                }
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