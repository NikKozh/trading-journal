package utils

import controllers.ApiError
import play.api.mvc.Result
import scala.concurrent.ExecutionContext

trait ErrorHandler {
    def databaseErrorResponse(errorCause: String, exception: Throwable)(implicit ec: ExecutionContext): Result =
        ApiError(
            caption = "DATABASE PROBLEM",
            cause = errorCause,
            details = Some(exception.getMessage)
        ).asResult

    def contractNotFound(id: String): ApiError =
        ApiError(
            caption = "CONTRACT NOT FOUND",
            cause = s"Сделка с id $id отсутствует в базе данных"
        )
}
