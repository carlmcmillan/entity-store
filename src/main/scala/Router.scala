import akka.http.scaladsl.server.{Directives, Route}

trait Router {

  def route: Route

}

class EntityRouter(entityRepository: EntityRepository) extends Router with Directives with EntityDirectives with ValidatorDirectives {

  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import io.circe.generic.auto._

  override def route: Route = pathPrefix("entities") {
    pathEndOrSingleSlash {
      get {
        parameters('remoteId1, 'remoteId2) { (remoteId1, remoteId2) =>
          handle(entityRepository.findByRemoteIds(remoteId1, remoteId2)) {
            case EntityRepository.EntityNotFoundForRemoteIds(_, _) =>
              ApiError.entityNotFound(remoteId1, remoteId2)
            case _ =>
              ApiError.generic
          } {
            entity =>
              complete(entity)
          }
        }
      } ~ get {
        handleWithGeneric(entityRepository.all()) { entities =>
          complete(entities)
        }
      } ~ post {
        entity(as[CreateEntity]) { createEntity =>
          validateWith(CreateEntityValidator)(createEntity) {
            handleWithGeneric(entityRepository.save(createEntity)) { entities =>
              complete(entities)
            }
          }
        }
      }
    } ~ path("active") {
      get {
        handleWithGeneric(entityRepository.active()) { entities =>
          complete(entities)
        }
      }
    } ~ path("inactive") {
      get {
        handleWithGeneric(entityRepository.inactive()) { entities =>
          complete(entities)
        }
      }
    } ~ path(Segment) { id: String =>
      put {
        entity(as[UpdateEntity]) { updateEntity =>
          validateWith(UpdateEntityValidator)(updateEntity) {
            handle(entityRepository.update(id, updateEntity)) {
              case EntityRepository.EntityNotFound(_) =>
                ApiError.entityNotFound(id)
              case _ =>
                ApiError.generic
            } {
              entity =>
                complete(entity)
            }
          }
        }
      } ~ get {
        handle(entityRepository.findById(id)) {
          case EntityRepository.EntityNotFound(_) =>
            ApiError.entityNotFound(id)
          case _ =>
            ApiError.generic
        } {
          entity =>
            complete(entity)
        }
      }
    }
  }
}