package services

import models.Contract

import scala.collection.mutable
import javax.inject.Singleton

trait ContractService {
    def saveContract(contract: Contract): Option[Contract]
    def loadContract(id: String): Option[Contract]
}

@Singleton
class ContractServiceInMemoryImpl extends ContractService {
    private val storage: mutable.Map[String, Contract] = mutable.Map.empty

    /**
     * В т.ч. пока работает как update
     */
    override def saveContract(contract: Contract): Option[Contract] = {
        storage += contract.id -> contract
        Some(contract)
    }

    override def loadContract(id: String): Option[Contract] = storage.get(id)
}