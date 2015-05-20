package services

import com.datastax.driver.core.{Row, PreparedStatement, Session}
import models.JsonMapping.Properties
import org.apache.commons.lang3.StringUtils
import models.{Entry, JsonMapping}
import scala.collection.mutable.Map
import scala.collection.mutable


/**
 * User: satya
 */

case class ScoredEntry(score: String, source: String)

class Index(session: Session) {
  var indexOptions: Map[String, Map[String, Properties]] = Map()
  var insertStatements: Map[String, Map[String, PreparedStatement]] = Map()
  var selectStatements: Map[String, Map[String, PreparedStatement]] = Map()
  val entryService: IndexEntry = new IndexEntry(session)


  def search(keyspace: String, table: String, query: String): Either[mutable.MutableList[ScoredEntry], String] = {
    if (!exists(keyspace, table)) tryLoadTable(keyspace.toLowerCase, table.toLowerCase)
    if (!exists(keyspace, table)) null
    else {
      val props = indexOptions(keyspace)(table)
      val ps = selectStatements(keyspace.toLowerCase())(table.toLowerCase)
      val rows = entryService.select(ps, query, props.numShards)
      val iter = rows.iterator()
      var errorStr: String = null
      val rowList = mutable.MutableList[ScoredEntry]()
      while (iter.hasNext && errorStr == null) {
        val row = iter.next
        val scoreOrErrorStr = row.getString("stargate")
        val json = JsonMapping.mapper.readValue(scoreOrErrorStr, classOf[Map[String, String]])
        val errorSeq = json.get("error")
        errorSeq match {
          case Some(err) =>
            errorStr = err
          case None =>
            rowList += ScoredEntry(json.get("score").get, json.get("source").get)
        }
      }
      if (errorStr != null) Right(errorStr) else Left(rowList.sortBy(_.score))
    }
  }


  def addEntry(entry: Entry) {
    if (!exists(entry.keyspace, entry.table)) tryLoadTable(entry.keyspace, entry.table)
    if (!exists(entry.keyspace, entry.table)) createTableAndIndex(entry.keyspace, entry.table)
    val ps = insertStatements(entry.keyspace.toLowerCase)(entry.table.toLowerCase)
    entryService.add(ps, entry, indexOptions(entry.keyspace)(entry.table).numShards)
  }


  def createTableAndIndex(keyspace: String, table: String) {
    val tableNameStr: String = tableName(keyspace, table)
    val jsonStr = JsonMapping.defaultMapping

    session.execute("CREATE TABLE " + tableNameStr + " (shard int," +
      "uid uuid, timestamp bigint, source text, stargate text, PRIMARY KEY(shard,uid))")

    session.execute("CREATE CUSTOM INDEX " + table + "_row_index ON " + tableNameStr + "(stargate) " +
      "USING 'com.tuplejump.stargate.RowIndex' WITH options ={'sg_options':'" + jsonStr + "'}")
    tryLoadTable(keyspace, table)
  }


  def tableName(keyspace: String, table: String): String = {
    keyspace.toLowerCase + "." + table.toLowerCase
  }

  def exists(keyspace: String, table: String): Boolean = {
    if (insertStatements.isEmpty) initTablesAndIndexes
    if (insertStatements.contains(keyspace.toLowerCase))
      insertStatements(keyspace.toLowerCase).contains(table.toLowerCase)
    else false
  }

  def tryLoadTable(keyspace: String, table: String) {
    val rs = session.execute("SELECT * FROM SYSTEM.schema_columns" +
      " WHERE keyspace_name='" + keyspace.toLowerCase + "' AND columnfamily_name='" + table.toLowerCase + "'").iterator()
    while (rs.hasNext) {
      val row = rs.next()
      val ks = row.getString("keyspace_name")
      val table = row.getString("columnfamily_name")
      addIndexOptionsEntry(row, ks, table)
    }

  }

  def initTablesAndIndexes() {
    val rs = session.execute("SELECT * FROM SYSTEM.schema_columns").iterator()
    while (rs.hasNext) {
      val row = rs.next()
      val ks = row.getString("keyspace_name")
      val table = row.getString("columnfamily_name")
      addIndexOptionsEntry(row, ks, table)
    }
  }


  def addIndexOptionsEntry(row: Row, keyspace: String, table: String) {
    val indexName = row.getString("index_name")
    val customIndex = "CUSTOM".equalsIgnoreCase(row.getString("index_type"))
    val indexOptionsStr = row.getString("index_options")
    if (indexName != null && customIndex && StringUtils.isNotBlank(indexOptionsStr)) {
      val indexProps = JsonMapping.mapper.readValue(indexOptionsStr, classOf[Map[String, String]])
      val className = indexProps.get("class_name").getOrElse(null)
      if ("com.tuplejump.stargate.RowIndex".equals(className)) {
        val propsStr = indexProps.get("sg_options").get
        //the props should never be null
        assert(propsStr != null)
        val props = JsonMapping.mapper.readValue(propsStr, classOf[Properties])
        if (!indexOptions.contains(keyspace)) {
          indexOptions += (keyspace -> Map())
          insertStatements += (keyspace -> Map())
          selectStatements += (keyspace -> Map())
        }

        //add it against the table as key because a table can have only one RowIndex in our design.
        indexOptions(keyspace) += (table -> props)
        insertStatements(keyspace) += (table -> prepareInsert(keyspace, table))
        selectStatements(keyspace) += (table -> prepareSelect(keyspace, table))
      }
    }
  }

  def prepareInsert(keyspace: String, table: String): PreparedStatement = {
    session.prepare("INSERT INTO " + tableName(keyspace.toLowerCase, table.toLowerCase) + "(shard,uid,timestamp,source) VALUES (?,?,?,?)")
  }

  def prepareSelect(keyspace: String, table: String): PreparedStatement = {
    session.prepare("SELECT * FROM " + tableName(keyspace.toLowerCase, table.toLowerCase) + " where stargate=? AND token(shard) > token(?) AND token(shard) < token(?)")
  }


}
