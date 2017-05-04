package fi.oph.scalaschema.annotation

import fi.oph.scalaschema.Metadata
import org.json4s.JsonAST.JObject

case class DefaultValue(value: Any) extends Metadata {
  override def appendMetadataToJsonSchema(obj: JObject) = appendToDescription(obj, s"(default value: ${value})")
}

object DefaultValue {
  def getDefaultValue[T](metadata: List[Metadata]): Option[T] = metadata.collect({ case DefaultValue(v) => v}).headOption.asInstanceOf[Option[T]]
}