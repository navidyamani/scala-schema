package fi.oph.scalaschema.extraction

import fi.oph.scalaschema._
import fi.oph.scalaschema.annotation.{DefaultValue, OnlyWhen, SerializableOnlyWhen}
import org.json4s.JsonAST.JObject
import org.json4s._

object ObjectExtractor {
  def extractObject(cursor: JsonCursor, cs: ClassSchema, metadata: List[Metadata])(implicit context: ExtractionContext, rootSchema: Schema): Either[List[ValidationError], AnyRef] = {
    cursor.json match {
      case o@JObject(values) =>
        val propertyResults: List[Either[List[ValidationError], Any]] = cs.properties
          .filterNot(_.synthetic)
          .map { property =>
            val propertyValueCursor = cursor.subCursor(o \ property.key, property.key)
            val valuePresent = propertyValueCursor.json match {
              case JNothing => false
              case JNull => false
              case _ => true
            }

            val onlyWhenAnnotations: List[SerializableOnlyWhen] = property.metadata.collect { case o:OnlyWhen if valuePresent => o.serializableForm }

            val extractedPropertyValue = SchemaValidatingExtractor.extract(propertyValueCursor, property.schema, property.metadata)

            val onlyWhenMismatch: Option[OnlyWhenMismatch] = onlyWhenAnnotations match {
              case Nil => None
              case _ =>
                val found = onlyWhenAnnotations.find { case SerializableOnlyWhen(path, expectedValue) =>
                  val valueFromPath: JValue = cursor.navigate(path).json
                  JsonCompare.equals(valueFromPath, expectedValue)
                }
                if (found.isDefined) {
                  None
                } else {
                  DefaultValue.getDefaultValue[Any](property.metadata) match {
                    case Some(v) if Right(v) == extractedPropertyValue => None // Allow default value even when field is otherwise rejected
                    case _ => Some(OnlyWhenMismatch(onlyWhenAnnotations))
                  }
                }
            }

            onlyWhenMismatch match {
              case None =>
                extractedPropertyValue
              case Some(mismatch) =>
                Left(List(ValidationError(propertyValueCursor.path, propertyValueCursor.json, mismatch)))
            }
          }
        val unexpectedProperties = if (context.ignoreUnexpectedProperties) Nil else values
          .filterNot(pair => cs.properties.find(_.key == pair._1).isDefined)
          .filterNot(pair => pair._2 == JNull)
          .map(pair => ValidationError(cursor.subPath(pair._1), pair._2, UnexpectedProperty()))
        val errors: List[ValidationError] = propertyResults.collect { case Left(errors) => errors }.flatten ++ unexpectedProperties
        errors match {
          case Nil =>
            val constructor = Class.forName(cs.fullClassName).getConstructors.apply(0)
            val constructorParams: List[Object] = propertyResults.map(_.right.get).asInstanceOf[List[Object]]
            try {
              Right(constructor.newInstance(constructorParams: _*).asInstanceOf[AnyRef])
            } catch {
              case e: Exception => throw new DeserializationException(cursor.path, s"instantiating ${cs.fullClassName} with ${constructorParams}", e)
            }

          case _ => Left(errors)
        }
      case json => Left(List(ValidationError(cursor.path, json, UnexpectedType("object"))))
    }
  }
}
