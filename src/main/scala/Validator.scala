trait Validator[T] {
  def validate(t: T): Option[ApiError]
}

object CreateEntityValidator extends Validator[CreateEntity] {
  override def validate(createEntity: CreateEntity): Option[ApiError] = {
    if (createEntity.remoteId1.isEmpty)
      Some(ApiError.emptyRemoteId1Field)
    else
      None
  }

}

object UpdateEntityValidator extends Validator[UpdateEntity] {
  override def validate(updateEntity: UpdateEntity): Option[ApiError] = {
    if (updateEntity.remoteId1.exists(_.isEmpty))
      Some(ApiError.emptyRemoteId1Field)
    else
      None
  }
}