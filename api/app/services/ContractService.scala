package services

import java.sql.Timestamp
import models.Contract
import javax.inject.{Inject, Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import scala.concurrent.{ExecutionContext, Future}

trait ContractService {
    /** В т.ч. пока работает как update
     *  @return None если апдейт, Some если вставка */
    def save(contract: Contract): Future[Option[Contract]]
    /** @return удалили или нет */
    def delete(id: String, forGuest: Boolean)(implicit ec: ExecutionContext): Future[Boolean]
    def get(id: String, forGuest: Boolean)(implicit ec: ExecutionContext): Future[Option[Contract]]
    def get(idOpt: Option[String], forGuest: Boolean)(implicit ec: ExecutionContext): Future[Option[Contract]]
    def list(forGuest: Boolean): Future[Seq[Contract]]

    def getNewNumber(forGuest: Boolean)(implicit ec: ExecutionContext): Future[Int] =
        list(forGuest).map(l => if (l.nonEmpty) l.map(_.number).max + 1 else 1)
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
        def forGuest = column[Boolean]("forGuest")

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
            forGuest,
            buyPrice,
            profitPercent,
        ) <> ((Contract.apply _).tupled, Contract.unapply)
    }

    private val contracts = TableQuery[ContractTable]

    override def save(contract: Contract): Future[Option[Contract]] = db.run {
        (contracts returning contracts).insertOrUpdate(contract)
    }

    private def findContract(id: String, forGuest: Boolean) =
        contracts.filter(c => (c.id === id) && (c.forGuest === forGuest))

    override def delete(id: String, forGuest: Boolean)(implicit ec: ExecutionContext): Future[Boolean] = db.run {
        findContract(id, forGuest)
            .delete
            .map( deletedRows =>
                if (deletedRows > 0) true else false
            )
    }

    override def get(id: String, forGuest: Boolean)(implicit ec: ExecutionContext): Future[Option[Contract]] = db.run {
        findContract(id, forGuest).result.headOption
    }

    override def get(idOpt: Option[String], forGuest: Boolean)
                    (implicit ec: ExecutionContext): Future[Option[Contract]] =
        idOpt.map(id => get(id, forGuest)).getOrElse(Future(None))

    override def list(forGuest: Boolean): Future[Seq[Contract]] = db.run {
        contracts.filter(_.forGuest === forGuest).result
    }
}