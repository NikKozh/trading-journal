package utils

import play.api.mvc._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class ExceptionHandler(mcc: MessagesControllerComponents) extends MessagesAbstractController(mcc) {
    def asyncActionWithExceptionPage(block: MessagesRequest[AnyContent] => Future[Result]): Action[AnyContent] =
        Action.async { implicit request =>
            withExceptionPage {
                Try {
                    block(request)
                }
            }
        }

    def asyncActionWithExceptionPage(block: =>Future[Result]): Action[AnyContent] =
        Action.async {
            withExceptionPage {
                Try {
                    block
                }
            }
        }

    def actionWithExceptionPage(block: MessagesRequest[AnyContent] => Result): Action[AnyContent] =
        Action { implicit request =>
            withExceptionPage {
                Try {
                    block(request)
                }
            }
        }

    private def withExceptionPage(tryBlock: Try[Future[Result]]): Future[Result] =
        tryBlock match {
            case Success(value) => value
            case Failure(exception) => Future.successful(BadRequest(views.html.viewUtils.exceptionPage(exception.getMessage, exception)))
        }

    private def withExceptionPage(tryBlock: Try[Result]): Result =
        tryBlock match {
            case Success(value) => value
            case Failure(exception) => BadRequest(views.html.viewUtils.exceptionPage(exception.getMessage, exception))
        }
}
