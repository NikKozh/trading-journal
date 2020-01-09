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