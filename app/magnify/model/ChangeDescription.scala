package magnify.model

import com.tinkerpop.blueprints.{Edge, Element, Vertex}
import magnify.model.graph.FullGraph

final case class ChangeDescription(
    revision: String,
    description: String,
    author: String,
    committer: String,
    time: Int,
    addedFiles: Set[String],
    changedFiles: Set[String],
    removedFiles: Set[String]) {

  def setProperties(element: Element) = {
    element.setProperty("rev", revision)
    element.setProperty("desc", description)
    element.setProperty("author", author)
    element.setProperty("committer", committer)
    element.setProperty("time", time)
  }
}
