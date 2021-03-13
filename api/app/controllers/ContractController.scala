package controllers

import java.sql.Timestamp
import java.util.UUID
import helpers.{BinaryHelper, ContractControllerHelper, OptionNullJsonWriter}
import javax.inject._
import play.api.mvc._
import services.ContractService
import models.Contract
import play.api.Configuration
import play.api.libs.json.{Json, OWrites, Reads, __}
import play.api.mvc.Results.BadRequest
import utils.ErrorHandler
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContractController @Inject()(mcc: MessagesControllerComponents, contractService: ContractService)
                                  (implicit ec: ExecutionContext, config: Configuration)
    extends MessagesAbstractController(mcc)
        with ContractControllerHelper
        with ErrorHandler {

    def contractListNew: Action[AnyContent] = Action.async {
        contractService
            .list
            .map(Json.toJson[Seq[Contract]])
            .map(Ok(_))
            // TODO: обобщить блок с .recover
            .recover { case e =>
                databaseErrorResponse("Что-то пошло не так при попытке загрузить список сделок", e)
            }
    }

    def contractCardNew(id: String): Action[AnyContent] = Action.async {
        contractService
            .get(id)
            .map(_
                .toRight(contractNotFound(id))
                .fold(error => BadRequest(Json.toJson(error)), contract => Ok(Json.toJson(contract)))
            )
            .recover { case e =>
                databaseErrorResponse(s"Что-то пошло не так при попытке загрузить сделку с id $id", e)
            }
    }

    def submitContractNew: Action[AnyContent] = Action.async { implicit request =>
        readAndParseJsonWithErrorHandling[Contract] { contract =>
            processScreenshots(contract.screenshotPaths)
                .map(screenshots => contract.copy(screenshotPaths = screenshots))
                .fold(
                    error => ApiError(
                        caption = "SCREENSHOT PARSING PROBLEM",
                        cause = error
                    ).asAsyncResult,
                    updatedContract =>
                        contractService
                            .save(updatedContract)
                            .map(_ => Ok)
                            .recover { case e =>
                                databaseErrorResponse(
                                    s"Что-то пошло не так при попытке сохранить сделку ${contract.id}", e
                                )
                            }
                )
        }
    }

    def deleteContractNew(id: String): Action[AnyContent] = Action.async { implicit request =>
        contractService
            .delete(id)
            .map(isDeleted =>
                if (isDeleted) Ok
                else           BadRequest(Json.toJson(contractNotFound(id)))
            )
            .recover { case e =>
                databaseErrorResponse(s"Что-то пошло не так при попытке удалить сделку $id", e)
            }
    }

    def newContractData: Action[AnyContent] = Action.async { implicit request =>
        contractService
            .getNewNumber
            .map(newNumber => Ok(Json.toJson(NewContractData(UUID.randomUUID().toString, newNumber))))
            .recover { case e =>
                databaseErrorResponse("Что-то пошло не так при попытке получить номер для новой сделки", e)
            }
    }

    def prefillContractNew: Action[AnyContent] = Action.async { implicit request =>
        readAndParseJsonWithErrorHandling[PrefillContractData] { prefillContractData =>
            BinaryHelper.getProfitTable.flatMap { profitTableJson =>
                contractService.getNewNumber.flatMap { contractNumber =>
                    createPrefilledContract(profitTableJson, prefillContractData, contractNumber)
                        .fold(
                            error => ApiError(
                                caption = "PREFILL CONTRACT PROBLEM",
                                cause = "Что-то пошло не так при попытке создать предзаполеннную сделку",
                                details = Some(error)
                            ).asAsyncResult,
                            prefilledContract =>
                                contractService
                                    .save(prefilledContract)
                                    .map(_ => Ok(Json.toJson(prefilledContract.id)))
                                    .recover { case e =>
                                        databaseErrorResponse(
                                            s"Что-то пошло не так при попытке сохранить" +
                                            s"предзаполненную сделку ${prefilledContract.id}", e
                                        )
                                    }
                        )
                }.recover { case e =>
                    databaseErrorResponse("Что-то пошло не так при попытке получить номер для новой сделки", e)
                }
            }.recover { e =>
                ApiError(
                    caption = "PROFIT TABLE REQUEST PROBLEM",
                    cause = "Что-то пошло не так при попытке запросить список сделок у брокера",
                    details = Some(e.getMessage)
                ).asResult
            }
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

case class NewContractData(id: String, number: Int)
object NewContractData {
    implicit val newContractDataWrites: OWrites[NewContractData] = Json.writes
}

case class Ping(message: String, status: String, code: Int)
object Ping {
    implicit val pingWrites: OWrites[Ping] = Json.writes
}

case class ApiError(caption: String, cause: String, details: Option[String] = None) {
    def asResult(implicit ec: ExecutionContext): Result =
        BadRequest(Json.toJson(this))

    def asAsyncResult(implicit ec: ExecutionContext): Future[Result] =
        Future(asResult)
}
object ApiError extends OptionNullJsonWriter {
    implicit val apiErrorWrites: OWrites[ApiError] = Json.writes
}

case class PrefillContractData(transactionId: String, screenshotUrls: String)
object PrefillContractData {
    implicit val prefillContractDataReads: Reads[PrefillContractData] = Json.reads
}