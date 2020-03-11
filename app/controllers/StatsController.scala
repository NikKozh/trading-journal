package controllers

import java.time.{DayOfWeek, LocalDate}
import java.time.temporal.WeekFields

import helpers.UserSettingHelper
import javax.inject.{Inject, Singleton}
import models.Contract
import models.stats.{GeneralStatsDailyItem, GeneralStatsWeeklyItem}
import models.user.UserSettingData
import utils.Utils.DateTime._
import play.api.Environment
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesAbstractController, MessagesControllerComponents}
import services.{ContractService, UserSettingsService}
import utils.ExceptionHandler
import utils.Utils.StringHelper

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

    def generalStats(): Action[AnyContent] = asyncActionWithExceptionPage { implicit request =>
        contractService.list.map { contractList =>
            val (userSettingForm, strategyFilterOptions) = getUserSettingFormAndInfo

            @scala.annotation.tailrec
            def findFirstDayOfTheWeekForDate(date: LocalDate): LocalDate =
                if (date.getDayOfWeek == DayOfWeek.MONDAY) date
                else findFirstDayOfTheWeekForDate(date minusDays 1)

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

            Ok(views.html.stats.generalStats(
                dailyStatsItems,
                weeklyStatsItems,
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
