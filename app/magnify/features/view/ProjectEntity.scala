package magnify.features.view


import com.tinkerpop.blueprints.Vertex
import magnify.model.graph.FullGraph

object ProjectEntity extends ((Vertex, Boolean) => Map[String, String]) with ElementView {
  def apply(revision: Option[String], graph: FullGraph): Map[String, String] = {
    apply(graph.revVertex(revision), true)
  }

  override def apply(vrtx: Vertex, fullDetails: Boolean): Map[String, String] = {
    val name = vrtx.getProperty("name").toString
    val kind = vrtx.getProperty("kind").toString
    val parentName = vrtx.getProperty[String]("parent-pkg-name")
    val additionalData = if (fullDetails) { additionalRevData(vrtx) } else { Map() }
    Map(
      "name" -> name,
      "kind" -> kind,
      "parent-pkg-name" -> parentName) ++ additionalData ++ pageRankMap(vrtx)
  }
}
