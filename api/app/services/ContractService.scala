package services

import java.sql.Timestamp

import models.Contract

import scala.collection.mutable
import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

trait ContractService {
    /** В т.ч. пока работает как update
     *  @return None если апдейт, Some если вставка */
    def save(contract: Contract): Future[Option[Contract]]
    /** @return удалили или нет */
    def delete(id: String)(implicit ec: ExecutionContext): Future[Boolean]
    def get(id: String)(implicit ec: ExecutionContext): Future[Option[Contract]]
    def get(idOpt: Option[String])(implicit ec: ExecutionContext): Future[Option[Contract]]
    def list: Future[Seq[Contract]]

    def getNewNumber(implicit ec: ExecutionContext): Future[Int] =
        list.map(l => if (l.nonEmpty) l.map(_.number).max + 1 else 1)
}

@Singleton
class ContractServicePostgresImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
    extends ContractService {

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
            isWin,
            screenshotPaths,
            tags,
            isCorrect,
            description,
            buyPrice,
            profitPercent,
        ) <> ((Contract.apply _).tupled, Contract.unapply)
    }

    private val contracts = TableQuery[ContractTable]

    override def save(contract: Contract): Future[Option[Contract]] = db.run {
        (contracts returning contracts).insertOrUpdate(contract)
    }

    override def delete(id: String)(implicit ec: ExecutionContext): Future[Boolean] = db.run {
        contracts.filter(_.id === id).delete.map( deletedRows =>
            if (deletedRows > 0) true else false
        )
    }

    override def get(id: String)(implicit ec: ExecutionContext): Future[Option[Contract]] = db.run {
        contracts.filter(_.id === id).result.headOption
    }

    override def get(idOpt: Option[String])(implicit ec: ExecutionContext): Future[Option[Contract]] =
        idOpt.map(id => db.run { contracts.filter(_.id === id).result.headOption }).getOrElse(Future(None))

    override def list: Future[Seq[Contract]] = db.run {
        contracts.result
    }
}