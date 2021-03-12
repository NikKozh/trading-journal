package controllers

import java.time.{DayOfWeek, Instant, LocalDate}
import java.time.temporal.WeekFields
import javax.inject.{Inject, Singleton}
import models.StatsItems._
import utils.Utils.DateTime._
import play.api.libs.json.{Json, __}
import play.api.mvc._
import services.ContractService
import utils.ErrorHandler
import java.sql.Timestamp
import scala.concurrent.ExecutionContext

@Singleton
class StatsController @Inject()(cc: ControllerComponents,
                                contractService: ContractService)
                               (implicit ec: ExecutionContext)
    extends AbstractController(cc)
        with ErrorHandler {

    private def getAllTimeStats =
        contractService
            .list
            .map(allContracts =>
                AllTimeStats(
                    income = allContracts.flatMap(_.income).sum,
                    contractsCount = allContracts.size,
                    winningContracts = allContracts.count(_.isWin)
                )
            )

    def allTimeStats: Action[AnyContent] = Action.async {
        getAllTimeStats
            .map(Json.toJson[AllTimeStats])
            .map(Ok(_))
            .recover { case e =>
                databaseErrorResponse("Что-то пошло не так при попытке загрузить список сделок", e)
            }
    }

    private def getDailyStatsItems =
        contractService
            .list
            .map(_
                .map(c => (c.created.toLocalDateTime.toLocalDate, c))
                .groupBy(_._1)
                .mapValues(_.map(_._2))
                .map { case (day, contracts) =>
                    DailyStatsItem(
                        day.toEpochMilli,
                        income = contracts.flatMap(_.income).sum,
                        contractsCount = contracts.size,
                        winningContracts = contracts.count(_.isWin)
                    )
                }
                .toSeq
                .sortBy(_.day)(Ordering.Long.reverse)
            )

    def dailyStats: Action[AnyContent] = Action.async {
        getDailyStatsItems
            .map(Json.toJson[Seq[DailyStatsItem]])
            .map(Ok(_))
            .recover { case e =>
                databaseErrorResponse("Что-то пошло не так при попытке загрузить список сделок", e)
            }
    }

    private def epochMilliToLocalDate(epochMillis: Long): LocalDate =
        Timestamp
            .from(Instant.ofEpochMilli(epochMillis))
            .toLocalDateTime
            .toLocalDate

    @scala.annotation.tailrec
    private def findFirstDayOfTheWeekForDate(date: LocalDate): LocalDate =
        if (date.getDayOfWeek == DayOfWeek.MONDAY) date
        else findFirstDayOfTheWeekForDate(date minusDays 1)

    private def getWeeklyStatsItems(dailyStatsItems: Seq[DailyStatsItem]) =
        dailyStatsItems
            .groupBy(dailyItem =>
                epochMilliToLocalDate(dailyItem.day).getYear
            )
            .view
            .mapValues(_
                .groupBy(dailyItem =>
                    epochMilliToLocalDate(dailyItem.day).get(WeekFields.ISO.weekOfYear())
                )
                .values
                .toSeq
                .map { dailyStatsItemsOnWeek =>
                    val sortedItems = dailyStatsItemsOnWeek.sortBy(_.day)
                    val day = epochMilliToLocalDate(sortedItems.head.day)
                    val firstDayOfWeek = findFirstDayOfTheWeekForDate(day)
                    val lastDayOfWeek = firstDayOfWeek plusDays 6

                    WeeklyStatsItem(
                        dayFrom = firstDayOfWeek.toEpochMilli,
                        dayTo = lastDayOfWeek.toEpochMilli,
                        income = sortedItems.map(_.income).sum,
                        contractsCount = sortedItems.map(_.contractsCount).sum,
                        winningContracts = sortedItems.map(_.winningContracts).sum
                    )
                }
            )
            .values
            .toSeq
            .flatten
            .sortBy(_.dayFrom)(Ordering.Long.reverse)

    def weeklyStats: Action[AnyContent] = Action.async {
        getDailyStatsItems
            .map(getWeeklyStatsItems)
            .map(Json.toJson[Seq[WeeklyStatsItem]])
            .map(Ok(_))
            .recover { case e =>
                databaseErrorResponse("Что-то пошло не так при попытке загрузить список сделок", e)
            }
    }

    private def getMonthlyStatsItems(dailyStatsItems: Seq[DailyStatsItem]) =
        dailyStatsItems
            .groupBy(dailyStatsItem =>
                epochMilliToLocalDate(dailyStatsItem.day).getYear
            )
            .view
            .mapValues(_
                .groupBy(dailyStatsItem =>
                    epochMilliToLocalDate(dailyStatsItem.day).getMonth
                )
                .values
                .toSeq
                .map { dailyStatsItemsWithinMonth =>
                    val anyDay = epochMilliToLocalDate(dailyStatsItemsWithinMonth.head.day)
                    val firstMonthDay = LocalDate.of(anyDay.getYear, anyDay.getMonth, 1).toEpochMilli

                    MonthlyStatsItem(
                        firstMonthDay,
                        income = dailyStatsItemsWithinMonth.map(_.income).sum,
                        contractsCount = dailyStatsItemsWithinMonth.map(_.contractsCount).sum,
                        winningContracts = dailyStatsItemsWithinMonth.map(_.winningContracts).sum
                    )
                }
            )
            .values
            .toSeq
            .flatten
            .sortBy(_.firstMonthDay)(Ordering.Long.reverse)

    def monthlyStats: Action[AnyContent] = Action.async {
        getDailyStatsItems
            .map(getMonthlyStatsItems)
            .map(Json.toJson[Seq[MonthlyStatsItem]])
            .map(Ok(_))
            .recover { case e =>
                databaseErrorResponse("Что-то пошло не так при попытке загрузить список сделок", e)
            }
    }

    def allStats: Action[AnyContent] = Action.async {
        val allStats =
            for {
                allTime <- getAllTimeStats
                daily <- getDailyStatsItems
                weekly = getWeeklyStatsItems(daily)
                monthly = getMonthlyStatsItems(daily)
            } yield AllStats(allTime, daily, weekly, monthly)

        allStats
            .map(Json.toJson[AllStats])
            .map(Ok(_))
            .recover { case e =>
                databaseErrorResponse("Что-то пошло не так при попытке загрузить список сделок", e)
            }
    }
}
