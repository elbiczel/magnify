package magnify.features.view

import scala.collection.JavaConversions._

import magnify.model.graph.{HasVertexToFilter, AsVertex, Actions, FullGraph}
import com.tinkerpop.blueprints.Direction

object Committers extends Actions {
  def apply(revision: Option[String], graph: FullGraph): Set[Map[String, String]] = {
    val commits = graph.commitsOlderThan(revision).toSet
    graph
        .vertices
        .has("kind", "author")
        .transform(new AsVertex)
        .filter(new HasVertexToFilter(Direction.OUT, "committed", commits))
        .property("name")
        .dedup()
        .toList
        .toSet
        .map((id: AnyRef) => Map("name" -> id.asInstanceOf[String]))
  }
}
