import akka.http.scaladsl.model.{StatusCode, StatusCodes}

final case class ApiError private(statusCode: StatusCode, message: String)

object ApiError {

  private def apply(statusCode: StatusCode, message: String): ApiError = new ApiError(statusCode, message)

  val generic: ApiError = new ApiError(StatusCodes.InternalServerError, "Unknown error.")

  val emptyRemoteId1Field: ApiError = new ApiError(StatusCodes.BadRequest, "The remoteId1 field must not be empty.")

  def entityNotFound(id: String): ApiError =
    new ApiError(StatusCodes.NotFound, s"The Entity with id $id could not be found.")

  def entityNotFound(remoteId1: String, remoteId2: String): ApiError =
    new ApiError(StatusCodes.NotFound, s"The Entity with remoteId1 $remoteId1 and remoteId2 $remoteId2 could not be found.")

}