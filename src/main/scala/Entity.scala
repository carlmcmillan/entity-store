case class Entity(id: String, remoteId1: String, remoteId2: String, active: Boolean)
case class CreateEntity(remoteId1: String, remoteId2: String)
case class UpdateEntity(remoteId1: Option[String], remoteId2: Option[String], active: Option[Boolean])