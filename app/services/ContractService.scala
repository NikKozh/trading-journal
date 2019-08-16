package services

import models.Contract

import scala.collection.mutable
import javax.inject.Singleton

trait ContractService {
    def save(contract: Contract): Option[Contract]
    def load(id: String): Option[Contract]
    def list: Seq[Contract]
}

@Singleton
class ContractServiceInMemoryImpl extends ContractService {
    private val storage: mutable.Map[String, Contract] = mutable.Map.empty

    /**
     * В т.ч. пока работает как update
     */
    override def save(contract: Contract): Option[Contract] = {
        storage += contract.id -> contract
        Some(contract)
    }

    override def load(id: String): Option[Contract] = storage.get(id)

    override def list: Seq[Contract] = storage.values.toSeq.sortBy(_.number)
}