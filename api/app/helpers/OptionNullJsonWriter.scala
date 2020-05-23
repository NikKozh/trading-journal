package helpers

trait OptionNullJsonWriter {
    import play.api.libs.json._
    implicit val config = JsonConfiguration(optionHandlers = OptionHandlers.WritesNull)
}

object OptionNullJsonWriter extends OptionNullJsonWriter
