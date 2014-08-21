package magnify.features.view

import java.util.Date

import com.tinkerpop.blueprints.Vertex
import magnify.model.graph.{Actions, FullGraph}

object Revision extends ((Vertex, Boolean) => Map[String, String]) with ElementView with Actions {


  def apply(revision: Option[String], graph: FullGraph): Map[String, String] = {
    apply(graph.revVertex(revision), true)
  }

  override def apply(vrtx: Vertex, fullDetails: Boolean): Map[String, String] = {
    val rev = vrtx.getProperty[String]("rev")
    val desc = vrtx.getProperty[String]("desc")
    val author = getName(vrtx.getProperty[String]("author"))
    val committer = getName(vrtx.getProperty[String]("committer"))
    val time = format.format(new Date(vrtx.getProperty[Integer]("time") * 1000L))
    val additionalData = if (fullDetails) { additionalRevData(vrtx) } else { Map() }
    Map("id" -> rev, "description" -> desc, "author" -> author, "committer" -> committer, "time" -> time) ++
        additionalData
  }
}
