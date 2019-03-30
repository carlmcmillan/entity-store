import scala.concurrent.Future

trait EntityMocks {

  class FailingRepository extends EntityRepository {
    override def all(): Future[Seq[Entity]] = Future.failed(new Exception("Mocked exception"))

    override def active(): Future[Seq[Entity]] = Future.failed(new Exception("Mocked exception"))

    override def inactive(): Future[Seq[Entity]] = Future.failed(new Exception("Mocked exception"))

    override def findById(id: String): Future[Entity] = Future.failed(new Exception("Mocked exception"))

    override def findByRemoteIds(remoteId1: String, remoteId2: String): Future[Entity] = Future.failed(new Exception("Mocked exception"))

    override def save(createEntity: CreateEntity): Future[Entity] = Future.failed(new Exception("Mocked exception"))

    override def update(id: String, updateEntity: UpdateEntity): Future[Entity] = Future.failed(new Exception("Mocked exception"))
  }

}
