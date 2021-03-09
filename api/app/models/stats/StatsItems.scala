package models.stats

import play.api.libs.json.{Json, OWrites, Reads}

object StatsItems {
    case class AllTimeStats(income: Double,
                            contractsCount: Int,
                            winningContracts: Int)

    object AllTimeStats {
        implicit val allTimeStatsItemWrites: OWrites[AllTimeStats] = Json.writes
        implicit val allTimeStatsItemReads: Reads[AllTimeStats] = Json.reads
    }

    case class DailyStatsItem(day: Long,
                              income: Double,
                              contractsCount: Int,
                              winningContracts: Int)

    object DailyStatsItem {
        implicit val dailyStatsItemWrites: OWrites[DailyStatsItem] = Json.writes
        implicit val dailyStatsItemReads: Reads[DailyStatsItem] = Json.reads
    }

    case class WeeklyStatsItem(dayFrom: Long,
                               dayTo: Long,
                               income: Double,
                               contractsCount: Int,
                               winningContracts: Int)

    object WeeklyStatsItem {
        implicit val weeklyStatsItemWrites: OWrites[WeeklyStatsItem] = Json.writes
        implicit val weeklyStatsItemReads: Reads[WeeklyStatsItem] = Json.reads
    }

    case class MonthlyStatsItem(firstMonthDay: Long, // Храним первое число месяца, чтобы знать год
                                income: Double,
                                contractsCount: Int,
                                winningContracts: Int)

    object MonthlyStatsItem {
        implicit val yearlyStatsItemWrites: OWrites[MonthlyStatsItem] = Json.writes
        implicit val yearlyStatsItemReads: Reads[MonthlyStatsItem] = Json.reads
    }

    case class AllStats(allTime: AllTimeStats,
                        daily: Seq[DailyStatsItem],
                        weekly: Seq[WeeklyStatsItem],
                        monthly: Seq[MonthlyStatsItem])

    object AllStats {
        implicit val allStatsItemWrites: OWrites[AllStats] = Json.writes
        implicit val allStatsItemReads: Reads[AllStats] = Json.reads
    }
}