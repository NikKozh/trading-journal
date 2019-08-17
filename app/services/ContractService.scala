package services

import models.Contract

import scala.collection.mutable
import javax.inject.Singleton
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

trait ContractService {
    def save(contract: Contract): Future[Contract]
    def get(id: String): Future[Option[Contract]]
    def list: Future[Seq[Contract]]
}

@Singleton
class ContractServiceInMemoryImpl extends ContractService {
    private val storage: mutable.Map[String, Contract] = mutable.Map.empty

    /**
     * В т.ч. пока работает как update
     */
    override def save(contract: Contract): Future[Contract] = {
        storage += contract.id -> contract
        Future.successful(contract)
    }

    override def get(id: String): Future[Option[Contract]] = Future(storage.get(id))(scala.concurrent.ExecutionContext.global)

    override def list: Future[Seq[Contract]] = Future.successful(storage.values.toSeq.sortBy(_.number))
}

    override def load(id: String): Option[Contract] = storage.get(id)

    override def list: Seq[Contract] = storage.values.toSeq.sortBy(_.number)
}