package models

import com.fasterxml.jackson.annotation.{JsonCreator, JsonProperty}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * User: satya
 */
object JsonMapping {

  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

  trait Condition

  case class BooleanCondition(@JsonProperty("boost") boost: Float = 1.0f,
                              @JsonProperty("must") must: List[Condition],
                              @JsonProperty("should") should: List[Condition],
                              @JsonProperty("not") not: List[Condition]) extends Condition

  case class FuzzyCondition(@JsonProperty("boost") val boost: Float = 1.0f,
                            @JsonProperty("field") val field: String,
                            @JsonProperty("value") val value: String,
                            @JsonProperty("maxEdits") val maxEdits: Integer = 2,
                            @JsonProperty("prefixLength") val prefixLength: Integer = 0,
                            @JsonProperty("maxExpansions") val maxExpansions: Integer = 50,
                            @JsonProperty("transpositions") val transpositions: Boolean = true) extends Condition

  case class LuceneCondition(@JsonProperty("boost") boost: Float = 1.0f,
                             @JsonProperty("field") defaultField: String,
                             @JsonProperty("value") query: String) extends Condition

  case class MatchCondition(@JsonProperty("boost") boost: Float = 1.0f,
                            @JsonProperty("field") field: String,
                            @JsonProperty("value") value: AnyRef) extends Condition

  case class PhraseCondition(@JsonProperty("boost") boost: Float = 1.0f,
                             @JsonProperty("field") field: String,
                             @JsonProperty("values") values: List[String],
                             @JsonProperty("slop") slop: Integer = 0) extends Condition

  case class PrefixCondition(@JsonProperty("boost") boost: Float = 1.0f,
                             @JsonProperty("field") field: String,
                             @JsonProperty("value") value: String) extends Condition

  case class RegexpCondition(@JsonProperty("boost") boost: Float = 1.0f,
                             @JsonProperty("field") field: String,
                             @JsonProperty("value") value: String) extends Condition

  case class WildcardCondition(@JsonProperty("boost") boost: Float = 1.0f,
                               @JsonProperty("field") field: String,
                               @JsonProperty("value") value: String) extends Condition


  case class SortField(@JsonProperty("field") field: String, @JsonProperty("reverse") reverse: Boolean)

  case class Sort(@JsonProperty("fields") sortFields: List[SortField])

  case class Search(@JsonProperty("query") queryCondition: Condition,
                    @JsonProperty("filter") filterCondition: Condition,
                    @JsonProperty("sort") sort: Sort)


  val defaultMapping: String = mapper.writeValueAsString(Properties(fields = Map("source" -> Properties(`type` = "object"))))

  @JsonCreator case class Properties(@JsonProperty metaColumn: Boolean = true,
                                     @JsonProperty numShards: Int = 1024,
                                     @JsonProperty routing: Boolean = false,
                                     @JsonProperty vnodes: Boolean = false,
                                     @JsonProperty `type`: String = null,
                                     @JsonProperty analyzer: String = null,
                                     @JsonProperty indexed: Boolean = true,
                                     @JsonProperty stored: Boolean = false,
                                     @JsonProperty tokenized: Boolean = true,
                                     @JsonProperty omitNorms: Boolean = true,
                                     @JsonProperty maxFieldLength: Int = 0,
                                     @JsonProperty storeTermVectors: Boolean = false,
                                     @JsonProperty storeTermVectorOffsets: Boolean = false,
                                     @JsonProperty storeTermVectorPositions: Boolean = false,
                                     @JsonProperty storeTermVectorPayloads: Boolean = false,
                                     @JsonProperty indexOptions: String = "DOCS_ONLY",
                                     @JsonProperty numericPrecisionStep: Int = 4,
                                     @JsonProperty fields: Map[String, Properties] = Map())


}
