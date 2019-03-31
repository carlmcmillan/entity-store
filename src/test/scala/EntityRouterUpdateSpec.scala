import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{Matchers, WordSpec}

class EntityRouterUpdateSpec extends WordSpec with Matchers with ScalatestRouteTest with EntityMocks {
  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  val entityId = UUID.randomUUID().toString
  val testEntity = Entity(
    entityId,
    UUID.randomUUID().toString,
    UUID.randomUUID().toString,
    active = false
  )
  val testUpdateEntity = UpdateEntity(
    Some(UUID.randomUUID().toString),
    None,
    Some(true)
  )

  "A EntityRouter" should {

    "update a entity with valid data" in {
      val repository = new InMemoryEntityRepository(Seq(testEntity))
      val router = new EntityRouter(repository)

      Put(s"/entities/$entityId", testUpdateEntity) ~> router.route ~> check {
        status shouldBe StatusCodes.OK
        val resp = responseAs[Entity]
        resp.remoteId1 shouldBe testUpdateEntity.remoteId1.get
        resp.remoteId2 shouldBe testEntity.remoteId2
        resp.active shouldBe testUpdateEntity.active.get
      }
    }

    "return not found with non existent entity" in {
      val repository = new InMemoryEntityRepository(Seq(testEntity))
      val router = new EntityRouter(repository)

      Put("/entities/1", testUpdateEntity) ~> router.route ~> check {
        status shouldBe ApiError.entityNotFound("1").statusCode
        val resp = responseAs[String]
        resp shouldBe ApiError.entityNotFound("1").message
      }
    }

    "not update a entity with invalid data" in {
      val repository = new InMemoryEntityRepository(Seq(testEntity))
      val router = new EntityRouter(repository)

      Put(s"/entities/$entityId", testUpdateEntity.copy(remoteId1 = Some(""))) ~> router.route ~> check {
        status shouldBe ApiError.emptyRemoteId1Field.statusCode
        val resp = responseAs[String]
        resp shouldBe ApiError.emptyRemoteId1Field.message
      }
    }

    "handle repository failures when updating entities" in {
      val repository = new FailingRepository
      val router = new EntityRouter(repository)

      Put(s"/entities/$entityId", testUpdateEntity) ~> router.route ~> check {
        status shouldBe ApiError.generic.statusCode
        val resp = responseAs[String]
        resp shouldBe ApiError.generic.message
      }
    }
  }

}
