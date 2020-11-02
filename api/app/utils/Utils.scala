package utils

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
        import java.sql.Timestamp
        import java.time.LocalDate
        import java.time.format.DateTimeFormatter

        implicit def timestampOrdering: Ordering[Timestamp] = (x: Timestamp, y: Timestamp) => x compareTo y
        implicit def localDateOrdering: Ordering[LocalDate] = (x: LocalDate, y: LocalDate) => x compareTo y

        implicit class LocalDateWithRusFormatting(d: LocalDate) {
            def formatRus: String = d.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        }
    }

    // TODO: Может, ему место всё-таки в пакете хелперов в отдельном файле?
    // TODO: Сделать имплиситную версию
    object StringHelper {
        def trimToOption(s: String): Option[String] = trimToOption(Option(s))
        def trimToOption(opt: Option[String]): Option[String] = opt.map(_.trim).filter(_.nonEmpty)
    }

    // TODO: тот же вопрос про пакет хелперов, пока запихну сюда
    object SeqHelper {
        // TODO: после добавления в проект Cats убрать эту реализацию, заменить на:
        //       https://stackoverflow.com/a/52547302/10253418
        def seqToOpt[A](seq: Seq[Option[A]]): Option[Seq[A]] =
            seq.foldLeft(Option(Seq.empty[A])){
                (res, opt) =>
                    for {
                        seq <- res
                        v <- opt
                    } yield seq :+ v
            }
    }

    // TODO: тот же вопрос про пакет хелперов, пока запихну сюда
    object Base64Helper {
        import java.util.Base64
        import scala.util.Try

        def isStringContainsValidBase64(str: String): Boolean = Try(Base64.getDecoder.decode(str)).isSuccess
    }
}
