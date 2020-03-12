package models.stats

import java.time.LocalDate

import utils.Utils.Math._

case class GeneralStatsDailyItem(day: LocalDate,
                                 income: Double,
                                 contractsCount: Int,
                                 winningContracts: Int) {
    val loosedContracts: Int = contractsCount - winningContracts
    val winRatePercent: Double = (winningContracts * 100 / contractsCount).round2
}

case class GeneralStatsWeeklyItem(daysRange: (LocalDate, LocalDate),
                                  income: Double,
                                  contractsCount: Int,
                                  winningContracts: Int) {
    val loosedContracts: Int = contractsCount - winningContracts
    val winRatePercent: Double = (winningContracts * 100 / contractsCount).round2
}

case class GeneralStatsMonthlyItem(month: LocalDate, // Храним первое число месяца, чтобы знать год
                                   income: Double,
                                   contractsCount: Int,
                                   winningContracts: Int) {
    val loosedContracts: Int = contractsCount - winningContracts
    val winRatePercent: Double = (winningContracts * 100 / contractsCount).round2
}