package fi.oph.scalaschema

import fi.oph.scalaschema.SchemaValidatingExtractor.extract
import fi.oph.scalaschema.annotation.ReadFlattened
import fi.oph.scalaschema.extraction.ValidationError
import org.json4s.jackson.JsonMethods

object ValidationExample extends App {
  implicit val context = ExtractionContext(SchemaFactory.default)

  println("*** Successful object extraction ***")
  val validInput = JsonMethods.parse("""{"name": "john", "stuff": [1,2,3]}""")
  val extractionResult: Either[List[ValidationError], ValidationTestClass] = extract[ValidationTestClass](validInput)
  println(extractionResult)
  println("*** Validation failure ***")
  println(SchemaValidatingExtractor.extract[ValidationTestClass]("""{}"""))

}

case class ValidationTestClass(name: String, stuff: List[Int])

object ReadFlattenedExample extends App {
  implicit val context = ExtractionContext(SchemaFactory.default)
  println("All of the below will be successfully extracted as FlattenableCat")
  println(SchemaValidatingExtractor.extract[FlattenableCat](""""john""""))
  println(SchemaValidatingExtractor.extract[FlattenableCat]("""{"name": "john"}"""))
  println(SchemaValidatingExtractor.extract[FlattenableCat]("""{"name": "john", "nickname": "jack"}"""))
}

@ReadFlattened
case class FlattenableCat(name: String, nickname: Option[String])