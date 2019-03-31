import java.util.UUID

import EntityRepository.{EntityNotFound, EntityNotFoundForRemoteIds}
import akka.stream.Materializer
import akka.stream.alpakka.cassandra.scaladsl.CassandraSource
import akka.stream.scaladsl.Sink
import com.datastax.driver.core._

import scala.concurrent.{ExecutionContext, Future}

trait EntityRepository {

  def all(): Future[Seq[Entity]]
  def active(): Future[Seq[Entity]]
  def inactive(): Future[Seq[Entity]]
  def findById(id: String): Future[Entity]
  def findByRemoteIds(remoteId1: String, remoteId2: String): Future[Entity]

  def save(createEntity: CreateEntity): Future[Entity]
  def update(id: String, updateEntity: UpdateEntity): Future[Entity]

}

object EntityRepository {

  final case class EntityNotFound(id: String) extends Exception(s"Entity with id $id not found.")
  final case class EntityNotFoundForRemoteIds(remoteId1: String, remoteId2: String) extends Exception(s"Entity with remoteId1 $remoteId1 and remoteId2 $remoteId2 not found.")
}

class CassandraEntityRepository()(implicit ec: ExecutionContext, implicit val mat: Materializer) extends EntityRepository {

  implicit val session = Cluster.builder
    .addContactPoint("127.0.0.1")
    .withPort(9042)
    .build
    .connect()

  session.execute("CREATE KEYSPACE IF NOT EXISTS test WITH REPLICATION = { 'class' : 'NetworkTopologyStrategy', 'datacenter1' : 1 };")
  session.execute("CREATE TABLE IF NOT EXISTS test.entity ( id text PRIMARY KEY, remoteid1 text, remoteid2 text, active boolean );")

  val stmtAll = new SimpleStatement("SELECT * FROM test.entity")
  val prepStmtFindById = session.prepare("SELECT * FROM test.entity WHERE id = ?")

  override def all(): Future[Seq[Entity]] = {
    val rows = CassandraSource(stmtAll).runWith(Sink.seq)
    rows.map(_.map(row => mapEntity(row)))
  }

  override def active(): Future[Seq[Entity]] = {
    val rows = CassandraSource(stmtAll).runWith(Sink.seq)
    rows.map(_.map(row => mapEntity(row)).filter(e => e.active))
  }

  override def inactive(): Future[Seq[Entity]] = {
    val rows = CassandraSource(stmtAll).runWith(Sink.seq)
    rows.map(_.map(row => mapEntity(row)).filterNot(e => e.active))
  }

  override def findById(id: String): Future[Entity] = {
    val boundStatement = new BoundStatement(prepStmtFindById)
    val rows = session.execute(boundStatement.bind(id))
    val row = rows.one()
    if (row == null) {
      Future.failed(EntityNotFound(id))
    } else {
      Future.successful(mapEntity(row))
    }
  }

  override def findByRemoteIds(remoteId1: String, remoteId2: String): Future[Entity] = ???

  override def save(createEntity: CreateEntity): Future[Entity] = ???

  override def update(id: String, updateEntity: UpdateEntity): Future[Entity] = ???

  def mapEntity(row: Row): Entity = {
    Entity(row.getString("id"), row.getString("remoteid1"), row.getString("remoteid2"), row.getBool("active"))
  }
}

class InMemoryEntityRepository(initalEntity: Seq[Entity] = Seq.empty)(implicit ec: ExecutionContext) extends EntityRepository {

  private var entities: Vector[Entity] = initalEntity.toVector

  override def all(): Future[Seq[Entity]] = Future.successful(entities)

  override def active(): Future[Seq[Entity]] = Future.successful(entities.filter(_.active))

  override def inactive(): Future[Seq[Entity]] = Future.successful(entities.filterNot(_.active))

  override def findById(id: String): Future[Entity] = {
    entities.find(_.id == id) match {
      case Some(foundEntity) =>
        Future.successful(foundEntity)
      case None =>
        Future.failed(EntityNotFound(id))
    }
  }

  override def findByRemoteIds(remoteId1: String, remoteId2: String): Future[Entity] = {
    entities.find(x => x.remoteId1 == remoteId1 && x.remoteId2 == remoteId2) match {
      case Some(foundEntity) =>
        Future.successful(foundEntity)
      case None =>
        Future.failed(EntityNotFoundForRemoteIds(remoteId1, remoteId2))
    }
  }

  override def save(createEntity: CreateEntity): Future[Entity] = Future.successful {
    val entity = Entity(
      UUID.randomUUID().toString,
      createEntity.remoteId1,
      createEntity.remoteId2,
      false
    )

    entities = entities :+ entity
    entity
  }

  override def update(id: String, updateEntity: UpdateEntity): Future[Entity] = {
    entities.find(_.id == id) match {
      case Some(foundEntity) =>
        val newEntity = updateHelper(foundEntity, updateEntity)
        entities = entities.map(t => if (t.id == id) newEntity else t)
        Future.successful(newEntity)
      case None =>
        Future.failed(EntityNotFound(id))
    }
  }

  private def updateHelper(entity: Entity, updateEntity: UpdateEntity): Entity = {
    val t1 = updateEntity.remoteId1.map(remoteId1 => entity.copy(remoteId1 = remoteId1)).getOrElse(entity)
    val t2 = updateEntity.remoteId2.map(remoteId2 => entity.copy(remoteId2 = remoteId2)).getOrElse(t1)
    updateEntity.active.map(active => t2.copy(active = active)).getOrElse(t2)
  }

}
