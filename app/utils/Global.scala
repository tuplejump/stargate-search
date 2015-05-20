package utils

import com.datastax.driver.core.{Cluster, QueryOptions, Session}
import java.util.Collections
import play.api.{Application, Configuration, GlobalSettings}
import scala.collection.convert.WrapAsScala._
import play.api.Play

/**
 * User: satya
 * The Global settings where we initialize our Cassandra Session
 */
object Global extends GlobalSettings {

  var session: Session = null

  override def onStart(app: Application): scala.Unit = {
    session = createSession(app.configuration)
  }

  override def onStop(app: Application) = {
    if (session != null) session.close()
  }

  def getSession(): Session = {
    if (session == null) createSession(Play.current.configuration)
    session
  }

  def createSession(conf: Configuration): Session = {
    val configList = conf.getConfigList("cassandra.seed-nodes").getOrElse(Collections.emptyList[Configuration]())
    val builder: Cluster.Builder = new Cluster.Builder
    configList.map {
      node =>
        val host = node.getString("hostname").getOrElse(null)
        val port = node.getInt("port").getOrElse(-1)
        if (host != null && port != -1)
          builder.addContactPoints(host).withPort(port)
    }
    val queryOptions: QueryOptions = new QueryOptions
    //queryOptions.setFetchSize(Integer.MAX_VALUE)
    builder.withQueryOptions(queryOptions)
    builder.build.connect
  }

}
