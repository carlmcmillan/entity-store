import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{Matchers, WordSpec}

class EntityRouterFindByRemoteIdsSpec extends WordSpec with Matchers with ScalatestRouteTest with EntityMocks {
  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  val entityId1 = UUID.randomUUID().toString
  val entity1RemoteId1 = UUID.randomUUID().toString
  val entity1RemoteId2 = UUID.randomUUID().toString
  val testEntity1 = Entity(
    entityId1,
    entity1RemoteId1,
    entity1RemoteId2,
    active = false
  )
  val entityId2 = UUID.randomUUID().toString
  val entity2RemoteId1 = UUID.randomUUID().toString
  val entity2RemoteId2 = UUID.randomUUID().toString
  val testEntity2 = Entity(
    entityId2,
    entity2RemoteId1,
    entity2RemoteId2,
    active = true
  )
  val entityId3 = UUID.randomUUID().toString
  val testEntity3 = Entity(
    entityId3,
    entity1RemoteId1,
    entity2RemoteId2,
    active = true
  )
  val entities = Seq(testEntity1, testEntity2, testEntity3)

  "A EntityRouter" should {

    "find an entity by its remote ids" in {
      val repository = new InMemoryEntityRepository(entities)
      val router = new EntityRouter(repository)

      Get(s"/entities?remoteId1=$entity1RemoteId1&remoteId2=$entity1RemoteId2") ~> router.route ~> check {
        status shouldBe StatusCodes.OK
        val resp = responseAs[Entity]
        resp.remoteId1 shouldBe testEntity1.remoteId1
        resp.remoteId2 shouldBe testEntity1.remoteId2
        resp.active shouldBe testEntity1.active
      }
    }

    "find another entity by its remote ids" in {
      val repository = new InMemoryEntityRepository(entities)
      val router = new EntityRouter(repository)

      Get(s"/entities?remoteId1=$entity2RemoteId1&remoteId2=$entity2RemoteId2") ~> router.route ~> check {
        status shouldBe StatusCodes.OK
        val resp = responseAs[Entity]
        resp.remoteId1 shouldBe testEntity2.remoteId1
        resp.remoteId2 shouldBe testEntity2.remoteId2
        resp.active shouldBe testEntity2.active
      }
    }

    "find another entity by its remote ids with a remote id from each other entities" in {
      val repository = new InMemoryEntityRepository(entities)
      val router = new EntityRouter(repository)

      Get(s"/entities?remoteId1=$entity1RemoteId1&remoteId2=$entity2RemoteId2") ~> router.route ~> check {
        status shouldBe StatusCodes.OK
        val resp = responseAs[Entity]
        resp.remoteId1 shouldBe testEntity3.remoteId1
        resp.remoteId2 shouldBe testEntity3.remoteId2
        resp.active shouldBe testEntity3.active
      }
    }

    "return not found with non existent entity" in {
      val repository = new InMemoryEntityRepository(entities)
      val router = new EntityRouter(repository)

      Get("/entities?remoteId1=1&remoteId2=2") ~> router.route ~> check {
        status shouldBe ApiError.entityNotFound("1", "2").statusCode
        val resp = responseAs[String]
        resp shouldBe ApiError.entityNotFound("1", "2").message
      }
    }

    "return not found using invalid remote id combo" in {
      val repository = new InMemoryEntityRepository(entities)
      val router = new EntityRouter(repository)

      Get(s"/entities?remoteId1=$entity2RemoteId2&remoteId2=$entity1RemoteId1") ~> router.route ~> check {
        status shouldBe ApiError.entityNotFound("1", "2").statusCode
        val resp = responseAs[String]
        resp shouldBe ApiError.entityNotFound(entity2RemoteId2, entity1RemoteId1).message
      }
    }

    "handle repository failures when finding entities by remote ids" in {
      val repository = new FailingRepository
      val router = new EntityRouter(repository)

      Get(s"/entities?remoteId1=$entity1RemoteId1&remoteId2=$entity1RemoteId2") ~> router.route ~> check {
        status shouldBe ApiError.generic.statusCode
        val resp = responseAs[String]
        resp shouldBe ApiError.generic.message
      }
    }

  }

}
