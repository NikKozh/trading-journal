package controllers

import java.sql.Timestamp
import java.time.{Instant, LocalDateTime, Month, ZoneId, ZoneOffset}
import java.util.UUID

import helpers.ContractHelper._
import helpers.{BinaryHelper, ContractHelper, OptionNullJsonWriter, ScreenshotHelper}
import javax.inject._
import play.api.mvc._
import services.ContractService
import models.{Contract, ContractData, ContractDraftData, ContractDraftRawData}
import play.api.Environment
import play.api.libs.json.{JsArray, JsObject, Json, OWrites}
import utils.ExceptionHandler
import utils.Utils.Math._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class ContractController @Inject()(mcc: MessagesControllerComponents,
                                   contractService: ContractService,
                                   af: AssetsFinder,
                                   env: Environment
                                  )(implicit ec: ExecutionContext)
    extends ExceptionHandler(mcc) {

    def contractList: Action[AnyContent] = asyncActionWithExceptionPage {
        import utils.Utils.DateTime._

        contractService.list.map(contracts =>
            Ok(views.html.contract.contractList(contracts.sortBy(_.created).reverse))
        )
    }

    def addEditContract(id: Option[String] = None): Action[AnyContent] = asyncActionWithExceptionPage { implicit request =>
        id.map {
            contractService.get(_).map {
                case Some(contract) => Ok(views.html.contract.contractAddEdit(contractForm.fill(ContractData(contract)), id, Some(contract)))
                case None => NotFound
            }
        }.getOrElse {
            contractService.getNewNumber.map { tm =>
                Ok(views.html.contract.contractAddEdit(contractForm.copy(data = contractForm.data + ("number" -> tm.toString))))
            }
        }
    }

    def submitContract(idForUpdate: Option[String] = None): Action[AnyContent] = asyncActionWithExceptionPage { implicit request =>
        contractForm.bindFromRequest.fold(
            errorForm => Future.successful(BadRequest(views.html.contract.contractAddEdit(errorForm))),
            contractData => {
                val contract = idForUpdate.map { newId =>
                    Contract.fill(contractData).copy(id = newId)
                }.getOrElse {
                    val urls = contractData.screenshotUrls.split(';').toSeq
                    val newScreenshotPaths =
                        urls.map { url =>
                            ScreenshotHelper
                                .screenshotFromUrlToBase64(url)
                                .getOrElse(sys.error("Error: something wrong in saving screenshot from given URL or in converting saved image to Base64"))
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

    def viewContract(id: String): Action[AnyContent] = asyncActionWithExceptionPage { implicit request =>
        contractService.get(id).map {
            case Some(contract) => Ok(views.html.contract.contractCard(contract))
            case None => NotFound
        }
    }

    def deleteContract(id: String): Action[AnyContent] = asyncActionWithExceptionPage { implicit request =>
        contractService.delete(id).map {
            case true => Redirect(routes.ContractController.contractList())
            case false => NotFound
        }
    }

    def addContractDraft(): Action[AnyContent] = actionWithExceptionPage { implicit request =>
        Ok(views.html.contract.contractAddDraft(ContractDraftData.form))
    }

    def addRawContractDraft(): Action[AnyContent] = actionWithExceptionPage { implicit request =>
        Ok(views.html.contract.contractAddRawDraft(ContractDraftRawData.form))
    }

    def submitContractDraft(): Action[AnyContent] = asyncActionWithExceptionPage { implicit request =>
        ContractDraftData.form.bindFromRequest.fold(
            errorForm => Future.successful(BadRequest(views.html.contract.contractAddDraft(errorForm))),
            contractDraftData => {
                val transactionId = contractDraftData.transactionId
                val urls = contractDraftData.screenshotsUrls.split(';').toSeq
                parseContractDataAndSubmit(transactionId, urls)
            }
        )
    }

    def submitRawContractDraft(): Action[AnyContent] = asyncActionWithExceptionPage { implicit request =>
        ContractDraftRawData.form.bindFromRequest.fold(
            errorForm => Future.successful(BadRequest(views.html.contract.contractAddRawDraft(errorForm))),
            contractDraftRawData => {
                val lines = contractDraftRawData.raw.lines.toSeq
                val (transactionId, urls) = (lines.head, lines.tail) // TODO: валидация
                parseContractDataAndSubmit(transactionId, urls)
            }
        )
    }

    private def parseContractDataAndSubmit(transactionId: String, urls: Seq[String]): Future[Result] = {
        val contractId = UUID.randomUUID().toString
        val newScreenshotPaths = {
            urls.map { url =>
                ScreenshotHelper
                    .screenshotFromUrlToBase64(url)
                    .getOrElse(sys.error("Error: something wrong in saving screenshot from given URL or in converting saved image to Base64"))
            }
        }
        val contractNumber = contractService.list.map(l => if (l.nonEmpty) l.map(_.number).max + 1 else 1)

        contractNumber.flatMap { newNumber =>
            val contract = Contract(
                id = contractId,
                number = newNumber,
                created = Timestamp.from(Instant.now),
                fxSymbol = "",
                direction = "",
                buyPrice = Some(0),
                profitPercent = Some(0),
                isWin = false,
                screenshotPaths = newScreenshotPaths.mkString(";"), // TODO: в строке на самом деле несколько путей, разделённых точкой с запятой
                tags = "",
                isCorrect = false,
                description = ""
            )
            contractService.save(contract).map(_ => Ok(views.html.viewUtils.binaryWebsocket(transactionId, contractId)))
        }
    }

    def submitProfitTable(): Action[AnyContent] = asyncActionWithExceptionPage { implicit request =>
        request.body.asJson.map { js =>
            val contractId = (js \ "contract_id").as[String]
            val transactionId = (js \ "transaction_id").as[String]
            val transactions = (js \ "table" \ "transactions").get match {
                case ts: JsArray => ts.value.map { case jsObject: JsObject =>
                    val fields = jsObject.fields.toMap
                    ContractTransactionData(
                        fields("buy_price").as[Double],
                        fields("longcode").as[String],
                        fields("payout").as[Double],
                        fields("sell_price").as[Double],
                        fields("shortcode").as[String],
                        fields("sell_time").as[Long],
                        fields("transaction_id").as[Long]
                    )
                }
            }

            contractService.get(contractId).flatMap {
                case Some(contract) =>
                    val updatedContract = transactions.find(_.transactionId.toString == transactionId).map { data =>
                        val direction = data.shortCode.split('_')(0)
                        if (!ContractHelper.ContractDirection.values.map(_.toString).contains(direction))
                            sys.error("Can't parse direction from js transaction!")

                        val profitPercent = (data.payout - data.buyPrice) / data.buyPrice

                        val isWin = data.sellPrice > 0

                        val rawExpiration = """spot at (\d+)""".r.findFirstMatchIn(data.longCode).map(
                            _.group(1).toInt
                        ).getOrElse(sys.error("Can't found regexp expiration in js transaction's longcode!"))
                        val expiration =
                            if (data.longCode.contains("hour"))
                                rawExpiration * 60
                            else
                                rawExpiration

                        val fxSymbol = """payout if (\w+/\w+)""".r.findFirstMatchIn(data.longCode).map(
                            _.group(1)
                        ).getOrElse(sys.error("Can't found regexp fx symbol in js transaction's longcode"))

                        if (!ContractHelper.FxSymbol.values.map(_.toString).contains(fxSymbol))
                            sys.error("Can't parse fx symbol from js transaction!")

                        contract.copy(
                            expiration = expiration,
                            direction = direction,
                            fxSymbol = fxSymbol,
                            buyPrice = Some(data.buyPrice.round2),
                            profitPercent = Some(profitPercent.round3),
                            isWin = isWin
                        )
                    }.getOrElse(sys.error(s"Error: can't find transaction $transactionId in js transactions"))

                    contractService.save(updatedContract).map { c =>
                        if (c.isDefined) BadRequest("Error: contract was created, not updated")
                        else Ok(s"/editPrefillContract/$contractId")
                    }

                case _ => Future.successful(BadRequest(s"Error: can't find contract $contractId in DB"))
            }
        } getOrElse {
            Future.successful(BadRequest("Error: can't get body as json"))
        }
    }

    def editPrefillContract(id: String): Action[AnyContent] = asyncActionWithExceptionPage { implicit request =>
        contractService.get(id).map {
            case Some(contract) => Ok(views.html.contract.contractAddEdit(contractForm.fill(ContractData(contract)), Some(id), Some(contract)))
            case _ => BadRequest("Can't find prefilled contract")
        }
    }

    // Оставлено для дебага
    def ping(): Action[AnyContent] = Action { implicit request =>
        Ok(Json.toJson(Ping("main message", "up", 1)))
    }
}

case class ContractTransactionData(buyPrice: Double,
                                   longCode: String,
                                   payout: Double,
                                   sellPrice: Double,
                                   shortCode: String,
                                   time: Long,
                                   transactionId: Long)

case class BinaryContractData(contractId: String, date: Timestamp)


case class Ping(message: String, status: String, code: Int)

object Ping {
    implicit val pingWrites: OWrites[Ping] = Json.writes
}

case class ApiError(caption: String, cause: String, details: Option[String] = None)

object ApiError extends OptionNullJsonWriter {
    implicit val apiErrorWrites: OWrites[ApiError] = Json.writes
}