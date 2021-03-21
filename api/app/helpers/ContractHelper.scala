package helpers

// TODO: Возможно, переделать это всё на обычные val'ы внутри объектов?
// TODO: Возможно, перенести это всё в utils.Constants?
object ContractHelper {
    //noinspection TypeAnnotation
    object ContractType extends Enumeration {
        type ContractType = Value
        val REAL = Value("Реал")
        val DEMO = Value("Демо")
        val FORECAST = Value("Форкаст")
        val TESTER = Value("Тестер")
    }

    //noinspection TypeAnnotation
    object FxSymbol extends Enumeration {
        type FxSymbol = Value
        val EUR_USD = Value("EUR/USD")
        val USD_JPY = Value("USD/JPY")
    }

    //noinspection TypeAnnotation
    object ContractDirection extends Enumeration {
        type ContractDirection = Value
        val CALL = Value("CALL")
        val PUT = Value("PUT")
    }
}