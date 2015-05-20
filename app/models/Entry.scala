package models

import com.datastax.driver.core.ConsistencyLevel
import models.CL.CL
import java.util.UUID

/**
 * User: satya
 */
case class Entry(keyspace: String, table: String, id: UUID, options: EntryOptions, json: String)

case class EntryOptions(ttl: Int, consistencyLevel: CL, timeout: Int)


object CL extends Enumeration {
  type CL = Value
  val QUORUM = Value("quorum")
  val ONE = Value("one")
  val ANY = Value("any")
  val ALL = Value("all")

  def getCL(value: CL) = {
    value match {
      case ONE => ConsistencyLevel.ONE
      case ANY => ConsistencyLevel.ANY
      case ALL => ConsistencyLevel.ALL
      case QUORUM => ConsistencyLevel.QUORUM
      case _ => ConsistencyLevel.QUORUM
    }
  }
}