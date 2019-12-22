package utils

import java.sql.Timestamp
import java.time.LocalDate

object Utils {
    object Math {
        implicit class DoubleWithRounding(x: Double) {
            def round2: Double = BigDecimal(x).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
            def round3: Double = BigDecimal(x).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble
        }

        implicit class OptDoubleWithRounding(x: Option[Double]) {
            def round2: Option[Double] = x.map(BigDecimal(_).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble)
            def round3: Option[Double] = x.map(BigDecimal(_).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble)
        }
    }

    object DateTime {
        implicit def timestampOrdering: Ordering[Timestamp] = (x: Timestamp, y: Timestamp) => x compareTo y
        implicit def localDateOrdering: Ordering[LocalDate] = (x: LocalDate, y: LocalDate) => x compareTo y
    }
}
