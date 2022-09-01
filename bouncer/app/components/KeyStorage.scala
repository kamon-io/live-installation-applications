package components

import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random


/**
  * Access layer for security credentials
  */
trait KeyStorage {

  /**
    * Retrieves a key from storage.
    */
  def retrieve(key: String): Future[Option[KeyStorage.Key]]

  /**
    * Creates a new, random key and stores it.
    */
  def create(organizationID: Long): Future[KeyStorage.Key]

  /**
    * Retrieves all keys for a given organization.
    */
  def list(organizationID: Long): Future[Seq[KeyStorage.Key]]

  /**
    * Revokes an API key if it exists.
    */
  def revoke(organizationID: Long, key: String): Future[Boolean]


}

object KeyStorage {

  case class Key(key: String, organizationID: Long)
  case class Organization(id: Long, name: String)

  class InDatabase @Inject()(val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) extends KeyStorage
      with HasDatabaseConfigProvider[JdbcProfile]{
    import profile.api._

    override def retrieve(key: String): Future[Option[Key]] =
      db.run(Keys.filter(k => k.key === key).result).map(_.headOption)

    override def create(organizationID: Long): Future[Key] =
      db.run((Keys returning Keys) += KeyStorage.Key(Random.alphanumeric.take(16).mkString.toUpperCase, organizationID))


    override def list(organizationID: Long): Future[Seq[Key]] =
      db.run(Keys.filter(k => k.organizationID === organizationID).result)

    override def revoke(organizationID: Long, key: String): Future[Boolean] =
      db.run(Keys.filter(k => k.organizationID === organizationID && k.key === key).delete).map(_ > 0)


    ////////////////////
    // Slick mappings //
    ////////////////////

    private val Keys = TableQuery[KeysTable]
    private val Organizations = TableQuery[OrganizationsTable]

    private class KeysTable(tag: Tag) extends Table[Key](tag, "keys") {
      def key = column[String]("key_value", O.PrimaryKey)
      def organizationID = column[Long]("organization_id")
      def * = (key, organizationID) <> (Key.tupled, Key.unapply)
    }

    private class OrganizationsTable(tag: Tag) extends Table[Organization](tag, "organizations") {
      def id = column[Long]("id", O.PrimaryKey)
      def name = column[String]("name")
      def * = (id, name) <> (Organization.tupled, Organization.unapply)
    }
  }
}
