package controllers

import javax.inject.{Inject, Singleton}
import models.stats.GeneralStatsDailyItem
import utils.Utils.DateTime._
import play.api.Environment
import play.api.mvc.{Action, AnyContent, MessagesAbstractController, MessagesControllerComponents}
import services.ContractService
import utils.ExceptionHandler

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StatsController @Inject()(mcc: MessagesControllerComponents,
                                contractService: ContractService,
                                af: AssetsFinder,
                                env: Environment
                               )(implicit ec: ExecutionContext)
    extends ExceptionHandler(mcc) {

    def generalStats(): Action[AnyContent] = asyncActionWithExceptionPage {
        contractService.list.map { contractList =>
            // TODO: стопроцентов это можно сделать как-то получше - подумать над этим
            val dailyStatsItems =
                contractList
                    .map(c => (c.created.toLocalDateTime.toLocalDate, c))
                    .groupBy(_._1)
                    .mapValues(_.map(_._2))
                    .map { case (day, contracts) =>
                        GeneralStatsDailyItem(
                            day,
                            contracts.flatMap(_.income).sum,
                            contracts.size,
                            contracts.count(_.isWin)
                        )
                    }
                    .toSeq
                    .sortBy(_.day)
                    .reverse

            Ok(views.html.stats.generalStats(dailyStatsItems))
        }
    }
}
