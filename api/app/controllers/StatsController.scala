package controllers

import java.time.{DayOfWeek, Instant, LocalDate, ZoneOffset}
import java.time.temporal.{ChronoField, WeekFields}
import helpers.UserSettingHelper

import javax.inject.{Inject, Singleton}
import models.Contract
import models.stats.{GeneralStatsDailyItem, GeneralStatsMonthlyItem, GeneralStatsWeeklyItem}
import models.stats.StatsItems._
import models.user.UserSettingData
import utils.Utils.DateTime._
import play.api.Environment
import play.api.data.Form
import play.api.libs.json.{Json, __}
import play.api.mvc.{Action, AnyContent, MessagesAbstractController, MessagesControllerComponents}
import services.{ContractService, UserSettingsService}
import utils.ExceptionHandler
import utils.Utils.StringHelper

import java.sql.Timestamp
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

@Singleton
class StatsController @Inject()(mcc: MessagesControllerComponents,
                                af: AssetsFinder,
                                env: Environment,
                                contractService: ContractService,
                                userSettingsService: UserSettingsService
                               )(implicit ec: ExecutionContext)
    extends ExceptionHandler(mcc) {

    private def databaseErrorResponse(errorCause: String, exception: Throwable) =
        ApiError.asResult(
            caption = "DATABASE PROBLEM",
            cause = errorCause,
            details = Some(exception.getMessage)
        )

    def allTimeStats(): Action[AnyContent] = Action.async {
        contractService
            .list
            .map(allContracts =>
                AllTimeStats(
                    income = allContracts.flatMap(_.income).sum,
                    contractsCount = allContracts.size,
                    winningContracts = allContracts.count(_.isWin)
                )
            )
            .map(Json.toJson[AllTimeStats])
            .map(Ok(_))
            .recover { case e =>
                databaseErrorResponse("Что-то пошло не так при попытке загрузить список сделок", e)
            }
    }

    private def dailyStatsItems =
        contractService
            .list
            //            .map(_.filter(_.created > Timestamp.valueOf("2020-11-05 00:00:00.0")))
            .map(_
                .map(c => (c.created.toLocalDateTime.toLocalDate, c))
                .groupBy(_._1)
                .mapValues(_.map(_._2))
                .map { case (day, contracts) =>
                    DailyStatsItem(
                        day.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000,
                        income = contracts.flatMap(_.income).sum,
                        contractsCount = contracts.size,
                        winningContracts = contracts.count(_.isWin)
                    )
                }
                .toSeq
                .sortBy(_.day)
                .reverse
            )

    @scala.annotation.tailrec
    private def findFirstDayOfTheWeekForDate(date: LocalDate): LocalDate =
        if (date.getDayOfWeek == DayOfWeek.MONDAY) date
        else findFirstDayOfTheWeekForDate(date minusDays 1)

    def dailyStats(): Action[AnyContent] = Action.async {
        dailyStatsItems
            .map(Json.toJson[Seq[DailyStatsItem]])
            .map(Ok(_))
            .recover { case e =>
                databaseErrorResponse("Что-то пошло не так при попытке загрузить список сделок", e)
            }
    }

    private def epochMillisToLocalDate(epochMillis: Long): LocalDate =
        Timestamp
            .from(Instant.ofEpochMilli(epochMillis))
            .toLocalDateTime
            .toLocalDate

    def weeklyStats(): Action[AnyContent] = Action.async {
        dailyStatsItems
            .map(_
                .groupBy(dailyItem =>
                    epochMillisToLocalDate(dailyItem.day).getYear
                )
                .mapValues(_
                    .groupBy(dailyItem =>
                        epochMillisToLocalDate(dailyItem.day).get(WeekFields.ISO.weekOfYear())
                    )
                    .values
                    .toSeq
                    .map { dailyStatsItemsOnWeek =>
                        val sortedItems = dailyStatsItemsOnWeek.sortBy(_.day)
                        val day = epochMillisToLocalDate(sortedItems.head.day)
                        val firstDayOfWeek = findFirstDayOfTheWeekForDate(day)
                        val lastDayOfWeek = firstDayOfWeek plusDays 6

                        WeeklyStatsItem(
                            dayFrom = firstDayOfWeek.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000,
                            dayTo = lastDayOfWeek.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000,
                            income = sortedItems.map(_.income).sum,
                            contractsCount = sortedItems.map(_.contractsCount).sum,
                            winningContracts = sortedItems.map(_.winningContracts).sum
                        )
                    }
                )
                .values
                .toSeq
                .flatten
                .sortBy(_.dayFrom)
                .reverse
            )
            .map(Json.toJson[Seq[WeeklyStatsItem]])
            .map(Ok(_))
            .recover { case e =>
                databaseErrorResponse("Что-то пошло не так при попытке загрузить список сделок", e)
            }
    }

    def yearlyStats(): Action[AnyContent] = Action.async {
        dailyStatsItems
            .map(_
                .groupBy(dailyStatsItem =>
                    epochMillisToLocalDate(dailyStatsItem.day).getYear
                )
                .mapValues(_
                    .groupBy(dailyStatsItem =>
                        epochMillisToLocalDate(dailyStatsItem.day).getMonth
                    )
                    .values
                    .toSeq
                    .map { dailyStatsItemsWithinMonth =>
                        val anyDay = epochMillisToLocalDate(dailyStatsItemsWithinMonth.head.day)
                        val firstMonthDay =
                            LocalDate
                                .of(anyDay.getYear, anyDay.getMonth, 1)
                                .atStartOfDay()
                                .toEpochSecond(ZoneOffset.UTC) * 1000

                        YearlyStatsItem(
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
//                .sortBy(yearlyStatsItem => epochMillisToLocalDate(yearlyStatsItem.firstMonthDay).getMonth)
                .sortBy(_.firstMonthDay)
                .reverse
            )
            .map(Json.toJson[Seq[YearlyStatsItem]])
            .map(Ok(_))
            .recover { case e =>
                databaseErrorResponse("Что-то пошло не так при попытке загрузить список сделок", e)
            }
    }

    def generalStats(): Action[AnyContent] = asyncActionWithExceptionPage { implicit request =>
        contractService.list.map { contractList =>
            val (userSettingForm, strategyFilterOptions) = getUserSettingFormAndInfo

            // TODO: стопроцентов это можно сделать как-то получше - подумать над этим
            val dailyStatsItems =
                contractList
                    .filter {
                        val strategyFilterOpt =
                            for {
                                userSettingData <- userSettingForm.value
                                strategyFilter <- userSettingData.strategyFilter
                            } yield strategyFilter

                        strategyFilterOpt
                            .map(strategyFilter => {
                                contract: Contract =>
                                    contract
                                        .tags
                                        .split(';')
                                        .headOption
                                        .map(_.trim)
                                        .contains(strategyFilter)
                            })
                            .getOrElse({ _ => true })
                    }
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

            val monthlyStatsItems =
                dailyStatsItems
                    .groupBy(_.day.getYear)
                    .mapValues(_
                        .groupBy(_.day.getMonth)
                        .values
                        .toSeq
                        .map { dailyStatsItemsWithinMonth =>
                            val anyDay = dailyStatsItemsWithinMonth.head.day

                            GeneralStatsMonthlyItem(
                                month = LocalDate.of(anyDay.getYear, anyDay.getMonth, 1),
                                income = dailyStatsItemsWithinMonth.map(_.income).sum,
                                contractsCount = dailyStatsItemsWithinMonth.map(_.contractsCount).sum,
                                winningContracts = dailyStatsItemsWithinMonth.map(_.winningContracts).sum
                            )
                        }
                    )
                    .values
                    .toSeq
                    .flatten
                    .sortBy(_.month)
                    .reverse

            Ok(views.html.stats.generalStats(
                dailyStatsItems,
                weeklyStatsItems,
                monthlyStatsItems,
                userSettingForm,
                strategyFilterOptions
            ))
        }
    }

    private def getUserSettingFormAndInfo: (Form[UserSettingData], Seq[(String, String)]) = {
        val userSettingF = userSettingsService.get("0")
        val contractListF = contractService.list

        val (userSettingOpt, contractList) = Await.result(userSettingF.zip(contractListF), 10 seconds)

        val form =
            userSettingOpt
                .map(UserSettingData.fill)
                .map(UserSettingHelper.userSettingForm.fill)
                .getOrElse(sys.error("Can't find UserSetting with id 0 in DataBase!"))

        val options =
            contractList
                .flatMap(_
                    .tags
                    .split(';')
                    .headOption
                    .flatMap(StringHelper.trimToOption)
                )
                .distinct
                .sorted
                .map(str => str -> str)

        (form, ("" -> "") +: options)
    }
}
