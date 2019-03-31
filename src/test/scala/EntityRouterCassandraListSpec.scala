import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{Matchers, WordSpec}

class EntityRouterCassandraListSpec extends WordSpec with Matchers with ScalatestRouteTest with EntityMocks {
  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  private val activeEntity =
    Entity(UUID.randomUUID().toString, UUID.randomUUID().toString, UUID.randomUUID().toString, active = true)
  private val inactiveEntity =
    Entity(UUID.randomUUID().toString, UUID.randomUUID().toString, UUID.randomUUID().toString, active = false)

  private val entities = Seq(activeEntity, inactiveEntity)

  def setup(repository: CassandraEntityRepository): Unit = {
    repository.session.execute("TRUNCATE test.entity")
    repository.session.execute("INSERT INTO test.entity(id, remoteid1, remoteid2, active) VALUES (?, ?, ?, ?)",
      activeEntity.id, activeEntity.remoteId1, activeEntity.remoteId2, Boolean.box(activeEntity.active))
    repository.session.execute("INSERT INTO test.entity(id, remoteid1, remoteid2, active) VALUES (?, ?, ?, ?)",
      inactiveEntity.id, inactiveEntity.remoteId1, inactiveEntity.remoteId2, Boolean.box(inactiveEntity.active))
  }

  "EntityRouter" should {

    "return all the entities" in {
      val repository = new CassandraEntityRepository()
      val router = new EntityRouter(repository)
      setup(repository)

      Get("/entities") ~> router.route ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[Seq[Entity]]
        response.size shouldEqual entities.size
        response.toSet shouldBe entities.toSet
      }
    }

    "return all the active entities" in {
      val repository = new CassandraEntityRepository()
      val router = new EntityRouter(repository)
      setup(repository)

      Get("/entities/active") ~> router.route ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[Seq[Entity]]
        response shouldBe Seq(activeEntity)
      }
    }

    "return all the inactive entities" in {
      val repository = new CassandraEntityRepository()
      val router = new EntityRouter(repository)
      setup(repository)

      Get("/entities/inactive") ~> router.route ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[Seq[Entity]]
        response shouldBe Seq(inactiveEntity)
      }
    }

    "handle repository failure in the entities route" in {
      val repository = new FailingRepository
      val router = new EntityRouter(repository)

      Get("/entities") ~> router.route ~> check {
        status shouldBe ApiError.generic.statusCode
        val resp = responseAs[String]
        resp shouldBe ApiError.generic.message
      }
    }

    "handle repository failure in the active entities route" in {
      val repository = new FailingRepository
      val router = new EntityRouter(repository)

      Get("/entities/active") ~> router.route ~> check {
        status shouldBe ApiError.generic.statusCode
        val resp = responseAs[String]
        resp shouldBe ApiError.generic.message
      }
    }

    "handle repository failure in the inactive entities route" in {
      val repository = new FailingRepository
      val router = new EntityRouter(repository)

      Get("/entities/inactive") ~> router.route ~> check {
        status shouldBe ApiError.generic.statusCode
        val resp = responseAs[String]
        resp shouldBe ApiError.generic.message
      }
    }
  }

}
