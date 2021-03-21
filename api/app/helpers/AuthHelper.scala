package helpers

import play.api.mvc.Request

// TODO: пока в очень примитивном виде
object AuthHelper {
    implicit class WrappedRequest(request: Request[_]) {
        def authForGuest: Boolean =
            request
                .headers
                .get("For-Guest")
                .flatMap(_.toBooleanOption)
                .getOrElse {
                    // TODO: потом поменять на нормальный логер
                    println("В заголовках запроса нет 'For-Guest' или не удалось распарсить его значение!")
                    true
                }
    }
}
