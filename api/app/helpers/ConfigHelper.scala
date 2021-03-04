package helpers

import play.api.{ConfigLoader, Configuration}
import scala.concurrent.{ExecutionContext, Future}

object ConfigHelper {
    def getConfigValue[T](param: String)
                         (implicit ec: ExecutionContext,
                                   config: Configuration,
                                   configLoader: ConfigLoader[T]): Future[T] =
        config
            .get[Option[T]](param)
            .map(Future(_))
            .getOrElse(Future.failed(sys.error(s"$param is not defined in config")))
}