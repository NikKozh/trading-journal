package controllers

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

import helpers.ContractHelper._
import helpers.OcrHelper._
import helpers.ScreenshotHelper
import javax.inject._
import play.api.mvc._
import services.ContractService
import models.{Contract, ContractData, ContractDraftData}
import play.api.Environment
import scalaj.http.Http
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
                case Some(contract) => Ok(views.html.contractAddEdit(contractForm.fill(ContractData(contract)), id))
                case None => NotFound
            }
        }.getOrElse {
            Future.successful(Ok(views.html.contractAddEdit(contractForm)))
        }
    }

    def submitContract(idForUpdate: Option[String] = None): Action[AnyContent] = Action.async { implicit request =>
        contractForm.bindFromRequest.fold(
            errorForm => Future.successful(BadRequest(views.html.contractAddEdit(errorForm))),
            contractData => {
                val contract = idForUpdate.map { newId =>
                    Contract.fill(contractData).copy(id = newId)
                }.getOrElse {
                    val urls = contractData.screenshotUrls.split(';').toSeq
                    val newScreenshotPaths =
                        urls.map { url =>
                            val screenshotId = UUID.randomUUID().toString
                            val path = env.rootPath + af.assetsBasePath + "/images/" + screenshotId + ".png"
                            // ScreenshotHelper.screenshotFromUrl(url, path)
                            screenshotId + ".png"
                        }

                    Contract.fill(contractData).copy(screenshotPaths = newScreenshotPaths.mkString(";"))
                }

                contractService.save(contract).map { contractOpt => // None если update, Some если insert
                    println("id: " + contract.id)
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

    def addContractDraft(): Action[AnyContent] = Action { implicit request =>
        Ok(views.html.contractAddDraft(ContractDraftData.form))
    }

    def submitContractDraft(): Action[AnyContent] = Action.async { implicit request =>
        ContractDraftData.form.bindFromRequest.fold(
            errorForm => Future.successful(BadRequest(views.html.contractAddDraft(errorForm))),
            contractDraftData => {
                val urls = contractDraftData.screenshotsUrls.split(';').toSeq
                val newScreenshotPaths =
                    urls.map { url =>
                        val screenshotId = UUID.randomUUID().toString
                        val path = env.rootPath + af.assetsBasePath + "/images/" + screenshotId + ".png"
                        ScreenshotHelper.screenshotFromUrl(url, path)
                        screenshotId + ".png"
                    }
                val ocrResult = Http("https://api.ocr.space/parse/imageurl").params("apikey" -> "ee03921ca788957", "url" -> urls.head).asString
                val ocrContractData = parseOcrResult(ocrResult.body)
                val parsedOcrResult = "OCR result: " + ocrResult.body.toLowerCase
                val contractNumber = contractService.list.map(_.map(_.number).max + 1)

                contractNumber.map { newNumber =>
                    val contract = Contract(
                        number = newNumber,
                        contractType = "",
                        created = Timestamp.from(Instant.now),
                        expiration = 5,
                        fxSymbol = "",
                        direction = "",
                        buyPrice = Some(0),
                        profitPercent = Some(0),
                        isWin = false,
                        screenshotPaths = newScreenshotPaths.mkString(";"), // TODO: в строке на самом деле несколько путей, разделённых точкой с запятой
                        tags = "",
                        isCorrect = false,
                        description = parsedOcrResult
                    )

                    Ok(views.html.contractAddEdit(contractForm.fill(ContractData(contract)), Some(contract.id)))
                }
            }
        )
    }
}