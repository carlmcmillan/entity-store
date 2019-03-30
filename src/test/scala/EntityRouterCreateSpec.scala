import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{Matchers, WordSpec}

class EntityRouterCreateSpec extends WordSpec with Matchers with ScalatestRouteTest with EntityMocks {
  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  var testCreateEntity = CreateEntity(
    "Test entity",
    "Test remoteId2"
  )

  "A EntityRouter" should {
    "create a entity with valid data" in {
      val repository = new InMemoryEntityRepository
      var router = new EntityRouter(repository)

      Post("/entities", testCreateEntity) ~> router.route ~> check {
        status shouldBe StatusCodes.OK
        var resp = responseAs[Entity]
        resp.remoteId1 shouldBe testCreateEntity.remoteId1
        resp.remoteId2 shouldBe testCreateEntity.remoteId2
        resp.active shouldBe false
      }
    }

    "not create a entity with invalid data" in {
      val repository = new FailingRepository
      var router = new EntityRouter(repository)

      Post("/entities", testCreateEntity.copy(remoteId1 = "")) ~> router.route ~> check {
        status shouldBe ApiError.emptyRemoteId1Field.statusCode
        var resp = responseAs[String]
        resp shouldBe ApiError.emptyRemoteId1Field.message
      }
    }

    "handle repository failures when creating entities" in {
      val repository = new FailingRepository
      var router = new EntityRouter(repository)

      Post("/entities", testCreateEntity) ~> router.route ~> check {
        status shouldBe ApiError.generic.statusCode
        var resp = responseAs[String]
        resp shouldBe ApiError.generic.message
      }
    }
  }

}
