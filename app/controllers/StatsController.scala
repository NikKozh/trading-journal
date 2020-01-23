package controllers

import java.time.{DayOfWeek, LocalDate}
import java.time.temporal.WeekFields

import javax.inject.{Inject, Singleton}
import models.stats.{GeneralStatsDailyItem, GeneralStatsWeeklyItem}
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
            @scala.annotation.tailrec
            def findFirstDayOfTheWeekForDate(date: LocalDate): LocalDate =
                if (date.getDayOfWeek == DayOfWeek.MONDAY) date
                else findFirstDayOfTheWeekForDate(date minusDays 1)

            // TODO: стопроцентов это можно сделать как-то получше - подумать над этим
            val dailyStatsItems =
                contractList
                    .map(c => (c.created.toLocalDateTime.toLocalDate, c))
                    .groupBy(_._1)
                    .mapValues(_.map(_._2))
                    .map { case (day, contracts) =>
                        GeneralStatsDailyItem(
                            day,
                            income = contracts.flatMap(_.income).sum,
                            contractsCount = contracts.size,
                            winningContracts = contracts.count(_.isWin)
                        )
                    }
                    .toSeq
                    .sortBy(_.day)
                    .reverse

            val weeklyStatsItems =
                dailyStatsItems
                    .groupBy(_.day.getYear)
                    .mapValues(_
                        .groupBy(_.day.get(WeekFields.ISO.weekOfYear()))
                        .values
                        .toSeq
                        .map { dailyStatsItemsOnWeek =>
                            val sortedItems = dailyStatsItemsOnWeek.sortBy(_.day)
                            val firstDayOfWeek = findFirstDayOfTheWeekForDate(sortedItems.head.day)
                            val lastDayOfWeek = firstDayOfWeek plusDays 6

                            GeneralStatsWeeklyItem(
                                daysRange = (firstDayOfWeek, lastDayOfWeek),
                                income = sortedItems.map(_.income).sum,
                                contractsCount = sortedItems.map(_.contractsCount).sum,
                                winningContracts = sortedItems.map(_.winningContracts).sum
                            )
                        }
                    )
                    .values
                    .toSeq
                    .flatten
                    .sortBy(_.daysRange._1)
                    .reverse

            Ok(views.html.stats.generalStats(dailyStatsItems, weeklyStatsItems))
        }
    }
}
