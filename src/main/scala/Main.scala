import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.concurrent.Await
import scala.util.{Failure, Success}

object Main extends App {

  val host = "0.0.0.0"
  val port = 9000

  implicit val system: ActorSystem = ActorSystem("entityapi")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import system.dispatcher

  val entityRepository = new InMemoryEntityRepository(Seq(
    Entity(UUID.randomUUID().toString, UUID.randomUUID().toString, UUID.randomUUID().toString, false),
    Entity(UUID.randomUUID().toString, UUID.randomUUID().toString, UUID.randomUUID().toString, true)
  ))
  val router = new EntityRouter(entityRepository)
  val server = new Server(router, host, port)

  val binding = server.bind()
  binding.onComplete {
    case Success(_) => println("Success!")
    case Failure(error) => println(s"Failed: ${error.getMessage}")
  }

  import scala.concurrent.duration._
  Await.result(binding, 3.seconds)

}
