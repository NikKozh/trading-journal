import com.google.inject.AbstractModule
import services.{ContractService, ContractServicePostgresImpl}

class Module extends AbstractModule {
    override def configure() = {
        /*
            // Use the system clock as the default implementation of Clock
            bind(classOf[Clock]).toInstance(Clock.systemDefaultZone)
            // Ask Guice to create an instance of ApplicationTimer when the
            // application starts.
            bind(classOf[ApplicationTimer]).asEagerSingleton()
            // Set AtomicCounter as the implementation for Counter.
            bind(classOf[Counter]).to(classOf[AtomicCounter])
        */
        bind(classOf[ContractService]).to(classOf[ContractServicePostgresImpl])
    }
}