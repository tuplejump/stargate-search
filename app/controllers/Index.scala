package controllers

import play.api.mvc.{Action, Controller}
import java.util.UUID
import models.{CL, EntryOptions, Entry}
import utils.Global

/**
 * User: satya
 */
object Index extends Controller {
  val indexService = new services.Index(Global.getSession)

  def add(keyspace: String, table: String) = Action {
    request =>
      val ttlStr = request.getQueryString("ttl").getOrElse(null)
      val ttl: Int = if (ttlStr == null) -1 else Integer.parseInt(ttlStr)
      val cl = request.getQueryString("cl").getOrElse("quorum")
      val timeoutStr = request.getQueryString("timeout").getOrElse(null)
      val timeout: Int = if (timeoutStr == null) -1 else Integer.parseInt(timeoutStr)
      val entryOptions = EntryOptions(ttl, CL.withName(cl), timeout)
      val uuid = UUID.randomUUID()
      val json = request.body.asJson.get
      val entry = Entry(keyspace, table, uuid, entryOptions, json.toString())
      indexService.addEntry(entry)
      Ok("{id:" + uuid + "}")
  }

  def search(keyspace: String, table: String) = Action {
    request =>
      val json = request.body.asJson.get
      val result = indexService.search(keyspace, table, json.toString())
      Ok(result match {
        case Right(errorStr) => "{\n\t\"_error\":\"" + errorStr + "\"\n}"
        case Left(rowList) =>
          val hits = rowList.size
          val res = new StringBuilder
          rowList.foreach {
            scoredEntry =>
              res.append("\t\"_score\":").append(scoredEntry.score).append(",\n").append("\t\"_source\":").append(scoredEntry.source).append("\n\t}")
          }
          "{\n\t\"_hits\":" + hits + ",\n " +
            "\t\"result\":\n[\n\t" + res.toString() + "\n]\n}"
      })
  }

}
