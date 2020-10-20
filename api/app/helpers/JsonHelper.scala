package helpers

import java.sql.Timestamp
import play.api.libs.json._

object JsonHelper extends JsonHelper

trait JsonHelper {
    implicit object timestampFormat extends Format[Timestamp] {
        def reads(json: JsValue): JsResult[Timestamp] = JsSuccess(new Timestamp(json.as[Long]))
        def writes(ts: Timestamp): JsValue = JsNumber(ts.getTime)
    }
}