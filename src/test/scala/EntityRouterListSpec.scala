import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{Matchers, WordSpec}

class EntityRouterListSpec extends WordSpec with Matchers with ScalatestRouteTest with EntityMocks {
  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  private val activeEntity =
    Entity("1", "Buy milk", "The cat is thirsty!", active = true)
  private val inactiveEntity =
    Entity("2", "Buy eggs", "Ran out of eggs, buy a dozen", active = false)

  private val entities = Seq(activeEntity, inactiveEntity)

  "EntityRouter" should {

    "return all the entities" in {
      val repository = new InMemoryEntityRepository(entities)
      val router = new EntityRouter(repository)

      Get("/entities") ~> router.route ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[Seq[Entity]]
        response shouldBe entities
      }
    }

    "return all the active entities" in {
      val repository = new InMemoryEntityRepository(entities)
      val router = new EntityRouter(repository)

      Get("/entities/active") ~> router.route ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[Seq[Entity]]
        response shouldBe Seq(activeEntity)
      }
    }

    "return all the inactive entities" in {
      val repository = new InMemoryEntityRepository(entities)
      val router = new EntityRouter(repository)

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
