package controllers

import java.sql.Timestamp
import java.time.{Instant, LocalDateTime, Month, ZoneId, ZoneOffset}
import java.util.UUID

import helpers.ContractHelper._
import helpers.OcrHelper._
import helpers.{BinaryHelper, ScreenshotHelper}
import javax.inject._
import play.api.mvc._
import services.ContractService
import models.{Contract, ContractData, ContractDraftData, ContractDraftRawData}
import play.api.Environment
import play.api.libs.json.{JsArray, JsObject}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class ContractController @Inject()(mcc: MessagesControllerComponents,
                                   contractService: ContractService,
                                   af: AssetsFinder,
                                   env: Environment
                                  )(implicit ec: ExecutionContext)
    extends MessagesAbstractController(mcc) {

    def contractList: Action[AnyContent] = asyncActionWithExceptionPage {
        contractService.list.map(contracts =>
            Ok(views.html.contractList(contracts.sortBy(_.number).reverse))
        )
    }

    def addEditContract(id: Option[String] = None): Action[AnyContent] = asyncActionWithExceptionPage { implicit request =>
        id.map {
            contractService.get(_).map {
                case Some(contract) => Ok(views.html.contractAddEdit(contractForm.fill(ContractData(contract)), id, Some(contract)))
                case None => NotFound
            }
        }.getOrElse {
            contractService.getNewNumber.map { tm =>
                Ok(views.html.contractAddEdit(contractForm.copy(data = contractForm.data + ("number" -> tm.toString))))
            }
        }
    }

    def submitContract(idForUpdate: Option[String] = None): Action[AnyContent] = asyncActionWithExceptionPage { implicit request =>
        contractForm.bindFromRequest.fold(
            errorForm => Future.successful(BadRequest(views.html.contractAddEdit(errorForm))),
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
                                ._1 // возвращаем fullImage TODO: заменить потом tuple на простенький кейс-класс, чтобы было понятнее
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
            case Some(contract) => Ok(views.html.contractCard(contract))
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
        Ok(views.html.contractAddDraft(ContractDraftData.form))
    }

    def addRawContractDraft(): Action[AnyContent] = actionWithExceptionPage { implicit request =>
        Ok(views.html.contractAddRawDraft(ContractDraftRawData.form))
    }

    def submitContractDraft(): Action[AnyContent] = asyncActionWithExceptionPage { implicit request =>
        ContractDraftData.form.bindFromRequest.fold(
            errorForm => Future.successful(BadRequest(views.html.contractAddDraft(errorForm))),
            contractDraftData => {
                val transactionId = contractDraftData.transactionId
                val urls = contractDraftData.screenshotsUrls.split(';').toSeq
                parseContractDataAndSubmit(transactionId, urls)
            }
        )
    }

    def submitRawContractDraft(): Action[AnyContent] = asyncActionWithExceptionPage { implicit request =>
        ContractDraftRawData.form.bindFromRequest.fold(
            errorForm => Future.successful(BadRequest(views.html.contractAddRawDraft(errorForm))),
            contractDraftRawData => {
                val lines = contractDraftRawData.raw.lines.toSeq
                val (transactionId, urls) = (lines.head, lines.tail) // TODO: валидация
                parseContractDataAndSubmit(transactionId, urls)
            }
        )
    }

    private def parseContractDataAndSubmit(transactionId: String, urls: Seq[String]): Future[Result] = {
        val contractId = UUID.randomUUID().toString
        val (screenshotForOCR, newScreenshotPaths) = {
            val result = urls.map { url =>
                ScreenshotHelper
                    .screenshotFromUrlToBase64(url)
                    .getOrElse(sys.error("Error: something wrong in saving screenshot from given URL or in converting saved image to Base64"))
            }
            (result.head._2, result.map(_._1)) // (firstCropImage, Seq[fullImage]) TODO: заменить tuple на кейс-класс
        }
        val ocrResult = getOcrResult(screenshotForOCR)
        val ocrContractData = parseOcrResult(contractId, ocrResult)
        val contractNumber = contractService.list.map(l => if (l.nonEmpty) l.map(_.number).max + 1 else 1)

        contractNumber.flatMap { newNumber =>
            val contract = Contract(
                id = contractId,
                number = newNumber,
                created = ocrContractData.screenshotDate.getOrElse(Timestamp.from(Instant.now)),
                fxSymbol = ocrContractData.fxSymbol.getOrElse(""),
                direction = "",
                buyPrice = Some(0),
                profitPercent = Some(0),
                isWin = false,
                screenshotPaths = newScreenshotPaths.mkString(";"), // TODO: в строке на самом деле несколько путей, разделённых точкой с запятой
                tags = "",
                isCorrect = false,
                description = ""
            )
            contractService.save(contract).map(_ => Ok(views.html.binaryWebsocket(ocrContractData, transactionId)))
        }
    }

    def submitProfitTable(): Action[AnyContent] = asyncActionWithExceptionPage { implicit request =>
        request.body.asJson.map { js =>
            val contractId = (js \ "contract_id").as[String]
            val date = Timestamp.valueOf((js \ "date").as[String]).getTime / 1000 // чтобы из миллисекунд получить секунды и корректно сравнить с временем сделки из Бинари (там секунды)
            val fxSymbol = (js \ "fx_symbol").as[String] // не используется, т.к. заполняется на предыдущем этапе, но на всякий случай оставил
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
                    val updatedContract = transactions.find(_.transactionId.toString == transactionId).map { data => // TODO: добавить фильтрацию по FX Symbol (когда OCR будет понадёжнее, а то щас не распознаёт)
                        val direction = data.shortCode.split('_')(0)
                        if (direction != "CALL" && direction != "PUT") sys.error("Can't parse direction from js transaction!")

                        val profitPercent = (data.payout - data.buyPrice) / data.buyPrice

                        val isWin = data.sellPrice > 0

                        val rawExpiration = """spot at (\d+)""".r.findFirstMatchIn(data.longCode).map(
                            _.group(1).toInt
                        ).getOrElse(sys.error("Can't found regex expiration in js transaction's longcode!"))
                        val expiration =
                            if (data.longCode.contains("hour"))
                                rawExpiration * 60
                            else
                                rawExpiration

                        val fxSymbol =
                            if (contract.fxSymbol.isEmpty || (contract.fxSymbol != "USD/JPY" && contract.fxSymbol != "EUR/USD")) // TODO: опять же хардкод пар, нужен константный список
                                """[A-Z]{3}/[A-Z]{3}""".r.findFirstIn(data.longCode).getOrElse(sys.error("Error: can't parse fx symbol neither from OCR or binary transaction!"))
                            else
                                contract.fxSymbol

                        contract.copy(
                            expiration = expiration,
                            fxSymbol = fxSymbol,
                            direction = direction,
                            buyPrice = Some(data.buyPrice.round2),
                            profitPercent = Some(profitPercent.round3),
                            isWin = isWin
                        )
                    }.getOrElse(sys.error("Error: can't find this contract time in js transactions"))

                    contractService.save(updatedContract).map { c =>
                        if (c.isDefined) BadRequest("Error: contract was created, not updated")
                        else Ok(s"/editPrefillContract/$contractId")
                    }

                case _ => Future.successful(BadRequest("Error: can't find contract in DB"))
            }
        } getOrElse {
            Future.successful(BadRequest("Error: can't get body as json"))
        }
    }

    def editPrefillContract(id: String): Action[AnyContent] = asyncActionWithExceptionPage { implicit request =>
        contractService.get(id).map {
            case Some(contract) => Ok(views.html.contractAddEdit(contractForm.fill(ContractData(contract)), Some(id), Some(contract)))
            case _ => BadRequest("Can't find prefilled contract")
        }
    }

    private def asyncActionWithExceptionPage(block: MessagesRequest[AnyContent] => Future[Result]): Action[AnyContent] =
        Action.async { implicit request =>
            withExceptionPage {
                Try {
                    block(request)
                }
            }
        }

    private def asyncActionWithExceptionPage(block: =>Future[Result]): Action[AnyContent] =
        Action.async {
            withExceptionPage {
                Try {
                    block
                }
            }
        }

    private def withExceptionPage(tryBlock: Try[Future[Result]]): Future[Result] =
        tryBlock match {
            case Success(value) => value
            case Failure(exception) => Future.successful(BadRequest(views.html.exceptionPage(exception.getMessage, exception)))
        }

    private def withExceptionPage(tryBlock: Try[Result]): Result =
        tryBlock match {
            case Success(value) => value
            case Failure(exception) => BadRequest(views.html.exceptionPage(exception.getMessage, exception))
        }

    private def actionWithExceptionPage(block: MessagesRequest[AnyContent] => Result): Action[AnyContent] =
        Action { implicit request =>
            withExceptionPage {
                Try {
                    block(request)
                }
            }
        }
}

case class ContractTransactionData(buyPrice: Double,
                                   longCode: String,
                                   payout: Double,
                                   sellPrice: Double,
                                   shortCode: String,
                                   time: Long,
                                   transactionId: Long)