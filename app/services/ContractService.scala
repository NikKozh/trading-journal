package services

import java.sql.Timestamp

import models.Contract

import scala.collection.mutable
import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

trait ContractService {
    def save(contract: Contract): Future[Option[Contract]]
    def get(id: String): Future[Option[Contract]]
    def list: Future[Seq[Contract]]
}

@Singleton
class ContractServiceInMemoryImpl extends ContractService {
    private val storage: mutable.Map[String, Contract] = mutable.Map.empty

    /**
     * В т.ч. пока работает как update
     */
    override def save(contract: Contract): Future[Option[Contract]] = {
        storage += contract.id -> contract
        Future.successful(Some(contract))
    }

    override def get(id: String): Future[Option[Contract]] = Future(storage.get(id))(scala.concurrent.ExecutionContext.global)

    override def list: Future[Seq[Contract]] = Future.successful(storage.values.toSeq.sortBy(_.number))
}

@Singleton
class ContractServicePostgresImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends ContractService {
    private val dbConfig = dbConfigProvider.get[JdbcProfile]

    import dbConfig._
    import profile.api._

    private class ContractTable(tag: Tag) extends Table[Contract](tag, "Contract") {
        def id = column[String]("id", O.PrimaryKey)
        def number = column[Int]("number")
        def contractType = column[String]("contractType")
        def created = column[Timestamp]("created")
        def expiration = column[Int]("expiration")
        def fxSymbol = column[String]("fxSymbol")
        def direction = column[String]("direction")
        def buyPrice = column[Option[Double]]("buyPrice")
        def profitPercent = column[Option[Double]]("profitPercent")
        def isWin = column[Boolean]("isWin")
        def screenshotPaths = column[String]("screenshotPaths")
        def tags = column[String]("tags")
        def isCorrect = column[Boolean]("isCorrect")
        def description = column[String]("description")

        def * = (
            id,
            number,
            contractType,
            created,
            expiration,
            fxSymbol,
            direction,
            buyPrice,
            profitPercent,
            isWin,
            screenshotPaths,
            tags,
            isCorrect,
            description
        ) <> ((Contract.apply _).tupled, Contract.unapply)
    }

    private val contracts = TableQuery[ContractTable]

    override def save(contract: Contract): Future[Option[Contract]] = db.run {
        (contracts returning contracts).insertOrUpdate(contract)
    }

    override def get(id: String): Future[Option[Contract]] = db.run {
        contracts.filter(_.id === id).result.headOption
    }

    override def list: Future[Seq[Contract]] = db.run {
        contracts.result
    }
}