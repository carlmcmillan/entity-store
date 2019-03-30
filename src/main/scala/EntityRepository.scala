import java.util.UUID

import EntityRepository.{EntityNotFound, EntityNotFoundForRemoteIds}

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
