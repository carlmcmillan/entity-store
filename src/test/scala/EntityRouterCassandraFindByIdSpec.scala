import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{Matchers, WordSpec}

class EntityRouterCassandraFindByIdSpec extends WordSpec with Matchers with ScalatestRouteTest with EntityMocks {
  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  val entityId1 = UUID.randomUUID().toString
  val testEntity1 = Entity(
    entityId1,
    UUID.randomUUID().toString,
    UUID.randomUUID().toString,
    active = false
  )
  val entityId2 = UUID.randomUUID().toString
  val testEntity2 = Entity(
    entityId2,
    UUID.randomUUID().toString,
    UUID.randomUUID().toString,
    active = true
  )
  val entities = Seq(testEntity1, testEntity2)

  def setup(repository: CassandraEntityRepository): Unit = {
    repository.session.execute("TRUNCATE test.entity")
    repository.session.execute("INSERT INTO test.entity(id, remoteid1, remoteid2, active) VALUES (?, ?, ?, ?)",
      testEntity1.id, testEntity1.remoteId1, testEntity1.remoteId2, Boolean.box(testEntity1.active))
    repository.session.execute("INSERT INTO test.entity(id, remoteid1, remoteid2, active) VALUES (?, ?, ?, ?)",
      testEntity2.id, testEntity2.remoteId1, testEntity2.remoteId2, Boolean.box(testEntity2.active))
  }

  "A EntityRouter" should {

    "find an entity by its id" in {
      val repository = new CassandraEntityRepository()
      val router = new EntityRouter(repository)
      setup(repository)

      Get(s"/entities/$entityId1") ~> router.route ~> check {
        status shouldBe StatusCodes.OK
        val resp = responseAs[Entity]
        resp.remoteId1 shouldBe testEntity1.remoteId1
        resp.remoteId2 shouldBe testEntity1.remoteId2
        resp.active shouldBe testEntity1.active
      }
    }

    "find another entity by its id" in {
      val repository = new CassandraEntityRepository()
      val router = new EntityRouter(repository)
      setup(repository)

      Get(s"/entities/$entityId2") ~> router.route ~> check {
        status shouldBe StatusCodes.OK
        val resp = responseAs[Entity]
        resp.remoteId1 shouldBe testEntity2.remoteId1
        resp.remoteId2 shouldBe testEntity2.remoteId2
        resp.active shouldBe testEntity2.active
      }
    }

    "return not found with non existent entity" in {
      val repository = new CassandraEntityRepository()
      val router = new EntityRouter(repository)
      setup(repository)

      Get("/entities/1") ~> router.route ~> check {
        status shouldBe ApiError.entityNotFound("1").statusCode
        val resp = responseAs[String]
        resp shouldBe ApiError.entityNotFound("1").message
      }
    }

    "handle repository failures when finding entities by id" in {
      val repository = new FailingRepository
      val router = new EntityRouter(repository)

      Get(s"/entities/$entityId1") ~> router.route ~> check {
        status shouldBe ApiError.generic.statusCode
        val resp = responseAs[String]
        resp shouldBe ApiError.generic.message
      }
    }

  }

}
